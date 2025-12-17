package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class BarrelRendererBase<T extends BarrelBlockEntity, R extends BarrelRendererBase.BarrelRenderStateBase> extends StorageRenderer<T, R> {
	public static final RenderType TRANSLUCENT = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
	protected final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	protected final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	public BarrelRendererBase(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	protected void submitHiddenTier(SubmitNodeCollector submitNodeCollector, BarrelRenderStateBase renderState, PoseStack poseStack) {
		if (!renderState.showsTier && holdsItemThatShowsHiddenTiers()) {
			submitTranslucentQuads(submitNodeCollector, poseStack, renderState.lightCoords, renderState.hiddenTierQuads);
		}
	}

	protected void submitHiddenLock(SubmitNodeCollector submitNodeCollector, BarrelRenderStateBase renderState, PoseStack poseStack) {
		if (!renderState.showsLock && renderState.isLocked && holdsToolInToggleLockOrLockDisplay()) {
			submitTranslucentQuads(submitNodeCollector, poseStack, renderState.lightCoords, renderState.hiddenLockQuads);
		}
	}

	private void submitTranslucentQuads(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, List<BakedQuad> quads) {
		if (quads.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(-0.005, -0.005, -0.005);
		poseStack.scale(1.01f, 1.01f, 1.01f);

		submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, vertexConsumer) -> {
			quads.forEach(quad -> vertexConsumer.putBulkData(pose, quad, 1, 1, 1, 0.5f, packedLight, OverlayTexture.NO_OVERLAY, false));
		});
		poseStack.popPose();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}

	@Override
	public void extractRenderState(T blockEntity, R renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		renderState.packed = blockEntity.isPacked();
		if (renderState.packed) {
			return;
		}

		BlockPos pos = blockEntity.getBlockPos();
		BlockState blockState = blockEntity.getBlockState();
		if (blockState.getBlock() instanceof BarrelBlock storageBlock && blockEntity.getLevel() != null && pos != BlockPos.ZERO) {
			Direction facing = storageBlock.getFacing(blockState);
			renderState.lightCoords = LevelRenderer.getLightColor(blockEntity.getLevel(), pos.relative(facing));
		}
		renderState.flatTop = blockState.getValue(BarrelBlock.FLAT_TOP);

		renderState.woodName = blockEntity.getWoodType().orElse(WoodType.ACACIA).name();
		if (!renderState.showsTier && holdsItemThatShowsHiddenTiers()) {
			BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);

			if (blockModel instanceof BarrelBlockStateModelBase barrelBlockStateModel) {
				barrelBlockStateModel.setWoodName(renderState.woodName);
				renderState.hiddenTierQuads = barrelBlockStateModel.getTierQuads().getAll();
			}
		}
		if (!renderState.showsLock && holdsToolInToggleLockOrLockDisplay()) {
			BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);

			if (blockModel instanceof BarrelBlockStateModelBase barrelBlockStateModel) {
				barrelBlockStateModel.setWoodName(renderState.woodName);
				renderState.hiddenLockQuads = barrelBlockStateModel.getLockQuads().getAll();
			}
		}
	}

	public static class BarrelRenderStateBase extends StorageRenderState {
		public boolean flatTop;
		public boolean packed;
		public String woodName = WoodType.ACACIA.name();
		public List<BakedQuad> hiddenTierQuads = Collections.emptyList();
		public List<BakedQuad> hiddenLockQuads = Collections.emptyList();

	}
}
