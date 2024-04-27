package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.BiFunction;

public class TranslucentVertexConsumer extends DefaultedVertexConsumer {
	public static final RenderType TRANSLUCENT = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
	private final VertexConsumer delegate;

	private static BiFunction<MultiBufferSource, Integer, VertexConsumer> factory = TranslucentVertexConsumer::new;

	public static void setFactory(BiFunction<MultiBufferSource, Integer, VertexConsumer> factory) {
		TranslucentVertexConsumer.factory = factory;
	}

	public TranslucentVertexConsumer(VertexConsumer delegate, int alpha) {
		this.delegate = delegate;
		defaultA = alpha;
	}

	public TranslucentVertexConsumer(MultiBufferSource buffer, int alpha) {
		this(buffer.getBuffer(TRANSLUCENT), alpha);
	}

	static MultiBufferSource wrapBuffer(MultiBufferSource buffer, int alpha) {
		return renderType -> factory.apply(buffer, alpha);
	}

	public static VertexConsumer getVertexConsumer(MultiBufferSource buffer, int alpha) {
		return factory.apply(buffer, alpha);
	}

	@Override
	public void vertex(float pX, float pY, float pZ, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ) {
		super.vertex(pX, pY, pZ, red, green, blue, defaultA / 256f, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ);
	}

	@Override
	public VertexConsumer vertex(double pX, double pY, double pZ) {
		return delegate.vertex(pX, pY, pZ);
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		return delegate.color(red, green, blue, defaultA);
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
