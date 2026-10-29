package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.UUID;

public record RequestStorageContentsPayload(UUID storageUuid, ItemStackReference itemReference) implements CustomPacketPayload {
	public static final Type<RequestStorageContentsPayload> TYPE = new Type<>(SophisticatedStorage.getRL("request_storage_contents"));
	public static final StreamCodec<ByteBuf, RequestStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			RequestStorageContentsPayload::storageUuid, ItemStackReference.STREAM_CODEC, RequestStorageContentsPayload::itemReference,
			RequestStorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestStorageContentsPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer serverPlayer)) {
			return;
		}

		ItemStack stack = payload.itemReference.resolve(serverPlayer);
		if (!(stack.getItem() instanceof StorageBlockItem) || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)
				|| !payload.storageUuid.equals(stack.get(ModCoreDataComponents.STORAGE_UUID))) {
			return;
		}
		ItemContentsStorage.get().getStorageContents(payload.storageUuid)
				.ifPresent(contents -> PacketDistributor.sendToPlayer(serverPlayer, new StorageContentsPayload(payload.storageUuid, contents)));
	}
}
