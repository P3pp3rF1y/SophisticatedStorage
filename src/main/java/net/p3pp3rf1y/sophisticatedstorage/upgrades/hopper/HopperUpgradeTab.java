package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.SideIOControl;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

public abstract class HopperUpgradeTab extends UpgradeSettingsTab<HopperUpgradeContainer> {

	protected ContentsFilterControl inputFilterLogicControl;
	protected ContentsFilterControl outputFilterLogicControl;

	protected HopperUpgradeTab(HopperUpgradeContainer container, Position position, StorageScreenBase<?> screen, Component tabLabel, Component closedTooltip) {
		super(container, position, screen, tabLabel, closedTooltip);
	}

	@Override
	protected void moveSlotsToTab() {
		inputFilterLogicControl.moveSlotsToView();
		outputFilterLogicControl.moveSlotsToView();
	}

	public static class Basic extends HopperUpgradeTab {
		public Basic(HopperUpgradeContainer container, Position pos, StorageScreenBase<?> screen) {
			super(container, pos, screen, StorageTranslationHelper.INSTANCE.translUpgrade("hopper"),
					StorageTranslationHelper.INSTANCE.translUpgradeTooltip("hopper"));
			inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 24), getContainer().getInputFilterLogicContainer(),
					Config.SERVER.hopperUpgrade.inputFilterSlotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4), getContainer().getOutputFilterLogicContainer(),
					Config.SERVER.hopperUpgrade.outputFilterSlotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		}
	}

	public static class Advanced extends HopperUpgradeTab {

		public Advanced(HopperUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
			super(container, position, screen, StorageTranslationHelper.INSTANCE.translUpgrade("advanced_hopper"),
					StorageTranslationHelper.INSTANCE.translUpgradeTooltip("advanced_hopper"));
			inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 24), getContainer().getInputFilterLogicContainer(),
					Config.SERVER.advancedHopperUpgrade.inputFilterSlotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));

			SideIOControl sideIOControl = new SideIOControl(getContainer().getSideIOContainer(), new Position(x + 3 + 9, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4));
			addHideableChild(sideIOControl);

			outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, sideIOControl.getY() + sideIOControl.getHeight() + 4), getContainer().getOutputFilterLogicContainer(),
					Config.SERVER.advancedHopperUpgrade.outputFilterSlotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		}
	}
}
