package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

public class ModBlockColors {
	private ModBlockColors() {}

	public static void registerBlockColorHandlers(RegisterColorHandlersEvent.Block event) {
		event.register(ModBlockColors::getBarrelTintColor, ModBlocks.BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get());
		event.register(ModBlockColors::getChestShulkerBoxColor, ModBlocks.CHEST.get(), ModBlocks.IRON_CHEST.get(), ModBlocks.GOLD_CHEST.get(), ModBlocks.DIAMOND_CHEST.get(), ModBlocks.NETHERITE_CHEST.get(),
				ModBlocks.SHULKER_BOX.get(), ModBlocks.IRON_SHULKER_BOX.get(), ModBlocks.GOLD_SHULKER_BOX.get(), ModBlocks.DIAMOND_SHULKER_BOX.get(), ModBlocks.NETHERITE_SHULKER_BOX.get());
	}

	private static int getBarrelTintColor(BlockState state, @Nullable BlockAndTintGetter blockDisplayReader, @Nullable BlockPos pos, int tintIndex) {
		if (tintIndex < 0 || pos == null) {
			return -1;
		}
		return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
				.map(be -> {
					if (tintIndex > 999) {
						return tintIndex == 1000 ? be.getStorageWrapper().getMainColor() : be.getStorageWrapper().getAccentColor();
					} else {
						ItemStack renderItem = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getItem();
						return renderItem.isEmpty() ? -1 : Minecraft.getInstance().getItemColors().getColor(renderItem, tintIndex);
					}
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
