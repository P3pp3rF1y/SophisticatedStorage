package net.p3pp3rf1y.sophisticatedstorage.compat.rubidium;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.renderer.MultiBufferSource;
import net.p3pp3rf1y.sophisticatedstorage.client.render.TranslucentVertexConsumer;
import org.lwjgl.system.MemoryStack;

public class RubidiumTranslucentVertexConsumer extends TranslucentVertexConsumer implements VertexBufferWriter {
	public static void register() {
		TranslucentVertexConsumer.setFactory(RubidiumTranslucentVertexConsumer::new);
	}

	private final MultiBufferSource buffer;

	public RubidiumTranslucentVertexConsumer(MultiBufferSource buffer, int alpha) {
		super(buffer, alpha);
		this.buffer = buffer;
	}

	@Override
	public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
		if (buffer instanceof MultiBufferSource.BufferSource bufferSource && bufferSource.builder instanceof VertexBufferWriter vertexBufferWriter) {
			vertexBufferWriter.push(stack, src, count, format);
		}
	}
}
