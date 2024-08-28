package net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.StringJoiner;

public class ShulkerBoxSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public ShulkerBoxSubtypeInterpreter() {
		addOptionalProperty(StorageBlockItem::getMainColorFromStack);
		addOptionalProperty(StorageBlockItem::getAccentColorFromStack);
	}

	@Override
	public String getLegacyStringSubtypeInfo(ItemStack itemStack, UidContext context) {
		StringJoiner result = new StringJoiner(",");
		StorageBlockItem.getMainColorFromStack(itemStack).ifPresent(mainColor -> result.add("mainColor:" + mainColor));
		StorageBlockItem.getAccentColorFromStack(itemStack).ifPresent(accentColor -> result.add("accentColor:" + accentColor));
		return "{" + result + "}";
	}
}
