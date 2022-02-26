package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ModItemColors {
	private ModItemColors() {}

	public static void init() {
		ItemColors itemColors = Minecraft.getInstance().getItemColors();

		itemColors.register((stack, tintIndex) -> {
			if (tintIndex < 0 || tintIndex > 1) {
				return -1;
			}
			if (tintIndex == 0) {
				return BarrelBlock.getMaincolorFromStack(stack).orElse(-1);
			} else {
				return BarrelBlock.getAccentColorFromStack(stack).orElse(-1);
			}
		}, ModBlocks.BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
	}
}
