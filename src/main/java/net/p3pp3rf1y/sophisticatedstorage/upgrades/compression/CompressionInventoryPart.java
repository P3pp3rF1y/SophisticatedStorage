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
			reindexCompressionSlots();
			syncControllerDataIfChanged(controllerDataFingerprintBefore);
			return;
		}

		int lastNonEmptySlot = getLastNonEmptySlot(existingStacks);
		setSlotDefinitions(getSlotDefinitions(existingStacks.get(lastNonEmptySlot), lastNonEmptySlot, existingStacks), initial);

		commitInternalCounts(new HashMap<>());
		reindexCompressionSlots();
		syncControllerDataIfChanged(controllerDataFingerprintBefore);
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

	private void updateCalculatedStacks(Map<Integer, Integer> counts, Set<Integer> affectedSlots) {
		int totalCalculated = 0;
		boolean prevFull = false;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			if (!slotDefinition.isAccessible()) {
				continue;
			}
			int previousCount = lastCalculatedCounts.getOrDefault(slot, -1);
			if (!slotDefinition.isCompressible()) {
				setCalculatedStack(slot, parent.getInternalStack(slot).copy());
				if (previousCount != lastCalculatedCounts.get(slot)) {
					affectedSlots.add(slot);
				}
				continue;
			}
			int internalCount = counts.getOrDefault(slot, parent.getInternalStack(slot).getCount());
			totalCalculated = Integer.MAX_VALUE / slotDefinition.prevSlotMultiplier() < totalCalculated
					? Integer.MAX_VALUE
					: totalCalculated * slotDefinition.prevSlotMultiplier();
			totalCalculated = Integer.MAX_VALUE - internalCount < totalCalculated ? Integer.MAX_VALUE : totalCalculated + internalCount;

			ItemStack calculatedStack = calculatedStacks.get(slot);
			if (calculatedStack == null || calculatedStack.isEmpty()) {
				calculatedStack = slotDefinition.itemResource().toStack(totalCalculated);
			} else {
				calculatedStack.setCount(totalCalculated);
			}

			int internalLimit = parent.getBaseCapacity(slotDefinition.itemResource());
			int maxStackSize = slotDefinition.itemResource.getMaxStackSize();
			if (Integer.MAX_VALUE - totalCalculated < maxStackSize) {
				calculatedStack.setCount(Integer.MAX_VALUE - (prevFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize));
			}
			setCalculatedStack(slot, calculatedStack);
			if (previousCount != calculatedStack.getCount()) {
				affectedSlots.add(slot);
			}

			prevFull = internalLimit <= internalCount;
		}
	}

	private void setCalculatedStack(int slot, ItemStack stack) {
		calculatedStacks.put(slot, stack);
		lastCalculatedCounts.put(slot, stack.getCount());
	}

	private void compactInternalSlots(Map<Integer, Integer> counts) {
		for (int pass = 1; pass < slotRange.size(); pass++) {
			boolean changed = false;
			for (int slot = slotRange.firstSlot() + 1; slot < slotRange.firstSlot() + slotRange.size(); slot++) {
				SlotDefinition definition = slotDefinitions.get(slot);
				SlotDefinition previous = slotDefinitions.get(slot - 1);
				if (definition == null || previous == null || !definition.isAccessible() || !previous.isAccessible() || definition.prevSlotMultiplier() < 2) {
					continue;
				}
				int count = counts.getOrDefault(slot, parent.getInternalStack(slot).getCount());
				int previousCount = counts.getOrDefault(slot - 1, parent.getInternalStack(slot - 1).getCount());
				int promoted = Math.min(Math.max(0, parent.getBaseCapacity(previous.itemResource()) - previousCount), count / definition.prevSlotMultiplier());
				if (promoted > 0) {
					counts.put(slot - 1, previousCount + promoted);
					counts.put(slot, count - promoted * definition.prevSlotMultiplier());
					changed = true;
				}
			}
			if (!changed) {
				break;
			}
		}
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

			parent.beginSlotStacks(tx);
			journal.updateSnapshots(tx);
			if (slotDefinition.isCompressible()) {
				extractFromInternal(slot, extracted);
			} else {
				ItemStack remaining = slotStack.copyWithCount(slotStack.getCount() - extracted);
				setCalculatedStack(slot, remaining.copy());
				parent.setSlotStacks(Map.of(slot, remaining), Set.of(slot));
			}
			removeDefinitionsIfEmpty(slot);

			return extracted;
		}

		return 0;
	}

	private boolean removeDefinitionsIfEmpty(int slotTriggeringChange) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			if (!parent.getInternalStack(slot).isEmpty() || getMemorySettings.get().getSlotFilterStack(slot, false).isPresent()) {
				return false;
			}
		}

		clearCollections();
		reindexCompressionSlots();
		parent.triggerOnChangeListeners(slotTriggeringChange);
		return true;
	}

	private void reindexCompressionSlots() {
		Set<Integer> slots = new LinkedHashSet<>();
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.size(); slot++) {
			slots.add(slot);
		}
		parent.setSlotStacks(Collections.emptyMap(), slots);
	}

	private void extractFromInternal(int slotToStartFrom, int amountToExtract) {
		Map<Integer, Integer> toUpdate = new HashMap<>();
		removeInternalCount(slotToStartFrom, amountToExtract, toUpdate);
		commitInternalCounts(toUpdate);
	}

	private void removeInternalCount(int slot, int amount, Map<Integer, Integer> counts) {
		int count = parent.getInternalStack(slot).getCount();
		if (count >= amount) {
			counts.put(slot, count - amount);
			return;
		}
		int multiplier = getPrevSlotMultiplier(slot);
		int missing = amount - count;
		int borrowed = (int) (((long) missing + multiplier - 1) / multiplier);
		removeInternalCount(slot - 1, borrowed, counts);
		counts.put(slot, (int) ((long) borrowed * multiplier - missing));
	}

	private int getPrevSlotMultiplier(int slot) {
		return slotDefinitions.get(slot).prevSlotMultiplier;
	}

	private void commitInternalCounts(Map<Integer, Integer> counts) {
		compactInternalSlots(counts);
		Map<Integer, ItemStack> updatedStacks = new LinkedHashMap<>();
		counts.forEach((slot, count) -> {
			if (parent.getInternalStack(slot).getCount() != count) {
				updatedStacks.put(slot, slotDefinitions.get(slot).itemResource().toStack(count));
			}
		});
		Set<Integer> affectedSlots = new LinkedHashSet<>(updatedStacks.keySet());
		updateCalculatedStacks(counts, affectedSlots);
		parent.setSlotStacks(updatedStacks, affectedSlots);
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
		int inserted = Math.min(Math.max(0,
				Math.max(parent.getBaseCapacity(slotDefinition.itemResource()) - parent.getInternalStack(slot).getCount(), limit - currentCalculatedCount)),
				amount);

		if (inserted == 0) {
			return 0;
		}

		parent.beginSlotStacks(tx);
		journal.updateSnapshots(tx);
		if (!slotDefinitions.containsKey(slot)) {
			setSlotDefinitions(definitions, false);
			commitInternalCounts(new HashMap<>());
			reindexCompressionSlots();
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
				parent.setSlotStacks(Map.of(slot, copy), Set.of(slot));
			} else {
				parent.setSlotStacks(Map.of(slot, slotStack.copyWithCount(slotStack.getCount() + inserted)), Set.of(slot));
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
		addInternalCount(slotToStartFrom, amountToInsert, toUpdate);
		commitInternalCounts(toUpdate);
	}

	private long addInternalCount(int slot, long amount, Map<Integer, Integer> counts) {
		SlotDefinition definition = slotDefinitions.get(slot);
		long count = amount + parent.getInternalStack(slot).getCount();
		SlotDefinition previous = slotDefinitions.get(slot - 1);
		int multiplier = definition.prevSlotMultiplier();
		if (multiplier > 1 && previous != null && previous.isAccessible() && count >= multiplier) {
			long groups = count / multiplier;
			long remainingGroups = addInternalCount(slot - 1, groups, counts);
			count -= (groups - remainingGroups) * multiplier;
		}
		int stored = (int) Math.min(count, Math.max(parent.getInternalStack(slot).getCount(), parent.getBaseCapacity(definition.itemResource())));
		counts.put(slot, stored);
		return count - stored;
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
		if (!slotDefinitions.isEmpty()) {
			commitInternalCounts(new HashMap<>());
		}
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
			parent.restoreSlotStacks(compressionSnapshot.internalStacks);
		}

		@Override
		protected void onRootCommit(CompressionSnapshot originalState) {
			if (originalState.controllerDataFingerprint != captureControllerDataFingerprint()) {
				parent.onFilterItemsChanged();
			}
		}
	}
}
