package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import javax.annotation.Nonnull;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContentsFilteredItemHandler implements ITrackedContentsItemResourceHandler {

	private final Supplier<ITrackedContentsItemResourceHandler> itemHandlerGetter;
	private final Supplier<ISlotTracker> slotTrackerGetter;
	private final Supplier<MemorySettingsCategory> memorySettingsGetter;

	public ContentsFilteredItemHandler(Supplier<ITrackedContentsItemResourceHandler> itemHandlerGetter, Supplier<ISlotTracker> slotTrackerGetter,
			Supplier<MemorySettingsCategory> memorySettingsGetter) {
		this.itemHandlerGetter = itemHandlerGetter;
		this.slotTrackerGetter = slotTrackerGetter;
		this.memorySettingsGetter = memorySettingsGetter;
	}

	@Override
	public int size() {
		return itemHandlerGetter.get().size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return itemHandlerGetter.get().getStackInSlot(slot);
	}

	@Override
	public ItemResource getResource(int index) {
		return itemHandlerGetter.get().getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return itemHandlerGetter.get().getAmountAsLong(index);
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext tx) {
		if (matchesContents(resource)) {
			return itemHandlerGetter.get().insert(resource, amount, tx);
		}
		return 0;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
		if (matchesContents(resource)) {
			return itemHandlerGetter.get().insert(index, resource, amount, tx);
		}
		return 0;
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext tx) {
		return itemHandlerGetter.get().extract(resource, amount, tx);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
		return itemHandlerGetter.get().extract(index, resource, amount, tx);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return itemHandlerGetter.get().getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return matchesContents(resource) && itemHandlerGetter.get().isValid(index, resource);
	}

	private boolean matchesContents(ItemResource resource) {
		return slotTrackerGetter.get().getItems().contains(resource.getItem()) || memorySettingsGetter.get().matchesFilter(resource);
	}

	@Override
	public Set<ItemStackKey> getTrackedStacks() {
		return itemHandlerGetter.get().getTrackedStacks();
	}

	@Override
	public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot,
			Runnable onRemoveLastEmptySlot) {
		itemHandlerGetter.get().registerTrackingListeners(onAddStackKey, onRemoveStackKey, onAddFirstEmptySlot, onRemoveLastEmptySlot);
	}

	@Override
	public void unregisterStackKeyListeners() {
		itemHandlerGetter.get().unregisterStackKeyListeners();
	}

	@Override
	public boolean hasEmptySlots() {
		return itemHandlerGetter.get().hasEmptySlots();
	}

	@Override
	public int getInternalSlotLimit(int slot) {
		return itemHandlerGetter.get().getInternalSlotLimit(slot);
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		itemHandlerGetter.get().setStackInSlot(slot, stack);
	}

	@Override
	public boolean isInsertBlocked() {
		return itemHandlerGetter.get().isInsertBlocked();
	}
}
