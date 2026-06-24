package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import javax.annotation.Nullable;

import java.util.Optional;

public class SimpleMaterialBlockItem extends BlockItemBase {
	public SimpleMaterialBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<ResourceLocation> getMaterial(ItemStack stack) {
		return Optional.ofNullable(stack.get(ModDataComponents.SIMPLE_MATERIAL));
	}

	public static void setMaterial(ItemStack stack, ResourceLocation material) {
		stack.set(ModDataComponents.SIMPLE_MATERIAL, material);
	}

	public static void removeMaterial(ItemStack stack) {
		stack.remove(ModDataComponents.SIMPLE_MATERIAL);
	}

	public static boolean materialMatches(ItemStack stack, @Nullable ResourceLocation material) {
		return getMaterial(stack).map(m -> m.equals(material)).orElse(material == null);
	}
}
