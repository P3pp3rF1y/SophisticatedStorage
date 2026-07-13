package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

public class DisplayItemRenderer {
	private static final ItemStackRenderState EMPTY_UPGRADE_STACK = new ItemStackRenderState();
	public static final float SMALL_BLOCK_ITEM_OFFSET = 0.5f;
	public static final float SMALL_BLOCK_ITEM_SCALE = 0.5f;
	static final float BIG_ITEM_SCALE = 0.5f;
	static final float SMALL_ITEM_SCALE = 0.25f;
	static final float UPGRADE_ITEM_SCALE = 0.125f;
	private static final double DISPLAY_ITEM_PIXEL_SIZE_DIVISOR = 15.95D;
	private static final ItemStackRenderState INACCESSIBLE_SLOT_STACK = new ItemStackRenderState();
	private final double yCenterTranslation;
	private final Vec3 upgradesOffset;

	public DisplayItemRenderer(double yCenterTranslation, Vec3 upgradesOffset) {
		this.yCenterTranslation = yCenterTranslation;
		this.upgradesOffset = upgradesOffset;

		ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
		itemModelResolver.updateForTopItem(EMPTY_UPGRADE_STACK, new ItemStack(ModItems.UPGRADE_BASE.get()), ItemDisplayContext.FIXED, null, null, 0);
		itemModelResolver.updateForTopItem(INACCESSIBLE_SLOT_STACK, new ItemStack(ModItems.INACCESSIBLE_SLOT.get()), ItemDisplayContext.FIXED, null, null, 0);
	}

	private static final Cache<Integer, Double> ITEM_HASHCODE_OFFSETS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();

	static boolean isGui3d(ItemStackRenderState renderState) {
		AABB.Builder builder = new AABB.Builder();
		renderState.visitExtents(builder::include);
		return builder.build().getZsize() > 0.0625F;
	}

