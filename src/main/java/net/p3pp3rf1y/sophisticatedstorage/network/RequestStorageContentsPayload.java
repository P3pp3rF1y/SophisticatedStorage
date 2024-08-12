package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import java.util.UUID;

public record
RequestStorageContentsPayload(UUID storageUuid) implements CustomPacketPayload {
	public static final Type<RequestStorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getRL("request_storage_contents"));
	public static final StreamCodec<ByteBuf, RequestStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			RequestStorageContentsPayload::storageUuid,
			RequestStorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestStorageContentsPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer serverPlayer)) {
			return;
		}

		PacketDistributor.sendToPlayer(serverPlayer, new StorageContentsPayload(payload.storageUuid, ItemContentsStorage.get().getOrCreateStorageContents(payload.storageUuid)));
	}
}
