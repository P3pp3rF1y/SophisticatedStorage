package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PackingTapeItem extends ItemBase {
	private final boolean showFoil;

	public PackingTapeItem(int durability, boolean showFoil) {
		super(new Properties().durability(durability));
		this.showFoil = showFoil;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return showFoil;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
		super.appendHoverText(stack, level, tooltip, isAdvanced);
		if (Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
			tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()) + ".disabled").withStyle(ChatFormatting.RED));
		} else {
			tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()),
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
