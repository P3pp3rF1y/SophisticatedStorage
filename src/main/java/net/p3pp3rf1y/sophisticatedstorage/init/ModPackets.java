package net.p3pp3rf1y.sophisticatedstorage.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.network.*;

public class ModPackets {
	private ModPackets() {
	}

	public static void registerPackets(final RegisterPayloadHandlerEvent event) {
		final IPayloadRegistrar registrar = event.registrar(SophisticatedStorage.MOD_ID).versioned("1.0");
		registrar.play(OpenStorageInventoryPacket.ID, OpenStorageInventoryPacket::new, play -> play.server(OpenStorageInventoryPacket::handle));
		registrar.play(RequestStorageContentsPacket.ID, RequestStorageContentsPacket::new, play -> play.server(RequestStorageContentsPacket::handle));
		registrar.play(StorageContentsPacket.ID, StorageContentsPacket::new, play -> play.client(StorageContentsPacket::handle));
		registrar.play(ScrolledToolPacket.ID, ScrolledToolPacket::new, play -> play.server(ScrolledToolPacket::handle));
		registrar.play(RequestPlayerSettingsPacket.ID, buf -> new RequestPlayerSettingsPacket(), play -> play.server(RequestPlayerSettingsPacket::handle));
	}
}
