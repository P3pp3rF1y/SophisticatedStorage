package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

public class ModItemColors {
	private ModItemColors() {}

	public static void init() {
		ItemColors itemColors = Minecraft.getInstance().getItemColors();

		itemColors.register((stack, tintIndex) -> {
			if (tintIndex < 1000 || tintIndex > 1001) {
				return -1;
			}
			if (tintIndex == 1000) {
				return BarrelBlockItem.getMaincolorFromStack(stack).orElse(-1);
			} else {
				return BarrelBlockItem.getAccentColorFromStack(stack).orElse(-1);
			}
		}, ModBlocks.BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
	}
}
