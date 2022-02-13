package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

public class StorageScreen extends StorageScreenBase<StorageContainerMenu> {
	public static StorageScreen constructScreen(StorageContainerMenu screenContainer, Inventory inv, Component title) {
		return new StorageScreen(screenContainer, inv, title);
	}

	protected StorageScreen(StorageContainerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected String getStorageSettingsTabTooltip() {
		return StorageTranslationHelper.INSTANCE.translGui("settings.tooltip");
	}
}
