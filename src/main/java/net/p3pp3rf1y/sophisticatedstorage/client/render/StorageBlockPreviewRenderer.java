package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StorageBlockPreviewRenderer {
	private StorageBlockPreviewRenderer() {
	}

	public static void submitStorageBlock(StorageBlockEntity renderBlockEntity, float partialTicks, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int packedLight) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockState state = getClosedPreviewState(renderBlockEntity.getBlockState());
		if (renderBlockEntity instanceof BarrelBlockEntity barrel && minecraft.level != null) {
			BlockStateModelSet blockModelSet = minecraft.getModelManager().getBlockStateModelSet();
			BlockStateModel blockStateModel = blockModelSet.get(state);
			if (blockStateModel instanceof BarrelBlockStateModelBase barrelModel) {
				barrelModel.setModelPropertiesFromBlockEntity(barrel);
			}
			SingleBlockEntityTintGetter wrappedLevel = new SingleBlockEntityTintGetter(minecraft.level, renderBlockEntity, state, packedLight);
			List<BlockStateModelPart> parts = new ArrayList<>();
			blockStateModel.collectParts(wrappedLevel, BlockPos.ZERO, state, RandomSource.create(42L), parts);
			submitModelParts(poseStack, submitNodeCollector, state, wrappedLevel, parts, packedLight);
		}

		BlockEntityRenderer<StorageBlockEntity, ? extends BlockEntityRenderState> renderer = minecraft.getBlockEntityRenderDispatcher()
				.getRenderer(renderBlockEntity);
		if (renderer != null) {
			submitBlockEntityRender(renderer, renderBlockEntity, state, partialTicks, poseStack, submitNodeCollector, packedLight);
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

	private static void submitModelParts(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, BlockState state,
			SingleBlockEntityTintGetter wrappedLevel, List<BlockStateModelPart> parts, int packedLight) {
		List<BakedQuad> cutoutQuads = new ArrayList<>();
		List<BakedQuad> translucentQuads = new ArrayList<>();
		for (BlockStateModelPart part : parts) {
			collectPartQuads(part, cutoutQuads, translucentQuads);
		}

		if (!cutoutQuads.isEmpty()) {
			submitQuads(poseStack, submitNodeCollector, state, wrappedLevel, packedLight, cutoutQuads, Sheets.cutoutBlockItemSheet());
		}
		if (!translucentQuads.isEmpty()) {
			submitQuads(poseStack, submitNodeCollector, state, wrappedLevel, packedLight, translucentQuads, Sheets.translucentBlockItemSheet());
		}
	}

	private static void collectPartQuads(BlockStateModelPart part, List<BakedQuad> cutoutQuads, List<BakedQuad> translucentQuads) {
		for (Direction direction : Direction.values()) {
			for (BakedQuad quad : part.getQuads(direction)) {
				(quad.materialInfo().layer().translucent() ? translucentQuads : cutoutQuads).add(quad);
			}
		}
		for (BakedQuad quad : part.getQuads(null)) {
			(quad.materialInfo().layer().translucent() ? translucentQuads : cutoutQuads).add(quad);
		}
	}

	private static void submitQuads(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, BlockState state, SingleBlockEntityTintGetter wrappedLevel,
			int packedLight, List<BakedQuad> quads, RenderType renderType) {
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			QuadInstance quadInstance = new QuadInstance();
			quadInstance.setLightCoords(packedLight);
			quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
			for (BakedQuad quad : quads) {
				int tintIndex = quad.materialInfo().tintIndex();
				quadInstance.setColor(getTintColor(state, wrappedLevel, tintIndex));
				vertexConsumer.putBakedQuad(pose, quad, quadInstance);
			}
		});
	}

	private static int getTintColor(BlockState state, SingleBlockEntityTintGetter wrappedLevel, int tintIndex) {
		if (tintIndex == -1) {
			return -1;
		}
		BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(state, tintIndex);
		return tintSource == null ? -1 : tintSource.colorInWorld(state, wrappedLevel, BlockPos.ZERO);
	}

	private static BlockState getClosedPreviewState(BlockState state) {
		return state.hasProperty(BarrelBlock.OPEN) ? state.setValue(BarrelBlock.OPEN, false) : state;
	}

	private static class SingleBlockEntityTintGetter implements BlockAndTintGetter {
		private final BlockAndTintGetter level;
		private final BlockEntity blockEntity;
		private final BlockState blockState;
		private final int packedLight;

		private SingleBlockEntityTintGetter(BlockAndTintGetter level, BlockEntity blockEntity, BlockState blockState, int packedLight) {
			this.level = level;
			this.blockEntity = blockEntity;
			this.blockState = blockState;
			this.packedLight = packedLight;
		}

		@Override
		public CardinalLighting cardinalLighting() {
			return new CardinalLighting(1F, 1F, 1F, 1F, 1F, 1F);
		}

		@Override
		public LevelLightEngine getLightEngine() {
			return level.getLightEngine();
		}

		@Override
		public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
			return level.getBlockTint(blockPos, colorResolver);
		}

		@Override
		public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockEntity : level.getBlockEntity(blockPos);
		}

		@Override
		public BlockState getBlockState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockState : level.getBlockState(blockPos);
		}

		@Override
		public FluidState getFluidState(BlockPos blockPos) {
			return BlockPos.ZERO.equals(blockPos) ? blockState.getFluidState() : level.getFluidState(blockPos);
		}

		@Override
		public int getHeight() {
			return level.getHeight();
		}

		@Override
		public int getMinY() {
			return level.getMinY();
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
