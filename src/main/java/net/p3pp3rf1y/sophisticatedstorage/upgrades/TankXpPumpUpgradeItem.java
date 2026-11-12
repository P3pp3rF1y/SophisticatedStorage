package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.Config;

public class TankXpPumpUpgradeItem extends XpPumpUpgradeItem {
	public TankXpPumpUpgradeItem() {
		super(Config.SERVER.xpPumpUpgrade, Config.SERVER.maxUpgradesPerStorage);
	}
}
