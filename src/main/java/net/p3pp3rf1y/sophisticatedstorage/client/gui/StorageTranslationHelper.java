package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class StorageTranslationHelper extends TranslationHelper {
	public static final StorageTranslationHelper INSTANCE = new StorageTranslationHelper();

	private StorageTranslationHelper() {
		super(SophisticatedStorage.MOD_ID);
	}

	public Component translItemOverlayMessage(Item item, String overlayMessage, Object... params) {
		return Component.translatable(item.getDescriptionId() + ".overlay." + overlayMessage, params);
	}
}
