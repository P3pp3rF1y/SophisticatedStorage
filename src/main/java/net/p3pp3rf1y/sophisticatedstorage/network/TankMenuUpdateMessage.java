package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;

import java.util.function.Supplier;

public record TankMenuUpdateMessage(int containerId, int amount, int capacity) {
	public static void encode(TankMenuUpdateMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.containerId);
		buffer.writeVarInt(message.amount);
		buffer.writeVarInt(message.capacity);
	}

	public static TankMenuUpdateMessage decode(FriendlyByteBuf buffer) {
		return new TankMenuUpdateMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
	}

	static void onMessage(TankMenuUpdateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof TankContainerMenu menu
					&& menu.containerId == message.containerId) {
				menu.setFluidData(message.amount, message.capacity);
			}
		});
		context.setPacketHandled(true);
	}
}
