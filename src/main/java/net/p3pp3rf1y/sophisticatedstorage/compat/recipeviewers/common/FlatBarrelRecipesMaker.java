package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class FlatBarrelRecipesMaker {
	private FlatBarrelRecipesMaker() {
	}

	public static List<RecipeHolder<CraftingRecipe>> getShapelessRecipes() {
		return getShapelessRecipes(RecipeHolder::new);
	}

	public static <R> List<R> getShapelessRecipes(BiFunction<ResourceLocation, CraftingRecipe, R> transformRecipe) {
		List<R> recipes = new ArrayList<>();

		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL.get()), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.toggleFlatTop(flatBarrel);

		recipes.add(transformRecipe.apply(SophisticatedStorage.getRL("flatten_barrel"), new ShapelessRecipe("", CraftingBookCategory.MISC, flatBarrel, NonNullList.of(Ingredient.EMPTY, Ingredient.of(barrel)))));
		recipes.add(transformRecipe.apply(SophisticatedStorage.getRL("unflatten_barrel"), new ShapelessRecipe("", CraftingBookCategory.MISC, barrel, NonNullList.of(Ingredient.EMPTY, Ingredient.of(flatBarrel)))));

		return recipes;
	}
}
