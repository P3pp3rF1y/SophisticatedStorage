package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.joml.Vector3f;

import java.util.List;

import static net.minecraft.client.Minecraft.UNIFORM_FONT;

public class LimitedBarrelRenderer extends BarrelRenderer<LimitedBarrelBlockEntity> {

	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96f;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48f;
	private static final Style COUNT_DISPLAY_STYLE = Style.EMPTY.withFont(UNIFORM_FONT).withBold(true);
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	private final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	@Override
	public void render(LimitedBarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		BlockState blockState = blockEntity.getBlockState();
		if (blockEntity.isPacked() || !(blockState.getBlock() instanceof StorageBlockBase)
				|| (!blockEntity.hasDynamicRenderer() && !blockEntity.shouldShowCounts() && !holdsItemThatShowsUpgrades() && !blockEntity.shouldShowUpgrades())) {
			return;
		}
		boolean flatTop = blockState.getValue(BarrelBlock.FLAT_TOP);

		Direction horizontalFacing = blockState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING);
		renderItemCounts(blockEntity, poseStack, bufferSource, packedLight, flatTop, horizontalFacing, blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING));

		renderFrontFace(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, blockState, flatTop, horizontalFacing);
		renderHiddenTier(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
		renderHiddenLock(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	private void renderFrontFace(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState blockState, boolean flatTop, Direction horizontalFacing) {
		if (blockEntity.hasDynamicRenderer() || holdsItemThatShowsUpgrades() || blockEntity.shouldShowUpgrades()) {
			poseStack.pushPose();

			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(horizontalFacing));
			VerticalFacing verticalFacing = blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(verticalFacing.getDirection()));
			}
			poseStack.translate(-0.5, -0.5, -(0.5 - (flatTop ? 0 : 1 / 16f)));

			if (blockEntity.hasDynamicRenderer()) {
				renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, flatTop);
			}

			boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();
			if (blockEntity.shouldShowUpgrades() || holdsItemThatShowsUpgrades) {
				renderUpgrades(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, flatTop, holdsItemThatShowsUpgrades);
			}

			poseStack.popPose();
		}
	}

	private void renderUpgrades(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop, boolean holdsItemThatShowsUpgrades) {
		if (flatTop) {
			flatDisplayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades, shouldShowDisabledUpgradesDisplay(blockEntity));
		} else {
			displayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(blockEntity));
		}
	}

	private void renderDisplayItems(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop) {
		if (flatTop) {
			flatDisplayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
		} else {
			displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
		}
	}

	private void renderItemCounts(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean flatTop, Direction horizontalFacing, VerticalFacing verticalFacing) {
		if (!blockEntity.shouldShowCounts()) {
			return;
		}

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(horizontalFacing.getOpposite()));// because of the font flipping
		if (verticalFacing != VerticalFacing.NO) {
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(verticalFacing.getDirection().getOpposite()));// because of the font flipping
		}
		poseStack.translate(0.5, -0.5, 0.5);

		List<Integer> slotCounts = blockEntity.getSlotCounts();
		float countDisplayYOffset = -(slotCounts.size() == 1 ? 0.25f : 0.11f);
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
			MutableComponent countString = Component.literal(CountAbbreviator.abbreviate(count, slotCounts.size() == 1 ? 6 : 5)).withStyle(COUNT_DISPLAY_STYLE);
			Font font = Minecraft.getInstance().font;
			float countDisplayXOffset = -font.getSplitter().stringWidth(countString) / 2f;
			poseStack.translate(countDisplayXOffset, 0, 0);
			font.drawInBatch(countString, 0, 0, blockEntity.getSlotColor(displayItemIndex), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);

			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
