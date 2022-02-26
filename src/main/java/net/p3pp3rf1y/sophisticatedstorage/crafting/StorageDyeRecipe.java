package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.p3pp3rf1y.sophisticatedcore.crafting.StorageDyeRecipeBase;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlock;

import java.util.List;

public class StorageDyeRecipe extends StorageDyeRecipeBase {
	public static final SimpleRecipeSerializer<StorageDyeRecipe> SERIALIZER = new SimpleRecipeSerializer<>(StorageDyeRecipe::new);
	public StorageDyeRecipe(ResourceLocation registryName) {
		super(registryName);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	protected boolean isStorageItem(Item item) {
		return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ITintableBlock;
	}

	@Override
	protected void applyColors(ItemStack coloredStorage, List<DyeColor> mainDyes, List<DyeColor> trimDyes) {
		if (coloredStorage.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ITintableBlock tintableBlock) {
			if (!mainDyes.isEmpty()) {
				tintableBlock.setMainColor(coloredStorage, ColorHelper.calculateColor(tintableBlock.getMainColor(coloredStorage).orElse(0), 0, mainDyes));
			}
			if (!trimDyes.isEmpty()) {
				tintableBlock.setAccentColor(coloredStorage, ColorHelper.calculateColor(tintableBlock.getAccentColor(coloredStorage).orElse(0), 0, trimDyes));
			}
		}
	}
}
