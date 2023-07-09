package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;

import java.util.Optional;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

	private static final String ACCENT_COLOR_TAG = "accentColor";
	private static final String MAIN_COLOR_TAG = "mainColor";
	private static final String SHOWS_TIER_TAG = "showsTier";

	public StorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<Integer> getMainColorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, MAIN_COLOR_TAG);
	}

	public static Optional<Integer> getAccentColorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, ACCENT_COLOR_TAG);
	}

	@Override
	public void setMainColor(ItemStack storageStack, int mainColor) {
		storageStack.getOrCreateTag().putInt(MAIN_COLOR_TAG, mainColor);
	}

	@Override
	public Optional<Integer> getMainColor(ItemStack storageStack) {
		return StorageBlockItem.getMainColorFromStack(storageStack);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		storageStack.getOrCreateTag().putInt(ACCENT_COLOR_TAG, accentColor);
	}

	@Override
	public void removeMainColor(ItemStack stack) {
		NBTHelper.removeTag(stack, MAIN_COLOR_TAG);
	}

	@Override
	public void removeAccentColor(ItemStack stack) {
		NBTHelper.removeTag(stack, ACCENT_COLOR_TAG);
	}

	@Override
	public Optional<Integer> getAccentColor(ItemStack stack) {
		return StorageBlockItem.getAccentColorFromStack(stack);
	}

	public static boolean showsTier(ItemStack stack) {
		return NBTHelper.getBoolean(stack, SHOWS_TIER_TAG).orElse(true);
	}

	public static void setShowsTier(ItemStack stack, boolean showsTier) {
		if (showsTier) {
			NBTHelper.removeTag(stack, SHOWS_TIER_TAG);
		} else {
			stack.getOrCreateTag().putBoolean(SHOWS_TIER_TAG, false);
		}
	}
}
