package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxStorage;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class ShulkerBoxContentsMessage {
	private final UUID shulkerBoxUuid;
	@Nullable
	private final CompoundTag contents;

	public ShulkerBoxContentsMessage(UUID shulkerBoxUuid, @Nullable CompoundTag contents) {
		this.shulkerBoxUuid = shulkerBoxUuid;
		this.contents = contents;
	}

	public static void encode(ShulkerBoxContentsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.shulkerBoxUuid);
		packetBuffer.writeNbt(msg.contents);
	}

	public static ShulkerBoxContentsMessage decode(FriendlyByteBuf packetBuffer) {
		return new ShulkerBoxContentsMessage(packetBuffer.readUUID(), packetBuffer.readNbt());
	}

	static void onMessage(ShulkerBoxContentsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(ShulkerBoxContentsMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || msg.contents == null) {
			return;
		}

		ShulkerBoxStorage.get().setShulkerBoxContents(msg.shulkerBoxUuid, msg.contents);
		ClientStorageContentsTooltip.refreshContents();
	}
}
