package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.Config;

public class TankPumpUpgradeItem extends PumpUpgradeItem {
	public TankPumpUpgradeItem(boolean interactWithHandDefault, Item.Properties properties) {
		super(interactWithHandDefault, false, true, Config.SERVER.pumpUpgrade, Config.SERVER.maxUpgradesPerStorage, properties);
	}
}
