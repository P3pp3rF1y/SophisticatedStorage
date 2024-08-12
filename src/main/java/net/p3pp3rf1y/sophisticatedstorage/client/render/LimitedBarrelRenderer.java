package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import org.joml.Vector3f;

import java.util.List;

import static net.minecraft.client.Minecraft.UNIFORM_FONT;

public class LimitedBarrelRenderer extends BarrelRenderer<LimitedBarrelBlockEntity> {

	public static final Material FILL_INDICATORS_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("block/fill_indicators"));
	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96f;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48f;
	private static final Style COUNT_DISPLAY_STYLE = Style.EMPTY.withFont(UNIFORM_FONT).withBold(true);
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	private final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	@Override
	public void render(LimitedBarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		BlockState blockState = blockEntity.getBlockState();
		if (blockEntity.isPacked() || !(blockState.getBlock() instanceof StorageBlockBase storageBlock)
				|| (!blockEntity.hasDynamicRenderer() && !blockEntity.shouldShowCounts() && !holdsItemThatShowsUpgrades() && !blockEntity.shouldShowUpgrades())) {
			return;
		}
		boolean flatTop = blockState.getValue(BarrelBlock.FLAT_TOP);

		Direction horizontalFacing = blockState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING);
		renderItemCounts(blockEntity, poseStack, bufferSource, flatTop, horizontalFacing, blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING));

		packedLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().relative(storageBlock.getFacing(blockState)));

		renderFrontFace(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, blockState, flatTop, horizontalFacing);
		renderHiddenTier(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
		renderHiddenLock(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	private void renderFrontFace(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState blockState, boolean flatTop, Direction horizontalFacing) {
		if (blockEntity.hasDynamicRenderer() || holdsItemThatShowsUpgrades() || blockEntity.shouldShowUpgrades() || blockEntity.shouldShowFillLevels() || holdsItemThatShowsFillLevels()) {
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

			if (blockEntity.shouldShowFillLevels() || holdsItemThatShowsFillLevels()) {
				renderFillLevels(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
			}

			poseStack.popPose();
		}
	}

	private void renderUpgrades(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop, boolean holdsItemThatShowsUpgrades) {
		if (flatTop) {
			flatDisplayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades, shouldShowDisabledUpgradesDisplay(blockEntity));
		} else {
			displayItemRenderer.renderUpgradeItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades, shouldShowDisabledUpgradesDisplay(blockEntity));
		}
	}

	private void renderFillLevels(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		poseStack.pushPose();
		poseStack.translate(0, 0, -0.001);

		List<Float> slotFillLevels = blockEntity.getSlotFillLevels();
		int slots = slotFillLevels.size();

		boolean translucentRender = !blockEntity.shouldShowFillLevels() && holdsToolInToggleFillLevelDisplay();

		switch (slots) {
			case 1 -> renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(0), 1 / 16F, 1 / 16F, true, translucentRender);
			case 2 -> {
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(0), 1 / 16F, 9 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(1), 1 / 16F, 1 / 16F, false, translucentRender);
			}
			case 3 -> {
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(0), 1 / 16F, 9 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(1), 14 / 16F, 1 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(2), 1 / 16F, 1 / 16F, false, translucentRender);
			}
			case 4 -> {
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(0), 14 / 16F, 9 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(1), 1 / 16F, 9 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(2), 14 / 16F, 1 / 16F, false, translucentRender);
				renderFillLevel(poseStack, bufferSource, packedLight, packedOverlay, slotFillLevels.get(3), 1 / 16F, 1 / 16F, false, translucentRender);
			}
		}
		poseStack.popPose();
	}

	private void renderDisplayItems(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean flatTop) {
		if (flatTop) {
			flatDisplayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
		} else {
			displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
		}
	}

	private void renderItemCounts(LimitedBarrelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, boolean flatTop, Direction horizontalFacing, VerticalFacing verticalFacing) {
		if (!blockEntity.shouldShowCounts()) {
			return;
		}

		int packedLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().relative(verticalFacing != VerticalFacing.NO ? verticalFacing.getDirection() : horizontalFacing));

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

	private void renderFillLevel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float fillLevel, float x, float y, boolean large, boolean translucentRender) {
		poseStack.pushPose();
		poseStack.translate(x + 1/16F/5F, y + 1/16F/5F, 0);
		int barHeight = large ? 14 : 6;
		poseStack.scale(1 / 16F / 5F * 3, fillLevel * 1 / 16F / 5F * (barHeight * 5 - 2), 1);
		poseStack.pushPose();
		VertexConsumer vertexConsumer;
		if (translucentRender) {
			TextureAtlasSprite sprite = FILL_INDICATORS_TEXTURE.sprite();
			vertexConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlasLocation())));
		} else {
			vertexConsumer = FILL_INDICATORS_TEXTURE.buffer(bufferSource, RenderType::entityCutoutNoCull);
		}
		PoseStack.Pose pose = poseStack.last();
		Vector3f normal = new Vector3f(0, 1, 0);
		pose.normal().transform(normal);
		float minU = large ? 0 : 3 / 128F;
		float maxV = large ? 68 / 128F : 28 / 128F;
		RenderHelper.renderQuad(vertexConsumer, pose.pose(), normal, packedOverlay, packedLight, translucentRender ? 0.5F : 1, minU, (1 - fillLevel) * maxV, minU + 3 / 128F, maxV);

		poseStack.popPose();
		poseStack.popPose();
	}
}
