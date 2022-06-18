package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ItemButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsTab;

import java.util.List;

public class StorageMainSettingsTab extends MainSettingsTab<MainSettingsContainer> {
	private static final List<Component> CONTEXT_TOOLTIP = List.of(
			Component.translatable(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip")),
			Component.translatable(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip_detail")).withStyle(ChatFormatting.GRAY)
	);

	public StorageMainSettingsTab(MainSettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, CONTEXT_TOOLTIP, Component.translatable(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage")),
				StorageTranslationHelper.INSTANCE.translSettings("storage"), StorageTranslationHelper.INSTANCE.translSettingsTooltip("storage"),
				onTabIconClicked -> new ItemButton(new Position(position.x() + 1, position.y() + 4), onTabIconClicked, container.getSettingsContainer().getStorageWrapper().getWrappedStorageStack(), Component.translatable("gui.sophisticatedstorage.narrate.global_tab_button")));
	}
}
