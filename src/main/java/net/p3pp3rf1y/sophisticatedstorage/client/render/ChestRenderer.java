package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.HashMap;
import java.util.Map;

public class ChestRenderer implements BlockEntityRenderer<ChestBlockEntity> {
	private static final String BOTTOM = "bottom";
	private static final String LID = "lid";
	private static final String LOCK = "lock";
	private final ModelPart lidPart;
	private final ModelPart bottomPart;
	private final ModelPart lockPart;

	private static final String ENTITY_CHEST_FOLDER = "entity/chest/";

	public static final Map<WoodType, Material> WOOD_MATERIALS = new HashMap<>();
	public static final Material WOOD_TIER_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "wood_tier"));
	public static final Material IRON_TIER_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "iron_tier"));
	public static final Material GOLD_TIER_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "gold_tier"));
	public static final Material DIAMOND_TIER_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "diamond_tier"));
	public static final Material NETHERITE_TIER_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "netherite_tier"));
	public static final Material TINTABLE_MAIN_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "tintable_main"));
	public static final Material TINTABLE_ACCENT_MATERIAL = new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + "tintable_accent"));

	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> WOOD_MATERIALS.put(woodType, new Material(Sheets.CHEST_SHEET, SophisticatedStorage.getRL(ENTITY_CHEST_FOLDER + woodType.name()))));
	}

	public ChestRenderer(BlockEntityRendererProvider.Context pContext) {
		ModelPart modelpart = pContext.bakeLayer(ClientEventHandler.CHEST_LAYER);
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

	public void render(ChestBlockEntity chestEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedlight, int packedOverlay) {
		BlockState blockstate = chestEntity.getBlockState();
		poseStack.pushPose();
		float f = blockstate.getValue(StorageBlockBase.FACING).toYRot();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Vector3f.YP.rotationDegrees(-f));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
		float lidAngle = chestEntity.getOpenNess(partialTick);
		lidAngle = 1.0F - lidAngle;
		lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;

		float finalLidAngle = lidAngle;
		chestEntity.getWoodType().ifPresent(wt -> {
			VertexConsumer vertexconsumer = WOOD_MATERIALS.get(wt).buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLid(poseStack, vertexconsumer, finalLidAngle, packedlight, packedOverlay);
		});
		if (chestEntity.getMainColor() > -1) {
			VertexConsumer vertexconsumer = TINTABLE_MAIN_MATERIAL.buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLidWithTint(poseStack, vertexconsumer, lidAngle, packedlight, packedOverlay, chestEntity.getMainColor());
		}
		if (chestEntity.getAccentColor() > -1) {
			VertexConsumer vertexconsumer = TINTABLE_ACCENT_MATERIAL.buffer(bufferSource, RenderType::entityCutout);
			renderBottomAndLidWithTint(poseStack, vertexconsumer, lidAngle, packedlight, packedOverlay, chestEntity.getAccentColor());
		}
		Material tierMaterial = getTierMaterial(blockstate.getBlock());
		VertexConsumer vertexconsumer = tierMaterial.buffer(bufferSource, RenderType::entityCutout);
		renderBottomAndLid(poseStack, vertexconsumer, lidAngle, packedlight, packedOverlay);
		renderLock(poseStack, vertexconsumer, lidAngle, packedlight, packedOverlay);
		poseStack.popPose();
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

	private void renderLock(PoseStack poseStack, VertexConsumer consumer, float lidAngle, int packedLight, int packedOverlay) {
		lockPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		lockPart.render(poseStack, consumer, packedLight, packedOverlay);
	}

	private Material getTierMaterial(Block block) {
		if (block == ModBlocks.IRON_CHEST.get()) {
			return IRON_TIER_MATERIAL;
		} else if (block == ModBlocks.GOLD_CHEST.get()) {
			return GOLD_TIER_MATERIAL;
		} else if (block == ModBlocks.DIAMOND_CHEST.get()) {
			return DIAMOND_TIER_MATERIAL;
		} else if (block == ModBlocks.NETHERITE_CHEST.get()) {
			return NETHERITE_TIER_MATERIAL;
		}
		return WOOD_TIER_MATERIAL;
	}
}
