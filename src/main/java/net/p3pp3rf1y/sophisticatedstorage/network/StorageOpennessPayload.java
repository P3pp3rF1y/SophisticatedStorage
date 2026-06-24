package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

public record StorageOpennessPayload(BlockPos pos, boolean shouldBeOpen) implements CustomPacketPayload {
	public static final Type<StorageOpennessPayload> TYPE = new Type<>(SophisticatedStorage.getIdentifier("storage_openness"));
	public static final StreamCodec<ByteBuf, StorageOpennessPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, StorageOpennessPayload::pos,
			ByteBufCodecs.BOOL, StorageOpennessPayload::shouldBeOpen, StorageOpennessPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(StorageOpennessPayload payload, IPayloadContext context) {
		WorldHelper.getLoadedBlockEntity(context.player().level(), payload.pos, StorageBlockEntity.class)
				.ifPresent(storageBlockEntity -> storageBlockEntity.setShouldBeOpen(payload.shouldBeOpen));
	}
}
