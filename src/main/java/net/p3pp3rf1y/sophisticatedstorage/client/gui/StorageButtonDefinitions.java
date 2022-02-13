package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;

import java.util.Map;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.getButtonStateData;

public class StorageButtonDefinitions {
	public static final ButtonDefinition.Toggle<ContentsFilterType> STORAGE_CONTENTS_FILTER_TYPE = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					ContentsFilterType.ALLOW, getButtonStateData(new UV(0, 0), StorageTranslationHelper.INSTANCE.translUpgradeButton("allow"), Dimension.SQUARE_16, new Position(1, 1)),
					ContentsFilterType.BLOCK, getButtonStateData(new UV(16, 0), StorageTranslationHelper.INSTANCE.translUpgradeButton("block"), Dimension.SQUARE_16, new Position(1, 1)),
					ContentsFilterType.STORAGE, getButtonStateData(new UV(80, 16), StorageTranslationHelper.INSTANCE.translUpgradeButton("match_storage_contents"), Dimension.SQUARE_16, new Position(1, 1))
			));

	private StorageButtonDefinitions() {}

	public static final ButtonDefinition.Toggle<Boolean> SHIFT_CLICK_TARGET = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					true, getButtonStateData(new UV(32, 48), Dimension.SQUARE_16, new Position(1, 1),
							StorageTranslationHelper.INSTANCE.getTranslatedLines(StorageTranslationHelper.INSTANCE.translUpgradeButton("shift_click_into_storage"), null)),
					false, getButtonStateData(new UV(48, 48), Dimension.SQUARE_16, new Position(1, 1),
							StorageTranslationHelper.INSTANCE.getTranslatedLines(StorageTranslationHelper.INSTANCE.translUpgradeButton("shift_click_into_inventory")))
			));
}
