package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import java.util.UUID;

public record StorageContentsPayload(UUID shulkerBoxUuid, ContainerContents contents) implements CustomPacketPayload {
	public static final Type<StorageContentsPayload> TYPE = new Type<>(SophisticatedStorage.getRL("storage_contents"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			StorageContentsPayload::shulkerBoxUuid, ContainerContents.STREAM_CODEC, StorageContentsPayload::contents, StorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(StorageContentsPayload payload, IPayloadContext context) {
		ItemContentsStorage.get().setContents(payload.shulkerBoxUuid, payload.contents);
		ClientStorageContentsTooltipBase.refreshContents();
	}
}
