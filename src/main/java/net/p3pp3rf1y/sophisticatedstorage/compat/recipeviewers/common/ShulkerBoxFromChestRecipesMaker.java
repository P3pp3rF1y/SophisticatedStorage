package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ShulkerBoxFromChestRecipesMaker {
	private ShulkerBoxFromChestRecipesMaker() {
	}

	public static void addRecipes(IRecipeDisplayGenerator<?> generator, Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter) {
		ClientRecipeHelper.addVariantRecipes(generator, ShulkerBoxFromChestRecipe.class, ShulkerBoxFromChestRecipesMaker::getChestItems, getSubtypeInterpreter, SophisticatedStorage.MOD_ID, "shulker_from_");
	}

	private static List<ItemStack> getChestItems(Recipe<?> recipe) {
		return ClientRecipeHelper.getIngredientCreativeTabVariants(recipe, ChestBlockItem.class);
	}
}
