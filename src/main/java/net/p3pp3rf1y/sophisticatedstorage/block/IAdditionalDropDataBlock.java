package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;

public interface IAdditionalDropDataBlock {
	void addDropData(ItemStack stack, StorageBlockEntity be);
}
