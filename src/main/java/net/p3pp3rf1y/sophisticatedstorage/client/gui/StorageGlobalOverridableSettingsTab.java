package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.settings.globaloverridable.GlobalOverridableSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.globaloverridable.GlobalOverridableSettingsTab;

import java.util.List;

public class StorageGlobalOverridableSettingsTab extends GlobalOverridableSettingsTab {
	private static final List<Component> CONTEXT_TOOLTIP = List.of(
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip")),
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip_detail")).withStyle(ChatFormatting.GRAY)
	);

	public StorageGlobalOverridableSettingsTab(GlobalOverridableSettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, CONTEXT_TOOLTIP, new TranslatableComponent(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage")),
				StorageTranslationHelper.INSTANCE.translSettings("storage"), StorageTranslationHelper.INSTANCE.translSettingsTooltip("storage"));
	}
}
