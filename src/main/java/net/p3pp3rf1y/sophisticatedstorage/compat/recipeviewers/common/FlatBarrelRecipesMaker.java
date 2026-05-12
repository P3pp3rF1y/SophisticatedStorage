package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerIngredients;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.List;

public class FlatBarrelRecipesMaker {
	private FlatBarrelRecipesMaker() {
	}

	public static List<RecipeHolder<CraftingRecipe>> getShapelessRecipes() {
		List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL.get()), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.toggleFlatTop(flatBarrel);

		recipes.add(new RecipeHolder<>(ClientRecipeHelper.recipeKey(SophisticatedStorage.getRL("flatten_barrel")), new ShapelessRecipe("", CraftingBookCategory.MISC, flatBarrel, NonNullList.of(RecipeViewerIngredients.empty(), DataComponentIngredient.of(false, barrel)))));
		recipes.add(new RecipeHolder<>(ClientRecipeHelper.recipeKey(SophisticatedStorage.getRL("unflatten_barrel")), new ShapelessRecipe("", CraftingBookCategory.MISC, barrel, NonNullList.of(RecipeViewerIngredients.empty(), DataComponentIngredient.of(false, flatBarrel)))));

		return recipes;
	}
}
