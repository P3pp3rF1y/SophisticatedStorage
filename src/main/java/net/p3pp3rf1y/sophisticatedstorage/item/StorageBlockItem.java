package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;

import java.util.Optional;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {
	public StorageBlockItem(Block block) {
		super(block, new Properties(), SophisticatedStorage.CREATIVE_TAB);
	}

	public static Optional<Integer> getMaincolorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "mainColor");
	}

	public static Optional<Integer> getAccentColorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "accentColor");
	}

	@Override
	public void setMainColor(ItemStack storageStack, int mainColor) {
		storageStack.getOrCreateTag().putInt("mainColor", mainColor);
	}

	@Override
	public Optional<Integer> getMainColor(ItemStack storageStack) {
		return StorageBlockItem.getMaincolorFromStack(storageStack);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		storageStack.getOrCreateTag().putInt("accentColor", accentColor);
	}

	@Override
	public Optional<Integer> getAccentColor(ItemStack stack) {
		return StorageBlockItem.getAccentColorFromStack(stack);
	}
}
