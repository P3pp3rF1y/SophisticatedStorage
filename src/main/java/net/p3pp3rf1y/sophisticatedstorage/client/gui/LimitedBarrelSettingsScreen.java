package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;

public class LimitedBarrelSettingsScreen extends StorageSettingsScreen {
	public LimitedBarrelSettingsScreen(SettingsContainerMenu<?> screenContainer, Inventory inv, Component title) {
		super(screenContainer, inv, title);
	}

	@Override
	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return LimitedBarrelScreen.STORAGE_SLOTS_HEIGHT;
	}

	@Override
	protected void updateStorageSlotsPositions() {
		LimitedBarrelScreen.updateSlotPositions(getMenu(), getMenu().getNumberOfStorageInventorySlots(), imageWidth);
	}

	@Override
	protected void drawSlotBg(GuiGraphics guiGraphics, int x, int y) {
		LimitedBarrelScreen.drawSlotBg(guiGraphics, x, y, getMenu().getNumberOfStorageInventorySlots(), getMenu());
	}

	@Override
	protected StorageSettingsTabControlBase initializeTabControl() {
		return new StorageSettingsTabControl(this, new Position(leftPos + imageWidth, topPos + 4)) {
			@Override
			protected boolean isSettingsCategoryDisabled(String categoryName) {
				return categoryName.equals(ItemDisplaySettingsCategory.NAME) || categoryName.equals(NoSortSettingsCategory.NAME);
			}
		};
	}
}
