package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.function.Consumer;

public class StorageHolderToolHandler {
	public static InteractionResult tryStorageToolInteract(ItemStack storageTool, StorageHolderBase storageHolder) {
		StorageToolItem.Mode mode = StorageToolItem.getMode(storageTool);
		switch (mode) {
			case LOCK -> {
				if (tryToggling(ILockable.class, ILockable::toggleLock, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
			case COUNT_DISPLAY -> {
				if (tryToggling(ICountDisplay.class, ICountDisplay::toggleCountVisibility, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
			case LOCK_DISPLAY -> {
				if (tryToggling(ILockable.class, ILockable::toggleLockVisibility, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
			case TIER_DISPLAY -> {
				if (tryToggling(ITierDisplay.class, ITierDisplay::toggleTierVisiblity, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
			case UPGRADES_DISPLAY -> {
				if (tryToggling(IUpgradeDisplay.class, IUpgradeDisplay::toggleUpgradesVisiblity, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
			case FILL_LEVEL_DISPLAY -> {
				if (tryToggling(IFillLevelDisplay.class, IFillLevelDisplay::toggleFillLevelVisibility, storageHolder)) {
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.PASS;
	}

	private static <T> boolean tryToggling(Class<T> clazz, Consumer<T> doToggle, StorageHolderBase storageHolder) {
		if (clazz.isInstance(storageHolder)) {
			doToggle.accept(clazz.cast(storageHolder));
			return true;
		}
		return false;
	}
}
