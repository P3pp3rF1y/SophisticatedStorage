package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

public class LimitedBarrelScreen extends StorageScreen {
	public static final ResourceLocation GUI_BACKGROUNDS = SophisticatedStorage.getRL("textures/gui/limited_barrels.png");
	public static final TextureBlitData LIMITED_I_BACKGROUND = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(0, 0), new Dimension(84, 82));
	public static final TextureBlitData LIMITED_II_BACKGROUND = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(84, 0), new Dimension(84, 82));
	public static final TextureBlitData LIMITED_III_BACKGROUND = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(0, 82), new Dimension(84, 82));
	public static final TextureBlitData LIMITED_IV_BACKGROUND = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(84, 82), new Dimension(84, 82));
	public static final TextureBlitData SMALL_BAR_FILL = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(171, 0), new Dimension(3, 28));
	public static final TextureBlitData LARGE_BAR_FILL = new TextureBlitData(GUI_BACKGROUNDS, Dimension.SQUARE_256, new UV(168, 0), new Dimension(3, 68));
	public static final int STORAGE_SLOTS_HEIGHT = 82;
	private static final int MIDDLE_OF_STORAGE_SLOTS = 18 + STORAGE_SLOTS_HEIGHT / 2;

	public LimitedBarrelScreen(StorageContainerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawSlotBg(PoseStack matrixStack, int x, int y) {
		LimitedBarrelScreen.drawSlotBg(this, matrixStack, x, y, getMenu().getNumberOfStorageInventorySlots());
	}

	public static void drawSlotBg(AbstractContainerScreen<?> screen, PoseStack matrixStack, int x, int y, int slotsNumber) {
		TextureBlitData backgroundTexture = getBackgroundTexture(slotsNumber);
		GuiHelper.blit(matrixStack, x + screen.getXSize() / 2 - backgroundTexture.getWidth() / 2 - 1, y + 17, backgroundTexture);
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
		super.renderLabels(poseStack, mouseX, mouseY);
		switch (getMenu().getNumberOfStorageInventorySlots()) {
			case 1 -> renderBar(poseStack, imageWidth / 2 + 37, 18 + 6, getMenu().getSlotFillPercentage(0), LARGE_BAR_FILL, false);
			case 2 -> {
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6, getMenu().getSlotFillPercentage(0), SMALL_BAR_FILL, false);
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6 + 40, getMenu().getSlotFillPercentage(1), SMALL_BAR_FILL, false);
			}
			case 3 -> {
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6, getMenu().getSlotFillPercentage(0), SMALL_BAR_FILL, false);
				renderBar(poseStack, imageWidth / 2 - 37 - 5, 18 + 6 + 40, getMenu().getSlotFillPercentage(1), SMALL_BAR_FILL, true);
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6 + 40, getMenu().getSlotFillPercentage(2), SMALL_BAR_FILL, false);
			}
			case 4 -> {
				renderBar(poseStack, imageWidth / 2 - 37 - 5, 18 + 6, getMenu().getSlotFillPercentage(0), SMALL_BAR_FILL, true);
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6, getMenu().getSlotFillPercentage(1), SMALL_BAR_FILL, false);
				renderBar(poseStack, imageWidth / 2 - 37 - 5, 18 + 6 + 40, getMenu().getSlotFillPercentage(2), SMALL_BAR_FILL, true);
				renderBar(poseStack, imageWidth / 2 + 37, 18 + 6 + 40, getMenu().getSlotFillPercentage(3), SMALL_BAR_FILL, false);
			}
		}
	}

	private static TextureBlitData getBackgroundTexture(int slotsNumber) {
		return switch (slotsNumber) {
			case 1 -> LIMITED_I_BACKGROUND;
			case 2 -> LIMITED_II_BACKGROUND;
			case 3 -> LIMITED_III_BACKGROUND;
			case 4 -> LIMITED_IV_BACKGROUND;
			default -> throw new IllegalStateException("Unexpected number of limited barrel slots: " + slotsNumber);
		};
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
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 29;

			slot = menu.getSlot(1);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS + 11;
		} else if (slotNumber == 3) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 9;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 29;

			slot = menu.getSlot(1);
			slot.x = halfWidth - 29;
			slot.y = MIDDLE_OF_STORAGE_SLOTS + 11;

			slot = menu.getSlot(2);
			slot.x = halfWidth + 11;
			slot.y = MIDDLE_OF_STORAGE_SLOTS + 11;
		} else if (slotNumber == 4) {
			Slot slot = menu.getSlot(0);
			slot.x = halfWidth - 29;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 29;

			slot = menu.getSlot(1);
			slot.x = halfWidth + 11;
			slot.y = MIDDLE_OF_STORAGE_SLOTS - 29;

			slot = menu.getSlot(2);
			slot.x = halfWidth - 29;
			slot.y = MIDDLE_OF_STORAGE_SLOTS + 11;

			slot = menu.getSlot(3);
			slot.x = halfWidth + 11;
			slot.y = MIDDLE_OF_STORAGE_SLOTS + 11;
		}
	}

	private void renderBar(PoseStack poseStack, int x, int y, float percentage, TextureBlitData barTexture, boolean left) {
		int barHeight = (int) (barTexture.getHeight() * percentage);
		int yOffset = barTexture.getHeight() - barHeight;

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, barTexture.getTextureName());
		GuiComponent.blit(poseStack, x, y + yOffset, barTexture.getU(), barTexture.getV() + yOffset, barTexture.getWidth(), barHeight, barTexture.getTextureWidth(), barTexture.getTextureHeight());

		String text = String.valueOf((int) (percentage * 100)) + "%";
		int percentageX = x;
		if (left) {
			percentageX -= 2 + minecraft.font.width(text);
		} else {
			percentageX += 6;
		}
		minecraft.font.draw(poseStack, text, percentageX, y + barTexture.getHeight() / 2 - 3, 0x2c2c2c);
	}

	@Override
	protected boolean shouldShowSortButtons() {
		return false;
	}
}
