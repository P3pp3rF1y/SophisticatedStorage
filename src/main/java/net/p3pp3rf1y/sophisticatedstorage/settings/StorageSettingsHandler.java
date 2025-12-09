package net.p3pp3rf1y.sophisticatedstorage.settings;

import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.function.Supplier;

public abstract class StorageSettingsHandler extends SettingsHandler {
	protected StorageSettingsHandler(ContainerContents.SettingsData settingsData, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier) {
		super(settingsData, markContentsDirty, inventoryHandlerSupplier, renderDataHandlerSupplier, SophisticatedStorage.MOD_ID);
	}

	protected abstract int getNumberOfDisplayItems();

	@Override
	protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier, ContainerContents.SettingsData settingsData) {
		this.<ItemDisplaySettingsCategoryData, ItemDisplaySettingsCategory>addSettingsCategory(settingsData, ItemDisplaySettingsCategory.NAME, markContentsDirty, (data, save) ->
				new ItemDisplaySettingsCategory(inventoryHandlerSupplier, renderDataHandlerSupplier, data, save, getNumberOfDisplayItems(), () -> getTypeCategory(MemorySettingsCategory.class)), ItemDisplaySettingsCategoryData::new);
	}
}
