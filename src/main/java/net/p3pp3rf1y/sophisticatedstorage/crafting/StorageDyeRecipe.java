package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.p3pp3rf1y.sophisticatedcore.crafting.StorageDyeRecipeBase;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.List;

public class StorageDyeRecipe extends StorageDyeRecipeBase {
	public StorageDyeRecipe(ResourceLocation registryName) {
		super(registryName);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.STORAGE_DYE_RECIPE_SERIALIZER.get();
	}

	@Override
	protected boolean isStorageItem(Item item) {
		return item instanceof BlockItem blockItem && blockItem instanceof ITintableBlockItem;
	}

	@Override
	protected void applyColors(ItemStack coloredStorage, List<DyeColor> mainDyes, List<DyeColor> trimDyes) {
		if (coloredStorage.getItem() instanceof BlockItem blockItem && blockItem instanceof ITintableBlockItem tintableBlockItem) {
			if (!mainDyes.isEmpty()) {
				tintableBlockItem.setMainColor(coloredStorage, ColorHelper.calculateColor(tintableBlockItem.getMainColor(coloredStorage).orElse(0), 0, mainDyes));
			}
			if (!trimDyes.isEmpty()) {
				tintableBlockItem.setAccentColor(coloredStorage, ColorHelper.calculateColor(tintableBlockItem.getAccentColor(coloredStorage).orElse(0), 0, trimDyes));
			}
		}
	}
}
