package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

public class DisplayItemRenderer {
	private static final ItemStack EMPTY_UPGRADE_STACK = new ItemStack(ModItems.UPGRADE_BASE.get());
	public static final float SMALL_3D_ITEM_SCALE = 0.5f;
	static final float BIG_2D_ITEM_SCALE = 0.5f;
	static final float SMALL_2D_ITEM_SCALE = 0.25f;
	static final float UPGRADE_ITEM_SCALE = 0.125f;
	private static final ItemStack INACCESSIBLE_SLOT_STACK = new ItemStack(ModItems.INACCESSIBLE_SLOT.get());
	private static final RandomSource RAND = new ThreadSafeLegacyRandomSource(RandomSupport.generateUniqueSeed());
	private final double yCenterTranslation;
	private final Vec3 upgradesOffset;

	public DisplayItemRenderer(double yCenterTranslation, Vec3 upgradesOffset) {
		this.yCenterTranslation = yCenterTranslation;
		this.upgradesOffset = upgradesOffset;
	}

	private static final Cache<Integer, Double> ITEM_HASHCODE_OFFSETS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();

	public void renderDisplayItem(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItem().ifPresent(displayItem ->
				renderDisplayItem(poseStack, bufferSource, packedLight, packedOverlay, displayItem));
	}

	public void renderDisplayItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, RenderInfo.DisplayItem displayItem) {
		renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, Minecraft.getInstance(), false, 0, 1, displayItem.getItem(), displayItem.getRotation());
	}

	public void renderDisplayItems(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean renderOnlyCustom) {
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
		int displayItemCount = storageBlock.getDisplayItemsCount(displayItems);
		for (int displayItemIndex = 0; displayItemIndex < displayItemCount; displayItemIndex++) {
			if (inaccessibleSlots.contains(displayItemIndex)) {
				renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, minecraft, renderOnlyCustom, displayItemIndex, displayItemCount, INACCESSIBLE_SLOT_STACK, 0);
			}
		}
		int displayItemIndex = 0;
		for (RenderInfo.DisplayItem displayItem : displayItems) {
			renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, minecraft, renderOnlyCustom, storageBlock.hasFixedIndexDisplayItems() ? displayItem.getSlotIndex() : displayItemIndex, displayItemCount, displayItem.getItem(), displayItem.getRotation());
			displayItemIndex++;
		}
	}

	public void renderUpgradeItems(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean renderEmptySlots, boolean renderDisabledUpgradeDisplay) {
		List<ItemStack> upgradeItems = blockEntity.getStorageWrapper().getRenderInfo().getUpgradeItems();

		poseStack.pushPose();

		Minecraft minecraft = Minecraft.getInstance();
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
			MultiBufferSource buffer = upgradeItem.isEmpty() ? TranslucentVertexConsumer.wrapBuffer(bufferSource, 128) : bufferSource;
			minecraft.getItemRenderer().render(itemToRender, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, packedOverlay, itemModel);
			if (renderDisabledUpgradeDisplay) {
				poseStack.pushPose();
				poseStack.translate(0, 0, -0.001f);
				itemModel = minecraft.getItemRenderer().getModel(INACCESSIBLE_SLOT_STACK, null, minecraft.player, 0);
				buffer = bufferSource;
				minecraft.getItemRenderer().render(INACCESSIBLE_SLOT_STACK, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, packedOverlay, itemModel);
				poseStack.popPose();
			}
			poseStack.popPose();
			i++;
		}

		poseStack.popPose();
	}

	private void renderSingleItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Minecraft minecraft, boolean renderOnlyCustom, int displayItemIndex, int displayItemCount, ItemStack stack, int rotation) {
		if (stack.isEmpty()) {
			return;
		}
		BakedModel itemModel = minecraft.getItemRenderer().getModel(stack, null, minecraft.player, 0);
		if (!itemModel.isCustomRenderer() && renderOnlyCustom) {
			return;
		}

		float itemOffset = (float) getDisplayItemOffset(stack, itemModel, displayItemCount == 1 ? 1 : SMALL_3D_ITEM_SCALE);
		poseStack.pushPose();

		Vector3f frontOffset = getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, (float) yCenterTranslation);
		poseStack.translate(frontOffset.x(), frontOffset.y(), -itemOffset);
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

		float itemScale;
		if (displayItemCount == 1) {
			itemScale = itemModel.isGui3d() ? 1.0f : BIG_2D_ITEM_SCALE;
		} else {
			itemScale = itemModel.isGui3d() ? SMALL_3D_ITEM_SCALE : SMALL_2D_ITEM_SCALE;
		}
		poseStack.scale(itemScale, itemScale, itemScale);

		minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
		poseStack.popPose();
	}

	public static double getDisplayItemOffset(ItemStack item, BakedModel itemModel, float additionalScale) {
		int hash = ItemStack.hashItemAndComponents(item) * 31 + Float.hashCode(additionalScale);
		Double offset = ITEM_HASHCODE_OFFSETS.getIfPresent(hash);
		if (offset != null) {
			return offset;
		}
		offset = calculateDisplayItemOffset(item, itemModel, additionalScale);
		ITEM_HASHCODE_OFFSETS.put(hash, offset);
		return offset;
	}

	private static double calculateDisplayItemOffset(ItemStack item, BakedModel itemModel, float additionalScale) {
		double itemOffset = 0;
		if (itemModel.isGui3d() && item.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			ClientLevel level = Minecraft.getInstance().level;
			if (level != null) {
				itemOffset = calculateOffsetFromModelOrShape(itemModel, block, level, additionalScale);
			}
		}
		return itemOffset;
	}

	private static double calculateOffsetFromModelOrShape(BakedModel itemModel, Block block, ClientLevel level, float additionalScale) {
		if (itemModel.isCustomRenderer()) {
			return transformBoundsCornersAndCalculateOffset(itemModel, getBoundsCornersFromShape(block, level), additionalScale);
		} else {
			return transformBoundsCornersAndCalculateOffset(itemModel, getBoundsCornersFromModel(itemModel), additionalScale);
		}
	}

	@SuppressWarnings("deprecation")
	private static double transformBoundsCornersAndCalculateOffset(BakedModel itemModel, Set<Vector3f> points, float additionalScale) {
		ItemTransform transform = itemModel.getTransforms().getTransform(ItemDisplayContext.FIXED);
		points = scalePoints(points, transform.scale);
		points = rotatePoints(points, transform.rotation);
		points = translatePoints(points, transform.translation);

		float zScale = transform.scale.z();
		return ((zScale * (2 / 15.95D)) - getMaxZ(points)) * additionalScale; //15.95 because of z-fighting if displayed model had surface offset exactly 1 pixel from the top most surface
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
		return getCornerPointsRelativeToCenter((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
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
			frontOffset = new Vector3f(displayItemIndex == 0 || displayItemIndex == 2 ? centerYOffset + halfCenterYOffset : halfCenterYOffset, displayItemIndex == 0 || displayItemIndex == 1 ? centerYOffset + halfCenterYOffset : halfCenterYOffset, 0.5f);
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
