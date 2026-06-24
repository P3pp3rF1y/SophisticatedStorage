package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IResourceExtractor;
import net.p3pp3rf1y.sophisticatedcore.inventory.IResourceInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.p3pp3rf1y.sophisticatedcore.util.MathHelper.intMaxCappedAddition;
import static net.p3pp3rf1y.sophisticatedcore.util.MathHelper.intMaxCappedMultiply;

public class CompressionInventoryPart implements IInventoryPartHandler {
	public static final String NAME = "compression";
	public static final Identifier EMPTY_COMPRESSION_SLOT = SophisticatedStorage.getIdentifier("container/slot/compression");
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final Supplier<MemorySettingsCategory> getMemorySettings;
	@SuppressWarnings("FieldCanBeLocal")
	// need field instead of local variable because it's wrapped in WeakReference in RecipeHelper
	private final Runnable recipeChangeListener = () -> calculateStacks(false);

	private Map<Integer, SlotDefinition> slotDefinitions = new HashMap<>();
	private Map<Integer, ItemStack> calculatedStacks = new HashMap<>();
	private final Map<Integer, Integer> lastCalculatedCounts = new HashMap<>();
	private final CompressionJournal journal = new CompressionJournal();

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
		long controllerDataFingerprintBefore = captureControllerDataFingerprint();
		clearCollections();
		Map<Integer, ItemStack> existingStacks = getExistingStacks();

		if (existingStacks.isEmpty()) {
			syncControllerDataIfChanged(controllerDataFingerprintBefore);
			return;
		}

		int lastNonEmptySlot = getLastNonEmptySlot(existingStacks);
		setSlotDefinitions(getSlotDefinitions(existingStacks.get(lastNonEmptySlot), lastNonEmptySlot, existingStacks), initial);

		compactInternalSlots();
		updateCalculatedStacks();
		syncControllerDataIfChanged(controllerDataFingerprintBefore);

