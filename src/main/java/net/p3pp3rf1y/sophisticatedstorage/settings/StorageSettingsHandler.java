package net.p3pp3rf1y.sophisticatedstorage.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import java.util.function.Supplier;

public abstract class StorageSettingsHandler extends SettingsHandler {
	public static final String SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG = "sophisticatedStorageSettings";

	static {
		MinecraftForge.EVENT_BUS.addListener(StorageSettingsHandler::onPlayerClone);
	}

	private static void onPlayerClone(PlayerEvent.Clone event) {
		CompoundTag oldData = event.getOriginal().getPersistentData();
		CompoundTag newData = event.getPlayer().getPersistentData();

		if (oldData.contains(SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG)) {
			//noinspection ConstantConditions
			newData.put(SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG, oldData.get(SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG));
		}
	}

	protected StorageSettingsHandler(CompoundTag contentsNbt, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier) {
		super(contentsNbt, markContentsDirty, inventoryHandlerSupplier, renderInfoSupplier);
	}

	protected abstract int getNumberOfDisplayItems();

	@Override
	protected CompoundTag getSettingsNbtFromContentsNbt(CompoundTag contentsNbt) {
		return contentsNbt;
	}

	@Override
	protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier, CompoundTag settingsNbt) {
		addSettingsCategory(settingsNbt, ItemDisplaySettingsCategory.NAME, markContentsDirty, (categoryNbt, saveNbt) ->
				new ItemDisplaySettingsCategory(inventoryHandlerSupplier, renderInfoSupplier, categoryNbt, saveNbt, getNumberOfDisplayItems(), () -> getTypeCategory(MemorySettingsCategory.class)));
	}

	@Override
	protected void addGlobalSettingsCategory(CompoundTag settingsNbt) {
		addSettingsCategory(settingsNbt, MainSettingsCategory.NAME, markContentsDirty, (categoryNbt, saveNbt) -> new MainSettingsCategory(categoryNbt, saveNbt, SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG));
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
