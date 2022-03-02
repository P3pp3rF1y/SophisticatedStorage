package net.p3pp3rf1y.sophisticatedstorage.settings;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;

import java.util.function.Supplier;

public class StorageSettingsHandler extends SettingsHandler {
	public static final String SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG = "sophisticatedStorageSettings";

	public StorageSettingsHandler(CompoundTag contentsNbt, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier) {
		super(contentsNbt, markContentsDirty, SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG, inventoryHandlerSupplier, renderInfoSupplier);
	}

	@Override
	protected CompoundTag getSettingsNbtFromContentsNbt(CompoundTag contentsNbt) {
		return contentsNbt;
	}

	@Override
	protected void saveCategoryNbt(CompoundTag settingsNbt, String categoryName, CompoundTag tag) {
		contentsNbt.put(categoryName, tag);
	}

	@Override
	public void reloadFrom(CompoundTag contentsNbt) {
		this.contentsNbt = contentsNbt;
		super.reloadFrom(contentsNbt);
	}
}
