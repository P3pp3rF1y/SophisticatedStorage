package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

public class DisplayItemRenderer {
	public static final float SMALL_3D_ITEM_SCALE = 0.5f;
	static final float BIG_2D_ITEM_SCALE = 0.5f;
	static final float SMALL_2D_ITEM_SCALE = 0.25f;
	private final double yCenterTranslation;
	private final double blockSideOffset;

	public DisplayItemRenderer(double yCenterTranslation, double blockSideOffset) {
		this.yCenterTranslation = yCenterTranslation;
		this.blockSideOffset = blockSideOffset;
	}

	private static final Cache<Integer, Double> ITEM_HASHCODE_OFFSETS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();

	public void renderDisplayItem(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItem().ifPresent(displayItem -> {
			BlockState blockState = blockEntity.getBlockState();
			if (!(blockState.getBlock() instanceof StorageBlockBase storageBlock)) {
				return;
			}
			Direction facing = storageBlock.getFacing(blockState);

			Minecraft minecraft = Minecraft.getInstance();
			renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, blockState, facing, minecraft, displayItem, false, 0, 1);
		});
	}

	public void renderDisplayItems(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean renderOnlyCustom) {
		List<RenderInfo.DisplayItem> displayItems = blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems();
		if (displayItems.isEmpty()) {
			return;
		}

		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		Direction facing = storageBlock.getFacing(blockState);

		Minecraft minecraft = Minecraft.getInstance();
		int displayItemIndex = 0;
		int displayItemCount = storageBlock.getDisplayItemsCount(displayItems);
		for (RenderInfo.DisplayItem displayItem : displayItems) {
			renderSingleItem(poseStack, bufferSource, packedLight, packedOverlay, blockState, facing, minecraft, displayItem, renderOnlyCustom, storageBlock.hasFixedIndexDisplayItems() ? displayItem.getSlotIndex() : displayItemIndex, displayItemCount);
			displayItemIndex++;
		}
	}

	private void renderSingleItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState blockState, Direction facing, Minecraft minecraft, RenderInfo.DisplayItem displayItem, boolean renderOnlyCustom, int displayItemIndex, int displayItemCount) {
		ItemStack item = displayItem.getItem();
		if (item.isEmpty()) {
			return;
		}
		BakedModel itemModel = minecraft.getItemRenderer().getModel(item, null, minecraft.player, 0);
		if (!itemModel.isCustomRenderer() && renderOnlyCustom) {
			return;
		}

		float itemOffset = (float) getDisplayItemOffset(item, itemModel, displayItemCount == 1 ? 1 : SMALL_3D_ITEM_SCALE);
		poseStack.pushPose();

		Vec3i normal = facing.getNormal();
		Vector3f offset = new Vector3f((float) (blockSideOffset + itemOffset), (float) blockSideOffset + itemOffset, (float) (blockSideOffset + itemOffset));
		offset.mul(normal.getX(), normal.getY(), normal.getZ());
		Vector3f frontOffset = getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, (float) yCenterTranslation);
		frontOffset.add(-0.5f, -0.5f, -0.5f);
		rotateFrontOffset(blockState, facing, frontOffset);
		frontOffset.add(0.5f, 0.5f, 0.5f);
		offset.add(frontOffset);
		poseStack.translate(offset.x(), offset.y(), offset.z());

		rotateToFront(poseStack, blockState, facing);

		poseStack.mulPose(Vector3f.ZP.rotationDegrees(displayItem.getRotation()));
		float itemScale;
		if (displayItemCount == 1) {
			itemScale = itemModel.isGui3d() ? 1.0f : BIG_2D_ITEM_SCALE;
		} else {
			itemScale = itemModel.isGui3d() ? SMALL_3D_ITEM_SCALE : SMALL_2D_ITEM_SCALE;
		}
		poseStack.scale(itemScale, itemScale, itemScale);

		minecraft.getItemRenderer().render(item, ItemTransforms.TransformType.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
		poseStack.popPose();
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected void rotateFrontOffset(BlockState state, Direction facing, Vector3f frontOffset) {
		frontOffset.transform(getNorthBasedRotation(facing));
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected void rotateToFront(PoseStack poseStack, BlockState state, Direction facing) {
		poseStack.mulPose(getNorthBasedRotation(facing));
	}

	public static double getDisplayItemOffset(ItemStack item, BakedModel itemModel, float additionalScale) {
		int hash = ItemStackKey.getHashCode(item) * 31 + Float.hashCode(additionalScale);
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
			return transformBoundsCornersAndCalculateOffset(itemModel, getBoundsCornersFromModel(itemModel, level), additionalScale);
		}
	}

	@SuppressWarnings("deprecation")
	private static double transformBoundsCornersAndCalculateOffset(BakedModel itemModel, Set<Vector3f> points, float additionalScale) {
		ItemTransform transform = itemModel.getTransforms().getTransform(ItemTransforms.TransformType.FIXED);
		points = scalePoints(points, transform.scale);
		points = rotatePoints(points, transform.rotation);
		points = translatePoints(points, transform.translation);

		float zScale = transform.scale.z();
		return ((zScale * 1 / 15.95D) - getMaxZ(points)) * additionalScale; //15.95 because of z-fighting if displayed model had surface offset exactly 1 pixel from the top most surface
	}

	@SuppressWarnings("deprecation")
	private static Set<Vector3f> getBoundsCornersFromShape(Block block, ClientLevel level) {
		VoxelShape shape = block.getShape(block.defaultBlockState(), level, BlockPos.ZERO, CollisionContext.empty());
		return getCornerPointsRelativeToCenter(shape.bounds());
	}

	private static Set<Vector3f> getBoundsCornersFromModel(BakedModel itemModel, Level level) {
		float minX = 2;
		float minY = 2;
		float minZ = 2;
		float maxX = -2;
		float maxY = -2;
		float maxZ = -2;

		for (Direction direction : Direction.values()) {
			List<BakedQuad> quads = itemModel.getQuads(null, direction, level.random, EmptyModelData.INSTANCE);
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
		List<BakedQuad> quads = itemModel.getQuads(null, null, level.random, EmptyModelData.INSTANCE);
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
		Quaternion rot = new Quaternion(rotation.x(), rotation.y(), rotation.z(), true);
		return transformPoints(points, point -> {
			point.transform(rot);
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

	public static Quaternion getNorthBasedRotation(Direction dir) {
		return switch (dir) {
			case DOWN -> Vector3f.XP.rotationDegrees(-90.0F);
			case UP -> Vector3f.XP.rotationDegrees(90.0F);
			case NORTH -> Quaternion.ONE.copy();
			case SOUTH -> Vector3f.YP.rotationDegrees(180.0F);
			case WEST -> Vector3f.YP.rotationDegrees(90.0F);
			case EAST -> Vector3f.YP.rotationDegrees(-90.0F);
		};
	}
}
