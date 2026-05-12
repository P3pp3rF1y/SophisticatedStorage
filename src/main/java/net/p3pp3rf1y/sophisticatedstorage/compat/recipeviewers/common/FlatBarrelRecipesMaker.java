package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.List;

public class FlatBarrelRecipesMaker {
	private FlatBarrelRecipesMaker() {
	}

	public static List<CraftingRecipe> getShapelessRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();

		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL.get()), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.toggleFlatTop(flatBarrel);

		recipes.add(new ShapelessRecipe(SophisticatedStorage.getRL("flatten_barrel"), "", CraftingBookCategory.MISC, flatBarrel, NonNullList.of(Ingredient.EMPTY, Ingredient.of(barrel))));
		recipes.add(new ShapelessRecipe(SophisticatedStorage.getRL("unflatten_barrel"), "", CraftingBookCategory.MISC, barrel, NonNullList.of(Ingredient.EMPTY, Ingredient.of(flatBarrel))));

		return recipes;
	}
}
