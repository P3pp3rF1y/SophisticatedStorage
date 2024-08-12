package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

public record ScrolledToolPayload(boolean next) implements CustomPacketPayload {
	public static final Type<ScrolledToolPayload> TYPE = new Type<>(SophisticatedCore.getRL("scrolled_tool"));
	public static final StreamCodec<ByteBuf, ScrolledToolPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			ScrolledToolPayload::next,
			ScrolledToolPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(ScrolledToolPayload payload, IPayloadContext context) {
		ItemStack stack = context.player().getMainHandItem();
		if (stack.getItem() == ModItems.STORAGE_TOOL.get()) {
			StorageToolItem.cycleMode(stack, payload.next);
		}
	}
}
