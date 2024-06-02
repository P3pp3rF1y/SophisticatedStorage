package net.p3pp3rf1y.sophisticatedstorage.network;

import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class StoragePacketHandler extends PacketHandler {
	public static final StoragePacketHandler INSTANCE = new StoragePacketHandler(SophisticatedStorage.MOD_ID);

	public StoragePacketHandler(String modId) {
		super(modId);
	}

	@Override
	public void init() {
		registerMessage(OpenStorageInventoryMessage.class, OpenStorageInventoryMessage::encode, OpenStorageInventoryMessage::decode, OpenStorageInventoryMessage::onMessage);
		registerMessage(RequestStorageContentsMessage.class, RequestStorageContentsMessage::encode, RequestStorageContentsMessage::decode, RequestStorageContentsMessage::onMessage);
		registerMessage(StorageContentsMessage.class, StorageContentsMessage::encode, StorageContentsMessage::decode, StorageContentsMessage::onMessage);
		registerMessage(ScrolledToolMessage.class, ScrolledToolMessage::encode, ScrolledToolMessage::decode, ScrolledToolMessage::onMessage);
	}
}
