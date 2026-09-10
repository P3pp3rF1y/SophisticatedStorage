package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedstorage.compat.compressium.CompressiumDisplayModel;

public class BarrelDisplayItem {
	private BarrelDisplayItem() {}

	public static BakedModel getModel(ItemStack stack, BakedModel model) {
		return CompressiumDisplayModel.wrap(stack, SporeBlossomDisplayModel.wrap(stack, model));
	}

	public static double getOffset(ItemStack stack, BakedModel model, float scale) {
		double offset = DisplayItemRenderer.getDisplayItemOffset(stack, model, scale);
		if (stack.is(Items.BIG_DRIPLEAF)) {
			return offset + 6 / 16D * scale;
		}
		if (stack.is(Items.SMALL_DRIPLEAF)) {
			return offset + 4 / 16D * scale;
		}
		return offset;
	}
}
