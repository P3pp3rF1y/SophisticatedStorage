package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface ITintableBlockItem {
	void setMainColor(ItemStack stack, int mainColor);

	void setAccentColor(ItemStack stack, int accentColor);

	void removeMainColor(ItemStack stack);

	void removeAccentColor(ItemStack stack);

	Optional<Integer> getMainColor(ItemStack stack);

	Optional<Integer> getAccentColor(ItemStack stack);
}
