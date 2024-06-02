package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.function.BooleanSupplier;

public class LockRenderer {
	private LockRenderer() {}

	public static final Material LOCK_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("block/lock"));

	public static void renderLock(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float yOffset, BooleanSupplier holdsCorrectItem) {
		if (!blockEntity.isLocked() || (!holdsCorrectItem.getAsBoolean() && !blockEntity.shouldShowLock())) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5 - 0.5/16D, yOffset, -0.001);
		poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
		poseStack.pushPose();
		VertexConsumer vertexConsumer;
		boolean translucentRender = !blockEntity.shouldShowLock() && holdsCorrectItem.getAsBoolean();
		if (translucentRender) {
			//noinspection resource
			TextureAtlasSprite sprite = LockRenderer.LOCK_TEXTURE.sprite();
			vertexConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlas().location())));
		} else {
			vertexConsumer = LockRenderer.LOCK_TEXTURE.buffer(bufferSource, RenderType::entityCutoutNoCull);
		}

		PoseStack.Pose pose = poseStack.last();
		Vector3f normal = new Vector3f(0, 1, 0);
		normal.transform(pose.normal());
		RenderHelper.renderQuad(vertexConsumer, pose.pose(), normal, packedOverlay, packedLight, translucentRender ? 0.5F : 1);

		poseStack.popPose();
		poseStack.popPose();
	}
}
