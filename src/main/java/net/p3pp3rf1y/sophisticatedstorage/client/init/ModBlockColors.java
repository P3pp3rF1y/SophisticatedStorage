package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.GenericWoodStorageTintCache;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.List;

public class ModBlockColors {
	private ModBlockColors() {
	}

	public static void registerBlockColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
		event.register(List.of(new MainColorTintSource(), new AccentColorTintSource()), ModBlocks.BARREL.get(), ModBlocks.COPPER_BARREL.get(),
				ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get(),
				ModBlocks.LIMITED_BARREL_1.get(), ModBlocks.LIMITED_COPPER_BARREL_1.get(), ModBlocks.LIMITED_IRON_BARREL_1.get(),
				ModBlocks.LIMITED_GOLD_BARREL_1.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1.get(),
				ModBlocks.LIMITED_BARREL_2.get(), ModBlocks.LIMITED_COPPER_BARREL_2.get(), ModBlocks.LIMITED_IRON_BARREL_2.get(),
				ModBlocks.LIMITED_GOLD_BARREL_2.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), ModBlocks.LIMITED_NETHERITE_BARREL_2.get(),
				ModBlocks.LIMITED_BARREL_3.get(), ModBlocks.LIMITED_COPPER_BARREL_3.get(), ModBlocks.LIMITED_IRON_BARREL_3.get(),
				ModBlocks.LIMITED_GOLD_BARREL_3.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3.get(),
				ModBlocks.LIMITED_BARREL_4.get(), ModBlocks.LIMITED_COPPER_BARREL_4.get(), ModBlocks.LIMITED_IRON_BARREL_4.get(),
				ModBlocks.LIMITED_GOLD_BARREL_4.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), ModBlocks.LIMITED_NETHERITE_BARREL_4.get());

		event.register(List.of(new MainColorTintSource()), ModBlocks.CHEST.get(), ModBlocks.COPPER_CHEST.get(), ModBlocks.IRON_CHEST.get(),
				ModBlocks.GOLD_CHEST.get(), ModBlocks.DIAMOND_CHEST.get(), ModBlocks.NETHERITE_CHEST.get(), ModBlocks.SHULKER_BOX.get(),
				ModBlocks.COPPER_SHULKER_BOX.get(), ModBlocks.IRON_SHULKER_BOX.get(), ModBlocks.GOLD_SHULKER_BOX.get(), ModBlocks.DIAMOND_SHULKER_BOX.get(),
				ModBlocks.NETHERITE_SHULKER_BOX.get());
	}

	private static int getTint(BlockState state, int displayItemIndex, int adjustedTintIndex, StorageBlockEntity be) {
		RenderData.DisplayData displayData = be.getStorageWrapper().getRenderDataHandler().getDisplayData();
		List<RenderData.DisplayItemData> displayItems = displayData.displayItems();
		ItemStack stack = getDisplayItemWithIndex(displayItemIndex, displayItems, state.getBlock() instanceof LimitedBarrelBlock);
		if (stack.isEmpty()) {
			return -1;
		}

		ItemStackRenderState renderState = new ItemStackRenderState();
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.FIXED, null, null, 0);
		for (ItemStackRenderState.LayerRenderState layer : renderState.layers) {
			if (layer.tintLayers != null && layer.tintLayers.size() > adjustedTintIndex) {
				return layer.tintLayers.getInt(adjustedTintIndex);
			}
		}

		return -1;
	}

	private static ItemStack getDisplayItemWithIndex(int displayItemIndex, List<RenderData.DisplayItemData> displayItems, boolean isLimitedBarrel) {
		if (isLimitedBarrel) {
			for (RenderData.DisplayItemData displayItem : displayItems) {
				if (displayItem.slotIndex() == displayItemIndex) {
					return displayItem.createItemStack();
				}
			}
		}
		return displayItems.size() > displayItemIndex ? displayItems.get(displayItemIndex).createItemStack() : ItemStack.EMPTY;
	}

	private abstract static class StorageBlockTintSource implements BlockTintSource {
		@Override
		public int color(BlockState state) {
			return -1;
		}

		@Override
		public int colorInWorld(BlockState state, BlockAndTintGetter blockDisplayReader, BlockPos pos) {
			return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class).map(this::getColor).orElse(-1);
		}

		protected abstract int getColor(StorageBlockEntity be);
	}

	private static class MainColorTintSource extends StorageBlockTintSource {
		@Override
		protected int getColor(StorageBlockEntity be) {
			int mainColor = be.getStorageWrapper().getMainColor();
			return mainColor != -1 ? mainColor : getGenericWoodColor(be, true);
		}
	}

	private static class AccentColorTintSource extends StorageBlockTintSource {
		@Override
		protected int getColor(StorageBlockEntity be) {
			int accentColor = be.getStorageWrapper().getAccentColor();
			return accentColor != -1 ? accentColor : getGenericWoodColor(be, false);
		}
	}

	private static int getGenericWoodColor(StorageBlockEntity be, boolean mainColor) {
		if (!(be instanceof WoodStorageBlockEntity woodStorageBlockEntity)) {
			return -1;
		}

		return woodStorageBlockEntity.getWoodType().filter(GenericWoodStorageHelper::isGenericWood)
				.map(woodType -> mainColor ? GenericWoodStorageTintCache.getMainColor(woodType) : GenericWoodStorageTintCache.getAccentColor(woodType))
				.orElse(-1);
	}
}
