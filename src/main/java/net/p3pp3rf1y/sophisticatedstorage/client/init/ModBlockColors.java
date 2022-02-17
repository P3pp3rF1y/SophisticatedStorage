package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ModBlockColors {
	private ModBlockColors() {}

	public static void init() {
		BlockColors blockColors = Minecraft.getInstance().getBlockColors();

		blockColors.register((state, blockDisplayReader, pos, tintIndex) -> {
			if (tintIndex < 0 || tintIndex > 1 || pos == null) {
				return -1;
			}
			return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
					.map(be -> tintIndex == 0 ? be.getMainColor() : be.getAccentColor())
					.orElse(-1);
		}, ModBlocks.BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get());
	}
}
