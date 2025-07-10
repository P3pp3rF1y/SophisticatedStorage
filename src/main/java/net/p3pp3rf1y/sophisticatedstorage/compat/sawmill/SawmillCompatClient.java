package net.p3pp3rf1y.sophisticatedstorage.compat.sawmill;

import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.compat.sawmill.SawmillUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.compat.sawmill.SawmillUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.compat.sawmill.SawmillUpgradeTab;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions;

public class SawmillCompatClient {
	private SawmillCompatClient() {
	}

	public static void registerUpgradeTab(UpgradeContainerType<SawmillUpgradeItem.Wrapper, SawmillUpgradeContainer> containerType) {
		UpgradeGuiManager.registerTab(containerType, (SawmillUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) -> new SawmillUpgradeTab(upgradeContainer, position, screen, StorageButtonDefinitions.SHIFT_CLICK_TARGET));
	}
}
