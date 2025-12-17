package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ModBlockColors {
	private ModBlockColors() {}

	public static void registerBlockColorHandlers(RegisterColorHandlersEvent.Block event) {
		event.register(ModBlockColors::getBarrelTintColor,
				ModBlocks.BARREL.get(), ModBlocks.COPPER_BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get(),
				ModBlocks.LIMITED_BARREL_1.get(), ModBlocks.LIMITED_COPPER_BARREL_1.get(), ModBlocks.LIMITED_IRON_BARREL_1.get(), ModBlocks.LIMITED_GOLD_BARREL_1.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1.get(),
				ModBlocks.LIMITED_BARREL_2.get(), ModBlocks.LIMITED_COPPER_BARREL_2.get(), ModBlocks.LIMITED_IRON_BARREL_2.get(), ModBlocks.LIMITED_GOLD_BARREL_2.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), ModBlocks.LIMITED_NETHERITE_BARREL_2.get(),
				ModBlocks.LIMITED_BARREL_3.get(), ModBlocks.LIMITED_COPPER_BARREL_3.get(), ModBlocks.LIMITED_IRON_BARREL_3.get(), ModBlocks.LIMITED_GOLD_BARREL_3.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3.get(),
				ModBlocks.LIMITED_BARREL_4.get(), ModBlocks.LIMITED_COPPER_BARREL_4.get(), ModBlocks.LIMITED_IRON_BARREL_4.get(), ModBlocks.LIMITED_GOLD_BARREL_4.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), ModBlocks.LIMITED_NETHERITE_BARREL_4.get()
		);

		event.register(ModBlockColors::getChestShulkerBoxColor, ModBlocks.CHEST.get(), ModBlocks.COPPER_CHEST.get(), ModBlocks.IRON_CHEST.get(), ModBlocks.GOLD_CHEST.get(), ModBlocks.DIAMOND_CHEST.get(), ModBlocks.NETHERITE_CHEST.get(),
				ModBlocks.SHULKER_BOX.get(), ModBlocks.COPPER_SHULKER_BOX.get(), ModBlocks.IRON_SHULKER_BOX.get(), ModBlocks.GOLD_SHULKER_BOX.get(), ModBlocks.DIAMOND_SHULKER_BOX.get(), ModBlocks.NETHERITE_SHULKER_BOX.get());
	}

	private static int getBarrelTintColor(BlockState state, @Nullable BlockAndTintGetter blockDisplayReader, @Nullable BlockPos pos, int tintIndex) {
		if (tintIndex < 0 || pos == null) {
			return -1;
		}
		return WorldHelper.getBlockEntity(blockDisplayReader, pos, StorageBlockEntity.class)
				.map(be -> {
					if (tintIndex == 0) {
						return be.getStorageWrapper().getMainColor();
					} else if (tintIndex == 1) {
						return be.getStorageWrapper().getAccentColor();
					}

					int displayItemIndex = tintIndex / 10 - 1;
					if (displayItemIndex >= 0) {
						int tintOffset = (displayItemIndex + 1) * 10;
						int adjustedTintIndex = tintIndex - tintOffset;
						return be.getOrComputeDisplayItemTint(displayItemIndex, adjustedTintIndex, () -> getTint(state, displayItemIndex, adjustedTintIndex, be));
					}

					return -1;
				})
				.orElse(-1);
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
			if (layer.tintLayers.length > adjustedTintIndex) {
				return layer.tintLayers[adjustedTintIndex];
			}
		}

		return -1;
	}

	private static ItemStack getDisplayItemWithIndex(int displayItemIndex, List<RenderData.DisplayItemData> displayItems, boolean isLimitedBarrel) {
		if (isLimitedBarrel) {
			for (RenderData.DisplayItemData displayItem : displayItems) {
				if (displayItem.slotIndex() == displayItemIndex) {
					return displayItem.item();
				}
			}
		}
		return displayItems.size() > displayItemIndex ? displayItems.get(displayItemIndex).item() : ItemStack.EMPTY;
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