		slotDefinitions.forEach((slot, definition) -> parent.triggerOnChangeListeners(slot));
	}

	private void setSlotDefinitions(Map<Integer, SlotDefinition> definitions, boolean initial) {
		slotDefinitions = definitions;
		if (initial) {
			parent.initFilterItems();
		}
	}

	private Integer getLastNonEmptySlot(Map<Integer, ItemStack> existingStacks) {
		for (int slot = slotRange.firstSlot() + slotRange.size() - 1; slot >= slotRange.firstSlot(); slot--) {
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
				ret.clear(); // clearing any compressible definition added before as the compression should no longer compress if there are incompatible items
								// present
				break;
			} else {
				Optional<CompressionChainHelper.CompressionDefinition> compressionDefinition = getCompressionDefinition(prevItem);
				if (compressionDefinition.isPresent()) {
					CompressionChainHelper.CompressionDefinition definition = compressionDefinition.get();
					ret.put(slot, new SlotDefinition(prevItem, definition.count(), true));
					prevItem = definition.result();
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
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			if (definitions.containsKey(slot) && definitions.get(slot).isAccessible()) {
				totalLimit = intMaxCappedAddition(parent.getBaseCapacity(definitions.get(slot).itemResource),
						intMaxCappedMultiply(definitions.get(slot).prevSlotMultiplier, totalLimit));

				definitions.get(slot).setSlotLimit(totalLimit);
			}
		}
	}

	private void updateCalculatedStacks() {
		int totalCalculated = 0;
		boolean prevFull = false;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			if (!slotDefinition.isAccessible()) {
				continue;
			}
			if (!slotDefinition.isCompressible()) {
				setCalculatedStack(slot, parent.getInternalStack(slot).copy());
				continue;
			}
			int internalCount = parent.getInternalStack(slot).getCount();
			totalCalculated = Integer.MAX_VALUE / slotDefinition.prevSlotMultiplier() < totalCalculated
					? Integer.MAX_VALUE
					: totalCalculated * slotDefinition.prevSlotMultiplier();
			totalCalculated = Integer.MAX_VALUE - internalCount < totalCalculated ? Integer.MAX_VALUE : totalCalculated + internalCount;

			ItemStack calculatedStack = slotDefinition.itemResource().toStack(totalCalculated);

			int internalLimit = parent.getBaseCapacity(slotDefinition.itemResource());
			int maxStackSize = slotDefinition.itemResource.getMaxStackSize();
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

		for (int slot = slotRange.firstSlot() + 1; slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			ItemStack slotStack = parent.getInternalStack(slot);
			int multiplier = getPrevSlotMultiplier(slot);
			if (slotStack.isEmpty() || multiplier < 2) {
				continue;
			}
			int prevSlot = slot - 1;
			ItemStack prevStack = parent.getInternalStack(prevSlot);
			int stackLimit = parent.getBaseCapacity(slotDefinitions.get(prevSlot).itemResource());
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
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			definitions.computeIfAbsent(slot, s -> {
				if (existingStacks.containsKey(s)) {
					return new SlotDefinition(existingStacks.get(s), 1, true);
				}
				return SlotDefinition.inaccesible();
			});
			if (!definitions.get(slot).isAccessible()) {
				continue;
			}
			boolean uncompressibledFromNext = definitions.containsKey(slot - 1) && definitions.get(slot - 1).isAccessible()
					&& definitions.get(slot).prevSlotMultiplier() > 1;
			boolean compressibleFromPrevious = definitions.containsKey(slot + 1) && definitions.get(slot + 1).isAccessible()
					&& definitions.get(slot + 1).prevSlotMultiplier() > 1;
			definitions.get(slot).setCompressible(uncompressibledFromNext || compressibleFromPrevious);
		}
	}

	private void clearCollections() {
		slotDefinitions.clear();
		calculatedStacks.clear();
		lastCalculatedCounts.clear();
	}

	private long captureControllerDataFingerprint() {
		long fingerprint = 1;
		int accessibleSlots = 0;
		int firstSlotWithItem = -1;
		int firstItemHash = 0;

		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			if (slotDefinition == null || !slotDefinition.isAccessible()) {
				fingerprint = 31 * fingerprint;
				continue;
			}

			accessibleSlots++;
			fingerprint = 31 * fingerprint + slotDefinition.itemResource().hashCode();

			ItemStack calculatedStack = getCalculatedStackInSlot(slot);
			if (calculatedStack.isEmpty()) {
				fingerprint = 31 * fingerprint + 1;
			} else {
				if (firstSlotWithItem < 0) {
					firstSlotWithItem = slot;
					firstItemHash = ItemStackKey.of(calculatedStack).hashCode();
				}
				fingerprint = 31 * fingerprint + (calculatedStack.getCount() < getCapacity(slot, ItemResource.of(calculatedStack)) ? 2 : 3);
			}
		}

		fingerprint = 31 * fingerprint + accessibleSlots;
		fingerprint = 31 * fingerprint + (firstSlotWithItem + 1);
		fingerprint = 31 * fingerprint + firstItemHash;
		return fingerprint;
	}

	private void syncControllerDataIfChanged(long controllerDataFingerprintBefore) {
		if (controllerDataFingerprintBefore != captureControllerDataFingerprint()) {
			parent.onFilterItemsChanged();
		}
	}

	private Optional<CompressionChainHelper.CompressionDefinition> getCompressionDefinition(ItemStack stack) {
		return CompressionChainHelper.getCompressionDefinition(stack, this::getDecompressionResultFromConfig, this::getCompressionResultFromConfig);
	}

	private void addPreviousItems(Map<Integer, SlotDefinition> slotDefinitions, int firstFilledSlot, ItemStack firstFilledItem) {
		ItemStack currentItem = firstFilledItem;
		for (int slot = firstFilledSlot + 1; slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			Optional<CompressionChainHelper.DecompressionDefinition> decompressionDefinition = CompressionChainHelper.getDecompressionDefinition(currentItem,
					this::getDecompressionResultFromConfig);
			if (decompressionDefinition.isEmpty()) {
				break;
			}

			ItemStack result = decompressionDefinition.get().result();
			int count = decompressionDefinition.get().count();
			slotDefinitions.put(slot, new SlotDefinition(result, count, true));
			currentItem = result;
		}
	}

	Optional<CompressionUpgradeConfig.DecompressionResult> getDecompressionResultFromConfig(Item currentItem) {
		return Config.SERVER.compressionUpgrade.getDecompressionResult(currentItem);
	}

	Optional<CompressionUpgradeConfig.CompressionResult> getCompressionResultFromConfig(ItemStack stack) {
		return Config.SERVER.compressionUpgrade.getCompressionResult(stack);
	}

	private Map<Integer, ItemStack> getExistingStacks() {
		Map<Integer, ItemStack> existingStacks = new LinkedHashMap<>();
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			ItemStack slotStack = parent.getInternalStack(slot);
			if (!slotStack.isEmpty()) {
				existingStacks.put(slot, slotStack);
			}
		}

		if (existingStacks.isEmpty()) {
			MemorySettingsCategory memorySettings = getMemorySettings.get();
			for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
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
	public int getCapacity(int slot, ItemResource resource) {
		if (!slotDefinitions.containsKey(slot)) {
			return parent.getBaseCapacity(resource);
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
	public int extract(int slot, ItemResource resource, int amount, TransactionContext tx, IResourceExtractor extractSuper) {
		return extractItem(slot, resource, amount, tx);
	}

	private int extractItem(int slot, ItemResource resource, int amount, TransactionContext tx) {
		if (!slotDefinitions.containsKey(slot) || !slotDefinitions.get(slot).itemResource.equals(resource) || !slotDefinitions.get(slot).isAccessible()
				|| !calculatedStacks.containsKey(slot)) {
			return 0;
		}
		int extracted = Math.min(calculatedStacks.get(slot).getCount(), amount);

		if (extracted > 0) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			ItemStack slotStack = parent.getInternalStack(slot);

			journal.updateSnapshots(tx);
			if (slotDefinition.isCompressible()) {
				extractFromCalculated(slot, extracted);
				extractFromInternal(slot, extracted);
				updateSlotTrackerAndListenersForCalculatedStacks();
			} else {
				slotStack.shrink(extracted);
				setCalculatedStack(slot, slotStack.copy());
				parent.setStackInSlotInternal(slot, slotStack);
			}
			removeDefinitionsIfEmpty(slot);

			return extracted;
		}

		return 0;
	}

	private void updateSlotTrackerAndListenersForCalculatedStacks() {
		calculatedStacks.forEach((slot, stack) -> {
			parent.getSlotTracker().removeAndSetSlotIndexes(parent, slot, stack);
			parent.triggerOnChangeListeners(slot);
		});
	}

	private boolean removeDefinitionsIfEmpty(int slotTriggeringChange) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			if (!parent.getInternalStack(slot).isEmpty() || getMemorySettings.get().getSlotFilterStack(slot, false).isPresent()) {
				return false;
			}
		}

		clearCollections();
		parent.triggerOnChangeListeners(slotTriggeringChange);
		return true;
	}

	private void extractFromInternal(int slotToStartFrom, int amountToExtract) {
		Map<Integer, Integer> toUpdate = new HashMap<>();
		int decompressedAmountToInsert = 0;
		int totalMultiplier = 1;
		while (amountToExtract > 0) {
			ItemStack slotStack = parent.getInternalStack(slotToStartFrom);
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
			ItemStack slotStack = parent.getInternalStack(s);
			if (slotStack.getCount() != count) {
				if (count == 0) {
					parent.setStackInSlotInternal(s, ItemStack.EMPTY);
				} else if (slotStack.isEmpty()) {
					parent.setStackInSlotInternal(s, slotDefinitions.get(s).itemResource().toStack(count));
				} else {
					slotStack.setCount(count);
					parent.setStackInSlotInternal(s, slotStack);
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
			ItemResource itemResource = slotDefinitions.get(slotCalculated).itemResource;
			int buffer = prevSlotFull ? getCapacity(slotCalculated, itemResource) - countCalculated : calculatedStack.getMaxStackSize();
			toSet = Integer.MAX_VALUE - buffer;
		}
		return toSet;
	}

	private void extractFromCalculatedThisAndStacksAfter(int extractCount, int slot) {
		while (slot < slotRange.firstSlot() + slotRange.size() && slotDefinitions.get(slot).isAccessible() && calculatedStacks.containsKey(slot)) {
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
	public int insert(int slot, ItemResource resource, int amount, TransactionContext tx, IResourceInserter insertSuper) {
		return insertItem(slot, resource, amount, tx);
	}

	private int insertItem(int slot, ItemResource resource, int amount, TransactionContext tx) {
		if (canNotBeInserted(slot, resource)) {
			return 0;
		}

		Map<Integer, SlotDefinition> definitions = slotDefinitions;

		if (definitions.isEmpty()) {
			definitions = getSlotDefinitions(resource.toStack(amount), slot, Map.of());
		}

		SlotDefinition slotDefinition = definitions.get(slot);
		int limit = getStackLimit(slotDefinition);

		int currentCalculatedCount = calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0;
		int inserted = Math.min(
				Math.max(parent.getBaseCapacity(slotDefinition.itemResource()) - parent.getInternalStack(slot).getCount(), limit - currentCalculatedCount),
				amount);

		if (inserted == 0) {
			return 0;
		}

		journal.updateSnapshots(tx);
		if (!slotDefinitions.containsKey(slot)) {
			setSlotDefinitions(definitions, false);
			compactInternalSlots();
			updateCalculatedStacks();

			slotDefinitions.forEach((s, definition) -> parent.triggerOnChangeListeners(s));
			slotDefinition = slotDefinitions.get(slot);
		}

		if (slotDefinitions.get(slot).isCompressible()) {
			insertIntoInternalAndCalculated(slot, inserted);
		} else if (inserted > 0) {
			ItemStack newCalculatedStack;
			if (calculatedStacks.containsKey(slot) && !calculatedStacks.get(slot).isEmpty()) {
				newCalculatedStack = calculatedStacks.get(slot);
				newCalculatedStack.grow(inserted);
			} else {
				newCalculatedStack = resource.toStack(inserted);
			}
			setCalculatedStack(slot, newCalculatedStack);
			ItemStack slotStack = parent.getInternalStack(slot);
			if (slotStack.isEmpty()) {
				ItemStack copy = slotDefinition.itemResource().toStack(inserted);
				parent.setStackInSlotInternal(slot, copy);
			} else {
				slotStack.grow(inserted);
				parent.setStackInSlotInternal(slot, slotStack);
			}
		}

		return inserted;
	}

	private boolean canNotBeInserted(int slot, ItemResource resource) {
		if (resource.isEmpty()) {
			return true;
		}

		if (!slotDefinitions.containsKey(slot)) {
			return false;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return !slotDefinition.isAccessible() || !slotDefinition.itemResource().equals(resource);
	}

	private void insertIntoInternalAndCalculated(int slotToStartFrom, long amountToInsert) {
		Map<Integer, Integer> toUpdate = new LinkedHashMap<>();
		Map<Integer, Integer> calculatedAdditions = new LinkedHashMap<>();
		int totalMultiplier = 1;
		int slot = slotToStartFrom;

		long amountToSet = amountToInsert + parent.getInternalStack(slot).getCount();

		while (amountToSet / ((long) totalMultiplier * getPrevSlotMultiplier(slot)) > 0 && slotDefinitions.containsKey(slot - 1)
				&& slotDefinitions.get(slot - 1).isAccessible()) {
			totalMultiplier *= getPrevSlotMultiplier(slot);
			slot--;
			amountToSet += (long) parent.getInternalStack(slot).getCount() * totalMultiplier;
		}

		long calculatedAddition = 0;
		while (slot <= slotToStartFrom) {
			calculatedAddition *= getPrevSlotMultiplier(slot);
			ItemResource slotResource = slotDefinitions.get(slot).itemResource();
			int toSet = (int) Math.min(amountToSet / totalMultiplier, parent.getBaseCapacity(slotResource));
			calculatedAddition += (toSet - parent.getInternalStack(slot).getCount());
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));
			if (toSet > 0) {
				toUpdate.put(slot, toSet);
				amountToSet -= (long) toSet * totalMultiplier;
			} else {
				toUpdate.put(slot, 0);
			}

			if (amountToSet != 0) {
				if (!slotDefinitions.containsKey(slot + 1) || !slotDefinitions.get(slot + 1).isAccessible()) {
					SophisticatedStorage.LOGGER.error(
							"Compression inventory is in an invalid state. Slot {} has a prevSlotMultiplier of 0 (likely because it's inaccessible), but there's remaining count of {} to insert.\nSlot Definitions\n{}",
							slot + 1, amountToSet, slotDefinitions);
					break;
				}
				totalMultiplier /= getPrevSlotMultiplier(slot + 1);
			}
			slot++;
		}

		// finish calculation of calculated addition to the follow up slots even though they are not getting their internal stack changed
		while (slot < slotRange.firstSlot() + slotRange.size()) {
			if (!slotDefinitions.containsKey(slot)) {
				break;
			}

			calculatedAddition *= getPrevSlotMultiplier(slot);
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));

			slot++;
		}

		updateInternalStacksWithCounts(toUpdate);

		calculatedAdditions.forEach(this::addToCalculatedStack);
		toUpdate.keySet().forEach(parent::triggerOnChangeListeners);
	}

	private void addToCalculatedStack(int slot, int countToAdd) {
		if (!calculatedStacks.containsKey(slot) || calculatedStacks.get(slot).isEmpty()) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			setCalculatedStack(slot, slotDefinition.itemResource().toStack(countToAdd));
			return;
		}
		ItemStack currentCalculated = calculatedStacks.get(slot);

		int totalCalculated = Integer.MAX_VALUE - countToAdd < currentCalculated.getCount() ? Integer.MAX_VALUE : currentCalculated.getCount() + countToAdd;

		int previousSlot = slot - 1;
		if (totalCalculated != Integer.MAX_VALUE || !slotDefinitions.containsKey(previousSlot)) {
			currentCalculated.setCount(totalCalculated);
			setCalculatedStack(slot, currentCalculated);
			return;
		}

		ItemStack previousInternalStack = parent.getInternalStack(previousSlot);

		boolean isPreviousFull = previousInternalStack.getCount() >= parent.getBaseCapacity(slotDefinitions.get(previousSlot).itemResource());

		int internalLimit = parent.getBaseCapacity(slotDefinitions.get(slot).itemResource());
		int internalCount = parent.getInternalStack(slot).getCount();

		int maxStackSize = previousInternalStack.getMaxStackSize();
		int spaceBeforeMaxInt = isPreviousFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize;
		currentCalculated.setCount(Integer.MAX_VALUE - spaceBeforeMaxInt);
	}

	@Override
	public void set(int slot, ItemResource resource, int amount, IndexModifier<ItemResource> setSuper) {
		if (!resource.isEmpty() && canNotBeInserted(slot, resource)) {
			return;
		}

		int currentCount = lastCalculatedCounts.getOrDefault(slot, 0);

		// go back to last known count if the stack was only changed externally using something like split / shrink / grow
		if (currentCount != (calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0)) {
			setCalculatedStack(slot, slotDefinitions.get(slot).itemResource().toStack(currentCount));
		}

		try (Transaction tx = Transaction.openRoot()) {
			if (currentCount < amount) {
				insertItem(slot, resource, amount - currentCount, tx);
			} else if (currentCount > amount) {
				extractItem(slot, slotDefinitions.get(slot).itemResource, currentCount - amount, tx);
			}
			tx.commit();
		}
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotInternal) {
		set(slot, ItemResource.of(stack), stack.getCount(), (s, resource, amount) -> {
		});
	}

	@Override
	public boolean isValid(int slot, ItemResource resource, @Nullable Player player, BiPredicate<Integer, ItemResource> isValidSuper) {
		if (!slotDefinitions.containsKey(slot)) {
			return true;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return slotDefinition.isAccessible() && slotDefinition.itemResource().equals(resource);
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return getCalculatedStackInSlot(slot);
	}

	private ItemStack getCalculatedStackInSlot(int slot) {
		return slotDefinitions.containsKey(slot) && slotDefinitions.get(slot).isAccessible() && calculatedStacks.containsKey(slot)
				? calculatedStacks.get(slot)
				: ItemStack.EMPTY;
	}

	@Override
	public ItemResource getResource(int index, IntFunction<ItemResource> getResourceSuper) {
		return ItemResource.of(getCalculatedStackInSlot(index));
	}

	@Override
	public long getAmountAsLong(int index, IntFunction<Long> amountAsLongSuper) {
		return getCalculatedStackInSlot(index).getCount();
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return !slotDefinitions.containsKey(slot) || slotDefinitions.get(slot).isAccessible();
	}

	@Override
	public boolean shouldRenderInaccessibleSlotOverlay(int slot) {
		return !isSlotAccessible(slot);
	}

	@Override
	public int size() {
		return slotRange.size();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Nullable
	@Override
	public Identifier getNoItemIcon(int slot) {
		return EMPTY_COMPRESSION_SLOT;
	}

	@Override
	public Item getFilterItem(int slot) {
		return slotDefinitions.containsKey(slot) ? slotDefinitions.get(slot).itemResource().getItem() : Items.AIR;
	}

	@Override
	public void onSlotLimitChange() {
		updateSlotLimits(slotDefinitions);
	}

	@Override
	public Set<Integer> getNoSortSlots() {
		return IntStream.rangeClosed(slotRange.firstSlot(), slotRange.firstSlot() + slotRange.size() - 1).boxed().collect(Collectors.toSet());
	}

	@Override
	public void onSlotFilterChanged(int slot) {
		calculateStacks(false);
	}

	@Override
	public boolean isFilterItem(Item item) {
		for (SlotDefinition slotDefinition : slotDefinitions.values()) {
			if (slotDefinition.itemResource().getItem() == item) {
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
				filterItems.computeIfAbsent(slotDefinition.itemResource().getItem(), k -> new HashSet<>()).add(entry.getKey());
			}
		}
		return filterItems;
	}

	private static final class SlotDefinition {
		private final ItemResource itemResource;
		private final int prevSlotMultiplier;
		private int slotLimit;
		private final boolean isAccessible;

		private boolean isCompressible = false;

		private SlotDefinition(ItemResource itemResource, int prevSlotMultiplier, int slotLimit, boolean isAccessible) {
			this.itemResource = itemResource;
			this.prevSlotMultiplier = prevSlotMultiplier;
			this.slotLimit = slotLimit;
			this.isAccessible = isAccessible;
		}

		public static SlotDefinition inaccesible() {
			return new SlotDefinition(ItemResource.EMPTY, 0, 0, false);
		}

		public SlotDefinition(ItemStack stack, int prevSlotMultiplier, boolean isAccessible) {
			this(ItemResource.of(stack), prevSlotMultiplier, -1, isAccessible);
		}

		public void setSlotLimit(int slotLimit) {
			this.slotLimit = slotLimit;
		}

		public void setCompressible(boolean compressible) {
			isCompressible = compressible;
		}

		public ItemResource itemResource() {
			return itemResource;
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
			return "SlotDefinition{" + "itemReosurce=" + itemResource + ", prevSlotMultiplier=" + prevSlotMultiplier + ", slotLimit=" + slotLimit
					+ ", isAccessible=" + isAccessible + ", isCompressible=" + isCompressible + '}';
		}
	}
	private record CompressionSnapshot(Map<Integer, SlotDefinition> slotDefinitions, Map<Integer, ItemStack> calculatedStacks,
			Map<Integer, ItemStack> internalStacks, long controllerDataFingerprint) {
	}

	private class CompressionJournal extends SnapshotJournal<CompressionSnapshot> {

		@Override
		protected CompressionSnapshot createSnapshot() {
			Map<Integer, ItemStack> internalStacks = new HashMap<>();
			for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
				internalStacks.put(slot, parent.getInternalStack(slot).copy());
			}

			Map<Integer, ItemStack> calculatedStacksCopy = new HashMap<>();
			CompressionInventoryPart.this.calculatedStacks.forEach((slot, stack) -> calculatedStacksCopy.put(slot, stack.copy()));

			return new CompressionSnapshot(new HashMap<>(CompressionInventoryPart.this.slotDefinitions), calculatedStacksCopy, internalStacks,
					captureControllerDataFingerprint());
		}

		@Override
		protected void revertToSnapshot(CompressionSnapshot compressionSnapshot) {
			CompressionInventoryPart.this.slotDefinitions = compressionSnapshot.slotDefinitions;
			CompressionInventoryPart.this.calculatedStacks.clear();
			CompressionInventoryPart.this.lastCalculatedCounts.clear();
			compressionSnapshot.calculatedStacks.forEach(CompressionInventoryPart.this::setCalculatedStack);
			compressionSnapshot.internalStacks.forEach((slot, stack) -> parent.setStackInSlotInternal(slot, stack.copy()));
			updateSlotTrackerAndListenersForCalculatedStacks();
		}

		@Override
		protected void onRootCommit(CompressionSnapshot originalState) {
			if (originalState.controllerDataFingerprint != captureControllerDataFingerprint()) {
				parent.onFilterItemsChanged();
			}
		}
	}
}
