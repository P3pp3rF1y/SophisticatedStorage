package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes;

import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class ChestSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public ChestSubtypeInterpreter() {
		addOptionalProperty(WoodStorageBlockItem::getWoodType, "woodName", WoodType::name);
		addOptionalProperty(StorageBlockItem::getMainColorFromStack, "mainColor", String::valueOf);
		addOptionalProperty(StorageBlockItem::getAccentColorFromStack, "accentColor", String::valueOf);
		addProperty(ChestBlockItem::isDoubleChest, "doubleChest", String::valueOf);
	}
}
