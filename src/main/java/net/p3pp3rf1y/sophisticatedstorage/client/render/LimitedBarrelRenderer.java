package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static net.minecraft.client.Minecraft.UNIFORM_FONT;

public class LimitedBarrelRenderer extends BarrelRendererBase<LimitedBarrelBlockEntity, LimitedBarrelRenderer.LimitedBarrelRenderState> {
	private static final Identifier FILL_INDICATORS_TEXTURE = SophisticatedStorage.getIdentifier("block/fill_indicators");
	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96f;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48f;
	public static final Style INFINITE_COUNT_DISPLAY_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(UNIFORM_FONT));
	private static final Style COUNT_DISPLAY_STYLE = INFINITE_COUNT_DISPLAY_STYLE.withBold(true);
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -1 / 16D));
	private final DisplayItemRenderer flatDisplayItemRenderer = new DisplayItemRenderer(0.5, Vec3.ZERO);

	public LimitedBarrelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	private void submitFrontFace(SubmitNodeCollector submitNodeCollector, LimitedBarrelRenderState renderState, PoseStack poseStack) {
		if (!renderState.displayItems.isEmpty() || holdsItemThatShowsUpgrades() || renderState.showsUpgrades || renderState.showsFillLevels || holdsItemThatShowsFillLevels()) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(renderState.horizontalFacing));
			if (renderState.verticalFacing != VerticalFacing.NO) {
				poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(renderState.verticalFacing.getDirection()));
			}
			poseStack.translate(-0.5, -0.5, -(0.5 - (renderState.flatTop ? 0 : 1 / 16f)));

			if (!renderState.displayItems.isEmpty()) {
				submitDisplayItems(submitNodeCollector, renderState, poseStack);
			}

			if (renderState.showsUpgrades || holdsItemThatShowsUpgrades()) {
				submitUpgrades(submitNodeCollector, renderState, poseStack);
			}

			if (renderState.showsFillLevels || holdsItemThatShowsFillLevels()) {
				submitFillLevels(submitNodeCollector, renderState, poseStack);
			}

			poseStack.popPose();
		}
	}

	private void submitUpgrades(SubmitNodeCollector submitNodeCollector, LimitedBarrelRenderState renderState, PoseStack poseStack) {
		boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();
		if (renderState.flatTop) {
			flatDisplayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades);
		} else {
			displayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades);
		}
	}

	private void submitFillLevels(SubmitNodeCollector submitNodeCollector, LimitedBarrelRenderState renderState, PoseStack poseStack) {
		poseStack.pushPose();
		poseStack.translate(0, 0, -0.001);
		int slots = renderState.fillLevels.size();
		boolean translucentRender = !renderState.showsFillLevels && holdsToolInToggleFillLevelDisplay();

		switch (slots) {
			case 1 -> submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(0), 1 / 16F, 1 / 16F, true, translucentRender);
			case 2 -> {
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(0), 1 / 16F, 9 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(1), 1 / 16F, 1 / 16F, false, translucentRender);
			}
			case 3 -> {
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(0), 1 / 16F, 9 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(1), 14 / 16F, 1 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(2), 1 / 16F, 1 / 16F, false, translucentRender);
			}
			case 4 -> {
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(0), 14 / 16F, 9 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(1), 1 / 16F, 9 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(2), 14 / 16F, 1 / 16F, false, translucentRender);
				submitFillLevel(submitNodeCollector, poseStack, renderState.lightCoords, renderState.fillLevels.get(3), 1 / 16F, 1 / 16F, false, translucentRender);
			}
			default -> {
			}
		}
		poseStack.popPose();
	}

	private void submitDisplayItems(SubmitNodeCollector submitNodeCollector, LimitedBarrelRenderState renderState, PoseStack poseStack) {
		if (renderState.flatTop) {
			flatDisplayItemRenderer.submitDisplayItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY);
		} else {
			displayItemRenderer.submitDisplayItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY);
		}
	}

	private void submitItemCounts(SubmitNodeCollector submitNodeCollector, LimitedBarrelRenderState renderState, PoseStack poseStack) {
		if (!renderState.showsCounts) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(renderState.horizontalFacing.getOpposite()));
		if (renderState.verticalFacing != VerticalFacing.NO) {
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(renderState.verticalFacing.getDirection().getOpposite()));
		}
		poseStack.translate(0.5, -0.5, 0.5);

		float countDisplayYOffset = -(renderState.slotCounts.size() == 1 ? 0.25f : 0.11f);
		for (int displayItemIndex = 0; displayItemIndex < renderState.slotCounts.size(); displayItemIndex++) {
			int count = renderState.slotCounts.get(displayItemIndex);
			if (count <= 0) {
				continue;
			}

			poseStack.pushPose();
			Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(displayItemIndex, renderState.slotCounts.size());
			double xTranslation = -frontOffset.x();
			boolean isInfinite = renderState.infiniteSlots.contains(displayItemIndex);
			float yTranslation = frontOffset.y() + (isInfinite ? countDisplayYOffset / 1.8f : countDisplayYOffset);
			double zTranslation = 0.001 - (renderState.flatTop ? 0 : 0.75 / 16D);
			poseStack.translate(xTranslation, yTranslation, zTranslation);

			float scale = renderState.slotCounts.size() == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE;
			if (isInfinite) {
				scale *= 2;
			}
			poseStack.scale(scale, -scale, scale);
			MutableComponent countString = isInfinite
					? Component.literal("∞").withStyle(INFINITE_COUNT_DISPLAY_STYLE)
					: Component.literal(CountAbbreviator.abbreviate(count, renderState.slotCounts.size() == 1 ? 6 : 5)).withStyle(COUNT_DISPLAY_STYLE);
			Font font = Minecraft.getInstance().font;
			float countDisplayXOffset = -font.getSplitter().stringWidth(countString) / 2f;
			poseStack.translate(countDisplayXOffset, 0, 0);
			submitNodeCollector.submitText(poseStack, 0, 0, countString.getVisualOrderText(), false, Font.DisplayMode.NORMAL, renderState.lightCoords, renderState.slotColors.get(displayItemIndex), 0, 0);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private void submitFillLevel(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, float fillLevel, float x, float y, boolean large, boolean translucentRender) {
		poseStack.pushPose();
		poseStack.translate(x + 1 / 16F / 5F, y + 1 / 16F / 5F, 0);
		int barHeight = large ? 14 : 6;
		poseStack.scale(1 / 16F / 5F * 3, fillLevel * 1 / 16F / 5F * (barHeight * 5 - 2), 1);
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(FILL_INDICATORS_TEXTURE);
		RenderType renderType = translucentRender ? RenderTypes.entityTranslucent(sprite.atlasLocation()) : RenderTypes.entityCutout(sprite.atlasLocation());

		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			Vector3f normal = new Vector3f(0, 1, 0);
			pose.normal().transform(normal);
			float minU = large ? 0 : 3 / 128F;
			float maxV = large ? 68 / 128F : 28 / 128F;
			RenderHelper.renderQuad(vertexConsumer, pose.pose(), normal, OverlayTexture.NO_OVERLAY, packedLight, translucentRender ? 0.5F : 1, minU, (1 - fillLevel) * maxV, minU + 3 / 128F, maxV, sprite);
		});
		poseStack.popPose();
	}

	@Override
	public LimitedBarrelRenderState createRenderState() {
		return new LimitedBarrelRenderState();
	}

	@Override
	public void extractRenderState(LimitedBarrelBlockEntity blockEntity, LimitedBarrelRenderState renderState, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		if (renderState.packed) {
			return;
		}

		renderState.showsCounts = blockEntity.shouldShowCounts();
		renderState.showsFillLevels = blockEntity.shouldShowFillLevels();
		BlockState blockState = blockEntity.getBlockState();
		renderState.horizontalFacing = blockState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING);
		renderState.verticalFacing = blockState.getValue(LimitedBarrelBlock.VERTICAL_FACING);

		if (blockEntity.getLevel() != null && blockEntity.shouldUseLightInFrontForFrontRender()) {
			renderState.lightCoords = LevelRenderer.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().relative(renderState.verticalFacing != VerticalFacing.NO ? renderState.verticalFacing.getDirection() : renderState.horizontalFacing));
		}

		RenderData.DisplayData displayData = blockEntity.getStorageWrapper().getRenderDataHandler().getDisplayData();
		renderState.slotCounts = displayData.slotCounts();
		renderState.infiniteSlots = new HashSet<>(displayData.infiniteSlots());
		renderState.slotColors = IntStream.range(0, renderState.slotCounts.size()).map(blockEntity::getSlotColor).boxed().toList();
		renderState.fillLevels = blockEntity.getSlotFillLevels();
	}

	@Override
	public void submit(LimitedBarrelRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		if (renderState.packed || (renderState.displayItems.isEmpty() && !renderState.showsCounts && !holdsItemThatShowsUpgrades() && !renderState.showsUpgrades && !renderState.showsFillLevels && !holdsItemThatShowsFillLevels())) {
			return;
		}

		submitItemCounts(submitNodeCollector, renderState, poseStack);
		submitFrontFace(submitNodeCollector, renderState, poseStack);
		submitHiddenTier(submitNodeCollector, renderState, poseStack);
		submitHiddenLock(submitNodeCollector, renderState, poseStack);
	}

	public static class LimitedBarrelRenderState extends BarrelRenderStateBase {
		public boolean showsCounts;
		public boolean showsFillLevels;
		public Direction horizontalFacing;
		public VerticalFacing verticalFacing;
		public List<Integer> slotCounts;
		public Set<Integer> infiniteSlots;
		public List<Integer> slotColors;
		public List<Float> fillLevels;
	}
}
