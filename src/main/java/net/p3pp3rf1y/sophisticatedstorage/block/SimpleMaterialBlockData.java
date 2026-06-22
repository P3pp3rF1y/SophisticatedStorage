package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

import java.util.Optional;

public class SimpleMaterialBlockData {
	public static final BooleanProperty OPAQUE = BooleanProperty.create("opaque");

	private SimpleMaterialBlockData() {}

	public static void copyToBlockEntity(BlockGetter level, BlockPos pos, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).ifPresent(holder -> {
			holder.setMaterial(SimpleMaterialBlockItem.getMaterial(stack).orElse(null));
			holder.setOverlayHidden(false);
		});
	}

	public static void copyFromBlockEntity(BlockGetter level, BlockPos pos, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).ifPresent(holder ->
				holder.getMaterial().ifPresentOrElse(material -> SimpleMaterialBlockItem.setMaterial(stack, material), () -> SimpleMaterialBlockItem.removeMaterial(stack)));
	}

	public static boolean isOpaque(ItemStack stack) {
		return isOpaque(SimpleMaterialBlockItem.getMaterial(stack));
	}

	public static boolean isOpaque(Optional<ResourceLocation> material) {
		return material.map(SimpleMaterialBlockData::isMaterialOpaque).orElse(true);
	}

	public static void updateOpaqueState(BlockEntity blockEntity, Optional<ResourceLocation> material) {
		if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) {
			return;
		}

		BlockState state = blockEntity.getBlockState();
		if (!state.getProperties().contains(OPAQUE)) {
			return;
		}

		boolean opaque = isOpaque(material);
		if (state.getValue(OPAQUE) != opaque) {
			blockEntity.getLevel().setBlock(blockEntity.getBlockPos(), state.setValue(OPAQUE, opaque), 3);
		}
	}

	private static boolean isMaterialOpaque(ResourceLocation materialLocation) {
		return BuiltInRegistries.BLOCK.getOptional(materialLocation)
				.map(block -> block.defaultBlockState().canOcclude())
				.orElse(true);
	}
}
