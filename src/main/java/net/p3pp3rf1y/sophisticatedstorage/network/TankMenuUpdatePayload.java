package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;

public record TankMenuUpdatePayload(int containerId, int amount, int capacity) implements CustomPacketPayload {
	public static final Type<TankMenuUpdatePayload> TYPE = new Type<>(SophisticatedStorage.getIdentifier("tank_menu_update"));
	public static final StreamCodec<ByteBuf, TankMenuUpdatePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
			TankMenuUpdatePayload::containerId, ByteBufCodecs.VAR_INT, TankMenuUpdatePayload::amount, ByteBufCodecs.VAR_INT, TankMenuUpdatePayload::capacity,
			TankMenuUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TankMenuUpdatePayload payload, IPayloadContext context) {
		if (context.player().containerMenu instanceof TankContainerMenu menu && menu.containerId == payload.containerId) {
			menu.setFluidData(payload.amount, payload.capacity);
		}
	}
}
