package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

class TranslucentVertexConsumer extends DefaultedVertexConsumer {
	public static final RenderType TRANSLUCENT = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
	private final VertexConsumer delegate;

	public TranslucentVertexConsumer(VertexConsumer delegate, int alpha) {
		this.delegate = delegate;
		defaultA = alpha;
	}

	public TranslucentVertexConsumer(MultiBufferSource buffer, int alpha) {
		this(buffer.getBuffer(TRANSLUCENT), alpha);
	}

	static MultiBufferSource wrapBuffer(MultiBufferSource buffer, int alpha) {
		return renderType -> new TranslucentVertexConsumer(buffer, alpha);
	}

	@Override
	public void vertex(float pX, float pY, float pZ, float pRed, float pGreen, float pBlue, float pAlpha, float pTexU, float pTexV, int pOverlayUV, int pLightmapUV, float pNormalX, float pNormalY, float pNormalZ) {
		super.vertex(pX, pY, pZ, pRed, pGreen, pBlue, defaultA / 256f, pTexU, pTexV, pOverlayUV, pLightmapUV, pNormalX, pNormalY, pNormalZ);
	}

	@Override
	public VertexConsumer vertex(double pX, double pY, double pZ) {
		return delegate.vertex(pX, pY, pZ);
	}

	@Override
	public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
		return delegate.color(pRed, pGreen, pBlue, defaultA);
	}

	@Override
	public VertexConsumer uv(float pU, float pV) {
		return delegate.uv(pU, pV);
	}

	@Override
	public VertexConsumer overlayCoords(int pU, int pV) {
		return delegate.overlayCoords(pU, pV);
	}

	@Override
	public VertexConsumer uv2(int pU, int pV) {
		return delegate.uv2(pU, pV);
	}

	@Override
	public VertexConsumer normal(float pX, float pY, float pZ) {
		return delegate.normal(pX, pY, pZ);
	}

	@Override
	public void endVertex() {
		delegate.endVertex();
	}
}
