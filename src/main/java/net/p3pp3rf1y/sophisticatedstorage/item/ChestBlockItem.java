package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

public class ChestBlockItem extends WoodStorageBlockItem {
	public ChestBlockItem(Block block) {
		this(block, new Properties());
	}

	public ChestBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static boolean isDoubleChest(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.DOUBLE_CHEST, false);
	}

	public static void setDoubleChest(ItemStack stack, boolean doubleChest) {
		stack.set(ModDataComponents.DOUBLE_CHEST, doubleChest);
	}
}
