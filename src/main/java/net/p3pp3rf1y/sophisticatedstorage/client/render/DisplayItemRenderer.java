package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public class DisplayItemRenderer {
	private static final ItemStack EMPTY_UPGRADE_STACK = new ItemStack(ModItems.UPGRADE_BASE.get());
	public static final float SMALL_BLOCK_ITEM_SCALE = 0.5f;
	static final float BIG_ITEM_SCALE = 0.5f;
	static final float SMALL_ITEM_SCALE = 0.25f;
	static final float UPGRADE_ITEM_SCALE = 0.125f;
	private static final double DISPLAY_ITEM_PIXEL_SIZE_DIVISOR = 15.95D;
	private static final ItemStack INACCESSIBLE_SLOT_STACK = new ItemStack(ModItems.INACCESSIBLE_SLOT.get());
	private static final RandomSource RAND = new ThreadSafeLegacyRandomSource(RandomSupport.generateUniqueSeed());
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final int MAX_OFFSET_CACHE_SIZE = 4096;
	private static final Map<BakedModel, Double> MODEL_BASE_OFFSETS = new ConcurrentHashMap<>();
	private static final Map<Block, Double> BLOCK_SHAPE_BASE_OFFSETS = new ConcurrentHashMap<>();
	private final RandomSource renderRand = RandomSource.create();
	private final Vector3f scratchNormal = new Vector3f();
	private int lastTintIndex = -1;
	private int lastTintColor = -1;
	private final double yCenterTranslation;
	private final Vec3 upgradesOffset;

	public DisplayItemRenderer(double yCenterTranslation, Vec3 upgradesOffset) {
		this.yCenterTranslation = yCenterTranslation;
		this.upgradesOffset = upgradesOffset;
	}

	public static void invalidateCache() {
		MODEL_BASE_OFFSETS.clear();
		BLOCK_SHAPE_BASE_OFFSETS.clear();
	}

	public void renderDisplayItem(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		boolean cullBackFaces = blockEntity.hasLevel();
		blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItem()
				.ifPresent(displayItem -> renderDisplayItem(poseStack, bufferSource, packedLight, packedOverlay, displayItem, cullBackFaces));
	}

	public void renderDisplayItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, RenderInfo.DisplayItem displayItem) {
		renderDisplayItem(poseStack, bufferSource, packedLight, packedOverlay, displayItem, false);
	}

	public void renderDisplayItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, RenderInfo.DisplayItem displayItem,
			boolean cullBackFaces) {
		renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, Minecraft.getInstance(), false, cullBackFaces, 0, 1, displayItem.getItem(),
				displayItem.getRotation(), displayItem.getZOffset());
	}

	public void renderDisplayItems(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			boolean renderOnlyCustom) {
		RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo();
		List<RenderInfo.DisplayItem> displayItems = itemDisplayRenderInfo.getDisplayItems();
		List<Integer> inaccessibleSlots = itemDisplayRenderInfo.getInaccessibleSlots();
		if (displayItems.isEmpty() && inaccessibleSlots.isEmpty()) {
			return;
		}

		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		boolean cullBackFaces = blockEntity.hasLevel();
		int displayItemCount = storageBlock.getDisplayItemsCount(displayItems);
		for (int displayItemIndex = 0; displayItemIndex < displayItemCount; displayItemIndex++) {
			if (inaccessibleSlots.contains(displayItemIndex)) {
				renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, minecraft, renderOnlyCustom, cullBackFaces, displayItemIndex,
						displayItemCount, INACCESSIBLE_SLOT_STACK, 0, 0);
			}
		}
		int displayItemIndex = 0;
		for (RenderInfo.DisplayItem displayItem : displayItems) {
			renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, minecraft, renderOnlyCustom, cullBackFaces,
					storageBlock.hasFixedIndexDisplayItems() ? displayItem.getSlotIndex() : displayItemIndex, displayItemCount, displayItem.getItem(),
					displayItem.getRotation(), displayItem.getZOffset());
			displayItemIndex++;
		}
	}

	public void renderUpgradeItems(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			boolean renderEmptySlots, boolean renderDisabledUpgradeDisplay) {
		List<ItemStack> upgradeItems = blockEntity.getStorageWrapper().getRenderInfo().getUpgradeItems();

		poseStack.pushPose();

		Minecraft minecraft = Minecraft.getInstance();
		boolean cullBackFaces = blockEntity.hasLevel();
		int i = 0;
		for (ItemStack upgradeItem : upgradeItems) {
			if (upgradeItem.isEmpty() && !renderEmptySlots) {
				continue;
			}

			poseStack.pushPose();
			poseStack.translate(1f - i * 2 / 16f - 1 / 16f + upgradesOffset.x(), 1 / 16f + upgradesOffset.y(), upgradesOffset.z());
			poseStack.scale(UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE);
			ItemStack itemToRender = upgradeItem.isEmpty() ? EMPTY_UPGRADE_STACK : upgradeItem;
			BakedModel itemModel = minecraft.getItemRenderer().getModel(itemToRender, null, minecraft.player, 0);
			renderItemModel(minecraft, itemToRender, itemModel, poseStack, bufferSource, packedLight, packedOverlay, cullBackFaces);
			if (renderDisabledUpgradeDisplay) {
				poseStack.pushPose();
				poseStack.translate(0, 0, -0.001f);
				itemModel = minecraft.getItemRenderer().getModel(INACCESSIBLE_SLOT_STACK, null, minecraft.player, 0);
				renderItemModel(minecraft, INACCESSIBLE_SLOT_STACK, itemModel, poseStack, bufferSource, packedLight, packedOverlay, cullBackFaces);
				poseStack.popPose();
			}
			poseStack.popPose();
			i++;
		}

		poseStack.popPose();
	}

	private void renderSingleItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Minecraft minecraft,
			boolean renderOnlyCustom, boolean cullBackFaces, int displayItemIndex, int displayItemCount, ItemStack stack, int rotation, int zOffset) {
		if (stack.isEmpty()) {
			return;
		}
		BakedModel itemModel = minecraft.getItemRenderer().getModel(stack, null, minecraft.player, 0);
		if (!itemModel.isCustomRenderer() && renderOnlyCustom) {
			return;
		}

		float itemScale;
		if (displayItemCount == 1) {
			itemScale = stack.getItem() instanceof BlockItem && itemModel.isGui3d() ? 1.0f : BIG_ITEM_SCALE;
		} else {
			itemScale = stack.getItem() instanceof BlockItem && itemModel.isGui3d() ? SMALL_BLOCK_ITEM_SCALE : SMALL_ITEM_SCALE;
		}

		float itemOffset = (float) getDisplayItemOffset(stack, itemModel, itemScale);
		double zPixelOffset = zOffset != 0 ? zOffset * getDisplayItemPixelOffset(itemModel, itemScale) : 0;
		poseStack.pushPose();

		Vector3f frontOffset = getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, (float) yCenterTranslation);
		poseStack.translate(frontOffset.x(), frontOffset.y(), -itemOffset - zPixelOffset);
		if (rotation != 0) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
		}
		poseStack.scale(itemScale, itemScale, itemScale);

		renderItemModel(minecraft, stack, itemModel, poseStack, bufferSource, packedLight, packedOverlay, cullBackFaces);
		poseStack.popPose();
	}

	private void renderItemModel(Minecraft minecraft, ItemStack stack, BakedModel itemModel, PoseStack poseStack, MultiBufferSource bufferSource,
			int packedLight, int packedOverlay, boolean cullBackFaces) {
		if (itemModel.isCustomRenderer() || stack.hasFoil() || stack.is(Items.TRIDENT) || stack.is(Items.SPYGLASS)) {
			minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
			return;
		}

		poseStack.pushPose();
		BakedModel transformedModel = ClientHooks.handleCameraTransforms(poseStack, itemModel, ItemDisplayContext.FIXED, false);
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		if (transformedModel.isCustomRenderer()) {
			poseStack.popPose();
			minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
			return;
		}

		boolean fabulous = true;
		if (stack.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block instanceof HalfTransparentBlock || block instanceof StainedGlassPaneBlock) {
				fabulous = false;
			}
		}

		List<BakedModel> passes = transformedModel.getRenderPasses(stack, fabulous);
		for (BakedModel pass : passes) {
			if (pass.isCustomRenderer()) {
				poseStack.popPose();
				minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
				return;
			}
		}

		ItemColors itemColors = minecraft.getItemColors();
		PoseStack.Pose pose = poseStack.last();
		for (BakedModel pass : passes) {
			for (RenderType renderType : pass.getRenderTypes(stack, fabulous)) {
				VertexConsumer consumer = bufferSource.getBuffer(renderType);
				if (consumer.getClass() == BufferBuilder.class) {
					boolean canCull = cullBackFaces && isCulledRenderType(renderType);
					renderModelListsDirect(pass, stack, itemColors, packedLight, packedOverlay, pose, consumer, canCull);
				} else {
					minecraft.getItemRenderer().renderModelLists(pass, stack, packedLight, packedOverlay, poseStack, consumer);
				}
			}
		}
		poseStack.popPose();
	}

	private void renderModelListsDirect(BakedModel model, ItemStack stack, ItemColors itemColors, int packedLight, int packedOverlay, PoseStack.Pose pose,
			VertexConsumer consumer, boolean cullBackFaces) {
		lastTintIndex = -1;
		lastTintColor = -1;
		for (Direction direction : DIRECTIONS) {
			renderRand.setSeed(42L);
			List<BakedQuad> quads = model.getQuads(null, direction, renderRand);
			if (!quads.isEmpty()) {
				renderQuadListDirect(pose, consumer, quads, stack, itemColors, packedLight, packedOverlay, cullBackFaces);
			}
		}
		renderRand.setSeed(42L);
		List<BakedQuad> quads = model.getQuads(null, null, renderRand);
		if (!quads.isEmpty()) {
			renderQuadListDirect(pose, consumer, quads, stack, itemColors, packedLight, packedOverlay, cullBackFaces);
		}
	}

	private void renderQuadListDirect(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, ItemStack stack, ItemColors itemColors,
			int packedLight, int packedOverlay, boolean cullBackFaces) {
		Matrix4f mat = pose.pose();
		float m00 = mat.m00();
		float m01 = mat.m01();
		float m02 = mat.m02();
		float m10 = mat.m10();
		float m11 = mat.m11();
		float m12 = mat.m12();
		float m20 = mat.m20();
		float m21 = mat.m21();
		float m22 = mat.m22();
		float m30 = mat.m30();
		float m31 = mat.m31();
		float m32 = mat.m32();

		int count = quads.size();
		for (int q = 0; q < count; q++) {
			BakedQuad quad = quads.get(q);
			int[] verts = quad.getVertices();
			if (verts.length != 32) {
				emitQuadFallback(pose, consumer, quad, stack, itemColors, packedLight, packedOverlay);
				continue;
			}

			float vx0 = Float.intBitsToFloat(verts[0]);
			float vy0 = Float.intBitsToFloat(verts[1]);
			float vz0 = Float.intBitsToFloat(verts[2]);
			float x0 = Math.fma(m00, vx0, Math.fma(m10, vy0, Math.fma(m20, vz0, m30)));
			float y0 = Math.fma(m01, vx0, Math.fma(m11, vy0, Math.fma(m21, vz0, m31)));
			float z0 = Math.fma(m02, vx0, Math.fma(m12, vy0, Math.fma(m22, vz0, m32)));

			float vx1 = Float.intBitsToFloat(verts[8]);
			float vy1 = Float.intBitsToFloat(verts[9]);
			float vz1 = Float.intBitsToFloat(verts[10]);
			float x1 = Math.fma(m00, vx1, Math.fma(m10, vy1, Math.fma(m20, vz1, m30)));
			float y1 = Math.fma(m01, vx1, Math.fma(m11, vy1, Math.fma(m21, vz1, m31)));
			float z1 = Math.fma(m02, vx1, Math.fma(m12, vy1, Math.fma(m22, vz1, m32)));

			float vx2 = Float.intBitsToFloat(verts[16]);
			float vy2 = Float.intBitsToFloat(verts[17]);
			float vz2 = Float.intBitsToFloat(verts[18]);
			float x2 = Math.fma(m00, vx2, Math.fma(m10, vy2, Math.fma(m20, vz2, m30)));
			float y2 = Math.fma(m01, vx2, Math.fma(m11, vy2, Math.fma(m21, vz2, m31)));
			float z2 = Math.fma(m02, vx2, Math.fma(m12, vy2, Math.fma(m22, vz2, m32)));

			if (cullBackFaces) {
				float e1x = x1 - x0;
				float e1y = y1 - y0;
				float e1z = z1 - z0;
				float e2x = x2 - x0;
				float e2y = y2 - y0;
				float e2z = z2 - z0;
				float cx = e1y * e2z - e1z * e2y;
				float cy = e1z * e2x - e1x * e2z;
				float cz = e1x * e2y - e1y * e2x;
				if (x0 * cx + y0 * cy + z0 * cz > 1.0e-6f) {
					continue;
				}
			}

			TextureAtlasSprite sprite = quad.getSprite();
			if (sprite != null && sprite.contents().animatedTexture != null) {
				emitQuadFallback(pose, consumer, quad, stack, itemColors, packedLight, packedOverlay);
				continue;
			}

			float vx3 = Float.intBitsToFloat(verts[24]);
			float vy3 = Float.intBitsToFloat(verts[25]);
			float vz3 = Float.intBitsToFloat(verts[26]);
			float x3 = Math.fma(m00, vx3, Math.fma(m10, vy3, Math.fma(m20, vz3, m30)));
			float y3 = Math.fma(m01, vx3, Math.fma(m11, vy3, Math.fma(m21, vz3, m31)));
			float z3 = Math.fma(m02, vx3, Math.fma(m12, vy3, Math.fma(m22, vz3, m32)));

			int tintColor = getTintColor(quad, stack, itemColors);
			Vec3i dirNormal = quad.getDirection().getNormal();
			pose.transformNormal(dirNormal.getX(), dirNormal.getY(), dirNormal.getZ(), scratchNormal);
			float nx = scratchNormal.x();
			float ny = scratchNormal.y();
			float nz = scratchNormal.z();

			consumer.addVertex(x0, y0, z0, combineColor(verts[3], tintColor), Float.intBitsToFloat(verts[4]), Float.intBitsToFloat(verts[5]), packedOverlay,
					combineLight(packedLight, verts[6]), nx, ny, nz);
			consumer.addVertex(x1, y1, z1, combineColor(verts[11], tintColor), Float.intBitsToFloat(verts[12]), Float.intBitsToFloat(verts[13]), packedOverlay,
					combineLight(packedLight, verts[14]), nx, ny, nz);
			consumer.addVertex(x2, y2, z2, combineColor(verts[19], tintColor), Float.intBitsToFloat(verts[20]), Float.intBitsToFloat(verts[21]), packedOverlay,
					combineLight(packedLight, verts[22]), nx, ny, nz);
			consumer.addVertex(x3, y3, z3, combineColor(verts[27], tintColor), Float.intBitsToFloat(verts[28]), Float.intBitsToFloat(verts[29]), packedOverlay,
					combineLight(packedLight, verts[30]), nx, ny, nz);
		}
	}

	private int getTintColor(BakedQuad quad, ItemStack stack, ItemColors itemColors) {
		int tintIndex = quad.getTintIndex();
		if (tintIndex < 0) {
			return -1;
		}
		if (tintIndex == lastTintIndex) {
			return lastTintColor;
		}
		int tintColor = itemColors.getColor(stack, tintIndex);
		lastTintIndex = tintIndex;
		lastTintColor = tintColor;
		return tintColor;
	}

	private void emitQuadFallback(PoseStack.Pose pose, VertexConsumer consumer, BakedQuad quad, ItemStack stack, ItemColors itemColors, int packedLight,
			int packedOverlay) {
		int tintColor = getTintColor(quad, stack, itemColors);
		float a = FastColor.ARGB32.alpha(tintColor) / 255.0F;
		float r = FastColor.ARGB32.red(tintColor) / 255.0F;
		float g = FastColor.ARGB32.green(tintColor) / 255.0F;
		float b = FastColor.ARGB32.blue(tintColor) / 255.0F;
		consumer.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, true);
	}

	private static int combineColor(int vertexAbgr, int tintArgb) {
		if (vertexAbgr == -1) {
			return tintArgb;
		}
		if (tintArgb == -1) {
			return (vertexAbgr & 0xFF00FF00) | ((vertexAbgr & 0xFF) << 16) | ((vertexAbgr >>> 16) & 0xFF);
		}
		int vr = vertexAbgr & 0xFF;
		int vg = (vertexAbgr >>> 8) & 0xFF;
		int vb = (vertexAbgr >>> 16) & 0xFF;
		int va = (vertexAbgr >>> 24) & 0xFF;
		int ta = (tintArgb >>> 24) & 0xFF;
		int tr = (tintArgb >>> 16) & 0xFF;
		int tg = (tintArgb >>> 8) & 0xFF;
		int tb = tintArgb & 0xFF;
		return ((va * ta / 255) << 24) | ((vr * tr / 255) << 16) | ((vg * tg / 255) << 8) | (vb * tb / 255);
	}

	private static int combineLight(int packedLight, int bakedLight) {
		if (bakedLight == 0) {
			return packedLight;
		}
		return LightTexture.pack(Math.max(LightTexture.block(packedLight), LightTexture.block(bakedLight)),
				Math.max(LightTexture.sky(packedLight), LightTexture.sky(bakedLight)));
	}

	private static boolean isCulledRenderType(RenderType renderType) {
		return renderType == Sheets.cutoutBlockSheet() || renderType == Sheets.solidBlockSheet() || renderType == Sheets.translucentCullBlockSheet()
				|| renderType == Sheets.translucentItemSheet() || renderType == RenderType.cutout() || renderType == RenderType.cutoutMipped()
				|| renderType == RenderType.solid() || renderType == RenderType.translucent();
	}

	public static double getDisplayItemOffset(ItemStack item, BakedModel itemModel, float additionalScale) {
		if (!itemModel.isGui3d() || !(item.getItem() instanceof BlockItem blockItem)) {
			return 0;
		}
		if (itemModel.isCustomRenderer()) {
			Block block = blockItem.getBlock();
			Double cached = BLOCK_SHAPE_BASE_OFFSETS.get(block);
			if (cached != null) {
				return cached * additionalScale;
			}
			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return 0;
			}
			double baseOffset = transformBoundsCornersAndCalculateBaseOffset(itemModel, getBoundsCornersFromShape(block, level));
			if (BLOCK_SHAPE_BASE_OFFSETS.size() < MAX_OFFSET_CACHE_SIZE) {
				BLOCK_SHAPE_BASE_OFFSETS.put(block, baseOffset);
			}
			return baseOffset * additionalScale;
		}

		Double cached = MODEL_BASE_OFFSETS.get(itemModel);
		if (cached != null) {
			return cached * additionalScale;
		}
		double baseOffset = transformBoundsCornersAndCalculateBaseOffset(itemModel, getBoundsCornersFromModel(itemModel));
		if (MODEL_BASE_OFFSETS.size() < MAX_OFFSET_CACHE_SIZE) {
			MODEL_BASE_OFFSETS.put(itemModel, baseOffset);
		}
		return baseOffset * additionalScale;
	}

	public static double getDisplayItemPixelOffset(BakedModel itemModel, float additionalScale) {
		ItemTransform transform = itemModel.getTransforms().getTransform(ItemDisplayContext.FIXED);
		return transform.scale.z() * (1 / DISPLAY_ITEM_PIXEL_SIZE_DIVISOR) * additionalScale;
	}

	@SuppressWarnings("deprecation")
	private static double transformBoundsCornersAndCalculateBaseOffset(BakedModel itemModel, Set<Vector3f> points) {
		ItemTransform transform = itemModel.getTransforms().getTransform(ItemDisplayContext.FIXED);
		points = scalePoints(points, transform.scale);
		points = rotatePoints(points, transform.rotation);
		points = translatePoints(points, transform.translation);

		float zScale = transform.scale.z();
		return (zScale * (2 / DISPLAY_ITEM_PIXEL_SIZE_DIVISOR)) - getMaxZ(points); // 15.95 because of z-fighting if displayed model had
																					// surface offset exactly 1
		// pixel from the top most surface
	}

	private static Set<Vector3f> getBoundsCornersFromShape(Block block, ClientLevel level) {
		VoxelShape shape = block.defaultBlockState().getShape(level, BlockPos.ZERO, CollisionContext.empty());
		return getCornerPointsRelativeToCenter(shape.bounds());
	}

	private static Set<Vector3f> getBoundsCornersFromModel(BakedModel itemModel) {
		float minX = 2;
		float minY = 2;
		float minZ = 2;
		float maxX = -2;
		float maxY = -2;
		float maxZ = -2;

		for (Direction direction : Direction.values()) {
			List<BakedQuad> quads = itemModel.getQuads(null, direction, RAND, ModelData.EMPTY, null);
			for (BakedQuad quad : quads) {
				int i = 0;
				int[] verts = quad.getVertices();
				while (i + 2 < verts.length) {
					float x = Float.intBitsToFloat(verts[i]);
					float y = Float.intBitsToFloat(verts[i + 1]);
					float z = Float.intBitsToFloat(verts[i + 2]);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					minZ = Math.min(minZ, z);
					maxZ = Math.max(maxZ, z);
					i += 8;
				}
			}
		}
		List<BakedQuad> quads = itemModel.getQuads(null, null, RAND, ModelData.EMPTY, null);
		for (BakedQuad quad : quads) {
			int i = 0;
			int[] verts = quad.getVertices();
			while (i + 2 < verts.length) {
				float x = Float.intBitsToFloat(verts[i]);
				float y = Float.intBitsToFloat(verts[i + 1]);
				float z = Float.intBitsToFloat(verts[i + 2]);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
				minZ = Math.min(minZ, z);
				maxZ = Math.max(maxZ, z);
				i += 8;
			}
		}

		return getCornerPointsRelativeToCenter(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static double getMaxZ(Set<Vector3f> points) {
		float maxZ = Float.MIN_VALUE;
		for (Vector3f point : points) {
			if (point.z() > maxZ) {
				maxZ = point.z();
			}
		}
		return maxZ;
	}

	private static Set<Vector3f> translatePoints(Set<Vector3f> points, Vector3f translation) {
		return transformPoints(points, point -> {
			point.sub(translation);
			return point;
		});
	}

	private static Set<Vector3f> rotatePoints(Set<Vector3f> points, Vector3f rotation) {
		Quaternionf rot = TransformationHelper.quatFromXYZ(rotation.x(), rotation.y(), rotation.z(), true);
		return transformPoints(points, point -> {
			point.rotate(rot);
			return point;
		});
	}

	private static Set<Vector3f> scalePoints(Set<Vector3f> points, Vector3f scale) {
		return transformPoints(points, point -> new Vector3f(point.x() * scale.x(), point.y() * scale.y(), point.z() * scale.z()));
	}

	private static Set<Vector3f> transformPoints(Set<Vector3f> points, UnaryOperator<Vector3f> transform) {
		Set<Vector3f> ret = new HashSet<>();

		for (Vector3f point : points) {
			ret.add(transform.apply(point));
		}

		return ret;
	}

	private static Set<Vector3f> getCornerPointsRelativeToCenter(AABB aabb) {
		return getCornerPointsRelativeToCenter((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ, (float) aabb.maxX, (float) aabb.maxY,
				(float) aabb.maxZ);
	}

	private static Set<Vector3f> getCornerPointsRelativeToCenter(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		Set<Vector3f> ret = new HashSet<>();
		ret.add(new Vector3f(0.5F - minX, 0.5F - minY, 0.5F - minZ));
		ret.add(new Vector3f(0.5F - minX, 0.5F - minY, 0.5F - maxZ));
		ret.add(new Vector3f(0.5F - minX, 0.5F - maxY, 0.5F - minZ));
		ret.add(new Vector3f(0.5F - minX, 0.5F - maxY, 0.5F - maxZ));
		ret.add(new Vector3f(0.5F - maxX, 0.5F - minY, 0.5F - minZ));
		ret.add(new Vector3f(0.5F - maxX, 0.5F - minY, 0.5F - maxZ));
		ret.add(new Vector3f(0.5F - maxX, 0.5F - maxY, 0.5F - minZ));
		ret.add(new Vector3f(0.5F - maxX, 0.5F - maxY, 0.5F - maxZ));
		return ret;
	}

	public static Vector3f getDisplayItemIndexFrontOffset(int displayItemIndex, int displayItemCount) {
		return getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, 0.5f);
	}

	public static Vector3f getDisplayItemIndexFrontOffset(int displayItemIndex, int displayItemCount, float centerYOffset) {
		Vector3f frontOffset;
		if (displayItemCount <= 0 || displayItemCount > 4) {
			frontOffset = new Vector3f(0f, 0f, 0.5f);
		} else if (displayItemCount == 1) {
			frontOffset = new Vector3f(0.5f, centerYOffset, 0.5f);
		} else if (displayItemCount == 2) {
			float halfCenterYOffset = centerYOffset / 2;
			frontOffset = new Vector3f(0.5f, displayItemIndex == 0 ? centerYOffset + halfCenterYOffset : halfCenterYOffset, 0.5f);
		} else if (displayItemCount == 3) {
			float xOffset = 0.5f;

			if (displayItemIndex > 0) {
				xOffset = 0.75f - (displayItemIndex - 1) * 0.5f;
			}

			float halfCenterYOffset = centerYOffset / 2;
			frontOffset = new Vector3f(xOffset, displayItemIndex == 0 ? centerYOffset + halfCenterYOffset : halfCenterYOffset, 0.5f);
		} else {
			float halfCenterYOffset = centerYOffset / 2;
			frontOffset = new Vector3f(displayItemIndex == 0 || displayItemIndex == 2 ? centerYOffset + halfCenterYOffset : halfCenterYOffset,
					displayItemIndex == 0 || displayItemIndex == 1 ? centerYOffset + halfCenterYOffset : halfCenterYOffset, 0.5f);
		}

		return frontOffset;
	}

	public static Quaternionf getNorthBasedRotation(Direction dir) {
		return switch (dir) {
			case DOWN -> Axis.XP.rotationDegrees(-90.0F);
			case UP -> Axis.XP.rotationDegrees(90.0F);
			case NORTH -> new Quaternionf();
			case SOUTH -> Axis.YP.rotationDegrees(180.0F);
			case WEST -> Axis.YP.rotationDegrees(90.0F);
			case EAST -> Axis.YP.rotationDegrees(-90.0F);
		};
	}
}
