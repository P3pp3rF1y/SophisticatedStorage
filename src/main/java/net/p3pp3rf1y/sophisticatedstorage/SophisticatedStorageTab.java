package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class SophisticatedStorageTab extends CreativeModeTab {
	private ItemStack tabIcon;

	SophisticatedStorageTab() {
		super(SophisticatedStorage.MOD_ID);
	}

	@Override
	public ItemStack makeIcon() {
		if (tabIcon == null) {
			tabIcon = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.GOLD_BARREL_ITEM.get()), WoodType.SPRUCE);
		}
		return tabIcon;
	}
}
