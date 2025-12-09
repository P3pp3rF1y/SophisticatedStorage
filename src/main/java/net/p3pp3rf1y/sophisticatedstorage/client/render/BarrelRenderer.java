package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

import javax.annotation.Nullable;

public class BarrelRenderer<T extends BarrelBlockEntity> extends BarrelRendererBase<T, BarrelRenderer.BarrelRenderState> {
	public BarrelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	private void submitFrontFace(SubmitNodeCollector submitNodeCollector, BarrelRenderState renderState, PoseStack poseStack) {
		if ((!renderState.hasDynamicRenderer && !holdsItemThatShowsUpgrades() && !renderState.showsUpgrades)) {
			return;
		}

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(renderState.facing));
		poseStack.translate(-0.5, -0.5, -(0.5 - (renderState.flatTop ? 0 : 1 / 16f)));

		boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();
		if (renderState.showsUpgrades || holdsItemThatShowsUpgrades) {
			if (renderState.flatTop) {
				flatDisplayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades);
			} else {
				displayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades());
			}
		}

		if (renderState.hasDynamicRenderer) {
			if (renderState.flatTop) {
				flatDisplayItemRenderer.submitDisplayItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, !renderState.hasFullyDynamicRenderer);
			} else {
				displayItemRenderer.submitDisplayItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, !renderState.hasFullyDynamicRenderer);
			}
		}

		poseStack.popPose();
	}

	@Override
	public BarrelRenderState createRenderState() {
		return new BarrelRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, BarrelRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		if (renderState.packed) {
			return;
		}

		BlockState blockState = blockEntity.getBlockState();
		renderState.facing = blockState.getValue(BarrelBlock.FACING);
	}

	@Override
	public void submit(BarrelRenderState barrelRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		if (barrelRenderState.packed) {
			return;
		}
		submitFrontFace(submitNodeCollector, barrelRenderState, poseStack);
		submitHiddenTier(submitNodeCollector, barrelRenderState, poseStack);
		submitHiddenLock(submitNodeCollector, barrelRenderState, poseStack);
	}

	public static class BarrelRenderState extends BarrelRenderStateBase {
		public Direction facing;
	}
}
