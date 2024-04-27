package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class StorageItemClient {
	private StorageItemClient() {
	}

	@Nullable
	public static TooltipComponent getTooltipImage(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		if (Screen.hasShiftDown() || (mc.player != null && !mc.player.containerMenu.getCarried().isEmpty())) {
			return new StorageContentsTooltip(stack);
		}
		return null;
	}
}
