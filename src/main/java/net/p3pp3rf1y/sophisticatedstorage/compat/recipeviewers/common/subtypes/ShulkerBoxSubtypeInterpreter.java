package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes;

import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

public class ShulkerBoxSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public ShulkerBoxSubtypeInterpreter() {
		addOptionalProperty(StorageBlockItem::getMainColorFromStack, "mainColor", String::valueOf);
		addOptionalProperty(StorageBlockItem::getAccentColorFromStack, "accentColor", String::valueOf);
	}
}
