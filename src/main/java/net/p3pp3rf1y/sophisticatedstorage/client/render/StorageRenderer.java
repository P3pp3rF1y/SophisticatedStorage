package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.function.Predicate;

public abstract class StorageRenderer<T extends StorageBlockEntity> implements BlockEntityRenderer<T> {
	private long lastCacheTime = -1;
	private boolean holdsItemThatShowsUpgrades = false;
	private boolean holdsStorageToolSetToToggleUpgrades = false;
	private boolean holdsItemThatShowsHiddenTiers = false;
	private boolean holdsToolInToggleLockOrLockDisplay = false;

	protected boolean holdsItemThatShowsUpgrades() {
		refreshCache();
		return holdsItemThatShowsUpgrades;
	}

	private void refreshCache() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getGameTime() != lastCacheTime) {
			lastCacheTime = level.getGameTime();

			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) {
				holdsItemThatShowsUpgrades = false;
				holdsStorageToolSetToToggleUpgrades = false;
				holdsItemThatShowsHiddenTiers = false;
				holdsToolInToggleLockOrLockDisplay = false;
				return;
			}

			boolean holdsStorageTool = holdsItem(player, this::isStorageTool);
			holdsStorageToolSetToToggleUpgrades = holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> StorageToolItem.getMode(item) == StorageToolItem.Mode.UPGRADES_DISPLAY).orElse(false);

			holdsItemThatShowsUpgrades = holdsStorageTool || holdsItem(player, this::isUpgrade);
			holdsItemThatShowsHiddenTiers = (holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> StorageToolItem.getMode(item) == StorageToolItem.Mode.TIER_DISPLAY).orElse(false))
					|| holdsItem(player, StorageTierUpgradeItem.class::isInstance);
			holdsToolInToggleLockOrLockDisplay = holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> {
						StorageToolItem.Mode mode = StorageToolItem.getMode(item);
						return mode == StorageToolItem.Mode.LOCK_DISPLAY || mode == StorageToolItem.Mode.LOCK;
					}).orElse(false);
		}
	}

	public boolean holdsItemThatShowsHiddenTiers() {
		refreshCache();
		return holdsItemThatShowsHiddenTiers;
	}

	public boolean holdsToolInToggleLockOrLockDisplay() {
		refreshCache();
		return holdsToolInToggleLockOrLockDisplay;
	}

	private boolean holdsItem(LocalPlayer player, Predicate<Item> itemMatcher) {
		return itemMatcher.test(player.getItemInHand(InteractionHand.MAIN_HAND).getItem())
				|| itemMatcher.test(player.getItemInHand(InteractionHand.OFF_HAND).getItem());
	}

	private boolean isStorageTool(Item item) {
		return item == ModItems.STORAGE_TOOL.get();
	}

	private boolean isUpgrade(Item item) {
		return item instanceof UpgradeItemBase && RegistryHelper.getItemKey(item).getNamespace().equals(SophisticatedStorage.MOD_ID);
	}

	public boolean shouldShowDisabledUpgradesDisplay(T storageBlockEntity) {
		refreshCache();
		return holdsStorageToolSetToToggleUpgrades && !storageBlockEntity.shouldShowUpgrades();
	}
}
