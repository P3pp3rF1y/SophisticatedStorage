package net.p3pp3rf1y.sophisticatedstorage.network;

import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class StoragePacketHandler extends PacketHandler {
	public StoragePacketHandler(String modId) {
		super(modId);
	}

	@Override
	public void init() {
		registerMessage(OpenStorageInventoryMessage.class, OpenStorageInventoryMessage::encode, OpenStorageInventoryMessage::decode, OpenStorageInventoryMessage::onMessage);
		registerMessage(RequestShulkerBoxContentsMessage.class, RequestShulkerBoxContentsMessage::encode, RequestShulkerBoxContentsMessage::decode, RequestShulkerBoxContentsMessage::onMessage);
		registerMessage(ShulkerBoxContentsMessage.class, ShulkerBoxContentsMessage::encode, ShulkerBoxContentsMessage::decode, ShulkerBoxContentsMessage::onMessage);
	}
}
