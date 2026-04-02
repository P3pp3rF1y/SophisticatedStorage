package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DisplayItemRenderer {
	private static final ItemStackRenderState EMPTY_UPGRADE_STACK = new ItemStackRenderState();
	public static final float SMALL_BLOCK_ITEM_OFFSET = 0.5f;
	public static final float SMALL_BLOCK_ITEM_SCALE = 0.5f;
	static final float BIG_ITEM_SCALE = 0.5f;
	static final float SMALL_ITEM_SCALE = 0.25f;
	static final float UPGRADE_ITEM_SCALE = 0.125f;
	private static final ItemStackRenderState INACCESSIBLE_SLOT_STACK = new ItemStackRenderState();
	private static final Field ITEM_TRANSFORM_FIELD = getItemTransformField();
	private static boolean helperStacksInitialized = false;
	private final double yCenterTranslation;
	private final Vec3 upgradesOffset;

	public DisplayItemRenderer(double yCenterTranslation, Vec3 upgradesOffset) {
		this.yCenterTranslation = yCenterTranslation;
		this.upgradesOffset = upgradesOffset;
	}

	private static void initHelperStacks() {
		if (helperStacksInitialized) {
			return;
		}
		ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
		itemModelResolver.updateForTopItem(EMPTY_UPGRADE_STACK, new ItemStack(ModItems.UPGRADE_BASE.get()), ItemDisplayContext.FIXED, null, null, 0);
		itemModelResolver.updateForTopItem(INACCESSIBLE_SLOT_STACK, new ItemStack(ModItems.INACCESSIBLE_SLOT.get()), ItemDisplayContext.FIXED, null, null, 0);
		helperStacksInitialized = true;
	}

	private static final Cache<Integer, Double> ITEM_HASHCODE_OFFSETS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();

	static boolean isGui3d(ItemStackRenderState renderState) {
		AABB.Builder builder = new AABB.Builder();
		renderState.visitExtents(builder::include);
		return builder.isDefined() && builder.build().getZsize() > 0.0625F;
	}

	public void submitDisplayItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, StorageRenderState.DisplayItemInfo displayItemInfo) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, 1, displayItemInfo.item(), displayItemInfo.index(), displayItemInfo.itemOffset(), displayItemInfo.rotation(), displayItemInfo.isBlockItem());
	}

	public void submitDisplayItems(SubmitNodeCollector submitNodeCollector, StorageRenderState storageRenderState, PoseStack poseStack, int packedOverlay) {
		if (storageRenderState.displayItems.isEmpty() && storageRenderState.inaccessibleSlots.isEmpty()) {
			return;
		}
		initHelperStacks();

		for (int displayItemIndex = 0; displayItemIndex < storageRenderState.displayItemSlots; displayItemIndex++) {
			if (storageRenderState.inaccessibleSlots.contains(displayItemIndex)) {
				submitSingleItem(submitNodeCollector, poseStack, storageRenderState.lightCoords, packedOverlay, storageRenderState.displayItemSlots, INACCESSIBLE_SLOT_STACK, displayItemIndex);
			}
		}
		for (StorageRenderState.DisplayItemInfo displayItemInfo : storageRenderState.displayItems) {
			submitSingleDisplayItem(submitNodeCollector, poseStack, storageRenderState.lightCoords, packedOverlay, displayItemInfo, storageRenderState.displayItemSlots);
		}
	}

	public void submitUpgradeItems(SubmitNodeCollector submitNodeCollector, StorageRenderState storageRenderState, PoseStack poseStack, int packedOverlay, boolean renderEmptySlots) {
		initHelperStacks();
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

	private void submitSingleDisplayItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, StorageRenderState.DisplayItemInfo displayItemInfo, int displayItemCount) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, displayItemCount, displayItemInfo.item(), displayItemInfo.index(), displayItemInfo.itemOffset(), displayItemInfo.rotation(), displayItemInfo.isBlockItem());
	}

	private void submitSingleItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, int displayItemCount, ItemStackRenderState item, int displayItemIndex) {
		submitSingleItem(submitNodeCollector, poseStack, packedLight, packedOverlay, displayItemCount, item, displayItemIndex, 0, 0, false);
	}

	private void submitSingleItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int packedLight, int packedOverlay, int displayItemCount, ItemStackRenderState item, int displayItemIndex, float itemOffset, int rotation, boolean isBlockItem) {
		if (item.layers.length < 1) {
			return;
		}

		poseStack.pushPose();

		Vector3f frontOffset = getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, (float) yCenterTranslation);
		poseStack.translate(frontOffset.x(), frontOffset.y(), - itemOffset);
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

		float itemScale;
		if (displayItemCount == 1) {
			itemScale = isBlockItem && isGui3d(item) ? 1.0f : BIG_ITEM_SCALE;
		} else {
			itemScale = isBlockItem && isGui3d(item) ? SMALL_BLOCK_ITEM_SCALE : SMALL_ITEM_SCALE;
		}
		poseStack.scale(itemScale, itemScale, itemScale);

		item.submit(poseStack, submitNodeCollector, packedLight, packedOverlay, 0);
		poseStack.popPose();
	}

	public static double getDisplayItemOffset(ItemStack item, ItemStackRenderState itemStackRenderState, boolean isGui3d, float additionalScale) {
		int hash = ItemStack.hashItemAndComponents(item) * 31 + Float.hashCode(additionalScale);
		Double offset = ITEM_HASHCODE_OFFSETS.getIfPresent(hash);
		if (offset != null) {
			return offset;
		}
		offset = calculateDisplayItemOffset(item, itemStackRenderState, isGui3d, additionalScale);
		ITEM_HASHCODE_OFFSETS.put(hash, offset);
		return offset;
	}

	private static double calculateDisplayItemOffset(ItemStack item, ItemStackRenderState itemStackRenderState, boolean isGui3d, float additionalScale) {
		return isGui3d && item.getItem() instanceof BlockItem
				? calculateOffsetFromBoundingBox(itemStackRenderState, additionalScale)
				: 0;
	}

	private static double calculateOffsetFromBoundingBox(ItemStackRenderState itemStackRenderState, float additionalScale) {
		AABB boundingBox = itemStackRenderState.getModelBoundingBox();
		double zScale = getFixedTransformScale(itemStackRenderState);
		return ((zScale * (2 / 15.95D)) - boundingBox.maxZ) * additionalScale; //15.95 because of z-fighting if displayed model had surface offset exactly 1 pixel from the top most surface
	}

	private static double getFixedTransformScale(ItemStackRenderState itemStackRenderState) {
		if (ITEM_TRANSFORM_FIELD == null || itemStackRenderState.layers.length == 0) {
			return 1;
		}

		try {
			ItemTransform itemTransform = (ItemTransform) ITEM_TRANSFORM_FIELD.get(itemStackRenderState.layers[0]);
			return itemTransform == null ? 1 : itemTransform.scale().z();
		} catch (IllegalAccessException e) {
			return 1;
		}
	}

	private static Field getItemTransformField() {
		try {
			Field field = ItemStackRenderState.LayerRenderState.class.getDeclaredField("itemTransform");
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException e) {
			return null;
		}
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
