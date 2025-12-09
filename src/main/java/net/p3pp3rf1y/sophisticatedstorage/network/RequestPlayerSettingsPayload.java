package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsPayload;
import net.p3pp3rf1y.sophisticatedcore.settings.main.PlayerMainSettingsSavedData;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class RequestPlayerSettingsPayload implements CustomPacketPayload {
	public static final Type<RequestPlayerSettingsPayload> TYPE = new Type<>(SophisticatedStorage.getRL("request_player_settings"));
	public static final StreamCodec<ByteBuf, RequestPlayerSettingsPayload> STREAM_CODEC = StreamCodecHelper.singleton(RequestPlayerSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(IPayloadContext context) {
		String name = SophisticatedStorage.MOD_ID;
		if (context.player() instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new SyncPlayerSettingsPayload(name, PlayerMainSettingsSavedData.get().get(serverPlayer.getUUID(), name)));
		}
	}
}
