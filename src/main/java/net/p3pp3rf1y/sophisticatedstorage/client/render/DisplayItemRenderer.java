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
import net.minecraftforge.client.model.data.ModelData;
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
	private DisplayItemRenderer() {}

	private static final Cache<Integer, Double> ITEM_HASHCODE_OFFSETS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();

	public static void renderDisplayItem(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, double yCenterTranslation, double blockSideOffset) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = blockEntity.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo();
		ItemStack item = itemDisplayRenderInfo.getItem();

		if (item.isEmpty()) {
			return;
		}

		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		Direction facing = storageBlock.getFacing(blockState);
		BakedModel itemModel = minecraft.getItemRenderer().getModel(item, null, minecraft.player, 0);
		double itemOffset = getDisplayItemOffset(item, itemModel);

		poseStack.pushPose();
		poseStack.translate(0.5, yCenterTranslation, 0.5);
		Vec3i normal = facing.getNormal();
		poseStack.translate(normal.getX() * (blockSideOffset + itemOffset), normal.getY() * (blockSideOffset + itemOffset), normal.getZ() * (blockSideOffset + itemOffset));
		poseStack.mulPose(facing.getRotation());
		if (facing.getAxis().isHorizontal()) {
			poseStack.mulPose(Vector3f.YN.rotationDegrees(180f + itemDisplayRenderInfo.getRotation()));
		}
		poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
		float itemScale = itemModel.isGui3d() ? 1.0f : 0.65f;
		poseStack.scale(itemScale, itemScale, itemScale);
		minecraft.getItemRenderer().render(item, ItemTransforms.TransformType.FIXED, false, poseStack, bufferSource, packedLight, packedOverlay, itemModel);
		poseStack.popPose();
	}

	public static double getDisplayItemOffset(ItemStack item, BakedModel itemModel) {
		int hash = ItemStackKey.getHashCode(item);
		Double offset = ITEM_HASHCODE_OFFSETS.getIfPresent(hash);
		if (offset != null) {
			return offset;
		}
		offset = calculateDisplayItemOffset(item, itemModel);
		ITEM_HASHCODE_OFFSETS.put(hash, offset);
		return offset;
	}

	private static double calculateDisplayItemOffset(ItemStack item, BakedModel itemModel) {
		double itemOffset = 0;
		if (itemModel.isGui3d() && item.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			ClientLevel level = Minecraft.getInstance().level;
			if (level != null) {
				itemOffset = calculateOffsetFromModelOrShape(itemModel, block, level);
			}
		}
		return itemOffset;
	}

	private static double calculateOffsetFromModelOrShape(BakedModel itemModel, Block block, ClientLevel level) {
		if (itemModel.isCustomRenderer()) {
			return transformBoundsCornersAndCalculateOffset(itemModel, getBoundsCornersFromShape(block, level));
		} else {
			return transformBoundsCornersAndCalculateOffset(itemModel, getBoundsCornersFromModel(itemModel, level));
		}
	}

	@SuppressWarnings("deprecation")
	private static double transformBoundsCornersAndCalculateOffset(BakedModel itemModel, Set<Vector3f> points) {
		ItemTransform transform = itemModel.getTransforms().getTransform(ItemTransforms.TransformType.FIXED);
		points = scalePoints(points, transform.scale);
		points = rotatePoints(points, transform.rotation);
		points = translatePoints(points, transform.translation);

		float zScale = transform.scale.z();
		return (zScale * 1 / 8D) - getMaxZ(points);
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
			List<BakedQuad> quads = itemModel.getQuads(null, direction, level.random, ModelData.EMPTY, null);
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
		List<BakedQuad> quads = itemModel.getQuads(null, null, level.random, ModelData.EMPTY, null);
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
}
