package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

public class ShulkerBoxRenderer extends StorageRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderer.ShulkerBoxRenderState> {
	private static final String ENTITY_SHULKER_BOX_FOLDER = "entity/shulker_box/";
	private static final SpriteId BASE_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "base_tier"));
	private static final SpriteId COPPER_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "copper_tier"));
	private static final SpriteId IRON_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "iron_tier"));
	private static final SpriteId GOLD_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "gold_tier"));
	private static final SpriteId DIAMOND_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "diamond_tier"));
	private static final SpriteId NETHERITE_TIER_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "netherite_tier"));
	private static final SpriteId TINTABLE_MAIN_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "tintable_main"));
	private static final SpriteId TINTABLE_ACCENT_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "tintable_accent"));
	private static final SpriteId NO_TINT_MATERIAL = new SpriteId(net.minecraft.client.renderer.Sheets.SHULKER_SHEET, SophisticatedStorage.getIdentifier(ENTITY_SHULKER_BOX_FOLDER + "no_tint"));
	private final ShulkerBoxModel model;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -0.0075));
	private final SpriteGetter sprites;

	public ShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		model = new ShulkerBoxModel(context.bakeLayer(ModelLayers.SHULKER_BOX));
		sprites = context.sprites();
	}

	public ModelPart rootModelPart() {
		return model.root();
	}

	private SpriteId getTierMaterial(Block block) {
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
	public void extractRenderState(ShulkerBoxBlockEntity blockEntity, ShulkerBoxRenderState renderState, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		BlockState blockState = blockEntity.getBlockState();
		renderState.block = blockState.getBlock();
		renderState.facing = !blockEntity.getBlockPos().equals(BlockPos.ZERO) || (blockEntity.hasLevel() && blockEntity.getLevel().getBlockState(BlockPos.ZERO).getBlock() instanceof ShulkerBoxBlock)
				? blockState.getValue(ShulkerBoxBlock.FACING) : Direction.UP;
		renderState.lidProgress = blockEntity.getProgress(partialTick);
		renderState.mainColor = blockEntity.getStorageWrapper().getMainColor();
		renderState.accentColor = blockEntity.getStorageWrapper().getAccentColor();
	}

	@Override
	public void submit(ShulkerBoxRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		poseStack.pushPose();
		poseStack.mulPose(net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer.modelTransform(renderState.facing));

		if (renderState.mainColor == -1 || renderState.accentColor == -1) {
			submitNodeCollector.submitModel(model, renderState, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, NO_TINT_MATERIAL, sprites, 0, renderState.breakProgress);
		}
		if (renderState.mainColor != -1) {
			submitNodeCollector.submitModel(model, renderState, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0xFF000000 | renderState.mainColor, TINTABLE_MAIN_MATERIAL, sprites, 0, renderState.breakProgress);
		}
		if (renderState.accentColor != -1) {
			submitNodeCollector.submitModel(model, renderState, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0xFF000000 | renderState.accentColor, TINTABLE_ACCENT_MATERIAL, sprites, 0, renderState.breakProgress);
		}
		SpriteId tierMaterial = getTierMaterial(renderState.block);
		if (renderState.showsTier) {
			submitNodeCollector.submitModel(model, renderState, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, tierMaterial, sprites, 0, renderState.breakProgress);
		} else if (holdsItemThatShowsHiddenTiers()) {
			poseStack.pushPose();
			poseStack.translate(0, -0.01, 0);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			submitNodeCollector.submitModel(model, renderState, poseStack, RenderTypes.entityTranslucent(tierMaterial.atlasLocation()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0x7FFFFFFF, sprites.get(tierMaterial), 0, renderState.breakProgress);
			poseStack.popPose();
		}

		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(getNorthBasedRotation(renderState.facing));
		boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();

		float zOffset = 0;
		if (renderState.lidProgress > 0) {
			zOffset = renderState.lidProgress * 0.5f;
			poseStack.mulPose(Axis.ZP.rotationDegrees(270.0F * renderState.lidProgress));
		}

		poseStack.translate(-0.5D, -0.5D, -0.5D - zOffset);

		if (renderState.showsUpgrades || holdsItemThatShowsUpgrades) {
			displayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades);
		}
		if (!renderState.displayItems.isEmpty()) {
			displayItemRenderer.submitDisplayItem(submitNodeCollector, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.displayItems.getFirst());
		}
		LockRenderer.submitLock(submitNodeCollector, renderState, poseStack, 15F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		poseStack.popPose();
	}

	public static class ShulkerBoxRenderState extends StorageRenderState {
		Block block;
		Direction facing;
		float lidProgress;
		int mainColor;
		int accentColor;
	}

	static class ShulkerBoxModel extends Model<ShulkerBoxRenderState> {
		private final ModelPart lid;

		public ShulkerBoxModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
			this.lid = root.getChild("lid");
		}

		public void setupAnim(ShulkerBoxRenderState renderState) {
			super.setupAnim(renderState);
			this.lid.setPos(0.0F, 24.0F - renderState.lidProgress * 0.5F * 16.0F, 0.0F);
			this.lid.yRot = 270.0F * renderState.lidProgress * ((float) Math.PI / 180F);
		}
	}
}
