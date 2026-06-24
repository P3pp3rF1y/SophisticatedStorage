package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;

import java.util.Optional;

public class SimpleMaterialBlockItem extends BlockItemBase {
	private static final String MATERIAL_TAG = "simpleMaterial";

	public SimpleMaterialBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<ResourceLocation> getMaterial(ItemStack stack) {
		return NBTHelper.getString(stack, MATERIAL_TAG).map(ResourceLocation::new);
	}

	public static void setMaterial(ItemStack stack, ResourceLocation material) {
		stack.getOrCreateTag().putString(MATERIAL_TAG, material.toString());
	}

	public static void removeMaterial(ItemStack stack) {
		NBTHelper.removeTag(stack, MATERIAL_TAG);
	}

	public static boolean materialMatches(ItemStack stack, @Nullable ResourceLocation material) {
		return getMaterial(stack).map(m -> m.equals(material)).orElse(material == null);
	}
}
