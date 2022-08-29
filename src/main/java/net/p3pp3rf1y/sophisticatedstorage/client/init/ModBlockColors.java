package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.List;

public class ModBlockColors {
	private ModBlockColors() {}

	public static void init() {
		Minecraft minecraft = Minecraft.getInstance();
		BlockColors blockColors = minecraft.getBlockColors();

		blockColors.register(ModBlockColors::getBarrelTintColor, ModBlocks.BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get());

		blockColors.register(ModBlockColors::getChestShulkerBoxColor, ModBlocks.CHEST.get(), ModBlocks.IRON_CHEST.get(), ModBlocks.GOLD_CHEST.get(), ModBlocks.DIAMOND_CHEST.get(), ModBlocks.NETHERITE_CHEST.get(),
				ModBlocks.SHULKER_BOX.get(), ModBlocks.IRON_SHULKER_BOX.get(), ModBlocks.GOLD_SHULKER_BOX.get(), ModBlocks.DIAMOND_SHULKER_BOX.get(), ModBlocks.NETHERITE_SHULKER_BOX.get());
	}

	private static int getBarrelTintColor(BlockState state, @Nullable BlockAndTintGetter blockDisplayReader, @Nullable BlockPos pos, int tintIndex) {
		if (tintIndex < 0 || pos == null) {
			return -1;
		}
		return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
				.map(be -> {
					if (tintIndex == 1000) {
						return be.getStorageWrapper().getMainColor();
					} else if (tintIndex == 1001) {
						return be.getStorageWrapper().getAccentColor();
					} else if (tintIndex >= 0){
						RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo();
						int displayItemIndex = (tintIndex > 1000 ? tintIndex - 1000 : tintIndex) / 10 - 1;
						List<RenderInfo.DisplayItem> displayItems = itemDisplayRenderInfo.getDisplayItems();
						if (displayItemIndex >= 0 && displayItems.size() > displayItemIndex) {
							int tintOffset = (displayItemIndex + 1) * 10;
							return Minecraft.getInstance().getItemColors().getColor(displayItems.get(displayItemIndex).getItem(), tintIndex - tintOffset);
						}
					}
					return -1;
				})
				.orElse(-1);
	}

	private static int getChestShulkerBoxColor(BlockState state, @Nullable BlockAndTintGetter blockDisplayReader, @Nullable BlockPos pos, int tintIndex) {
		if (tintIndex < 0 || pos == null) {
			return -1;
		}
		return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
				.map(be -> {
					if (tintIndex == 0) { //this is only needed for particle texture handling so no need to handle anything other than just the main color
						return be.getStorageWrapper().getMainColor();
					}
					return -1;
				})
				.orElse(-1);
	}
}
