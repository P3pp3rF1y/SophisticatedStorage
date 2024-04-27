package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
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
					if (tintIndex == 1000) {
						return be.getStorageWrapper().getMainColor();
					} else if (tintIndex == 1001) {
						return be.getStorageWrapper().getAccentColor();
					} else {
						RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo();
						int displayItemIndex = (tintIndex > 1000 ? tintIndex - 1000 : tintIndex) / 10 - 1;
						List<RenderInfo.DisplayItem> displayItems = itemDisplayRenderInfo.getDisplayItems();
						if (displayItemIndex >= 0) {
							int tintOffset = (displayItemIndex + 1) * 10;
							ItemStack stack = getDisplayItemWithIndex(displayItemIndex, displayItems, state.getBlock() instanceof LimitedBarrelBlock);
							if (stack.isEmpty()) {
								return -1;
							}
							return Minecraft.getInstance().getItemColors().getColor(stack, tintIndex - tintOffset);
						}
					}
					return -1;
				})
				.orElse(-1);
	}

	private static ItemStack getDisplayItemWithIndex(int displayItemIndex, List<RenderInfo.DisplayItem> displayItems, boolean isLimitedBarrel) {
		if (isLimitedBarrel) {
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				if (displayItem.getSlotIndex() == displayItemIndex) {
					return displayItem.getItem();
				}
			}
		}
		return displayItems.size() > displayItemIndex ? displayItems.get(displayItemIndex).getItem() : ItemStack.EMPTY;
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
