package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class StoragePacketHandler extends PacketHandler {
	public static final StoragePacketHandler INSTANCE = new StoragePacketHandler(SophisticatedStorage.MOD_ID, SophisticatedStorage.getNetworkProtocolVersion());

	private StoragePacketHandler(String modId, String protocol) {
		super(modId, protocol);
	}

	@Override
	public void registerMessages() {
		registerMessage(OpenStorageInventoryMessage.class, OpenStorageInventoryMessage::encode, OpenStorageInventoryMessage::decode,
				OpenStorageInventoryMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RequestStorageContentsMessage.class, RequestStorageContentsMessage::encode, RequestStorageContentsMessage::decode,
				RequestStorageContentsMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(StorageContentsMessage.class, StorageContentsMessage::encode, StorageContentsMessage::decode, StorageContentsMessage::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(ScrolledToolMessage.class, ScrolledToolMessage::encode, ScrolledToolMessage::decode, ScrolledToolMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(StorageOpennessMessage.class, StorageOpennessMessage::encode, StorageOpennessMessage::decode, StorageOpennessMessage::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(RequestControllerTargetHighlightsMessage.class, RequestControllerTargetHighlightsMessage::encode,
				RequestControllerTargetHighlightsMessage::decode, RequestControllerTargetHighlightsMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
	}
}
