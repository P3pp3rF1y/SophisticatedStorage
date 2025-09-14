package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.function.Consumer;

public class PackingTapeItem extends ItemBase {
	private final boolean showFoil;

	public PackingTapeItem(Properties properties, int durability, boolean showFoil) {
		super(properties.durability(durability));
		this.showFoil = showFoil;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return showFoil;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
		if (Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
			tooltipAdder.accept(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()) + ".disabled").withStyle(ChatFormatting.RED));
		} else {
			tooltipAdder.accept(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()),
							Component.literal(String.valueOf(getMaxDamage(stack) - getDamage(stack))).withStyle(ChatFormatting.GREEN)
					).withStyle(ChatFormatting.DARK_GRAY)
			);
		}
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (!Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
			super.addCreativeTabItems(itemConsumer);
		}
	}
}
