package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ModBlockColors {
	private ModBlockColors() {}

	public static void init() {
		Minecraft minecraft = Minecraft.getInstance();
		BlockColors blockColors = minecraft.getBlockColors();

		blockColors.register((state, blockDisplayReader, pos, tintIndex) -> {
			if (tintIndex < 0 || pos == null) {
				return -1;
			}
			return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
					.map(be -> {
						if (tintIndex > 999) {
							return tintIndex == 1000 ? be.getMainColor() : be.getAccentColor();
						} else {
							return minecraft.getItemColors().getColor(be.getRenderInfo().getItemDisplayRenderInfo().getItem(), tintIndex);
						}
					})
					.orElse(-1);
		}, ModBlocks.BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get());

		blockColors.register((state, blockDisplayReader, pos, tintIndex) -> {
			if (tintIndex < 0 || pos == null) {
				return -1;
			}
			return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
					.map(be -> {
						if (tintIndex == 0) { //this is only needed for particle texture handling so no need to handle anything other than just the main color
							return be.getMainColor();
						}
						return -1;
					})
					.orElse(-1);
		}, ModBlocks.CHEST.get(), ModBlocks.IRON_CHEST.get(), ModBlocks.GOLD_CHEST.get(), ModBlocks.DIAMOND_CHEST.get(), ModBlocks.NETHERITE_CHEST.get());
	}
}
