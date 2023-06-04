package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderer extends StorageRenderer<ShulkerBoxBlockEntity> {
	private static final String ENTITY_SHULKER_BOX_FOLDER = "entity/shulker_box/";

	public static final Material BASE_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "base_tier"));
	public static final Material IRON_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "iron_tier"));
	public static final Material GOLD_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "gold_tier"));
	public static final Material DIAMOND_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "diamond_tier"));
	public static final Material NETHERITE_TIER_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "netherite_tier"));
	public static final Material TINTABLE_MAIN_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "tintable_main"));
	public static final Material TINTABLE_ACCENT_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "tintable_accent"));
	public static final Material NO_TINT_MATERIAL = new Material(Sheets.SHULKER_SHEET, SophisticatedStorage.getRL(ENTITY_SHULKER_BOX_FOLDER + "no_tint"));
	private final ShulkerModel<?> model;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, new Vec3(0, 0, -0.0075));

	public ShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
		model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
	}

	public void render(ShulkerBoxBlockEntity shulkerBoxEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		BlockState blockState = shulkerBoxEntity.getBlockState();
		Direction direction = Direction.UP;
		if (shulkerBoxEntity.hasLevel()) {
			//noinspection ConstantConditions
			BlockState blockstate = shulkerBoxEntity.getLevel().getBlockState(shulkerBoxEntity.getBlockPos());
			if (blockstate.getBlock() instanceof ShulkerBoxBlock) {
				direction = blockstate.getValue(ShulkerBoxBlock.FACING);
			}
		}

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.scale(0.9995F, 0.9995F, 0.9995F);
		poseStack.mulPose(direction.getRotation());
		poseStack.scale(1.0F, -1.0F, -1.0F);
		poseStack.translate(0.0D, -1.0D, 0.0D);
		ModelPart lidPart = model.getLid();
		float lidProgress = shulkerBoxEntity.getProgress(partialTick);
		lidPart.setPos(0.0F, 24.0F - lidProgress * 0.5F * 16.0F, 0.0F);
		lidPart.yRot = 270.0F * lidProgress * ((float) Math.PI / 180F);

		int mainColor = shulkerBoxEntity.getStorageWrapper().getMainColor();
		int accentColor = shulkerBoxEntity.getStorageWrapper().getAccentColor();

		if (mainColor == -1 || accentColor == -1) {
			VertexConsumer vertexconsumer = NO_TINT_MATERIAL.buffer(bufferSource, RenderType::entityCutoutNoCull);
			model.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (mainColor > -1) {
			renderTintedModel(poseStack, bufferSource, packedLight, packedOverlay, mainColor, TINTABLE_MAIN_MATERIAL);
		}
		if (accentColor > -1) {
			renderTintedModel(poseStack, bufferSource, packedLight, packedOverlay, accentColor, TINTABLE_ACCENT_MATERIAL);
		}
		if (shulkerBoxEntity.shouldShowTier()) {
			VertexConsumer vertexconsumer = getTierMaterial(blockState.getBlock()).buffer(bufferSource, RenderType::entityCutoutNoCull);
			model.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		} else if (holdsItemThatShowsHiddenTiers()) {
			//noinspection resource
			TextureAtlasSprite sprite = getTierMaterial(blockState.getBlock()).sprite();
			VertexConsumer vertexconsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlas().location())));
			poseStack.pushPose();
			poseStack.translate(0, -0.01, 0);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			model.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.5F);
			poseStack.popPose();
		}

		poseStack.popPose();

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(getNorthBasedRotation(direction));

		float zOffset = 0;
		if (lidProgress > 0) {
			zOffset = lidProgress * 0.5f;
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(270.0F * lidProgress));
		}

		poseStack.translate(-0.5D, -0.5D, -0.5D - zOffset);

		if (shulkerBoxEntity.shouldShowUpgrades() || holdsItemThatShowsUpgrades()) {
			displayItemRenderer.renderUpgradeItems(shulkerBoxEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(shulkerBoxEntity));
		}
		displayItemRenderer.renderDisplayItem(shulkerBoxEntity, poseStack, bufferSource, packedLight, packedOverlay);
		LockRenderer.renderLock(shulkerBoxEntity, poseStack, bufferSource, packedLight, packedOverlay, 15F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		poseStack.popPose();
	}

	private Material getTierMaterial(Block block) {
		if (block == ModBlocks.IRON_SHULKER_BOX.get()) {
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

	private void renderTintedModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, int mainColor, Material material) {
		float tintRed = (mainColor >> 16 & 255) / 255.0F;
		float tingGreen = (mainColor >> 8 & 255) / 255.0F;
		float tintBlue = (mainColor & 255) / 255.0F;

		VertexConsumer vertexconsumer = material.buffer(bufferSource, RenderType::entityCutoutNoCull);
		model.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay, tintRed, tingGreen, tintBlue, 1.0F);
	}
}