package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class SophisticatedStorageTab extends CreativeModeTab {
	private ItemStack tabIcon;

	SophisticatedStorageTab() {
		super(SophisticatedStorage.MOD_ID);
	}

	@Override
	public ItemStack makeIcon() {
		if (tabIcon == null) {
			tabIcon = new ItemStack(ModBlocks.BARREL.get());
		}
		return tabIcon;
	}
}
