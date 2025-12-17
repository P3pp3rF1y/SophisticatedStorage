package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.joml.Vector3f;

import java.util.function.BooleanSupplier;

public class LockRenderer {
	private LockRenderer() {
	}

	public static final Material LOCK_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, SophisticatedStorage.getIdentifier("block/lock"));

	public static void submitLock(SubmitNodeCollector submitNodeCollector, StorageRenderState renderState, PoseStack poseStack, float yOffset, BooleanSupplier holdsCorrectItem, MaterialSet materialSet) {
		if (!renderState.isLocked || (!holdsCorrectItem.getAsBoolean() && !renderState.showsLock)) {
			return;
		}
		RenderType renderType;
		boolean translucentRender = !renderState.showsLock && holdsCorrectItem.getAsBoolean();
		if (translucentRender) {
			renderType = RenderTypes.entityTranslucent(LockRenderer.LOCK_TEXTURE.atlasLocation());
		} else {
			renderType = RenderTypes.entitySmoothCutout(LockRenderer.LOCK_TEXTURE.atlasLocation());
		}

		poseStack.pushPose();
		poseStack.translate(0.5 - 0.5 / 16D, yOffset, -0.001);
		poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
		TextureAtlasSprite sprite = materialSet.get(LOCK_TEXTURE);
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			Vector3f normal = new Vector3f(0, 1, 0);
			pose.normal().transform(normal);
			RenderHelper.renderQuad(vertexConsumer, pose.pose(), normal, OverlayTexture.NO_OVERLAY, renderState.lightCoords, translucentRender ? 0.5F : 1, sprite);
		});
		poseStack.popPose();
	}
}
