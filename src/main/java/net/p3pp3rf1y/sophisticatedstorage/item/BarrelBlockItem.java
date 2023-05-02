package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

public class BarrelBlockItem extends WoodStorageBlockItem {
	private static final String FLAT_TOP_TAG = "flatTop";

	public BarrelBlockItem(Block block) {
		super(block);
	}

	public static void toggleFlatTop(ItemStack stack) {
		boolean flatTop = isFlatTop(stack);
		setFlatTop(stack, !flatTop);
	}

	public static void setFlatTop(ItemStack stack, boolean flatTop) {
		if (flatTop) {
			NBTHelper.setBoolean(stack, FLAT_TOP_TAG, true);
		} else {
			NBTHelper.removeTag(stack, FLAT_TOP_TAG);
		}
	}

	public static boolean isFlatTop(ItemStack stack) {
		return NBTHelper.getBoolean(stack, FLAT_TOP_TAG).orElse(false);
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name = super.getName(stack);
		if (isFlatTop(stack)) {
			return name.copy().append(Component.translatable(StorageTranslationHelper.INSTANCE.translBlockTooltipKey("barrel") + ".flat_top"));
		}
		return name;
	}
}
