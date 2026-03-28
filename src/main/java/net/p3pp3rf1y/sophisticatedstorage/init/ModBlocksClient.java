package net.p3pp3rf1y.sophisticatedstorage.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.*;

public class ModBlocksClient {
	private ModBlocksClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener(ModBlocksClient::onMenuScreenRegister);
	}

	private static void onMenuScreenRegister(RegisterMenuScreensEvent event) {
		event.register(ModBlocks.STORAGE_CONTAINER_TYPE.get(), StorageScreen::constructScreen);
		event.register(ModBlocks.SETTINGS_CONTAINER_TYPE.get(), StorageSettingsScreen::constructScreen);
		event.register(ModBlocks.LIMITED_BARREL_CONTAINER_TYPE.get(), LimitedBarrelScreen::new);
		event.register(ModBlocks.LIMITED_BARREL_SETTINGS_CONTAINER_TYPE.get(), LimitedBarrelSettingsScreen::new);
		event.register(ModBlocks.DECORATION_TABLE_CONTAINER_TYPE.get(), DecorationTableScreen::new);
	}
}
