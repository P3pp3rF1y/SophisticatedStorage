package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
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
import net.p3pp3rf1y.sophisticatedstorage.client.GenericWoodStorageTintCache;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public class ChestRenderer extends StorageRenderer<ChestBlockEntity> {
	private static final String BOTTOM = "bottom";
	private static final String LID = "lid";
	private static final String LOCK = "lock";
	private static final int TEXTURE_SIZE = 64;
	private static final Map<ChestType, LayerDefinition> DEFAULT_LAYERS = Map.of(
			ChestType.SINGLE, createBodyLayer(ChestType.SINGLE, true, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE),
			ChestType.RIGHT, createBodyLayer(ChestType.RIGHT, true, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE),
			ChestType.LEFT, createBodyLayer(ChestType.LEFT, true, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE));
	private static final int GENERIC_WOOD_CHEST_ACCENT_COLOR = 0x463E32;
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5 * (14.01 / 16), new Vec3(-1 / 16D, 0, -0.0075));

	private final Map<ChestType, ChestSubRenderer> chestSubRenderers;

	public ChestRenderer(BlockEntityRendererProvider.Context context) {
		EntityModelSet modelSet = context.getModelSet();
		chestSubRenderers = Map.of(
				ChestType.SINGLE, new ChestSubRenderer(ChestType.SINGLE, modelSet, ClientEventHandler.CHEST_LAYER),
				ChestType.RIGHT, new ChestSubRenderer(ChestType.RIGHT, modelSet, ClientEventHandler.CHEST_RIGHT_LAYER),
				ChestType.LEFT, new ChestSubRenderer(ChestType.LEFT, modelSet, ClientEventHandler.CHEST_LEFT_LAYER));
	}

	public static LayerDefinition createSingleBodyLayer(boolean addLock) {
		return addLock ? DEFAULT_LAYERS.get(ChestType.SINGLE) : createBodyLayer(ChestType.SINGLE, false, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	public static LayerDefinition createDoubleBodyRightLayer() {
		return DEFAULT_LAYERS.get(ChestType.RIGHT);
	}

	public static LayerDefinition createDoubleBodyLeftLayer() {
		return DEFAULT_LAYERS.get(ChestType.LEFT);
	}

	private static LayerDefinition createBodyLayer(ChestType chestType, boolean addLock, int u, int v, int textureWidth, int textureHeight) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		int x = chestType == ChestType.LEFT ? 0 : 1;
		int width = chestType == ChestType.SINGLE ? 14 : 15;
		root.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(u, v + 19).addBox(x, 0, 1, width, 10, 14), PartPose.ZERO);
		root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(u, v).addBox(x, 0, 0, width, 5, 14), PartPose.offset(0, 9, 1));
		if (addLock) {
			switch (chestType) {
				case SINGLE -> root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(7, -1, 15, 2, 4, 1), PartPose.offset(0, 8, 0));
				case RIGHT -> root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(15, -2, 14, 1, 4, 1), PartPose.offset(0, 9, 1));
				case LEFT -> root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(0, -2, 14, 1, 4, 1), PartPose.offset(0, 9, 1));
			}
		}
		return LayerDefinition.create(mesh, textureWidth, textureHeight);
	}

	public void render(ChestBlockEntity chestEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			int packedOverlay) {
		render(chestEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, false);
	}

	public void render(ChestBlockEntity chestEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			boolean forceClosed) {
		BlockState blockstate = chestEntity.getBlockState();
		Optional<WoodType> woodType = chestEntity.getWoodType();
		ChestType chestType = blockstate.getValue(ChestBlock.TYPE);
		ChestSubRenderer subRenderer = chestSubRenderers.get(chestType);
		boolean isGenericWood = woodType.map(GenericWoodStorageHelper::isGenericWood).orElse(false);
		if (!subRenderer.setChestMaterialsFrom(isGenericWood ? WoodType.ACACIA : woodType.orElse(WoodType.ACACIA), blockstate.getBlock())) {
			return;
		}

		poseStack.pushPose();
		Direction facing = blockstate.getValue(ChestBlock.FACING);
		float f = facing.toYRot();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(-f));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
		float lidAngle = forceClosed ? 0 : chestEntity.getOpenNess(partialTick);
		lidAngle = 1.0F - lidAngle;
		lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;

		StorageWrapper storageWrapper = chestEntity.getMainStorageWrapper();
		boolean hasMainColor = storageWrapper.hasMainColor();
		boolean hasAccentColor = storageWrapper.hasAccentColor();
		Optional<GenericWoodStorageTintCache.TintColors> genericTintColors = isGenericWood
				? woodType.flatMap(GenericWoodStorageTintCache::getTintColors)
				: Optional.empty();

		if ((!isGenericWood && (woodType.isPresent() || !(hasMainColor && hasAccentColor))) || (isGenericWood && genericTintColors.isEmpty())) {
			subRenderer.renderBottomAndLid(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, StorageTextureManager.ChestMaterial.BASE);
		}
		if (hasMainColor || genericTintColors.isPresent()) {
			subRenderer.renderBottomAndLidWithTint(poseStack, bufferSource, lidAngle, packedLight, packedOverlay,
					hasMainColor ? storageWrapper.getMainColor() : genericTintColors.get().mainColor(), StorageTextureManager.ChestMaterial.TINTABLE_MAIN);
		}
		if (hasAccentColor || genericTintColors.isPresent()) {
			subRenderer.renderBottomAndLidWithTint(poseStack, bufferSource, lidAngle, packedLight, packedOverlay,
					hasAccentColor ? storageWrapper.getAccentColor() : GENERIC_WOOD_CHEST_ACCENT_COLOR, StorageTextureManager.ChestMaterial.TINTABLE_ACCENT);
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
		} else {
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

				if (chestEntity.showUpgradesOnTop) {
					if (lidAngle > 0) {
						poseStack.translate(0, 9 / 16D, 14 / 16D);
						poseStack.mulPose(Axis.XP.rotationDegrees(lidAngle * 90));
						poseStack.translate(0, -9 / 16D, -14 / 16D);
					}
					poseStack.translate(0.5, 0.5, (0.5 - 1 / 16f));
					poseStack.mulPose(Axis.XP.rotationDegrees(90));
					poseStack.translate(-0.5, -(0.5 - 1 / 16f), -(0.5 - 2 / 16f));
				}

				displayItemRenderer.renderUpgradeItems(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, holdsItemThatShowsUpgrades(),
						shouldShowDisabledUpgradesDisplay(chestEntity));
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

	private void renderDisplayItem(RenderInfo.DisplayItem displayItem, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			ChestType chestType) {
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

	private void renderLocked(ChestBlockEntity chestEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			ChestType chestType, float lidAngle) {
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
		LockRenderer.renderLock(chestEntity, poseStack, bufferSource, packedLight, packedOverlay, 13F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		poseStack.popPose();
	}

	private record ChestBody(ModelPart lid, ModelPart bottom) {
		private ChestBody(ModelPart root) {
			this(root.getChild(LID), root.getChild(BOTTOM));
		}

		private boolean matchesStock(ChestBody stock) {
			return matchesStockPart(lid, stock.lid) && matchesStockPart(bottom, stock.bottom);
		}

		private static boolean matchesStockPart(ModelPart part, ModelPart stock) {
			if (part.getClass() != ModelPart.class || !part.children.isEmpty() || part.cubes.size() != 1
					|| !part.visible || part.skipDraw || part.x != stock.x || part.y != stock.y || part.z != stock.z
					|| part.xRot != stock.xRot || part.yRot != stock.yRot || part.zRot != stock.zRot
					|| part.xScale != stock.xScale || part.yScale != stock.yScale || part.zScale != stock.zScale) {
				return false;
			}

			ModelPart.Cube cube = part.cubes.getFirst();
			ModelPart.Polygon[] expected = stock.cubes.getFirst().polygons;
			if (cube.getClass() != ModelPart.Cube.class || cube.polygons.length != expected.length) {
				return false;
			}
			for (int face = 0; face < expected.length; face++) {
				if (!matchesStockFace(cube.polygons[face], expected[face])) {
					return false;
				}
			}
			return true;
		}

		private static boolean matchesStockFace(ModelPart.Polygon face, ModelPart.Polygon stock) {
			if (!face.normal.equals(stock.normal) || face.vertices.length != stock.vertices.length) {
				return false;
			}
			for (int i = 0; i < face.vertices.length; i++) {
				ModelPart.Vertex vertex = face.vertices[i];
				ModelPart.Vertex expected = stock.vertices[i];
				if (vertex.u != expected.u || vertex.v != expected.v || !vertex.pos.equals(expected.pos)) {
					return false;
				}
			}
			return true;
		}

		private void render(PoseStack poseStack, VertexConsumer consumer, float lidAngle, int packedLight, int packedOverlay, int color) {
			lid.xRot = -(lidAngle * ((float) Math.PI / 2F));
			if (lidAngle > 0) {
				poseStack.pushPose();
				poseStack.translate(-0.0005F, -0.001F, -0.0005F);
				poseStack.scale(1.001F, 1.001F, 1.001F);
			}
			lid.render(poseStack, consumer, packedLight, packedOverlay, color);
			if (lidAngle > 0) {
				poseStack.popPose();
			}
			bottom.render(poseStack, consumer, packedLight, packedOverlay, color);
		}
	}

	private static class ChestSubRenderer {
		private final ChestType chestType;
		private final boolean usesStockBody;
		private final ChestBody body;
		private final ModelPart lockPart;
		private final Map<TextureAtlasSprite, ChestBody> tierBodies = new IdentityHashMap<>();
		private Map<StorageTextureManager.ChestMaterial, Material> chestMaterials;
		private Material tierMaterial;

		public ChestSubRenderer(ChestType chestType, EntityModelSet modelSet, ModelLayerLocation modelLayer) {
			this.chestType = chestType;
			ModelPart root = modelSet.bakeLayer(modelLayer);
			body = new ChestBody(root);
			lockPart = root.getChild(LOCK);
			LayerDefinition stock = DEFAULT_LAYERS.get(chestType);
			usesStockBody = modelSet.roots.get(modelLayer) == stock && root.getClass() == ModelPart.class
					&& body.matchesStock(new ChestBody(stock.bakeRoot()));
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
			if (bufferSource instanceof MultiBufferSource.BufferSource multiBufferSource) {
				multiBufferSource.endBatch();
			}
			VertexConsumer translucentConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlasLocation())));
			poseStack.pushPose();
			poseStack.translate(-0.005D, -0.005D, -0.005D);
			poseStack.scale(1.01f, 1.01f, 1.01f);

			int color = 0x7F_FFFFFF;

			body.lid().render(poseStack, translucentConsumer, packedLight, packedOverlay, color);
			body.bottom().render(poseStack, translucentConsumer, packedLight, packedOverlay, color);
			poseStack.popPose();
		}

		private void renderBottomAndLid(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay,
				StorageTextureManager.ChestMaterial chestMaterial) {
			renderBottomAndLidWithTint(poseStack, bufferSource, lidAngle, packedLight, packedOverlay, -1, chestMaterial);
		}

		private void renderBottomAndLidWithTint(PoseStack poseStack, MultiBufferSource bufferSource, float lidAngle, int packedLight, int packedOverlay,
				int tint, StorageTextureManager.ChestMaterial chestMaterial) {
			VertexConsumer consumer = chestMaterials.get(chestMaterial).buffer(bufferSource, RenderType::entityCutout);
			body.render(poseStack, consumer, lidAngle, packedLight, packedOverlay, 0xFF_000000 | tint);
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
			VertexConsumer consumer = tierMaterial.buffer(bufferSource, RenderType::entityCutout);
			ChestBody tierBody = body;
			if (usesStockBody && consumer.getClass() == SpriteCoordinateExpander.class) {
				SpriteCoordinateExpander expander = (SpriteCoordinateExpander) consumer;
				if (expander.delegate.getClass() == BufferBuilder.class && expander.sprite == tierMaterial.sprite()) {
					tierBody = tierBodies.computeIfAbsent(expander.sprite, this::bakeTierBody);
					if (tierBody != body) {
						consumer = expander.delegate;
					}
				}
			}
			tierBody.render(poseStack, consumer, lidAngle, packedLight, packedOverlay, -1);
		}

		private ChestBody bakeTierBody(TextureAtlasSprite sprite) {
			SpriteContents contents = sprite.contents();
			if (sprite.getClass() != TextureAtlasSprite.class || contents.getClass() != SpriteContents.class
					|| contents.width() != TEXTURE_SIZE || contents.height() != TEXTURE_SIZE) {
				return body;
			}

			int atlasWidth = atlasSize(sprite.getX(), sprite.getU0(), sprite.getU1());
			int atlasHeight = atlasSize(sprite.getY(), sprite.getV0(), sprite.getV1());
			if (atlasWidth == 0 || atlasHeight == 0) {
				return body;
			}
			// Bake the UVs before renderers cache the cuboid geometry.
			return new ChestBody(createBodyLayer(chestType, false, sprite.getX(), sprite.getY(), atlasWidth, atlasHeight).bakeRoot());
		}

		private static int atlasSize(int offset, float min, float max) {
			int size = Math.round(TEXTURE_SIZE / (max - min));
			return size >= TEXTURE_SIZE && Integer.bitCount(size) == 1 && offset >= 0 && offset <= size - TEXTURE_SIZE
					&& min == (float) offset / size && max == (float) (offset + TEXTURE_SIZE) / size ? size : 0;
		}
	}
}
