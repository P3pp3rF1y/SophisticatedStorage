package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class StorageContentsMessage {
	private final UUID shulkerBoxUuid;
	@Nullable
	private final CompoundTag contents;

	public StorageContentsMessage(UUID shulkerBoxUuid, @Nullable CompoundTag contents) {
		this.shulkerBoxUuid = shulkerBoxUuid;
		this.contents = contents;
	}

	public static void encode(StorageContentsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.shulkerBoxUuid);
		packetBuffer.writeNbt(msg.contents);
	}

	public static StorageContentsMessage decode(FriendlyByteBuf packetBuffer) {
		return new StorageContentsMessage(packetBuffer.readUUID(), packetBuffer.readNbt());
	}

	static void onMessage(StorageContentsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(StorageContentsMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || msg.contents == null) {
			return;
		}

		ItemContentsStorage.get().setStorageContents(msg.shulkerBoxUuid, msg.contents);
		ClientStorageContentsTooltipBase.refreshContents();
	}
}
