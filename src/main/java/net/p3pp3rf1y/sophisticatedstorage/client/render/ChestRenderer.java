package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.client.GenericWoodStorageTintCache;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ChestRenderer extends StorageRenderer<ChestBlockEntity, ChestRenderer.ChestRenderState> {
	private static final String BOTTOM = "bottom";
	private static final String LID = "lid";
	private static final String LOCK = "lock";
	private static final int GENERIC_WOOD_CHEST_ACCENT_COLOR = 0x463E32;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5 * (14.01 / 16), new Vec3(-1 / 16D, 0, -0.0075));

	private final Map<ChestType, ChestSubRenderer> chestSubRenderers;

	private final ModelPart root;
	private final MaterialSet materialSet;

	public ChestRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		ModelPart corePart = context.bakeLayer(ClientEventHandler.CHEST_LAYER);
		ModelPart lockPart = context.bakeLayer(ClientEventHandler.CHEST_LOCK_LAYER);
		root = corePart;
		ChestSubRenderer singleChestRenderer = new ChestSubRenderer(ChestType.SINGLE, corePart, lockPart);
		corePart = context.bakeLayer(ClientEventHandler.CHEST_RIGHT_LAYER);
		lockPart = context.bakeLayer(ClientEventHandler.CHEST_LOCK_RIGHT_LAYER);
		ChestSubRenderer doubleChestRightRenderer = new ChestSubRenderer(ChestType.RIGHT, corePart, lockPart);
		corePart = context.bakeLayer(ClientEventHandler.CHEST_LEFT_LAYER);
		lockPart = context.bakeLayer(ClientEventHandler.CHEST_LOCK_LEFT_LAYER);
		ChestSubRenderer doubleChestLeftRenderer = new ChestSubRenderer(ChestType.LEFT, corePart, lockPart);
		chestSubRenderers = Map.of(ChestType.SINGLE, singleChestRenderer, ChestType.RIGHT, doubleChestRightRenderer, ChestType.LEFT, doubleChestLeftRenderer);
		materialSet = context.materials();
	}

	public static LayerDefinition createSingleBodyLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F),
				PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createSingleLockLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F),
				PartPose.offset(0.0F, 8.0F, 0.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleBodyRightLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F),
				PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleLockRightLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(15.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F),
				PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleBodyLeftLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
		partDefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F),
				PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public static LayerDefinition createDoubleLockLeftLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F),
				PartPose.offset(0.0F, 9.0F, 1.0F));
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	public ModelPart rootModelPart() {
		return root;
	}

	private void submitDisplayItem(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack,
			StorageRenderState.DisplayItemInfo displayItem) {
		DisplaySide displaySide = displayItem.displaySide();

		if (displaySide == DisplaySide.LEFT) {
			poseStack.mulPose(Axis.YP.rotationDegrees(-90));
			if (renderState.chestType == ChestType.LEFT) {
				poseStack.translate(0, 0, -1);
			}
		} else if (displaySide == DisplaySide.RIGHT) {
			poseStack.mulPose(Axis.YP.rotationDegrees(90));
			if (renderState.chestType == ChestType.RIGHT) {
				poseStack.translate(0, 0, -1);
			}
		} else if (displaySide == DisplaySide.FRONT) {
			if (renderState.chestType == ChestType.RIGHT) {
				poseStack.translate(-0.5, 0, 0);
			} else if (renderState.chestType == ChestType.LEFT) {
				poseStack.translate(0.5, 0, 0);
			}
		}
		poseStack.translate(-0.5, -0.5, -(0.5 - 1 / 16f));

		displayItemRenderer.submitDisplayItem(submitNodeCollector, poseStack, renderState.lightCoords, OverlayTexture.NO_OVERLAY, displayItem);
	}

	private void submitLocked(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack, ChestType chestType, float lidAngle) {
		poseStack.pushPose();
		if (lidAngle > 0) {
			poseStack.translate(0, 9 / 16D, 14 / 16D);
			poseStack.mulPose(Axis.XP.rotationDegrees(lidAngle * 90));
			poseStack.translate(0, -9 / 16D, -14 / 16D);
		}
		if (chestType == ChestType.LEFT) {
			poseStack.translate(0.5, 0, 0);
		} else if (chestType == ChestType.RIGHT) {
			poseStack.translate(-0.5, 0, 0);
		}
		LockRenderer.submitLock(submitNodeCollector, renderState, poseStack, 13F / 16F, this::holdsToolInToggleLockOrLockDisplay, materialSet);
		poseStack.popPose();
	}

	@Override
	public ChestRenderState createRenderState() {
		return new ChestRenderState();
	}

	@Override
	public void extractRenderState(ChestBlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 cameraPos,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		BlockState blockState = blockEntity.getBlockState();
		renderState.woodType = blockEntity.getWoodType();
		renderState.isGenericWood = renderState.woodType.map(GenericWoodStorageHelper::isGenericWood).orElse(false);
		renderState.genericTintColors = renderState.isGenericWood ? renderState.woodType.flatMap(GenericWoodStorageTintCache::getTintColors) : Optional.empty();
		renderState.chestType = blockState.getValue(ChestBlock.TYPE);
		renderState.block = blockState.getBlock();
		renderState.facing = blockState.getValue(ChestBlock.FACING);
		float openNess = blockEntity.getOpenNess(partialTick);
		openNess = 1.0F - openNess;
		openNess = 1.0F - openNess * openNess * openNess;
		renderState.open = openNess;

		StorageWrapper storageWrapper = blockEntity.getMainStorageWrapper();
		renderState.hasMainColor = storageWrapper.hasMainColor();
		renderState.hasAccentColor = storageWrapper.hasAccentColor();
		renderState.mainColor = storageWrapper.getMainColor();
		renderState.accentColor = storageWrapper.getAccentColor();
		renderState.packed = blockEntity.isPacked();
		renderState.isMainChest = blockEntity.isMainChest();
		renderState.showUpgradesOnTop = blockEntity.showUpgradesOnTop;
	}

	@Override
	public void submit(ChestRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		ChestSubRenderer subRenderer = chestSubRenderers.get(renderState.chestType);
		if (!subRenderer.setChestMaterialsFrom(renderState.isGenericWood ? WoodType.ACACIA : renderState.woodType.orElse(WoodType.ACACIA), renderState.block)) {
			return;
		}

		poseStack.pushPose();
		float f = renderState.facing.toYRot();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(-f));
		poseStack.translate(-0.5D, -0.5D, -0.5D);

		if (!renderState.isGenericWood && (renderState.woodType.isPresent() || !(renderState.hasMainColor && renderState.hasAccentColor))
				|| renderState.isGenericWood && renderState.genericTintColors.isEmpty()) {
			subRenderer.submitBottomAndLid(submitNodeCollector, renderState, poseStack, StorageTextureManager.ChestMaterial.BASE, materialSet);
		}
		if (renderState.hasMainColor || renderState.genericTintColors.isPresent()) {
			subRenderer.submitBottomAndLidWithTint(submitNodeCollector, renderState, poseStack,
					renderState.hasMainColor ? renderState.mainColor : renderState.genericTintColors.get().mainColor(),
					StorageTextureManager.ChestMaterial.TINTABLE_MAIN, materialSet);
		}
		if (renderState.hasAccentColor || renderState.genericTintColors.isPresent()) {
			subRenderer.submitBottomAndLidWithTint(submitNodeCollector, renderState, poseStack,
					renderState.hasAccentColor ? renderState.accentColor : GENERIC_WOOD_CHEST_ACCENT_COLOR, StorageTextureManager.ChestMaterial.TINTABLE_ACCENT,
					materialSet);
		}
		if (renderState.showsTier) {
			subRenderer.submitTier(submitNodeCollector, renderState, poseStack, materialSet);
		} else if (holdsItemThatShowsHiddenTiers()) {
			subRenderer.submitHiddenTier(submitNodeCollector, poseStack, renderState, materialSet);
		}

		if (renderState.displayItems.isEmpty() || renderState.displayItems.getFirst().displaySide() != DisplaySide.FRONT) {
			subRenderer.submitChestLock(submitNodeCollector, renderState, poseStack, materialSet);
		}

		if (renderState.packed) {
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			subRenderer.submitBottomAndLid(submitNodeCollector, renderState, poseStack, StorageTextureManager.ChestMaterial.PACKED, materialSet);
			poseStack.popPose();
		} else {
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(180));
			boolean holdsItemThatShowsUpgrades = holdsItemThatShowsUpgrades();

			poseStack.pushPose();
			poseStack.translate(-0.5, -0.5, -(0.5 - 1 / 16f));

			if (renderState.isMainChest && (renderState.showsUpgrades || holdsItemThatShowsUpgrades())) {
				poseStack.pushPose();
				if (renderState.chestType == ChestType.LEFT) {
					poseStack.translate(1, 0, 0);
				}

				if (renderState.showUpgradesOnTop) {
					if (renderState.open > 0) {
						poseStack.translate(0, 9 / 16D, 14 / 16D);
						poseStack.mulPose(Axis.XP.rotationDegrees(renderState.open * 90));
						poseStack.translate(0, -9 / 16D, -14 / 16D);
					}
					poseStack.translate(0.5, 0.5, (0.5 - 1 / 16f));
					poseStack.mulPose(Axis.XP.rotationDegrees(90));
					poseStack.translate(-0.5, -(0.5 - 1 / 16f), -(0.5 - 2 / 16f));
				}

				displayItemRenderer.submitUpgradeItems(submitNodeCollector, renderState, poseStack, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades);
				poseStack.popPose();
			}

			if (renderState.isMainChest) {
				submitLocked(submitNodeCollector, renderState, poseStack, renderState.chestType, renderState.open);
			}
			poseStack.popPose();

			if (renderState.isMainChest) {
				if (!renderState.displayItems.isEmpty()) {
					submitDisplayItem(submitNodeCollector, renderState, poseStack, renderState.displayItems.getFirst());
				}
			}
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	@Override
	protected StorageWrapper getStorageWrapper(ChestBlockEntity blockEntity) {
		return blockEntity.getMainStorageWrapper();
	}

	private static class ChestSubRenderer {
		private final ChestType chestType;
		private final ChestCoreModel coreModel;
		private final ChestCoreModel hiddenTierModel;
		private final ChestLockModel lockModel;

		private Map<StorageTextureManager.ChestMaterial, Material> chestMaterials;
		private Material tierMaterial;

		public ChestSubRenderer(ChestType chestType, ModelPart corePart, ModelPart lockPart) {
			this.chestType = chestType;
			this.coreModel = new ChestCoreModel(corePart, RenderType::entityCutout);
			this.hiddenTierModel = new ChestCoreModel(corePart, RenderType::entityTranslucent);
			this.lockModel = new ChestLockModel(lockPart, RenderType::entityCutout);
		}

		private boolean setChestMaterialsFrom(WoodType woodType, Block block) {
			chestMaterials = StorageTextureManager.INSTANCE.getWoodChestMaterials(chestType, woodType);
			if (chestMaterials == null) {
				return false;
			}

			tierMaterial = getTierMaterial(block);

			return true;
		}

		private void submitHiddenTier(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, ChestRenderState renderState, MaterialSet materialSet) {
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);

			int color = 0x7F_FFFFFF;

			RenderType renderType = RenderType.entityTranslucent(tierMaterial.atlasLocation());
			TextureAtlasSprite sprite = materialSet.get(tierMaterial);
			submitNodeCollector.submitModel(hiddenTierModel, renderState.open, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, color,
					sprite, 0, renderState.breakProgress);
			poseStack.popPose();
		}

		private void submitBottomAndLid(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack,
				StorageTextureManager.ChestMaterial chestMaterial, MaterialSet materialSet) {
			Material material = chestMaterials.get(chestMaterial);
			RenderType renderType = material.renderType(RenderType::entityCutout);
			TextureAtlasSprite sprite = materialSet.get(material);

			submitBottomAndLid(submitNodeCollector, renderState, poseStack, renderType, sprite);
		}

		private void submitBottomAndLid(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack, RenderType renderType,
				TextureAtlasSprite sprite) {
			if (renderState.open > 0) {
				poseStack.pushPose();
				poseStack.translate(-0.0005F, -0.001F, -0.0005F);
				poseStack.scale(1.001F, 1.001F, 1.001F);
			}
			submitNodeCollector.submitModel(coreModel, renderState.open, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite,
					0, renderState.breakProgress);
			if (renderState.open > 0) {
				poseStack.popPose();
			}
		}

		private void submitBottomAndLidWithTint(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack, int tint,
				StorageTextureManager.ChestMaterial chestMaterial, MaterialSet materialSet) {
			Material material = chestMaterials.get(chestMaterial);
			RenderType renderType = material.renderType(RenderType::entityCutout);
			TextureAtlasSprite sprite = materialSet.get(material);
			int color = 0xFF_000000 | tint;

			if (renderState.open > 0) {
				poseStack.pushPose();
				poseStack.translate(-0.0005F, -0.001F, -0.0005F);
				poseStack.scale(1.001F, 1.001F, 1.001F);
			}
			submitNodeCollector.submitModel(coreModel, renderState.open, poseStack, renderType, renderState.lightCoords, OverlayTexture.NO_OVERLAY, color,
					sprite, 0, renderState.breakProgress);
			if (renderState.open > 0) {
				poseStack.popPose();
			}
		}

		private void submitChestLock(SubmitNodeCollector submitNodeCollector, ChestRenderState renderState, PoseStack poseStack, MaterialSet materialSet) {
			submitNodeCollector.submitModel(lockModel, renderState.open, poseStack, RenderType.entityCutout(tierMaterial.atlasLocation()),
					renderState.lightCoords, OverlayTexture.NO_OVERLAY, -1, materialSet.get(tierMaterial), 0, renderState.breakProgress);
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

		public void submitTier(SubmitNodeCollector submitNodeCollector, ChestRenderState chestRenderState, PoseStack poseStack, MaterialSet materialSet) {
			RenderType renderType = RenderType.entityCutout(tierMaterial.atlasLocation());
			TextureAtlasSprite sprite = materialSet.get(tierMaterial);
			submitBottomAndLid(submitNodeCollector, chestRenderState, poseStack, renderType, sprite);
		}
	}

	private static class ChestCoreModel extends Model<Float> {
		private final ModelPart lidPart;

		public ChestCoreModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
			super(root, renderType);
			this.lidPart = root.getChild("lid");
		}

		@Override
		public void setupAnim(Float lidAngle) {
			super.setupAnim(lidAngle);

			lidPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		}
	}

	private static class ChestLockModel extends Model<Float> {
		private final ModelPart lockPart;

		public ChestLockModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
			super(root, renderType);
			this.lockPart = root.getChild("lock");
		}

		@Override
		public void setupAnim(Float lidAngle) {
			super.setupAnim(lidAngle);
			lockPart.xRot = -(lidAngle * ((float) Math.PI / 2F));
		}
	}

	public static class ChestRenderState extends StorageRenderState {
		public Optional<WoodType> woodType;
		public boolean isGenericWood;
		public Optional<GenericWoodStorageTintCache.TintColors> genericTintColors = Optional.empty();
		public ChestType chestType;
		public Block block;
		public Direction facing;
		public float open;
		public boolean hasMainColor;
		public boolean hasAccentColor;
		public int mainColor;
		public int accentColor;
		public boolean packed;
		public boolean isMainChest;
		public boolean showUpgradesOnTop;
	}
}
