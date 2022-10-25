package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;

import java.util.List;

public class LimitedBarrelRenderer implements BlockEntityRenderer<LimitedBarrelBlockEntity> {

	private static final double BLOCK_SIDE_OFFSET = 0.5;
	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 128f;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 64f;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, BLOCK_SIDE_OFFSET) {
		@Override
		protected void rotateToFront(PoseStack poseStack, BlockState state, Direction facing) {
			poseStack.mulPose(getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
			LimitedBarrelBlock.VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != LimitedBarrelBlock.VerticalFacing.NO) {
				poseStack.mulPose(getNorthBasedRotation(verticalFacing.getDirection()));
			}
		}

		@Override
		protected void rotateFrontOffset(BlockState state, Direction facing, Vector3f frontOffset) {
			LimitedBarrelBlock.VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != LimitedBarrelBlock.VerticalFacing.NO) {
				frontOffset.transform(getNorthBasedRotation(verticalFacing.getDirection()));
			}
			frontOffset.transform(getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
		}
	};

	@Override
	public void render(LimitedBarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (blockEntity.isPacked()) {
			return;
		}
		renderItemCounts(blockEntity, poseStack);

		if (blockEntity.hasDynamicRenderer()) {
			displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
		}
	}

	private void renderItemCounts(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack) {
		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase)) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<Integer> slotCounts = blockEntity.getSlotCounts();
		float countDisplayYOffset = -(slotCounts.size() == 1 ? 0.27f : 0.132f);

		Direction horizontalFacing = blockState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING);

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(horizontalFacing.getOpposite()));// because of the font flipping
		LimitedBarrelBlock.VerticalFacing verticalFacing = blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING);
		if (verticalFacing != LimitedBarrelBlock.VerticalFacing.NO) {
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(verticalFacing.getDirection().getOpposite()));// because of the font flipping
		}
		poseStack.translate(0.5, -0.5, 0.5);

		for (int displayItemIndex = 0; displayItemIndex < slotCounts.size(); displayItemIndex++) {
			int count = slotCounts.get(displayItemIndex);
			if (count <= 0) {
				continue;
			}

			poseStack.pushPose();
			Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(displayItemIndex, slotCounts.size());

			double xTranslation = - frontOffset.x();
			float yTranslation = frontOffset.y() + countDisplayYOffset;
			double zTranslation = 0.001;
			poseStack.translate(xTranslation, yTranslation, zTranslation);

			float scale = slotCounts.size() == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE;
			poseStack.scale(scale, -scale, scale);
			String countString = CountAbbreviator.abbreviate(count);
			float countDisplayXOffset = -font.width(countString) / 2f;
			poseStack.translate(countDisplayXOffset, 0, 0);
			font.draw(poseStack, countString, 0, 0, DyeColor.LIGHT_GRAY.getTextColor());

			poseStack.popPose();
		}
		poseStack.popPose();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}
}
