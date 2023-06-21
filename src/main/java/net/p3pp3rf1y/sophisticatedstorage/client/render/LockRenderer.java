package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.BooleanSupplier;

public class LockRenderer {
	private LockRenderer() {}

	public static final Material LOCK_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("block/lock"));

	public static void renderLock(StorageBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float yOffset, BooleanSupplier holdsCorrectItem) {
		if (!blockEntity.isLocked() || (!holdsCorrectItem.getAsBoolean() && !blockEntity.shouldShowLock())) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5 - 0.5 / 16D, yOffset, -0.001);
		poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
		poseStack.pushPose();
		VertexConsumer vertexConsumer;
		boolean translucentRender = !blockEntity.shouldShowLock() && holdsCorrectItem.getAsBoolean();
		if (translucentRender) {
			//noinspection resource
			TextureAtlasSprite sprite = LockRenderer.LOCK_TEXTURE.sprite();
			vertexConsumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlasLocation())));
		} else {
			vertexConsumer = LockRenderer.LOCK_TEXTURE.buffer(bufferSource, RenderType::entityCutoutNoCull);
		}

		PoseStack.Pose pose = poseStack.last();
		Vector3f normal = new Vector3f(0, 1, 0);
		pose.normal().transform(normal);
		renderQuad(vertexConsumer, pose.pose(), normal, packedOverlay, packedLight, translucentRender ? 0.5F : 1);

		poseStack.popPose();
		poseStack.popPose();
	}

	private static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight, float alpha) {
		float minX = 0;
		int minY = 0;
		int maxY = 1;
		int maxX = 1;
		float minU = 0;
		float minV = 0;
		float maxU = 1;
		float maxV = 1;

		addVertex(pose, normal, consumer, maxY, minX, packedOverlay, packedLight, maxU, minV, alpha);
		addVertex(pose, normal, consumer, minY, minX, packedOverlay, packedLight, maxU, maxV, alpha);
		addVertex(pose, normal, consumer, minY, maxX, packedOverlay, packedLight, minU, maxV, alpha);
		addVertex(pose, normal, consumer, maxY, maxX, packedOverlay, packedLight, minU, minV, alpha);
	}

	private static void addVertex(Matrix4f pose, Vector3f normal, VertexConsumer pConsumer, int pY, float pX, int packedOverlay, int packedLight, float u, float v, float alpha) {
		Vector4f pos = new Vector4f(pX, pY, 0, 1.0F);
		pose.transform(pos);
		pConsumer.vertex(pos.x(), pos.y(), pos.z(), 1, 1, 1, alpha, u, v, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
	}
}
