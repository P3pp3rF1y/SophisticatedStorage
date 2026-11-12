package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class TankUpgradeHandler extends UpgradeHandler {
	private final IStorageWrapper storage;

	public TankUpgradeHandler(int slots, IStorageWrapper storage, CompoundTag contents, Runnable save, Runnable invalidate) {
		super(slots, storage, contents, save, invalidate);
		this.storage = storage;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return canReplace(slot, stack.copyWithCount(1));
	}
	private boolean canReplace(int slot, ItemStack replacement) {
		if (!super.isItemValid(slot, replacement) || replacement.getCount() > 1) {
			return false;
		}
		double multiplier = storage.getBaseStackSizeMultiplier();
		for (int index = 0; index < getSlots(); index++) {
			ItemStack candidate = index == slot ? replacement : getStackInSlot(index);
			if (candidate.getItem() instanceof StackUpgradeItem stackUpgrade && NBTHelper.getBoolean(candidate, "enabled").orElse(true)) {
				multiplier *= stackUpgrade.getStackSizeMultiplier();
			}
		}
		if (multiplier < storage.getMinimumStackMultiplierRequired()) {
			return false;
		}
		ItemStack original = getStackInSlot(slot);
		if (replacement.isEmpty() || original.getItem() == replacement.getItem()) {
			return true;
		}
		if (original.getItem() instanceof IUpgradeItem<?> originalUpgrade) {
			return originalUpgrade.canSwapUpgradeFor(replacement, slot, storage, false).isSuccessful();
		}
		return replacement.getItem() instanceof IUpgradeItem<?> upgrade && upgrade.canAddUpgradeTo(storage, replacement, true, false).isSuccessful();
	}
	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (!canReplace(slot, stack)) {
			throw new IllegalArgumentException("Upgrade would violate the tank's contents or upgrade restrictions");
		}
		super.setStackInSlot(slot, stack);
	}
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return amount > 0 && !canReplace(slot, ItemStack.EMPTY) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
	}
}
