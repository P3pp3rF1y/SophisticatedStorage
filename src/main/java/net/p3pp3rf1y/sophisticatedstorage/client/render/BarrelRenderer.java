package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

import java.util.List;
import java.util.Random;

public class BarrelRenderer<T extends BarrelBlockEntity> extends StorageRenderer<T> {
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	private final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	@Override
	public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		boolean flatTop = Boolean.TRUE.equals(blockEntity.getBlockState().getValue(BarrelBlock.FLAT_TOP));
		if (blockEntity.isPacked()) {
			return;
		}

		renderFrontFace(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, flatTop);
		renderHiddenTier(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
		renderHiddenLock(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	private void renderFrontFace(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop) {
		if ((!blockEntity.hasDynamicRenderer() && !holdsItemThatShowsUpgrades() && !blockEntity.shouldShowUpgrades())) {
			return;
		}

		BlockState blockState = blockEntity.getBlockState();
		Direction facing = blockState.getValue(BarrelBlock.FACING);

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(facing));
		poseStack.translate(-0.5, -0.5, -(0.5 - (flatTop ? 0 : 1 / 16f)));

		boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();
		if (blockEntity.shouldShowUpgrades() || holdsItemThatShowsUpgrades) {
			if (flatTop) {
				flatDisplayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades, shouldShowDisabledUpgradesDisplay(blockEntity));
			} else {
				displayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(blockEntity));
			}
		}

		if (blockEntity.hasDynamicRenderer()) {
			if (flatTop) {
				flatDisplayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
			} else {
				displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
			}
		}

		poseStack.popPose();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}

	protected void renderHiddenTier(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.shouldShowTier() && holdsItemThatShowsHiddenTiers()) {
			renderTranslucentQuads(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, BarrelBakedModelBase::getTierQuads);
		}
	}

	private void renderHiddenLock(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.shouldShowLock() && blockEntity.isLocked() && holdsToolInToggleLockOrLockDisplay()) {
			renderTranslucentQuads(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, BarrelBakedModelBase::getLockQuads);
		}
	}

	private void renderTranslucentQuads(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, GetQuadsFunction getQuads) {
		String woodName = blockEntity.getWoodType().orElse(WoodType.ACACIA).name();
		BlockState state = blockEntity.getBlockState();
		BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);

		poseStack.pushPose();
		poseStack.translate(-0.005, -0.005, -0.005);
		poseStack.scale(1.01f, 1.01f, 1.01f);

		if (blockEntity.getLevel() != null && blockModel instanceof BarrelBakedModelBase barrelBakedModel) {
			TranslucentVertexConsumer vertexConsumer = new TranslucentVertexConsumer(bufferSource, 128);
			getQuads.apply(barrelBakedModel, state, blockEntity.getLevel().random, woodName).forEach(quad -> vertexConsumer.putBulkData(poseStack.last(), quad, 1, 1, 1, packedLight, packedOverlay));
			barrelBakedModel.getTierQuads(state, blockEntity.getLevel().random, woodName).forEach(quad -> vertexConsumer.putBulkData(poseStack.last(), quad, 1, 1, 1, packedLight, packedOverlay));
		}
		poseStack.popPose();
	}
	private interface GetQuadsFunction {
		List<BakedQuad> apply(BarrelBakedModelBase model, BlockState state, Random rand, String woodName);

	}
}
