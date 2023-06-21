package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

public class LimitedBarrelScreen extends StorageScreen {
	public static final int STORAGE_SLOTS_HEIGHT = 3 * 18;
	private static final int MIDDLE_OF_STORAGE_SLOTS = 18 + STORAGE_SLOTS_HEIGHT / 2;

	public LimitedBarrelScreen(StorageContainerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawSlotBg(GuiGraphics guiGraphics, int x, int y) {
		LimitedBarrelScreen.drawSlotBg(guiGraphics, x, y, getMenu().getNumberOfStorageInventorySlots(), getMenu());
	}

	public static void drawSlotBg(GuiGraphics guiGraphics, int x, int y, int slotsNumber, AbstractContainerMenu menu) {
		Slot firstSlot = menu.getSlot(0);
		if (slotsNumber == 4) {
			GuiHelper.renderSlotsBackground(guiGraphics, x + firstSlot.x - 1, y + firstSlot.y - 1, 2, 2);
			return;
		}

		GuiHelper.renderSlotsBackground(guiGraphics, x + firstSlot.x - 1, y + firstSlot.y - 1, 1, 1);

		if (slotsNumber == 1) {
			return;
		}

		Slot secondSlot = menu.getSlot(1);
		GuiHelper.renderSlotsBackground(guiGraphics, x + secondSlot.x - 1, y + secondSlot.y - 1, slotsNumber == 3 ? 2 : 1, 1);
	}

	@Override
	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return STORAGE_SLOTS_HEIGHT;
	}

	@Override
	protected void updateStorageSlotsPositions() {
		updateSlotPositions(getMenu(), getMenu().getNumberOfStorageInventorySlots(), imageWidth);
	}

	public static void updateSlotPositions(AbstractContainerMenu menu, int slotNumber, int imageWidth) {
		int halfWidth = imageWidth / 2;
		if (slotNumber == 1) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 9;
		} else if (slotNumber == 2) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 18;

			slot = menu.getSlot(1);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS;
		} else if (slotNumber == 3) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 18;

			slot = menu.getSlot(1);
			slot.x = halfWidth - 18;
			slot.y = MIDDLE_OF_STORAGE_SLOTS;

			slot = menu.getSlot(2);
			slot.x = halfWidth;
			slot.y = MIDDLE_OF_STORAGE_SLOTS;
		} else if (slotNumber == 4) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 18;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 18;

			slot = menu.getSlot(1);
			slot.x = halfWidth;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 18;

			slot = menu.getSlot(2);
			slot.x = halfWidth - 18;
			slot.y = MIDDLE_OF_STORAGE_SLOTS;

			slot = menu.getSlot(3);
			slot.x = halfWidth;
			slot.y = MIDDLE_OF_STORAGE_SLOTS;
		}
	}

	@Override
	protected boolean shouldShowSortButtons() {
		return false;
	}
}
