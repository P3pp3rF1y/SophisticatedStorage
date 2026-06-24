package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IFocusBehavior;
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

		recipes.add(new RecipeHolder<>(ClientRecipeHelper.recipeKey(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "flatten_barrel")),
				new ShapelessRecipe("", CraftingBookCategory.MISC, flatBarrel,
						NonNullList.of(ClientRecipeHelper.emptyDisplayIngredient(), DataComponentIngredient.of(false, barrel)))));
		recipes.add(new RecipeHolder<>(ClientRecipeHelper.recipeKey(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "unflatten_barrel")),
				new ShapelessRecipe("", CraftingBookCategory.MISC, barrel,
						NonNullList.of(ClientRecipeHelper.emptyDisplayIngredient(), DataComponentIngredient.of(false, flatBarrel)))));

		return recipes;
	}

	public static List<CraftingDisplaySpec> getShapelessSpecs() {
		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL.get()), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.toggleFlatTop(flatBarrel);

		return List.of(shapelessSpec("flatten_barrel", barrel, flatBarrel), shapelessSpec("unflatten_barrel", flatBarrel, barrel));
	}

	private static CraftingDisplaySpec shapelessSpec(String path, ItemStack input, ItemStack output) {
		NonNullList<Ingredient> ingredients = NonNullList.of(ClientRecipeHelper.emptyDisplayIngredient(), DataComponentIngredient.of(false, input));
		CraftingDisplayVariant variant = new CraftingDisplayVariant(List.of(input), List.of(output));
		return new CraftingDisplaySpec(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, path), true, 0, 0, ingredients, List.of(variant),
				EXACT_FOCUS_BEHAVIOR);
	}

	private static final IFocusBehavior<CraftingDisplayVariant> EXACT_FOCUS_BEHAVIOR = new IFocusBehavior<>() {
		@Override
		public List<CraftingDisplayVariant> allDisplays(List<CraftingDisplayVariant> variants) {
			return variants;
		}

		@Override
		public List<CraftingDisplayVariant> recipesFor(List<CraftingDisplayVariant> variants, ItemStack focusedOutput) {
			return variants.stream().filter(variant -> variant.outputs().stream().anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusedOutput)))
					.toList();
		}

		@Override
		public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
			return variants.stream().filter(variant -> variant.inputs().stream().anyMatch(input -> ItemStack.isSameItemSameComponents(input, focusedInput)))
					.toList();
		}
	};
}
