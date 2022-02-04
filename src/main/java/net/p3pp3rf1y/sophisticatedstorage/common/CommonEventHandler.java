package net.p3pp3rf1y.sophisticatedstorage.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsMessage;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

public class CommonEventHandler {

	public void registerHandlers() {
		IEventBus eventBus = MinecraftForge.EVENT_BUS;

		eventBus.addListener(this::onPlayerLoggedIn);
		eventBus.addListener(this::onPlayerChangedDimension);
	}

	private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		SophisticatedCore.PACKET_HANDLER.sendToClient((ServerPlayer) event.getPlayer(), new SyncPlayerSettingsMessage(playerTagName, SettingsManager.getPlayerSettingsTag(event.getPlayer(), playerTagName)));
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		SophisticatedCore.PACKET_HANDLER.sendToClient((ServerPlayer) event.getPlayer(), new SyncPlayerSettingsMessage(playerTagName, SettingsManager.getPlayerSettingsTag(event.getPlayer(), playerTagName)));
	}
}
