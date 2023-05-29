package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

public class LockRenderer {
	private LockRenderer() {}

	public static final Material LOCK_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("block/lock"));

	public static void renderLock(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float yOffset) {
		if (!blockEntity.isLocked() || !blockEntity.shouldShowLock()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5 - 0.5/16D, yOffset, -0.001);
		poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
		poseStack.pushPose();
		VertexConsumer vertexConsumer = LockRenderer.LOCK_TEXTURE.buffer(bufferSource, RenderType::entityCutoutNoCull);

		PoseStack.Pose pose = poseStack.last();
		Vector3f normal = new Vector3f(0, 1, 0);
		normal.transform(pose.normal());
		renderQuad(vertexConsumer, pose.pose(), normal, packedOverlay, packedLight);

		poseStack.popPose();
		poseStack.popPose();
	}

	private static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight) {
		float minX = 0;
		int minY = 0;
		int maxY = 1;
		int maxX = 1;
		float minU = 0;
		float minV = 0;
		float maxU = 1;
		float maxV = 1;

		addVertex(pose, normal, consumer, maxY, minX, packedOverlay, packedLight, maxU, minV);
		addVertex(pose, normal, consumer, minY, minX, packedOverlay, packedLight, maxU, maxV);
		addVertex(pose, normal, consumer, minY, maxX, packedOverlay, packedLight, minU, maxV);
		addVertex(pose, normal, consumer, maxY, maxX, packedOverlay, packedLight, minU, minV);
	}

	private static void addVertex(Matrix4f pose, Vector3f normal, VertexConsumer pConsumer, int pY, float pX, int packedOverlay, int packedLight, float u, float v) {
		Vector4f pos = new Vector4f(pX, pY, 0, 1.0F);
		pos.transform(pose);
		pConsumer.vertex(pos.x(), pos.y(), pos.z(), 1, 1, 1, 1, u, v, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
	}
}
