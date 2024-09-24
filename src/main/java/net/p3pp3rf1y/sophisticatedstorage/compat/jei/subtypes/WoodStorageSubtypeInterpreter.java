package net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.StringJoiner;

public class WoodStorageSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public WoodStorageSubtypeInterpreter() {
		addOptionalProperty(WoodStorageBlockItem::getWoodType);
		addOptionalProperty(StorageBlockItem::getMainColorFromStack);
		addOptionalProperty(StorageBlockItem::getAccentColorFromStack);
	}

	@Override
	public String getLegacyStringSubtypeInfo(ItemStack itemStack, UidContext context) {
		StringJoiner result = new StringJoiner(",");
		WoodStorageBlockItem.getWoodType(itemStack).ifPresent(woodName -> result.add("woodName:" + woodName));
		StorageBlockItem.getMainColorFromStack(itemStack).ifPresent(mainColor -> result.add("mainColor:" + mainColor));
		StorageBlockItem.getAccentColorFromStack(itemStack).ifPresent(accentColor -> result.add("accentColor:" + accentColor));
		return "{" + result + "}";
	}
}
