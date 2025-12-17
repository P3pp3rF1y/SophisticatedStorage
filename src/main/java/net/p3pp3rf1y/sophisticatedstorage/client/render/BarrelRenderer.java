package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

public class BarrelRenderer<T extends BarrelBlockEntity> extends StorageRenderer<T> {
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	private final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	@Override
	public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		BlockState blockState = blockEntity.getBlockState();
		boolean flatTop = blockState.getValue(BarrelBlock.FLAT_TOP);
		if (blockEntity.isPacked() || !(blockState.getBlock() instanceof BarrelBlock storageBlock) || Minecraft.getInstance().player == null) {
			return;
		}
		BlockPos pos = blockEntity.getBlockPos();

		if (blockEntity.getLevel() != null && pos != BlockPos.ZERO) {
			packedLight = LevelRenderer.getLightColor(blockEntity.getLevel(), pos.relative(storageBlock.getFacing(blockState)));
		}

		renderFrontFace(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, flatTop, blockState);
		renderHiddenTier(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
		renderHiddenLock(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	private void renderFrontFace(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop, BlockState blockState) {
		if (hasNoDisplayItems(blockEntity) && !holdsItemThatShowsUpgrades() && !blockEntity.shouldShowUpgrades()) {
			return;
		}

		poseStack.pushPose();

		Direction facing = blockState.getValue(BarrelBlock.FACING);

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

		if (!hasNoDisplayItems(blockEntity)) {
			if (flatTop) {
				flatDisplayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
			} else {
				displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
			}
		}

		poseStack.popPose();
	}

	protected static <T extends BarrelBlockEntity> boolean hasNoDisplayItems(T blockEntity) {
		return blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().isEmpty();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}

	protected void renderHiddenTier(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.shouldShowTier() && holdsItemThatShowsHiddenTiers()) {
			renderTranslucentQuads(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, BarrelBlockStateModelBase::getTierQuads);
		}
	}

	protected void renderHiddenLock(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.shouldShowLock() && blockEntity.isLocked() && holdsToolInToggleLockOrLockDisplay()) {
			renderTranslucentQuads(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, BarrelBlockStateModelBase::getLockQuads);
		}
	}

	private void renderTranslucentQuads(T blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, GetQuadsFunction getQuads) {
		String woodName = blockEntity.getWoodType().orElse(WoodType.ACACIA).name();
		BlockState state = blockEntity.getBlockState();
		BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);

		poseStack.pushPose();
		poseStack.translate(-0.005, -0.005, -0.005);
		poseStack.scale(1.01f, 1.01f, 1.01f);

		if (blockModel instanceof BarrelBlockStateModelBase barrelBlockStateModel) {
			VertexConsumer vertexConsumer = TranslucentVertexConsumer.getVertexConsumer(bufferSource, 128);
			barrelBlockStateModel.setWoodName(woodName);
			getQuads.apply(barrelBlockStateModel).getAll().forEach(quad -> vertexConsumer.putBulkData(poseStack.last(), quad, 1, 1, 1, 1, packedLight, packedOverlay, false));
		}
		poseStack.popPose();
	}

	private interface GetQuadsFunction {
		QuadCollection apply(BarrelBlockStateModelBase model);

	}
}
