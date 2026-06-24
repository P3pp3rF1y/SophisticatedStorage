package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.function.Consumer;

public class LimitedBarrelBlockItem extends BarrelBlockItem {
	public LimitedBarrelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder,
			TooltipFlag tooltipFlag) {
		if (getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock) {
			int numberOfInventorySlots = limitedBarrelBlock.getNumberOfInventorySlots();
			String translationKey = numberOfInventorySlots == 1 ? "limited_barrel_singular" : "limited_barrel_plural";
			tooltipAdder.accept(
					Component.translatable(StorageTranslationHelper.INSTANCE.translBlockTooltipKey(translationKey), String.valueOf(numberOfInventorySlots),
							String.valueOf(limitedBarrelBlock.getBaseStackSizeMultiplier())).withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
