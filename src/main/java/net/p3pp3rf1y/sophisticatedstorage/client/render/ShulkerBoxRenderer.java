package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

public class ShulkerBoxRenderer extends StorageRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderer.ShulkerBoxRenderState> {
	private static final String ENTITY_SHULKER_BOX_FOLDER = "entity/shulker_box/";

	public static final Material BASE_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "base_tier"));
	public static final Material COPPER_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "copper_tier"));
	public static final Material IRON_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "iron_tier"));
	public static final Material GOLD_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "gold_tier"));
	public static final Material DIAMOND_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "diamond_tier"));
	public static final Material NETHERITE_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "netherite_tier"));
	public static final Material TINTABLE_MAIN_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "tintable_main"));
	public static final Material TINTABLE_ACCENT_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "tintable_accent"));
	public static final Material NO_TINT_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "no_tint"));
	private final ShulkerBoxModel model;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -0.0075));
	private final MaterialSet materialSet;

	public ShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		model = new ShulkerBoxModel(context.bakeLayer(ModelLayers.SHULKER));
		materialSet = context.materials();
	}

	public ModelPart rootModelPart() {
		return model.root();
	}

	private Material getTierMaterial(Block block) {
		if (block == ModBlocks.COPPER_SHULKER_BOX.get()) {
			return COPPER_TIER_MATERIAL;
		} else if (block == ModBlocks.IRON_SHULKER_BOX.get()) {
			return IRON_TIER_MATERIAL;
		} else if (block == ModBlocks.GOLD_SHULKER_BOX.get()) {
			return GOLD_TIER_MATERIAL;
		} else if (block == ModBlocks.DIAMOND_SHULKER_BOX.get()) {
			return DIAMOND_TIER_MATERIAL;
		} else if (block == ModBlocks.NETHERITE_SHULKER_BOX.get()) {
			return NETHERITE_TIER_MATERIAL;
		}
		return BASE_TIER_MATERIAL;
	}

	@Override
	public ShulkerBoxRenderState createRenderState() {
		return new ShulkerBoxRenderState();
	}

	@Override
	public void extractRenderState(ShulkerBoxBlockEntity blockEntity, ShulkerBoxRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		BlockState blockState = blockEntity.getBlockState();
		renderState.facing = !blockEntity.getBlockPos().equals(BlockPos.ZERO) || (blockEntity.hasLevel() && blockEntity.getLevel().getBlockState(BlockPos.ZERO).getBlock() instanceof ShulkerBoxBlock) ? blockState.getValue(ShulkerBoxBlock.FACING) : Direction.UP;
		renderState.lidProgress = blockEntity.getProgress(partialTick);
		renderState.mainColor = blockEntity.getStorageWrapper().getMainColor();
		renderState.accentColor = blockEntity.getStorageWrapper().getAccentColor();
	}

	@Override
	public void submit(ShulkerBoxRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.scale(0.9995F, 0.9995F, 0.9995F);
		poseStack.mulPose(renderState.facing.getRotation());
		poseStack.scale(1.0F, -1.0F, -1.0F);
		poseStack.translate(0.0D, -1.0D, 0.0D);
		ModelPart lidPart = model.lid;
		lidPart.setPos(0.0F, 24.0F - renderState.lidProgress * 0.5F * 16.0F, 0.0F);
		lidPart.yRot = 270.0F * renderState.lidProgress * ((float) Math.PI / 180F);

		if (renderState.mainColor == -1 || renderState.accentColor == -1) {
			RenderType renderType = NO_TINT_MATERIAL.renderType(RenderType::entityCutoutNoCull);
			submitNodeCollector.submitModel(model, renderState, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, materialSet.get(NO_TINT_MATERIAL), 0, renderState.breakProgress);
		}
		if (renderState.mainColor != -1) {
			int color = 0xFF_000000 | renderState.mainColor;
			submitNodeCollector.submitModel(model, renderState, poseStack, TINTABLE_MAIN_MATERIAL.renderType(RenderType::entityCutoutNoCull), renderState.lightCoords, OverlayTexture.NO_OVERLAY, color, materialSet.get(TINTABLE_MAIN_MATERIAL), 0, renderState.breakProgress);
		}
		if (renderState.accentColor != -1) {
			int accentColor = 0xFF_000000 | renderState.accentColor;
			submitNodeCollector.submitModel(model, renderState, poseStack, TINTABLE_ACCENT_MATERIAL.renderType(RenderType::entityCutoutNoCull), renderState.lightCoords, OverlayTexture.NO_OVERLAY, accentColor, materialSet.get(TINTABLE_ACCENT_MATERIAL), 0, renderState.breakProgress);
		}
		if (renderState.showsTier) {
			Material tierMaterial = getTierMaterial(renderState.blockState.getBlock());
			RenderType renderType = RenderType.entityCutoutNoCull(tierMaterial.atlasLocation());
			submitNodeCollector.submitModel(model, renderState, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, materialSet.get(tierMaterial), 0, renderState.breakProgress);
		} else if (holdsItemThatShowsHiddenTiers()) {
			Material tierMaterial = getTierMaterial(renderState.blockState.getBlock());
			RenderType renderType = RenderType.entityTranslucent(tierMaterial.atlasLocation());
			poseStack.pushPose();
			poseStack.translate(0, -0.01, 0);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			int color = 0x7F_FFFFFF;
			submitNodeCollector.submitModel(model, renderState, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, color, materialSet.get(tierMaterial), 0, renderState.breakProgress);
			poseStack.popPose();
		}

		poseStack.popPose();

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(getNorthBasedRotation(renderState.facing));

		float zOffset = 0;
		if (renderState.lidProgress > 0) {
			zOffset = renderState.lidProgress * 0.5f;
			poseStack.mulPose(Axis.ZP.rotationDegrees(270.0F * renderState.lidProgress));
		}

		poseStack.translate(-0.5D, -0.5D, -0.5D - zOffset);

		if (renderState.showsUpgrades || holdsItemThatShowsUpgrades()) {
			displayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, renderState.showsDisabledUpgradeDisplay);
		}
		if (!renderState.displayItems.isEmpty()) {
			displayItemRenderer.submitDisplayItem(submitNodeCollector, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.displayItems.getFirst());
		}
		LockRenderer.submitLock(submitNodeCollector, renderState, poseStack, 15F / 16F, this::holdsToolInToggleLockOrLockDisplay, materialSet);
		poseStack.popPose();
	}

	public static class ShulkerBoxRenderState extends StorageRenderState {
		Direction facing;
		float lidProgress;
		int mainColor;
		int accentColor;
	}

	static class ShulkerBoxModel extends Model<ShulkerBoxRenderState> {
		private final ModelPart lid;

		public ShulkerBoxModel(ModelPart root) {
			super(root, RenderType::entityCutoutNoCull);
			this.lid = root.getChild("lid");
		}

		public void setupAnim(ShulkerBoxRenderState renderState) {
			super.setupAnim(renderState);
			this.lid.setPos(0.0F, 24.0F - renderState.lidProgress * 0.5F * 16.0F, 0.0F);
			this.lid.yRot = 270.0F * renderState.lidProgress * ((float) Math.PI / 180F);
		}
	}
}