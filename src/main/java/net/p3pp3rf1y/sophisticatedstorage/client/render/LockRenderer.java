package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.joml.Vector3f;

import java.util.function.BooleanSupplier;

public class LockRenderer {
	private static final net.minecraft.resources.Identifier LOCK_TEXTURE = SophisticatedStorage.getIdentifier("block/lock");

	private LockRenderer() {
	}

	public static void submitLock(SubmitNodeCollector submitNodeCollector, StorageRenderState renderState, PoseStack poseStack, float yOffset, BooleanSupplier holdsCorrectItem) {
		if (!renderState.isLocked || (!holdsCorrectItem.getAsBoolean() && !renderState.showsLock)) {
			return;
		}

		boolean translucentRender = !renderState.showsLock && holdsCorrectItem.getAsBoolean();
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(LOCK_TEXTURE);
		RenderType renderType = translucentRender ? RenderTypes.entityTranslucent(sprite.atlasLocation()) : RenderTypes.entityCutout(sprite.atlasLocation());

		poseStack.pushPose();
		poseStack.translate(0.5 - 0.5 / 16D, yOffset, -0.001);
		poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			Vector3f normal = new Vector3f(0, 1, 0);
			pose.normal().transform(normal);
			RenderHelper.renderQuad(vertexConsumer, pose.pose(), normal, OverlayTexture.NO_OVERLAY, renderState.lightCoords, translucentRender ? 0.5F : 1, sprite);
		});
		poseStack.popPose();
	}
}
