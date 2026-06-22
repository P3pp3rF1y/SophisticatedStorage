package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DecorationTableInputSlotPreview {
	private static final List<Supplier<ItemStack>> PREVIEW_STACKS = new ArrayList<>();

	static {
		registerPreviewStack(() -> new ItemStack(ModBlocks.BARREL_ITEM.get()));
		registerPreviewStack(() -> new ItemStack(ModBlocks.CHEST_ITEM.get()));
		registerPreviewStack(() -> new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
		registerPreviewStack(() -> new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
		registerPreviewStack(() -> new ItemStack(Items.LEATHER_CHESTPLATE));
	}

	private DecorationTableInputSlotPreview() {
	}

	public static void registerPreviewStack(Supplier<ItemStack> stackSupplier) {
		PREVIEW_STACKS.add(stackSupplier);
	}

	public static ItemStack getPreviewStack(long currentTime, long rotationIntervalMs) {
		List<ItemStack> stacks = PREVIEW_STACKS.stream().map(Supplier::get).filter(stack -> !stack.isEmpty()).toList();
		if (stacks.isEmpty()) {
			return ItemStack.EMPTY;
		}

		return stacks.get((int) (currentTime / rotationIntervalMs % stacks.size()));
	}
}
