package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import java.util.UUID;

public record StorageContentsPayload(UUID shulkerBoxUuid, CompoundTag contents) implements CustomPacketPayload {
	public static final Type<StorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getRL("storage_contents"));
	public static final StreamCodec<ByteBuf, StorageContentsPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			StorageContentsPayload::shulkerBoxUuid,
			ByteBufCodecs.COMPOUND_TAG,
			StorageContentsPayload::contents,
			StorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(StorageContentsPayload payload, IPayloadContext context) {
		ItemContentsStorage.get().setStorageContents(payload.shulkerBoxUuid, payload.contents);
		ClientStorageContentsTooltipBase.refreshContents();
	}
}
