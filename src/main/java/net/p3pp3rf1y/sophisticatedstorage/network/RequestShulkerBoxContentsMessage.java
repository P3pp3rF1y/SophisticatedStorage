package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxStorage;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestShulkerBoxContentsMessage {
	private final UUID shulkerBoxUuid;

	public RequestShulkerBoxContentsMessage(UUID shulkerBoxUuid) {
		this.shulkerBoxUuid = shulkerBoxUuid;
	}

	public static void encode(RequestShulkerBoxContentsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.shulkerBoxUuid);
	}

	public static RequestShulkerBoxContentsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestShulkerBoxContentsMessage(packetBuffer.readUUID());
	}

	static void onMessage(RequestShulkerBoxContentsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, RequestShulkerBoxContentsMessage msg) {
		if (player == null) {
			return;
		}

		SophisticatedStorage.PACKET_HANDLER.sendToClient(player, new ShulkerBoxContentsMessage(msg.shulkerBoxUuid, ShulkerBoxStorage.get().getOrCreateShulkerBoxContents(msg.shulkerBoxUuid)));
	}
}
