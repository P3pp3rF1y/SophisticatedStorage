package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import javax.annotation.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestStorageContentsMessage {
	private final UUID storageUuid;
	private final ItemStackReference itemReference;

	public RequestStorageContentsMessage(UUID storageUuid, ItemStackReference itemReference) {
		this.storageUuid = storageUuid;
		this.itemReference = itemReference;
	}

	public static void encode(RequestStorageContentsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.storageUuid);
		ItemStackReference.encode(msg.itemReference, packetBuffer);
	}

	public static RequestStorageContentsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestStorageContentsMessage(packetBuffer.readUUID(), ItemStackReference.decode(packetBuffer));
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

		ItemStack stack = msg.itemReference.resolve(player);
		if (!(stack.getItem() instanceof StorageBlockItem) || LinkedStorageStackData.getEndpoint(stack) != null
				|| NBTHelper.getUniqueId(stack, "uuid").filter(msg.storageUuid::equals).isEmpty()) {
			return;
		}
		ItemContentsStorage.get().getStorageContents(msg.storageUuid)
				.ifPresent(contents -> StoragePacketHandler.INSTANCE.sendToClient(player, new StorageContentsMessage(msg.storageUuid, contents)));
	}
}
