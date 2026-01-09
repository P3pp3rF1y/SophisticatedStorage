package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContentsFilteredItemHandler implements ITrackedContentsItemHandler {

	private final Supplier<ITrackedContentsItemHandler> itemHandlerGetter;
	private final Supplier<ISlotTracker> slotTrackerGetter;
	private final Supplier<MemorySettingsCategory> memorySettingsGetter;

	public ContentsFilteredItemHandler(Supplier<ITrackedContentsItemHandler> itemHandlerGetter, Supplier<ISlotTracker> slotTrackerGetter, Supplier<MemorySettingsCategory> memorySettingsGetter) {
		this.itemHandlerGetter = itemHandlerGetter;
		this.slotTrackerGetter = slotTrackerGetter;
		this.memorySettingsGetter = memorySettingsGetter;
	}

	@Override
	public int getSlots() {
		return itemHandlerGetter.get().getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return itemHandlerGetter.get().getStackInSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (matchesContents(stack)) {
			return itemHandlerGetter.get().insertItem(slot, stack, simulate);
		}
		return stack;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return itemHandlerGetter.get().extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return itemHandlerGetter.get().getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return matchesContents(stack) && itemHandlerGetter.get().isItemValid(slot, stack);
	}

	private boolean matchesContents(ItemStack stack) {
		return slotTrackerGetter.get().getItems().contains(stack.getItem()) || memorySettingsGetter.get().matchesFilter(stack);
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		if (matchesContents(stack)) {
			return itemHandlerGetter.get().insertItem(stack, simulate);
		}
		return stack;
	}

	@Override
	public ItemStack extractItem(ItemStack stack, boolean simulate) {
		return itemHandlerGetter.get().extractItem(stack, simulate);
	}

	@Override
	public Set<ItemStackKey> getTrackedStacks() {
		return itemHandlerGetter.get().getTrackedStacks();
	}

	@Override
	public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
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
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		itemHandlerGetter.get().setStackInSlot(slot, stack);
	}

	@Override
	public boolean isInsertBlocked() {
		return itemHandlerGetter.get().isInsertBlocked();
	}
}
