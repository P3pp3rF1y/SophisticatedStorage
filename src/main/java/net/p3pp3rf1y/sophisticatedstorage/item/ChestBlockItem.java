package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestItemRenderer;

import java.util.function.Consumer;

public class ChestBlockItem extends WoodStorageBlockItem {
	private static final String DOUBLE_CHEST_TAG = "doubleChest";
	public ChestBlockItem(Block block) {
		this(block, new Properties());
	}
	public ChestBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(ChestItemRenderer.getItemRenderProperties());
	}

	public static boolean isDoubleChest(ItemStack stack) {
		return NBTHelper.getBoolean(stack, DOUBLE_CHEST_TAG).orElse(false);
	}

	public static void setDoubleChest(ItemStack stack, boolean doubleChest) {
		stack.getOrCreateTag().putBoolean(DOUBLE_CHEST_TAG, doubleChest);
	}
}
