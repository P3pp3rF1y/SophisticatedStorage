package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.BiFunction;

public class TranslucentVertexConsumer implements VertexConsumer {
	public static final RenderType TRANSLUCENT = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
	private final VertexConsumer delegate;

	private static BiFunction<MultiBufferSource, Integer, VertexConsumer> factory = TranslucentVertexConsumer::new;
	private final int defaultA;

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
	public void addVertex(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ) {
		int modifiedColor = defaultA << 24 | color & 0xFFFFFF;
		VertexConsumer.super.addVertex(x, y, z, modifiedColor, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ);
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		return delegate.addVertex(x, y, z);
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		return delegate.setColor(red, green, blue, defaultA);
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		return delegate.setUv(u, v);
	}

	@Override
	public VertexConsumer setUv1(int u1, int v1) {
		return delegate.setUv1(u1, v1);
	}

	@Override
	public VertexConsumer setUv2(int u2, int v2) {
		return delegate.setUv2(u2, v2);
	}

	@Override
	public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
		return delegate.setNormal(normalX, normalY, normalZ);
	}
}
