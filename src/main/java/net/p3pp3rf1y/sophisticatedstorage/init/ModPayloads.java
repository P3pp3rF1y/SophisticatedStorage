package net.p3pp3rf1y.sophisticatedstorage.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.network.*;

public class ModPayloads {
	private ModPayloads() {
	}

	public static void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedStorage.MOD_ID).versioned("1.0");
		registrar.playToServer(OpenStorageInventoryPayload.TYPE, OpenStorageInventoryPayload.STREAM_CODEC, OpenStorageInventoryPayload::handlePayload);
		registrar.playToServer(RequestStorageContentsPayload.TYPE, RequestStorageContentsPayload.STREAM_CODEC, RequestStorageContentsPayload::handlePayload);
		registrar.playToClient(StorageContentsPayload.TYPE, StorageContentsPayload.STREAM_CODEC, StorageContentsPayload::handlePayload);
		registrar.playToServer(ScrolledToolPayload.TYPE, ScrolledToolPayload.STREAM_CODEC, ScrolledToolPayload::handlePayload);
		registrar.playToServer(RequestPlayerSettingsPayload.TYPE, RequestPlayerSettingsPayload.STREAM_CODEC, (payload, context) -> RequestPlayerSettingsPayload.handlePayload(context));
	}
}
