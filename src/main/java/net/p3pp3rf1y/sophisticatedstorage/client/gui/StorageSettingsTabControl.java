package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsTab;

public class StorageSettingsTabControl extends StorageSettingsTabControlBase {

	public static void registerFactories() {
		addFactory(MainSettingsCategory.NAME, StorageMainSettingsTab::new);
		addFactory(ItemDisplaySettingsCategory.NAME, ItemDisplaySettingsTab::new);
		addFactory(NoSortSettingsCategory.NAME, NoSortSettingsTab::new);
		addFactory(MemorySettingsCategory.NAME, MemorySettingsTab::new);
	}

	protected StorageSettingsTabControl(StorageSettingsScreen screen, Position position) {
		super(screen, position);
	}

	@Override
	protected Tab instantiateReturnBackTab() {
		return new BackToStorageTab(new Position(x, getTopY()), screen.getMenu().getBlockPosition());
	}

}
