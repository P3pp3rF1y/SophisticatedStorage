package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestStorageContentsMessage {
	private final UUID storageUuid;

	public RequestStorageContentsMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public static void encode(RequestStorageContentsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.storageUuid);
	}

	public static RequestStorageContentsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestStorageContentsMessage(packetBuffer.readUUID());
	}

	static void onMessage(RequestStorageContentsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, RequestStorageContentsMessage msg) {
		if (player == null) {
			return;
		}

		SophisticatedStorage.PACKET_HANDLER.sendToClient(player, new StorageContentsMessage(msg.storageUuid, ItemContentsStorage.get().getOrCreateStorageContents(msg.storageUuid)));
	}
}
