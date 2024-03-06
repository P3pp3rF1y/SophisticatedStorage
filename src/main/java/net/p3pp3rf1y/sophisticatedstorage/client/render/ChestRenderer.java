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
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Map;
import java.util.Optional;

public class ChestRenderer extends StorageRenderer<ChestBlockEntity> {
	private static final String BOTTOM = "bottom";
	private static final String LID = "lid";
	private static final String LOCK = "lock";
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5 * (14.01 / 16), new Vec3(-1 / 16D, 0, -0.0075));

	private final Map<ChestType, ChestSubRenderer> chestSubRenderers;

	public ChestRenderer(BlockEntityRendererProvider.Context context) {
		ModelPart modelpart = context.bakeLayer(ClientEventHandler.CHEST_LAYER);
		ChestSubRenderer singleChestRenderer = new ChestSubRenderer(ChestType.SINGLE, modelpart.getChild(LID), modelpart.getChild(BOTTOM), modelpart.getChild(LOCK));
		modelpart = context.bakeLayer(ClientEventHandler.CHEST_RIGHT_LAYER);
		ChestSubRenderer doubleChestRightRenderer = new ChestSubRenderer(ChestType.RIGHT, modelpart.getChild(LID), modelpart.getChild(BOTTOM), modelpart.getChild(LOCK));
		modelpart = context.bakeLayer(ClientEventHandler.CHEST_LEFT_LAYER);
		ChestSubRenderer doubleChestLeftRenderer = new ChestSubRenderer(ChestType.LEFT, modelpart.getChild(LID), modelpart.getChild(BOTTOM), modelpart.getChild(LOCK));
		chestSubRenderers = Map.of(ChestType.SINGLE, singleChestRenderer, ChestType.RIGHT, doubleChestRightRenderer, ChestType.LEFT, doubleChestLeftRenderer);
	}

	public static LayerDefinition createSingleBodyLayer(boolean addLock) {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		if (addLock) {
			partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
		}
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleBodyRightLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(15.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleBodyLeftLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public void render(ChestBlockEntity chestEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		BlockState blockstate = chestEntity.getBlockState();
		Optional<WoodType> woodType = chestEntity.getWoodType();
		ChestType chestType = blockstate.getValue(ChestBlock.TYPE);
		ChestSubRenderer subRenderer = chestSubRenderers.get(chestType);
		if (!subRenderer.setChestMaterialsFrom(woodType.orElse(WoodType.ACACIA), blockstate.getBlock())) {
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

		StorageWrapper storageWrapper = chestEntity.getMainStorageWrapper();
		boolean hasMainColor = storageWrapper.hasMainColor();
		boolean hasAccentColor = storageWrapper.hasAccentColor();

		if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
			subRenderer.renderBottomAndLid(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, StorageTextureManager.ChestMaterial.BASE);
		}
		if (hasMainColor) {
			subRenderer.renderBottomAndLidWithTint(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, storageWrapper.getMainColor(), StorageTextureManager.ChestMaterial.TINTABLE_MAIN);
		}
		if (hasAccentColor) {
			subRenderer.renderBottomAndLidWithTint(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, storageWrapper.getAccentColor(), StorageTextureManager.ChestMaterial.TINTABLE_ACCENT);
		}
		if (chestEntity.shouldShowTier()) {
			subRenderer.renderTier(poseStack, bufferSource, lidAngle, packedLight, packedOverlay);
		} else if (holdsItemThatShowsHiddenTiers()) {
			subRenderer.renderHiddenTier(poseStack, bufferSource, packedLight, packedOverlay);
		}

		Optional<RenderInfo.DisplayItem> displayItem = storageWrapper.getRenderInfo().getItemDisplayRenderInfo().getDisplayItem();
		if (displayItem.map(di -> di.getDisplaySide() != DisplaySide.FRONT).orElse(true)) {
			subRenderer.renderChestLock(poseStack, bufferSource, lidAngle, packedLight, packedOverlay);
		}

		if (chestEntity.isPacked()) {
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			subRenderer.renderBottomAndLid(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, StorageTextureManager.ChestMaterial.PACKED);
			poseStack.popPose();
		} else if (shouldRenderFrontFace(chestEntity.getBlockPos())) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(180));

			poseStack.pushPose();
			poseStack.translate(-0.5, -0.5, -(0.5 - 1 / 16f));

			if (chestEntity.isMainChest() && (chestEntity.shouldShowUpgrades() || holdsItemThatShowsUpgrades())) {
				poseStack.pushPose();
				if (chestType == ChestType.LEFT) {
					poseStack.translate(1, 0, 0);
				}
				displayItemRenderer.renderUpgradeItems(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(chestEntity));
				poseStack.popPose();
			}

			if (chestEntity.isMainChest()) {
				renderLocked(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, chestType, lidAngle);
			}
			poseStack.popPose();

			if (chestEntity.isMainChest()) {
				displayItem.ifPresent(di -> renderDisplayItem(di, poseStack, bufferSource, packedLight, packedOverlay, chestType));
			}
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private void renderDisplayItem(RenderInfo.DisplayItem displayItem, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ChestType chestType) {
		DisplaySide displaySide = displayItem.getDisplaySide();

		if (displaySide == DisplaySide.LEFT) {
			poseStack.mulPose(Axis.YP.rotationDegrees(-90));
			if (chestType == ChestType.LEFT) {
				poseStack.translate(0, 0, -1);
			}
		} else if (displaySide == DisplaySide.RIGHT) {
			poseStack.mulPose(Axis.YP.rotationDegrees(90));
			if (chestType == ChestType.RIGHT) {
				poseStack.translate(0, 0, -1);
			}
		} else if (displaySide == DisplaySide.FRONT) {
			if (chestType == ChestType.RIGHT) {
				poseStack.translate(-0.5, 0, 0);
			} else if (chestType == ChestType.LEFT) {
				poseStack.translate(0.5, 0, 0);
			}
		}
		poseStack.translate(-0.5, -0.5, -(0.5 - 1 / 16f));

		displayItemRenderer.renderDisplayItem(poseStack, bufferSource, packedLight, packedOverlay, displayItem);
	}

	private void renderLocked(ChestBlockEntity chestEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ChestType chestType, float lidAngle) {
		poseStack.pushPose();
		if (lidAngle > 0) {
			poseStack.translate(0, 9/16D, 14/16D);
			poseStack.mulPose(Axis.XP.rotationDegrees(lidAngle * 90));
			poseStack.translate(0, -9/16D, -14/16D);
		}
		if (chestType == ChestType.LEFT) {
			poseStack.translate(0.5, 0, 0);
		} else if (chestType == ChestType.RIGHT) {
			poseStack.translate(-0.5, 0, 0);
		}
		LockRenderer.renderLock(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, 13F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		poseStack.popPose();
	}

	private boolean shouldRenderFrontFace(BlockPos chestPos) {
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		return Vec3.atCenterOf(chestPos).closerThan(camera.getPosition(), 32);
	}

	private static class ChestSubRenderer {
		private final ChestType chestType;
		private final ModelPart lidPart;
		private final ModelPart bottomPart;
		private final ModelPart lockPart;
		private Map<StorageTextureManager.ChestMaterial, Material> chestMaterials;
		private Material tierMaterial;

		public ChestSubRenderer(ChestType chestType, ModelPart lidPart, ModelPart bottomPart, ModelPart lockPart) {
			this.chestType = chestType;
			this.lidPart = lidPart;
			this.bottomPart = bottomPart;
			this.lockPart = lockPart;
		}

		private boolean setChestMaterialsFrom(WoodType woodType, Block block) {
			chestMaterials = StorageTextureManager.INSTANCE.getWoodChestMaterials(chestType, woodType);
			if (chestMaterials == null) {
				return false;
			}

			tierMaterial = getTierMaterial(block);

			return true;
		}

		private void renderHiddenTier(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
			TextureAtlasSprite sprite = tierMaterial.sprite();
			VertexConsumer translucentConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlasLocation())));
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			lidPart.render(poseStack, translucentConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.5F);
			bottomPart.render(poseStack, translucentConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.5F);
			poseStack.popPose();
		}

		private void renderBottomAndLid(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay, StorageTextureManager.ChestMaterial chestMaterial) {
			VertexConsumer consumer = chestMaterials.get(chestMaterial).buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLid(poseStack, lidAngle, packedLight, packedOverlay, consumer);
		}

		private void renderBottomAndLid(PoseStack poseStack, float lidAngle, int packedLight, int packedOverlay, VertexConsumer consumer) {
			lidPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
			lidPart.render(poseStack, consumer, packedLight, packedOverlay);
			bottomPart.render(poseStack, consumer, packedLight, packedOverlay);
		}

		private void renderBottomAndLidWithTint(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay, int tint, StorageTextureManager.ChestMaterial chestMaterial) {
			float tintRed = (tint >> 16 & 255) / 255.0F;
			float tingGreen = (tint >> 8 & 255) / 255.0F;
			float tintBlue = (tint & 255) / 255.0F;

			VertexConsumer consumer = chestMaterials.get(chestMaterial).buffer(bufferSource, RenderType::entityCutout);
			lidPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
			lidPart.render(poseStack, consumer, packedLight, packedOverlay, tintRed, tingGreen, tintBlue, 1);
			bottomPart.render(poseStack, consumer, packedLight, packedOverlay, tintRed, tingGreen, tintBlue, 1);
		}

		private void renderChestLock(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay) {
			VertexConsumer consumer = tierMaterial.buffer(bufferSource, RenderType::entityCutout);
			lockPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
			lockPart.render(poseStack, consumer, packedLight, packedOverlay);
		}

		private Material getTierMaterial(Block block) {
			if (block == ModBlocks.COPPER_CHEST.get()) {
				return chestMaterials.get(StorageTextureManager.ChestMaterial.COPPER_TIER);
			} else if (block == ModBlocks.IRON_CHEST.get()) {
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

		public void renderTier(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay) {
			VertexConsumer vertexconsumer = tierMaterial.buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLid(poseStack, lidAngle, packedLight, packedOverlay, vertexconsumer);
		}
	}
}
