package net.p3pp3rf1y.sophisticatedstorage.compat.sb;

import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsCategory;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsTab;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsTabControl;

public class SBClientCompat {
	private SBClientCompat() {
	}

	public static void setup() {
		StorageSettingsTabControl.registerCompatFactory(BackpackMainSettingsCategory.NAME, BackpackMainSettingsTab::new);
	}
}
