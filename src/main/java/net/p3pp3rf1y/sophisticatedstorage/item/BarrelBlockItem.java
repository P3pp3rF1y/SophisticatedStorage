package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

public class BarrelBlockItem extends WoodStorageBlockItem {
	public BarrelBlockItem(Block block) {
		super(block);
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name = super.getName(stack);
		if (BarrelBlock.isFlatTop(stack)) {
			return name.copy().append(new TranslatableComponent(StorageTranslationHelper.INSTANCE.translBlockTooltipKey("barrel") + ".flat_top"));
		}
		return name;
	}
}
