package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;

public class TankUpgradeHandler extends UpgradeHandler {
	private final IStorageWrapper storage;

	public TankUpgradeHandler(int slots, IStorageWrapper storage, ContainerContents contents, Runnable save, Runnable invalidate) {
		super(slots, storage, contents, save, invalidate);
		this.storage = storage;
	}

	@Override
	public boolean isValid(int slot, ItemResource resource) {
		return canReplace(slot, resource.toStack(1));
	}

	private boolean canReplace(int slot, ItemStack replacement) {
		if (!super.isValid(slot, ItemResource.of(replacement)) || replacement.getCount() > 1) {
			return false;
		}
		double multiplier = storage.getBaseStackSizeMultiplier();
		for (int index = 0; index < size(); index++) {
			ItemStack candidate = index == slot ? replacement : getStackInSlot(index);
			if (candidate.getItem() instanceof StackUpgradeItem stackUpgrade && candidate.getOrDefault(ModCoreDataComponents.ENABLED, true)) {
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
			return originalUpgrade.canSwapUpgradeFor(replacement, slot, storage, false).successful();
		}
		return replacement.getItem() instanceof IUpgradeItem<?> upgrade && upgrade.canAddUpgradeTo(storage, replacement, true, false).successful();
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (!canReplace(slot, stack)) {
			throw new IllegalArgumentException("Upgrade would violate the tank's contents or upgrade restrictions");
		}
		super.setStackInSlot(slot, stack);
	}

	@Override
	public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
		return amount > 0 && !canReplace(slot, ItemStack.EMPTY) ? 0 : super.extract(slot, resource, amount, tx);
	}
}
