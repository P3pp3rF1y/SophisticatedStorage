package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;

import java.util.List;

import static net.minecraft.client.Minecraft.DEFAULT_FONT;
import static net.minecraft.client.Minecraft.UNIFORM_FONT;

public class LimitedBarrelRenderer implements BlockEntityRenderer<LimitedBarrelBlockEntity> {

	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96f;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48f;
	private final DisplayItemRenderer displayItemRenderer = new LimitedBarreDisplayItemRenderer(0.5 - 1 / 16D);
	private final DisplayItemRenderer flatDisplayItemRenderer = new LimitedBarreDisplayItemRenderer(0.5);

	private static final Font font = new Font(rl -> {
		Minecraft mc = Minecraft.getInstance();
		return mc.fontManager.fontSets.getOrDefault(UNIFORM_FONT, mc.fontManager.fontSets.getOrDefault(DEFAULT_FONT, mc.fontManager.missingFontSet));
	}, false);

	@Override
	public void render(LimitedBarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (blockEntity.isPacked()) {
			return;
		}
		boolean flatTop = blockEntity.getBlockState().getValue(BarrelBlock.FLAT_TOP);

		renderItemCounts(blockEntity, poseStack, bufferSource, packedLight, flatTop);

		if (blockEntity.hasDynamicRenderer()) {
			if (flatTop) {
				flatDisplayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
			} else {
				displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
			}
		}
	}

	private void renderItemCounts(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean flatTop) {
		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase) || !blockEntity.shouldShowCounts()) {
			return;
		}
		List<Integer> slotCounts = blockEntity.getSlotCounts();
		float countDisplayYOffset = -(slotCounts.size() == 1 ? 0.25f : 0.11f);

		Direction horizontalFacing = blockState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING);

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(horizontalFacing.getOpposite()));// because of the font flipping
		VerticalFacing verticalFacing = blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING);
		if (verticalFacing != VerticalFacing.NO) {
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

			double xTranslation = -frontOffset.x();
			float yTranslation = frontOffset.y() + countDisplayYOffset;
			double zTranslation = 0.001 - (flatTop ? 0 : 0.75 / 16D);
			poseStack.translate(xTranslation, yTranslation, zTranslation);

			float scale = slotCounts.size() == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE;
			poseStack.scale(scale, -scale, scale);
			String countString = CountAbbreviator.abbreviate(count, slotCounts.size() == 1 ? 6 : 5);
			float countDisplayXOffset = -font.width(countString) / 2f;
			poseStack.translate(countDisplayXOffset, 0, 0);
			font.drawInBatch(Component.literal(countString).withStyle(ChatFormatting.BOLD), 0, 0, blockEntity.getSlotColor(displayItemIndex), false, poseStack.last().pose(), bufferSource, false, 0, packedLight);

			poseStack.popPose();
		}
		poseStack.popPose();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}

	private static class LimitedBarreDisplayItemRenderer extends DisplayItemRenderer {
		public LimitedBarreDisplayItemRenderer(double blockSideOffset) {
			super(0.5, blockSideOffset);
		}

		@Override
		protected void rotateToFront(PoseStack poseStack, BlockState state, Direction facing) {
			poseStack.mulPose(getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				poseStack.mulPose(getNorthBasedRotation(verticalFacing.getDirection()));
			}
		}

		@Override
		protected void rotateFrontOffset(BlockState state, Direction facing, Vector3f frontOffset) {
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				frontOffset.transform(getNorthBasedRotation(verticalFacing.getDirection()));
			}
			frontOffset.transform(getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
		}
	}
}
