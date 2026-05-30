package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.p3pp3rf1y.sophisticatedcore.util.MathHelper.intMaxCappedAddition;
import static net.p3pp3rf1y.sophisticatedcore.util.MathHelper.intMaxCappedMultiply;

public class CompressionInventoryPart implements IInventoryPartHandler {
	public static final String NAME = "compression";
	public static final ResourceLocation EMPTY_COMPRESSION_SLOT = SophisticatedStorage.getRL("container/slot/compression");
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final Supplier<MemorySettingsCategory> getMemorySettings;
	@SuppressWarnings("FieldCanBeLocal")
	//need field instead of local variable because it's wrapped in WeakReference in RecipeHelper
	private final Runnable recipeChangeListener = () -> calculateStacks(false);

	private Map<Integer, SlotDefinition> slotDefinitions = new HashMap<>();
	private final Map<Integer, ItemStack> calculatedStacks = new HashMap<>();
	private final Map<Integer, Integer> lastCalculatedCounts = new HashMap<>();
	private boolean reconcilingStacks = false;

	public CompressionInventoryPart(InventoryHandler parent, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.parent = parent;
		this.slotRange = slotRange;
		this.getMemorySettings = getMemorySettings;

		RecipeHelper.addRecipeChangeListener(recipeChangeListener);
	}

	@Override
	public void onInit() {
		calculateStacks(true);
	}

	private void calculateStacks(boolean initial) {
		reconcilingStacks = true;
		clearCollections();
		Map<Integer, ItemStack> existingStacks = getExistingStacks();

		if (existingStacks.isEmpty()) {
			return;
		}

		int lastNonEmptySlot = getLastNonEmptySlot(existingStacks);
		setSlotDefinitions(getSlotDefinitions(existingStacks.get(lastNonEmptySlot), lastNonEmptySlot, existingStacks), initial);

		compactInternalSlots();
		updateCalculatedStacks();

		slotDefinitions.forEach((slot, definition) -> parent.triggerOnChangeListeners(slot));
		reconcilingStacks = false;
	}

	private void setSlotDefinitions(Map<Integer, SlotDefinition> definitions, boolean initial) {
		slotDefinitions = definitions;
		if (initial) {
			parent.initFilterItems();
		} else {
			parent.onFilterItemsChanged();
		}
	}

	private Integer getLastNonEmptySlot(Map<Integer, ItemStack> existingStacks) {
		for (int slot = slotRange.firstSlot() + slotRange.numberOfSlots() - 1; slot >= slotRange.firstSlot(); slot--) {
			if (existingStacks.containsKey(slot)) {
				return slot;
			}
		}

		return -1;
	}

	private Map<Integer, SlotDefinition> getSlotDefinitions(ItemStack firstItem, int lastSlot, Map<Integer, ItemStack> existingStacks) {
		Map<Integer, SlotDefinition> ret = new HashMap<>();
		addPreviousItems(ret, lastSlot, firstItem);

		ItemStack prevItem = firstItem;
		for (int slot = lastSlot; slot >= slotRange.firstSlot(); slot--) {
			if (existingStacks.containsKey(slot) && !ItemStack.isSameItemSameComponents(existingStacks.get(slot), prevItem)) {
				ret.clear(); //clearing any compressible definition added before as the compression should no longer compress if there are incompatible items present
				break;
			} else {
				Optional<RecipeHelper.CompactingShape> compressionShape = getCompressionShape(prevItem);
				if (compressionShape.isPresent()) {
					RecipeHelper.CompactingShape shape = compressionShape.get();
					ret.put(slot, new SlotDefinition(prevItem, shape.getNumberOfIngredients(), true));
					prevItem = RecipeHelper.getCompactingResult(prevItem, shape).getResult();
				} else {
					ret.put(slot, new SlotDefinition(prevItem, 1, true));
					break;
				}
			}
		}

		updateSlotLimits(ret);
		updateInaccessibleAndCompressible(ret, existingStacks);

		return ret;
	}

