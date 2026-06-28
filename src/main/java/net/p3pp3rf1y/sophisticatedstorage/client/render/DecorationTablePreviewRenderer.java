package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class DecorationTablePreviewRenderer extends PictureInPictureRenderer<DecorationTablePreviewRenderState> {
	public DecorationTablePreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<DecorationTablePreviewRenderState> getRenderStateClass() {
		return DecorationTablePreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(DecorationTablePreviewRenderState renderState, PoseStack poseStack) {
		poseStack.scale(1.0F, -1.0F, -1.0F);
		TrackingItemStackRenderState itemStackRenderState = renderState.itemStackRenderState();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gameRenderer.getLighting().setupFor(itemStackRenderState.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
		FeatureRenderDispatcher featureRenderDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
		SubmitNodeStorage submitNodeStorage = featureRenderDispatcher.getSubmitNodeStorage();
		itemStackRenderState.submit(poseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
		featureRenderDispatcher.renderAllFeatures();
	}

	@Override
	protected boolean textureIsReadyToBlit(DecorationTablePreviewRenderState renderState) {
		return false;
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2.0F;
	}

	@Override
	protected String getTextureLabel() {
		return "decoration_table_preview";
	}
}
