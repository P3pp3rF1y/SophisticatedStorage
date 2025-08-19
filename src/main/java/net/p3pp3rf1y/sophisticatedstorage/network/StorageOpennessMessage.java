package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.function.Supplier;

public record StorageOpennessMessage(BlockPos pos, boolean shouldBeOpen) {
	public static void encode(StorageOpennessMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBlockPos(msg.pos);
		packetBuffer.writeBoolean(msg.shouldBeOpen);
	}

	public static StorageOpennessMessage decode(FriendlyByteBuf packetBuffer) {
		return new StorageOpennessMessage(packetBuffer.readBlockPos(), packetBuffer.readBoolean());
	}

	static void onMessage(StorageOpennessMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(StorageOpennessMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		WorldHelper.getLoadedBlockEntity(player.level(), msg.pos, StorageBlockEntity.class).ifPresent(
				storageBlockEntity -> storageBlockEntity.setShouldBeOpen(msg.shouldBeOpen)
		);
	}
}
