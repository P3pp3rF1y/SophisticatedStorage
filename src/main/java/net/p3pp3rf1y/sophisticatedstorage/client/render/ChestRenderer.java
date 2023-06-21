package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Map;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

public class ChestRenderer extends StorageRenderer<ChestBlockEntity> {
	private static final String BOTTOM = "bottom";
	private static final String LID = "lid";
	private static final String LOCK = "lock";
	private final ModelPart lidPart;
	private final ModelPart bottomPart;
	private final ModelPart lockPart;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5 * (14.01 / 16), new Vec3(-1 / 16D, 0, -0.0075));

	public ChestRenderer(BlockEntityRendererProvider.Context context) {
		ModelPart modelpart = context.bakeLayer(ClientEventHandler.CHEST_LAYER);
		bottomPart = modelpart.getChild(BOTTOM);
		lidPart = modelpart.getChild(LID);
		lockPart = modelpart.getChild(LOCK);
	}

	public static LayerDefinition createSingleBodyLayer(boolean addLock) {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
		partdefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		if (addLock) {
			partdefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
		}
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	public void render(ChestBlockEntity chestEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		BlockState blockstate = chestEntity.getBlockState();
		StorageTextureManager matManager = StorageTextureManager.INSTANCE;
		Optional<WoodType> woodType = chestEntity.getWoodType();
		Map<StorageTextureManager.ChestMaterial, Material> chestMaterials = matManager.getWoodChestMaterials(woodType.orElse(WoodType.ACACIA));
		if (chestMaterials == null) {
			return;
		}

		poseStack.pushPose();
		Direction facing = blockstate.getValue(ChestBlock.FACING);
		float f = facing.toYRot();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(-f));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
		float lidAngle = chestEntity.getOpenNess(partialTick);
		lidAngle = 1.0F - lidAngle;
		lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;

		float finalLidAngle = lidAngle;
		StorageWrapper storageWrapper = chestEntity.getStorageWrapper();
		boolean hasMainColor = storageWrapper.hasMainColor();
		boolean hasAccentColor = storageWrapper.hasAccentColor();

		if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
			VertexConsumer vertexconsumer = chestMaterials.get(StorageTextureManager.ChestMaterial.BASE).buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLid(poseStack, vertexconsumer, finalLidAngle, packedLight, packedOverlay);
		}
		if (hasMainColor) {
			VertexConsumer vertexconsumer = chestMaterials.get(StorageTextureManager.ChestMaterial.TINTABLE_MAIN).buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLidWithTint(poseStack, vertexconsumer, lidAngle, packedLight, packedOverlay, storageWrapper.getMainColor());
		}
		if (hasAccentColor) {
			VertexConsumer vertexconsumer = chestMaterials.get(StorageTextureManager.ChestMaterial.TINTABLE_ACCENT).buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLidWithTint(poseStack, vertexconsumer, lidAngle, packedLight, packedOverlay, storageWrapper.getAccentColor());
		}
		Material tierMaterial = getTierMaterial(chestMaterials, blockstate.getBlock());
		VertexConsumer vertexconsumer = tierMaterial.buffer(bufferSource, RenderType::entityCutout);
		if (chestEntity.shouldShowTier()) {
			renderBottomAndLid(poseStack, vertexconsumer, lidAngle, packedLight, packedOverlay);
		} else if (holdsItemThatShowsHiddenTiers()) {
			renderHiddenTier(poseStack, bufferSource, packedLight, packedOverlay, tierMaterial);
		}

		if (storageWrapper.getRenderInfo().getItemDisplayRenderInfo().getDisplayItem().isEmpty()) {
			renderChestLock(poseStack, vertexconsumer, lidAngle, packedLight, packedOverlay);
		}
		poseStack.popPose();

		if (chestEntity.isPacked()) {
			VertexConsumer consumer = chestMaterials.get(StorageTextureManager.ChestMaterial.PACKED).buffer(bufferSource, RenderType::entityCutout);
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			renderBottomAndLid(poseStack, consumer, finalLidAngle, packedLight, packedOverlay);
			poseStack.popPose();
		} else if (shouldRenderFrontFace(chestEntity.getBlockPos())) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(getNorthBasedRotation(facing));

			poseStack.translate(-0.5, -0.5, -(0.5 - 1 / 16f));

			LockRenderer.renderLock(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, 13F / 16F, this::holdsToolInToggleLockOrLockDisplay);
			if (chestEntity.shouldShowUpgrades() || holdsItemThatShowsUpgrades()) {
				displayItemRenderer.renderUpgradeItems(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(chestEntity));
			}
			displayItemRenderer.renderDisplayItem(chestEntity, poseStack, bufferSource, packedLight, packedOverlay);
			poseStack.popPose();
		}
	}

	private void renderHiddenTier(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Material tierMaterial) {
		//noinspection resource
		TextureAtlasSprite sprite = tierMaterial.sprite();
		VertexConsumer translucentConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlasLocation())));
		poseStack.pushPose();
		poseStack.translate(-0.005D, -0.005D, -0.005D);
		poseStack.scale(1.01f, 1.01f, 1.01f);
		lidPart.render(poseStack, translucentConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.5F);
		bottomPart.render(poseStack, translucentConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.5F);
		poseStack.popPose();
	}

	private boolean shouldRenderFrontFace(BlockPos chestPos) {
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		return Vec3.atCenterOf(chestPos).closerThan(camera.getPosition(), 32);
	}

	private void renderBottomAndLid(PoseStack poseStack, VertexConsumer consumer, float lidAngle, int packedLight, int packedOverlay) {
		lidPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		lidPart.render(poseStack, consumer, packedLight, packedOverlay);
		bottomPart.render(poseStack, consumer, packedLight, packedOverlay);
	}

	private void renderBottomAndLidWithTint(PoseStack poseStack, VertexConsumer consumer, float lidAngle, int packedLight, int packedOverlay, int tint) {
		float tintRed = (tint >> 16 & 255) / 255.0F;
		float tingGreen = (tint >> 8 & 255) / 255.0F;
		float tintBlue = (tint & 255) / 255.0F;

		lidPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		lidPart.render(poseStack, consumer, packedLight, packedOverlay, tintRed, tingGreen, tintBlue, 1);
		bottomPart.render(poseStack, consumer, packedLight, packedOverlay, tintRed, tingGreen, tintBlue, 1);
	}

	private void renderChestLock(PoseStack poseStack, VertexConsumer consumer, float lidAngle, int packedLight, int packedOverlay) {
		lockPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		lockPart.render(poseStack, consumer, packedLight, packedOverlay);
	}

	private Material getTierMaterial(Map<StorageTextureManager.ChestMaterial, Material> chestMaterials, Block block) {
		if (block == ModBlocks.IRON_CHEST.get()) {
			return chestMaterials.get(StorageTextureManager.ChestMaterial.IRON_TIER);
		} else if (block == ModBlocks.GOLD_CHEST.get()) {
			return chestMaterials.get(StorageTextureManager.ChestMaterial.GOLD_TIER);
		} else if (block == ModBlocks.DIAMOND_CHEST.get()) {
			return chestMaterials.get(StorageTextureManager.ChestMaterial.DIAMOND_TIER);
		} else if (block == ModBlocks.NETHERITE_CHEST.get()) {
			return chestMaterials.get(StorageTextureManager.ChestMaterial.NETHERITE_TIER);
		}
		return chestMaterials.get(StorageTextureManager.ChestMaterial.WOOD_TIER);
	}
}
