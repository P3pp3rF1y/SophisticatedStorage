package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

public class ToolInfoOverlay {

	public static final LayeredDraw.Layer HUD_TOOL_INFO = (guiGraphics, deltaTracker) -> {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get()).ifPresent(storageTool -> {
			Component overlayMessage = StorageToolItem.getOverlayMessage(storageTool);
			Font font = Minecraft.getInstance().font;
			int i = font.width(overlayMessage);
			int x = (guiGraphics.guiWidth() - i) / 2;
			int y = guiGraphics.guiHeight() - 75;
			guiGraphics.drawString(font, overlayMessage, x + 1, y, DyeColor.WHITE.getTextColor(), false);
		});
	};
}
