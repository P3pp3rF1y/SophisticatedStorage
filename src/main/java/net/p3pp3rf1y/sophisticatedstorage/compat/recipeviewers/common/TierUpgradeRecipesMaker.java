package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {
	}

	public static void addRecipes(IRecipeDisplayGenerator<?> generator, Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> subtypeInterpreterGetter) {
		String idPrefix = "tier_upgrade_";
		ClientRecipeHelper.addVariantRecipes(generator, StorageTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getStorageItems, subtypeInterpreterGetter, SophisticatedStorage.MOD_ID, idPrefix);
		ClientRecipeHelper.addVariantRecipes(generator, DoubleChestTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, SophisticatedStorage.MOD_ID, idPrefix);
		ClientRecipeHelper.addVariantRecipes(generator, StorageTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getStorageItems, subtypeInterpreterGetter, SophisticatedStorage.MOD_ID, idPrefix);
		ClientRecipeHelper.addVariantRecipes(generator, DoubleChestTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, SophisticatedStorage.MOD_ID, idPrefix);
	}

	private static List<ItemStack> getDoubleChestItems(Recipe<?> recipe) {
		return ClientRecipeHelper.getIngredientCreativeTabVariants(recipe, ChestBlockItem.class, stack -> ChestBlockItem.setDoubleChest(stack, true));
	}

	private static List<ItemStack> getStorageItems(Recipe<?> recipe) {
		return ClientRecipeHelper.getIngredientCreativeTabVariants(recipe, StorageBlockItem.class);
	}
}