	public void submitDisplayItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay,
			StorageRenderState.DisplayItemInfo displayItemInfo) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, 1, displayItemInfo.item(), displayItemInfo.index(),
				displayItemInfo.itemOffset(), displayItemInfo.rotation(), displayItemInfo.zOffset(), displayItemInfo.isBlockItem());
	}

	public void submitDisplayItems(SubmitNodeCollector submitNodeCollector, StorageRenderState storageRenderState, PoseStack poseStack, int packedOverlay) {
		if (storageRenderState.displayItems.isEmpty() && storageRenderState.inaccessibleSlots.isEmpty()) {
			return;
		}

		for (int displayItemIndex = 0; displayItemIndex < storageRenderState.displayItemSlots; displayItemIndex++) {
			if (storageRenderState.inaccessibleSlots.contains(displayItemIndex)) {
				submitSingleItem(submitNodeCollector, poseStack, storageRenderState.lightCoords, packedOverlay, storageRenderState.displayItemSlots,
						INACCESSIBLE_SLOT_STACK, displayItemIndex);
			}
		}
		for (StorageRenderState.DisplayItemInfo displayItemInfo : storageRenderState.displayItems) {
			submitSingleDisplayItem(submitNodeCollector, poseStack, storageRenderState.lightCoords, packedOverlay, displayItemInfo,
					storageRenderState.displayItemSlots);
		}
	}

	public void submitUpgradeItems(SubmitNodeCollector submitNodeCollector, StorageRenderState storageRenderState, PoseStack poseStack, int packedOverlay,
			boolean renderEmptySlots) {
		poseStack.pushPose();
		int i = 0;
		for (ItemStackRenderState upgradeItem : storageRenderState.upgradeItems) {
			if (upgradeItem.isEmpty() && !renderEmptySlots) {
				continue;
			}

			poseStack.pushPose();
			poseStack.translate(1f - i * 2 / 16f - 1 / 16f + upgradesOffset.x(), 1 / 16f + upgradesOffset.y(), upgradesOffset.z());
			poseStack.scale(UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE);
			ItemStackRenderState itemToRender = upgradeItem.isEmpty() ? EMPTY_UPGRADE_STACK : upgradeItem;
			itemToRender.submit(poseStack, submitNodeCollector, storageRenderState.lightCoords, packedOverlay, 0);
			if (storageRenderState.showsDisabledUpgradeDisplay) {
				poseStack.pushPose();
				poseStack.translate(0, 0, -0.001f);
				INACCESSIBLE_SLOT_STACK.submit(poseStack, submitNodeCollector, storageRenderState.lightCoords, packedOverlay, 0);
				poseStack.popPose();
			}
			poseStack.popPose();
			i++;
		}

		poseStack.popPose();
	}

	private void submitSingleDisplayItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay,
			StorageRenderState.DisplayItemInfo displayItemInfo, int displayItemCount) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, displayItemCount, displayItemInfo.item(), displayItemInfo.index(),
				displayItemInfo.itemOffset(), displayItemInfo.rotation(), displayItemInfo.zOffset(), displayItemInfo.isBlockItem());
	}

	private void submitSingleItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, int displayItemCount,
			ItemStackRenderState item, int displayItemIndex) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, displayItemCount, item, displayItemIndex, 0, 0, 0, false);
	}

	private void submitSingleItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, int displayItemCount,
			ItemStackRenderState item, int displayItemIndex, float itemOffset, int rotation, int zOffset, boolean isBlockItem) {
		if (item.layers.length < 1) {
			return;
		}

		float itemScale;
		if (displayItemCount == 1) {
			itemScale = isBlockItem && isGui3d(item) ? 1.0f : BIG_ITEM_SCALE;
		} else {
			itemScale = isBlockItem && isGui3d(item) ? SMALL_BLOCK_ITEM_SCALE : SMALL_ITEM_SCALE;
		}

		poseStack.pushPose();

		Vector3f frontOffset = getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, (float) yCenterTranslation);
		poseStack.translate(frontOffset.x(), frontOffset.y(), -itemOffset - zOffset * getDisplayItemPixelOffset(item, itemScale));
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
		poseStack.scale(itemScale, itemScale, itemScale);

		item.submit(poseStack, submitNodeCollector, packedLight, packedOverlay, 0);
		poseStack.popPose();
	}

	public static double getDisplayItemOffset(ItemStack item, ItemStackRenderState itemStackRenderState, boolean isGui3d, float additionalScale) {
		ItemStackRenderState.LayerRenderState layer = itemStackRenderState.layers[0];
		ItemTransform transform = layer.transform;
		List<BakedQuad> quads = layer.prepareQuadList();
		int hash = ItemStack.hashItemAndComponents(item) * 31 + Float.hashCode(additionalScale);
		Double offset = ITEM_HASHCODE_OFFSETS.getIfPresent(hash);
		if (offset != null) {
			return offset;
		}
		offset = calculateDisplayItemOffset(item, itemStackRenderState, transform, quads, isGui3d, additionalScale);
		ITEM_HASHCODE_OFFSETS.put(hash, offset);
		return offset;
	}

	public static double getDisplayItemPixelOffset(ItemStackRenderState itemStackRenderState, float additionalScale) {
		ItemTransform transform = itemStackRenderState.layers[0].transform;
		return transform.scale().z() * (1 / DISPLAY_ITEM_PIXEL_SIZE_DIVISOR) * additionalScale;
	}

	private static double calculateDisplayItemOffset(ItemStack item, ItemStackRenderState itemStackRenderState, ItemTransform transform, List<BakedQuad> quads,
			boolean isGui3d, float additionalScale) {
		double itemOffset = 0;
		if (isGui3d && item.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			ClientLevel level = Minecraft.getInstance().level;
			if (level != null) {
				itemOffset = calculateOffsetFromModelOrShape(itemStackRenderState, transform, quads, block, level, additionalScale);
			}
		}
		return itemOffset;
	}

	private static double calculateOffsetFromModelOrShape(ItemStackRenderState itemStackRenderState, ItemTransform transform, List<BakedQuad> quads,
			Block block, ClientLevel level, float additionalScale) {
		if (RenderHelper.isSpecialRenderer(itemStackRenderState)) {
			return transformBoundsCornersAndCalculateOffset(transform, getBoundsCornersFromShape(block, level), additionalScale);
		} else {
			return transformBoundsCornersAndCalculateOffset(transform, getBoundsCornersFromModel(quads), additionalScale);
		}
	}

	private static double transformBoundsCornersAndCalculateOffset(ItemTransform transform, Set<Vector3f> points, float additionalScale) {
		points = scalePoints(points, transform.scale());
		points = rotatePoints(points, transform.rotation());
		points = translatePoints(points, transform.translation());

		float zScale = transform.scale().z();
		return ((zScale * (2 / DISPLAY_ITEM_PIXEL_SIZE_DIVISOR)) - getMaxZ(points)) * additionalScale; // 15.95 because of z-fighting if displayed model had surface offset exactly 1
																// pixel from the top most surface
	}

	private static Set<Vector3f> getBoundsCornersFromShape(Block block, ClientLevel level) {
		VoxelShape shape = block.defaultBlockState().getShape(level, BlockPos.ZERO, CollisionContext.empty());
		return getCornerPointsRelativeToCenter(shape.bounds());
	}

	private static Set<Vector3f> getBoundsCornersFromModel(List<BakedQuad> quads) {
		float minX = 2;
		float minY = 2;
		float minZ = 2;
		float maxX = -2;
		float maxY = -2;
		float maxZ = -2;

		for (BakedQuad quad : quads) {
			int i = 0;
			int[] verts = quad.vertices();
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

	private static Set<Vector3f> translatePoints(Set<Vector3f> points, Vector3fc translation) {
		return transformPoints(points, point -> {
			point.sub(translation);
			return point;
		});
	}

	private static Set<Vector3f> rotatePoints(Set<Vector3f> points, Vector3fc rotation) {
		Quaternionf rot = TransformationHelper.quatFromXYZ(rotation.x(), rotation.y(), rotation.z(), true);
		return transformPoints(points, point -> {
			point.rotate(rot);
			return point;
		});
	}

	private static Set<Vector3f> scalePoints(Set<Vector3f> points, Vector3fc scale) {
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
