package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsPacket;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

public class RequestPlayerSettingsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedStorage.MOD_ID, "request_player_settings");

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		PacketHelper.sendToPlayer(new SyncPlayerSettingsPacket(playerTagName, SettingsManager.getPlayerSettingsTag(player, playerTagName)), (ServerPlayer) player);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		//noop
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
