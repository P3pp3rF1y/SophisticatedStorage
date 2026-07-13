package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
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
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StorageBlockPreviewRenderer {
	private StorageBlockPreviewRenderer() {}

	public static void submitStorageBlock(StorageBlockEntity renderBlockEntity, float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			int packedLight) {
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
			List<BlockModelPart> translucentParts = new ArrayList<>();
			Iterator<BlockModelPart> iterator = parts.iterator();
			while (iterator.hasNext()) {
				BlockModelPart part = iterator.next();
				if (part.getRenderType(state) == ChunkSectionLayer.TRANSLUCENT) {
					translucentParts.add(part);
					iterator.remove();
				}
			}
			submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, vertexConsumer) -> {
				for (BlockModelPart part : parts) {
					renderBlockModelPart(packedLight, pose, vertexConsumer, part, state, wrappedLevel);
				}
			});
			if (!translucentParts.isEmpty()) {
				submitNodeCollector.submitCustomGeometry(poseStack, RenderTypeHelper.getEntityRenderType(ChunkSectionLayer.TRANSLUCENT), (pose, vertexConsumer) -> {
					for (BlockModelPart part : translucentParts) {
						renderBlockModelPart(packedLight, pose, vertexConsumer, part, state, wrappedLevel);
					}
				});
			}
		}

		BlockEntityRenderer<StorageBlockEntity, ? extends BlockEntityRenderState> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(renderBlockEntity);
		if (renderer != null) {
			submitBlockEntityRender(renderer, renderBlockEntity, state, partialTicks, poseStack, submitNodeCollector, packedLight);
		}
	}

	private static void renderBlockModelPart(int packedLight, PoseStack.Pose pose, VertexConsumer vertexConsumer, BlockModelPart part, BlockState state,
			BlockAndTintGetter wrappedLevel) {
		for (Direction direction : Direction.values()) {
			renderBlockModelPartQuads(packedLight, pose, vertexConsumer, part, direction, state, wrappedLevel);
		}
		renderBlockModelPartQuads(packedLight, pose, vertexConsumer, part, null, state, wrappedLevel);
	}

	private static void renderBlockModelPartQuads(int packedLight, PoseStack.Pose pose, VertexConsumer vertexConsumer, BlockModelPart part,
			@Nullable Direction direction, BlockState state, BlockAndTintGetter wrappedLevel) {
		for (BakedQuad quad : part.getQuads(direction)) {
			float red = 1.0F;
			float green = 1.0F;
			float blue = 1.0F;
			if (quad.isTinted()) {
				int tint = Minecraft.getInstance().getBlockColors().getColor(state, wrappedLevel, BlockPos.ZERO, quad.tintIndex());
				red = ARGB.redFloat(tint);
				green = ARGB.greenFloat(tint);
				blue = ARGB.blueFloat(tint);
			}
			vertexConsumer.putBulkData(pose, quad, red, green, blue, 1, packedLight, OverlayTexture.NO_OVERLAY);
		}
	}

	private static <T extends BlockEntity, S extends BlockEntityRenderState> void submitBlockEntityRender(BlockEntityRenderer<T, S> renderer, T blockEntity,
			BlockState previewState, float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
		S renderState = renderer.createRenderState();
		renderer.extractRenderState(blockEntity, renderState, partialTicks, Vec3.ZERO, null);
		renderState.lightCoords = packedLight;
		if (renderState instanceof ChestRenderer.ChestRenderState chestRenderState) {
			chestRenderState.open = 0;
		} else if (renderState instanceof ShulkerBoxRenderer.ShulkerBoxRenderState shulkerBoxRenderState) {
			shulkerBoxRenderState.lidProgress = 0;
			if (previewState.getBlock() instanceof ShulkerBoxBlock) {
				shulkerBoxRenderState.facing = previewState.getValue(ShulkerBoxBlock.FACING);
			}
		}
		renderer.submit(renderState, poseStack, submitNodeCollector, RenderHelper.ZERO_POS_CAMERA_RENDER_STATE);
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

		@Override
		@Nullable
		public BlockEntity getBlockEntity(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockEntity : level == null ? null : level.getBlockEntity(blockPos);
		}

		@Override
		public BlockState getBlockState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockState : level == null ? blockState : level.getBlockState(blockPos);
		}

		@Override
		public FluidState getFluidState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockState.getFluidState() : level == null ? blockEntity.getBlockState().getFluidState() : level.getFluidState(blockPos);
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