	private void updateSlotLimits(Map<Integer, SlotDefinition> definitions) {
		int totalLimit = 0;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			if (definitions.containsKey(slot) && definitions.get(slot).isAccessible()) {
				totalLimit = intMaxCappedAddition(parent.getBaseStackLimit(definitions.get(slot).item), intMaxCappedMultiply(definitions.get(slot).prevSlotMultiplier, totalLimit));

				definitions.get(slot).setSlotLimit(totalLimit);
			}
		}
	}

	private void updateCalculatedStacks() {
		int totalCalculated = 0;
		boolean prevFull = false;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			if (!slotDefinition.isAccessible()) {
				continue;
			}
			if (!slotDefinition.isCompressible()) {
				setCalculatedStack(slot, parent.getSlotStack(slot).copy());
				continue;
			}
			int internalCount = parent.getSlotStack(slot).getCount();
			totalCalculated = Integer.MAX_VALUE / slotDefinition.prevSlotMultiplier() < totalCalculated ? Integer.MAX_VALUE : totalCalculated * slotDefinition.prevSlotMultiplier();
			totalCalculated = Integer.MAX_VALUE - internalCount < totalCalculated ? Integer.MAX_VALUE : totalCalculated + internalCount;

			ItemStack calculatedStack = slotDefinition.item().copyWithCount(totalCalculated);

			int internalLimit = parent.getBaseStackLimit(calculatedStack);
			int maxStackSize = calculatedStack.getMaxStackSize();
			if (Integer.MAX_VALUE - totalCalculated < maxStackSize) {
				calculatedStack.setCount(Integer.MAX_VALUE - (prevFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize));
			}
			setCalculatedStack(slot, calculatedStack);

			prevFull = internalLimit <= internalCount;
		}
	}

	private void setCalculatedStack(int slot, ItemStack stack) {
		calculatedStacks.put(slot, stack);
		lastCalculatedCounts.put(slot, stack.getCount());
	}

	private void compactInternalSlots() {
		Map<Integer, Integer> toUpdate = new HashMap<>();

		for (int slot = slotRange.firstSlot() + 1; slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			ItemStack slotStack = parent.getSlotStack(slot);
			int multiplier = getPrevSlotMultiplier(slot);
			if (slotStack.isEmpty() || multiplier < 2) {
				continue;
			}
			int prevSlot = slot - 1;
			ItemStack prevStack = parent.getSlotStack(prevSlot);
			int stackLimit = parent.getBaseStackLimit(prevStack);
			int prevStackCount = toUpdate.containsKey(prevSlot) ? toUpdate.get(prevSlot) : prevStack.getCount();
			int availableSpace = stackLimit - prevStackCount;
			int countToInsert = Math.min(availableSpace, slotStack.getCount() / multiplier);
			if (countToInsert > 0) {
				toUpdate.put(prevSlot, prevStackCount + countToInsert);
				toUpdate.put(slot, slotStack.getCount() - countToInsert * multiplier);
			}
		}

		updateInternalStacksWithCounts(toUpdate);
	}

	private void updateInaccessibleAndCompressible(Map<Integer, SlotDefinition> definitions, Map<Integer, ItemStack> existingStacks) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			definitions.computeIfAbsent(slot, s -> {
				if (existingStacks.containsKey(s)) {
					return new SlotDefinition(existingStacks.get(s), 1, true);
				}
				return SlotDefinition.inaccesible();
			});
			if (!definitions.get(slot).isAccessible()) {
				continue;
			}
			boolean uncompressibledFromNext = definitions.containsKey(slot - 1) && definitions.get(slot - 1).isAccessible() && definitions.get(slot).prevSlotMultiplier() > 1;
			boolean compressibleFromPrevious = definitions.containsKey(slot + 1) && definitions.get(slot + 1).isAccessible() && definitions.get(slot + 1).prevSlotMultiplier() > 1;
			definitions.get(slot).setCompressible(uncompressibledFromNext || compressibleFromPrevious);
		}
	}

	private void clearCollections() {
		slotDefinitions.clear();
		calculatedStacks.clear();
		parent.onFilterItemsChanged();
	}

	private Optional<RecipeHelper.CompactingShape> getCompressionShape(ItemStack stack) {
		Set<RecipeHelper.CompactingShape> compactingShapes = RecipeHelper.getItemCompactingShapes(stack);

		if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)) {
			return Optional.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE);
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE)) {
			return Optional.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE);
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE)) {
			Item compressedItem = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.THREE_BY_THREE).getResult().getItem();
			return getDecompressionResultFromConfig(compressedItem).isPresent() ? Optional.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE) : Optional.empty();
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO)) {
			Item compressedItem = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.TWO_BY_TWO).getResult().getItem();
			return getDecompressionResultFromConfig(compressedItem).isPresent() ? Optional.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE) : Optional.empty();
		}
		return Optional.empty();
	}


	private void addPreviousItems(Map<Integer, SlotDefinition> slotDefinitions, int firstFilledSlot, ItemStack firstFilledItem) {
		ItemStack currentItem = firstFilledItem;
		for (int slot = firstFilledSlot + 1; slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			RecipeHelper.UncompactingResult uncompactingResult = RecipeHelper.getUncompactingResult(currentItem);
			if (uncompactingResult.getCompactUsingShape() == RecipeHelper.CompactingShape.NONE) {
				Optional<RecipeHelper.UncompactingResult> decompressionResult = getDecompressionResultFromConfig(currentItem.getItem());
				if (decompressionResult.isEmpty()) {
					break;
				}
				uncompactingResult = decompressionResult.get();
			}
			slotDefinitions.put(slot, new SlotDefinition(uncompactingResult.getResult(), uncompactingResult.getCompactUsingShape() == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE ? 4 : 9, true));
			currentItem = uncompactingResult.getResult();
		}
	}

	Optional<RecipeHelper.UncompactingResult> getDecompressionResultFromConfig(Item currentItem) {
		return Config.SERVER.compressionUpgrade.getDecompressionResult(currentItem);
	}

	private Map<Integer, ItemStack> getExistingStacks() {
		Map<Integer, ItemStack> existingStacks = new LinkedHashMap<>();
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			ItemStack slotStack = parent.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				existingStacks.put(slot, slotStack);
			}
		}

		if (existingStacks.isEmpty()) {
			MemorySettingsCategory memorySettings = getMemorySettings.get();
			for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
				int finalSlot = slot;
				memorySettings.getSlotFilterStack(slot, true).ifPresent(stack -> existingStacks.put(finalSlot, stack));
			}
		}

		return existingStacks;
	}

	@Override
	public int getSlotLimit(int slot) {
		return slotDefinitions.containsKey(slot) ? slotDefinitions.get(slot).slotLimit() : parent.getBaseSlotLimit();
	}

	@Override
	public int getStackLimit(int slot, ItemStack stack) {
		if (!slotDefinitions.containsKey(slot)) {
			return parent.getBaseStackLimit(stack);
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return getStackLimit(slotDefinition);
	}

	private int getStackLimit(SlotDefinition slotDefinition) {
		if (!slotDefinition.isAccessible()) {
			return 0;
		}

		return slotDefinition.slotLimit();
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return extractItem(slot, amount, simulate, s -> s.isEmpty() ? 64 : s.getMaxStackSize());
	}

	private ItemStack extractItem(int slot, int amount, boolean simulate, ToIntFunction<ItemStack> getLimit) {
		if (!slotDefinitions.containsKey(slot) || !slotDefinitions.get(slot).isAccessible() || !calculatedStacks.containsKey(slot)) {
			return ItemStack.EMPTY;
		}
		int toExtract = Math.min(calculatedStacks.get(slot).getCount(), amount);

		if (toExtract > 0) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			ItemStack slotStack = parent.getSlotStack(slot);
			toExtract = Math.min(toExtract, getLimit.applyAsInt(slotStack));
			ItemStack result = slotDefinition.isCompressible() ? slotDefinition.item().copyWithCount(toExtract) : slotStack.copyWithCount(toExtract);

			if (!simulate) {
				if (slotDefinition.isCompressible()) {
					extractFromCalculated(slot, toExtract);
					extractFromInternal(slot, toExtract);
				} else {
					slotStack.shrink(toExtract);
					setCalculatedStack(slot, slotStack.copy());
					parent.setSlotStack(slot, slotStack);
				}
				removeDefinitionsIfEmpty(slot);
			}

			return result;
		}

		return ItemStack.EMPTY;
	}

	private void removeDefinitionsIfEmpty(int slotTriggeringChange) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			if (!parent.getSlotStack(slot).isEmpty() || getMemorySettings.get().getSlotFilterStack(slot, false).isPresent()) {
				return;
			}
		}

		clearCollections();
		parent.triggerOnChangeListeners(slotTriggeringChange);
	}

	private void extractFromInternal(int slotToStartFrom, int amountToExtract) {
		Map<Integer, Integer> toUpdate = new HashMap<>();
		int decompressedAmountToInsert = 0;
		int totalMultiplier = 1;
		while (amountToExtract > 0) {
			ItemStack slotStack = parent.getSlotStack(slotToStartFrom);
			if (totalMultiplier == 1) {
				int toRemove = Math.min(amountToExtract, slotStack.getCount());
				toUpdate.put(slotToStartFrom, slotStack.getCount() - toRemove);
				amountToExtract -= toRemove;
			} else {
				int ceiledAmount = (int) Math.ceil((double) amountToExtract / totalMultiplier);
				int toRemove = Math.min(ceiledAmount, slotStack.getCount());
				toUpdate.put(slotToStartFrom, slotStack.getCount() - toRemove);

				int totalToRemove = toRemove * totalMultiplier;
				if (totalToRemove > amountToExtract) {
					decompressedAmountToInsert = totalToRemove - amountToExtract;
					break;
				}
				amountToExtract -= totalToRemove;
			}

			totalMultiplier *= getPrevSlotMultiplier(slotToStartFrom);
			slotToStartFrom--;
		}

		while (decompressedAmountToInsert > 0) {
			slotToStartFrom++;
			totalMultiplier /= getPrevSlotMultiplier(slotToStartFrom);

			int toInsert = decompressedAmountToInsert / totalMultiplier;
			if (toInsert > 0) {
				toUpdate.put(slotToStartFrom, toUpdate.getOrDefault(slotToStartFrom, 0) + toInsert);
				decompressedAmountToInsert -= toInsert * totalMultiplier;
			}
		}

		updateInternalStacksWithCounts(toUpdate);
	}

	private int getPrevSlotMultiplier(int slot) {
		return slotDefinitions.get(slot).prevSlotMultiplier;
	}

	private void updateInternalStacksWithCounts(Map<Integer, Integer> toUpdate) {
		toUpdate.forEach((s, count) -> {
			ItemStack slotStack = parent.getSlotStack(s);
			if (slotStack.getCount() != count) {
				if (count == 0) {
					parent.setSlotStack(s, ItemStack.EMPTY);
				} else if (slotStack.isEmpty()) {
					parent.setSlotStack(s, slotDefinitions.get(s).item().copyWithCount(count));
				} else {
					slotStack.setCount(count);
					parent.setSlotStack(s, slotStack);
				}
			}
		});
	}

	private void extractFromCalculated(int slot, int extractCount) {
		extractFromCalculatedThisAndPreviousStacks(extractCount, slot);
		extractFromCalculatedThisAndStacksAfter(extractCount, slot + 1);
	}

	private void extractFromCalculatedThisAndPreviousStacks(int extractCount, int slotCalculated) {
		int countBeforeChange = -1;
		int multiplier = 1;
		while (extractCount != 0 && calculatedStacks.containsKey(slotCalculated)) {
			ItemStack calculatedStack = calculatedStacks.get(slotCalculated);

			if (countBeforeChange > 0 && countBeforeChange / multiplier > calculatedStack.getCount()) {
				extractCount = calculatedStack.getCount() - (countBeforeChange - extractCount * multiplier) / multiplier;
				if (extractCount <= 0) {
					break;
				}
			}

			countBeforeChange = calculatedStack.getCount();
			int toSet = getCountChangeLeavingSpaceBeforeMaxInt(countBeforeChange - extractCount, slotCalculated, calculatedStack);
			calculatedStack.setCount(toSet);

			setCalculatedStack(slotCalculated, calculatedStack);

			multiplier = getPrevSlotMultiplier(slotCalculated);
			extractCount = countBeforeChange / multiplier - calculatedStack.getCount() / multiplier;

			slotCalculated--;
		}
	}

	private int getCountChangeLeavingSpaceBeforeMaxInt(int countCalculated, int slotCalculated, ItemStack calculatedStack) {
		int toSet = countCalculated;
		int prevSlot = slotCalculated - 1;
		SlotDefinition prevSlotDefinition = slotDefinitions.get(prevSlot);
		boolean hasPrevious = prevSlotDefinition != null && prevSlotDefinition.isAccessible();
		if (countCalculated > 0 && Integer.MAX_VALUE - countCalculated < calculatedStack.getMaxStackSize() && hasPrevious) {
			boolean prevSlotFull = calculatedStacks.containsKey(prevSlot) && getSlotLimit(prevSlot) == calculatedStacks.get(prevSlot).getCount();
			int buffer = prevSlotFull ? getStackLimit(slotCalculated, calculatedStack) - countCalculated : calculatedStack.getMaxStackSize();
			toSet = Integer.MAX_VALUE - buffer;
		}
		return toSet;
	}

	private void extractFromCalculatedThisAndStacksAfter(int extractCount, int slot) {
		while (slot < slotRange.firstSlot() + slotRange.numberOfSlots() && slotDefinitions.get(slot).isAccessible() && calculatedStacks.containsKey(slot)) {
			ItemStack calculatedStack = calculatedStacks.get(slot);
			int multiplier = getPrevSlotMultiplier(slot);
			extractCount *= multiplier;
			int countSet = calculatedStack.getCount() - extractCount;
			countSet = getCountChangeLeavingSpaceBeforeMaxInt(countSet, slot, calculatedStack);
			calculatedStack.setCount(countSet);
			setCalculatedStack(slot, calculatedStack);
			slot++;
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, TriFunction<Integer, ItemStack, Boolean, ItemStack> insertSuper) {
		return insertItem(slot, stack, simulate);
	}

	private ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (canNotBeInserted(slot, stack)) {
			return stack;
		}

		Map<Integer, SlotDefinition> definitions = slotDefinitions;

		if (definitions.isEmpty()) {
			definitions = getSlotDefinitions(stack, slot, Map.of());
		}

		int limit = getStackLimit(definitions.get(slot));

		int currentCalculatedCount = calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0;
		int inserted = Math.min(Math.max(parent.getBaseStackLimit(stack) - parent.getSlotStack(slot).getCount(), limit - currentCalculatedCount), stack.getCount());

		if (inserted == 0) {
			return stack;
		}

		ItemStack result = stack.copyWithCount(stack.getCount() - inserted);

		if (simulate) {
			return result;
		}

		if (!slotDefinitions.containsKey(slot)) {
			setSlotDefinitions(definitions, false);
			compactInternalSlots();
			updateCalculatedStacks();

			slotDefinitions.forEach((s, definition) -> parent.triggerOnChangeListeners(s));
		}

		if (slotDefinitions.get(slot).isCompressible()) {
			insertIntoInternalAndCalculated(slot, inserted);
		} else if (inserted > 0) {
			calculatedStacks.compute(slot, (s, st) -> {
				if (st == null || st.isEmpty()) {
					ItemStack copy = stack.copy();
					copy.setCount(inserted);
					return copy;
				}
				st.grow(inserted);
				return st;
			});
			lastCalculatedCounts.put(slot, calculatedStacks.get(slot).getCount());
			ItemStack slotStack = parent.getSlotStack(slot);
			if (slotStack.isEmpty()) {
				ItemStack copy = stack.copy();
				copy.setCount(inserted);
				parent.setSlotStack(slot, copy);
			} else {
				slotStack.grow(inserted);
				parent.setSlotStack(slot, slotStack);
			}
		}

		return result;
	}

	private boolean canNotBeInserted(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}

		if (!slotDefinitions.containsKey(slot)) {
			return false;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return !slotDefinition.isAccessible() || !ItemStack.isSameItemSameComponents(slotDefinition.item(), stack);
	}

	private void insertIntoInternalAndCalculated(int slotToStartFrom, long amountToInsert) {
		Map<Integer, Integer> toUpdate = new LinkedHashMap<>();
		Map<Integer, Integer> calculatedAdditions = new LinkedHashMap<>();
		int totalMultiplier = 1;
		int slot = slotToStartFrom;

		long amountToSet = amountToInsert + parent.getSlotStack(slot).getCount();

		while (amountToSet / ((long) totalMultiplier * getPrevSlotMultiplier(slot)) > 0 && slotDefinitions.containsKey(slot - 1) && slotDefinitions.get(slot - 1).isAccessible()) {
			totalMultiplier *= getPrevSlotMultiplier(slot);
			slot--;
			amountToSet += (long) parent.getSlotStack(slot).getCount() * totalMultiplier;
		}

		long calculatedAddition = 0;
		while (slot <= slotToStartFrom) {
			calculatedAddition *= getPrevSlotMultiplier(slot);
			ItemStack slotStack = parent.getSlotStack(slot);
			int toSet = (int) Math.min(amountToSet / totalMultiplier, parent.getBaseStackLimit(slotStack));
			calculatedAddition += (toSet - slotStack.getCount());
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));
			if (toSet > 0) {
				toUpdate.put(slot, toSet);
				amountToSet -= (long) toSet * totalMultiplier;
			} else {
				toUpdate.put(slot, 0);
			}

			if (amountToSet != 0) {
				if (!slotDefinitions.containsKey(slot + 1) || !slotDefinitions.get(slot + 1).isAccessible()) {
					SophisticatedStorage.LOGGER.error("Compression inventory is in an invalid state. Slot {} has a prevSlotMultiplier of 0 (likely because it's inaccessible), but there's remaining count of {} to insert.\nSlot Definitions\n{}", slot + 1, amountToSet, slotDefinitions);
					break;
				}
				totalMultiplier /= getPrevSlotMultiplier(slot + 1);
			}
			slot++;
		}

		//finish calculation of calculated addition to the follow up slots even though they are not getting their internal stack changed
		while (slot < slotRange.firstSlot() + slotRange.numberOfSlots()) {
			if (!slotDefinitions.containsKey(slot)) {
				break;
			}

			calculatedAddition *= getPrevSlotMultiplier(slot);
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));

			slot++;
		}

		updateInternalStacksWithCounts(toUpdate);

		calculatedAdditions.forEach((s, countToAdd) -> {
			addToCalculatedStack(s, countToAdd);
			lastCalculatedCounts.put(s, calculatedStacks.get(s).getCount());
		});
		toUpdate.keySet().forEach(parent::triggerOnChangeListeners);
	}

	private void addToCalculatedStack(int slot, int countToAdd) {
		if (!calculatedStacks.containsKey(slot) || calculatedStacks.get(slot).isEmpty()) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			setCalculatedStack(slot, slotDefinition.item().copyWithCount(countToAdd));
			return;
		}
		ItemStack currentCalculated = calculatedStacks.get(slot);

		int totalCalculated = Integer.MAX_VALUE - countToAdd < currentCalculated.getCount() ? Integer.MAX_VALUE : currentCalculated.getCount() + countToAdd;

		int previousSlot = slot - 1;
		if (totalCalculated != Integer.MAX_VALUE || !slotDefinitions.containsKey(previousSlot)) {
			currentCalculated.setCount(totalCalculated);
			return;
		}

		ItemStack previousInternalStack = parent.getSlotStack(previousSlot);
		boolean isPreviousFull = previousInternalStack.getCount() >= parent.getBaseStackLimit(previousInternalStack);

		int internalLimit = parent.getBaseStackLimit(currentCalculated);
		int internalCount = parent.getSlotStack(slot).getCount();

		int maxStackSize = previousInternalStack.getMaxStackSize();
		int spaceBeforeMaxInt = isPreviousFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize;
		currentCalculated.setCount(Integer.MAX_VALUE - spaceBeforeMaxInt);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		if (!stack.isEmpty() && canNotBeInserted(slot, stack)) {
			return;
		}

		int currentCount = lastCalculatedCounts.getOrDefault(slot, 0);

		// go back to last known count if the stack was only changed externally using something like split / shrink / grow
		if (currentCount != (calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0)) {
			setCalculatedStack(slot, slotDefinitions.get(slot).item().copyWithCount(currentCount));
		}

		int newCount = stack.getCount();
		if (currentCount < newCount) {
			insertItem(slot, stack.copyWithCount(newCount - currentCount), false);
		} else if (currentCount > newCount) {
			extractItem(slot, currentCount - newCount, false, s -> Integer.MAX_VALUE);
		}
	}

	@Override
	public void onContentsChanged(int slot, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		if (reconcilingStacks || !slotDefinitions.containsKey(slot)) {
			return; //prevent infinite loop when updating calculated stacks
		}
		reconcilingStacks = true;
		setStackInSlot(slot, calculatedStacks.getOrDefault(slot, ItemStack.EMPTY), setStackInSlotSuper);
		reconcilingStacks = false;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack, @Nullable Player player, BiPredicate<Integer, ItemStack> isItemValidSuper) {
		if (!slotDefinitions.containsKey(slot)) {
			return true;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return slotDefinition.isAccessible() && ItemStack.isSameItemSameComponents(slotDefinition.item(), stack);
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return slotDefinitions.containsKey(slot) && slotDefinitions.get(slot).isAccessible() && calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return !slotDefinitions.containsKey(slot) || slotDefinitions.get(slot).isAccessible();
	}

	@Override
	public boolean shouldRenderInaccessibleSlotOverlay(int slot) {
		return false;
	}

	@Override
	public int getSlots() {
		return slotRange.numberOfSlots();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Nullable
	@Override
	public ResourceLocation getNoItemIcon(int slot) {
		return EMPTY_COMPRESSION_SLOT;
	}

	@Override
	public Item getFilterItem(int slot) {
		return slotDefinitions.containsKey(slot) ? slotDefinitions.get(slot).item().getItem() : Items.AIR;
	}

	@Override
	public void onSlotLimitChange() {
		updateSlotLimits(slotDefinitions);
	}

	@Override
	public Set<Integer> getNoSortSlots() {
		return IntStream.rangeClosed(slotRange.firstSlot(), slotRange.firstSlot() + slotRange.numberOfSlots() - 1).boxed().collect(Collectors.toSet());
	}

	@Override
	public void onSlotFilterChanged(int slot) {
		calculateStacks(false);
	}

	@Override
	public boolean isFilterItem(Item item) {
		for (SlotDefinition slotDefinition : slotDefinitions.values()) {
			if (slotDefinition.item().getItem() == item) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<Item, Set<Integer>> getFilterItems() {
		Map<Item, Set<Integer>> filterItems = new HashMap<>();
		for (Map.Entry<Integer, SlotDefinition> entry : slotDefinitions.entrySet()) {
			SlotDefinition slotDefinition = entry.getValue();
			if (slotDefinition.isAccessible()) {
				filterItems.computeIfAbsent(slotDefinition.item().getItem(), k -> new HashSet<>()).add(entry.getKey());
			}
		}
		return filterItems;
	}

	private static final class SlotDefinition {
		private final ItemStack item;
		private final int prevSlotMultiplier;
		private int slotLimit;
		private final boolean isAccessible;

		private boolean isCompressible = false;

		private SlotDefinition(ItemStack item, int prevSlotMultiplier, int slotLimit, boolean isAccessible) {
			this.item = item.copyWithCount(1);
			this.prevSlotMultiplier = prevSlotMultiplier;
			this.slotLimit = slotLimit;
			this.isAccessible = isAccessible;
		}

		public static SlotDefinition inaccesible() {
			return new SlotDefinition(ItemStack.EMPTY, 0, 0, false);
		}

		public SlotDefinition(ItemStack stack, int prevSlotMultiplier, boolean isAccessible) {
			this(stack, prevSlotMultiplier, -1, isAccessible);
		}

		public void setSlotLimit(int slotLimit) {
			this.slotLimit = slotLimit;
		}

		public void setCompressible(boolean compressible) {
			isCompressible = compressible;
		}

		public ItemStack item() {
			return item;
		}

		public int prevSlotMultiplier() {
			return prevSlotMultiplier;
		}

		public int slotLimit() {
			return slotLimit;
		}

		public boolean isAccessible() {
			return isAccessible;
		}

		public boolean isCompressible() {
			return isCompressible;
		}

		@Override
		public String toString() {
			return "SlotDefinition{" +
					"item=" + item +
					", prevSlotMultiplier=" + prevSlotMultiplier +
					", slotLimit=" + slotLimit +
					", isAccessible=" + isAccessible +
					", isCompressible=" + isCompressible +
					'}';
		}
	}
}
