package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nullable;

import java.util.List;

public final class StorageBlockPreviewRenderer {
	private StorageBlockPreviewRenderer() {
	}

	public static void render(StorageBlockEntity renderBlockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			int packedOverlay) {
		BlockState state = getClosedPreviewState(renderBlockEntity.getBlockState());
		Minecraft minecraft = Minecraft.getInstance();
		if (renderBlockEntity instanceof BarrelBlockEntity barrel) {
			BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
			BlockStateModel blockStateModel = blockRenderer.getBlockModel(state);
			if (blockStateModel instanceof BarrelBlockStateModelBase barrelModel) {
				barrelModel.setModelPropertiesFromBlockEntity(barrel);
			}
			BlockAndTintGetter wrappedLevel = new SingleBlockEntityTintGetter(minecraft.level, renderBlockEntity, state, packedLight);
			List<BlockModelPart> parts = blockStateModel.collectParts(wrappedLevel, BlockPos.ZERO, state, RandomSource.create(42L));
			blockRenderer.getModelRenderer().tesselateWithoutAO(wrappedLevel, parts, state, BlockPos.ZERO, poseStack,
					renderType -> buffer.getBuffer(RenderTypeHelper.getEntityRenderType(renderType)), false, packedOverlay);
		}

		BlockEntityRenderer<?> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(renderBlockEntity);
		if (renderer instanceof ChestRenderer chestRenderer && renderBlockEntity instanceof ChestBlockEntity chestBlockEntity) {
			chestRenderer.render(chestBlockEntity, partialTicks, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, Vec3.ZERO, true);
		} else if (renderer instanceof ShulkerBoxRenderer shulkerBoxRenderer && renderBlockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
			shulkerBoxRenderer.render(shulkerBoxBlockEntity, partialTicks, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, Vec3.ZERO, true);
		} else if (renderer != null) {
			renderBlockEntity(renderer, renderBlockEntity, partialTicks, poseStack, buffer, packedLight);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> void renderBlockEntity(BlockEntityRenderer<?> renderer, T blockEntity, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		((BlockEntityRenderer<T>) renderer).render(blockEntity, partialTicks, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, Vec3.ZERO);
	}

	private static BlockState getClosedPreviewState(BlockState state) {
		return state.hasProperty(BarrelBlock.OPEN) ? state.setValue(BarrelBlock.OPEN, false) : state;
	}

	private static class SingleBlockEntityTintGetter implements BlockAndTintGetter {
		@Nullable
		private final BlockAndTintGetter level;
		private final BlockEntity blockEntity;
		private final BlockState blockState;
		private final int packedLight;

		private SingleBlockEntityTintGetter(@Nullable BlockAndTintGetter level, BlockEntity blockEntity, BlockState blockState, int packedLight) {
			this.level = level;
			this.blockEntity = blockEntity;
			this.blockState = blockState;
			this.packedLight = packedLight;
		}

		@Override
		public float getShade(Direction direction, boolean shade) {
			return 1F;
		}

		@Override
		public LevelLightEngine getLightEngine() {
			return level == null ? null : level.getLightEngine();
		}

		@Override
		public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
			return level == null ? 0xFF_FFFFFF : level.getBlockTint(blockPos, colorResolver);
		}

		@Nullable
		@Override
		public BlockEntity getBlockEntity(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockEntity : level == null ? null : level.getBlockEntity(blockPos);
		}

		@Override
		public BlockState getBlockState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockState : level == null ? blockState : level.getBlockState(blockPos);
		}

		@Override
		public FluidState getFluidState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos)
					? blockState.getFluidState()
					: level == null ? blockEntity.getBlockState().getFluidState() : level.getFluidState(blockPos);
		}

		@Override
		public int getHeight() {
			return level == null ? 0 : level.getHeight();
		}

		@Override
		public int getMinY() {
			return level == null ? 0 : level.getMinY();
		}

		@Override
		public int getLightEmission(BlockPos pos) {
			return 0;
		}

		@Override
		public int getBrightness(LightLayer lightType, BlockPos blockPos) {
			return lightType == LightLayer.SKY ? packedLight >> 20 : packedLight >> 4 & 15;
		}

		@Override
		public int getRawBrightness(BlockPos blockPos, int amount) {
			int skyValue = packedLight >> 20;
			int blockValue = packedLight >> 4 & 15;
			return Math.max(blockValue, skyValue - amount);
		}
	}
}
