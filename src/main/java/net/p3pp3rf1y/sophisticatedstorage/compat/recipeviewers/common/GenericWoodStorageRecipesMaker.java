package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GenericWoodStorageRecipesMaker {
	private GenericWoodStorageRecipesMaker() {
	}

	public static List<RecipeHolder<CraftingRecipe>> getRecipes() {
		return ClientRecipeHelper.transformAllRecipeHoldersOfTypeIntoMultiple(RecipeType.CRAFTING, GenericWoodStorageRecipe.class, recipeHolder -> {
			List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
			GenericWoodStorageHelper.getGenericWoodTypes().forEach(woodType -> GenericWoodStorageHelper.getGenericWoodInfo(woodType)
					.ifPresent(info -> recipes.add(createRecipe(recipeHolder, woodType, info))));
			return recipes;
		});
	}

	static RecipeHolder<CraftingRecipe> createRecipe(RecipeHolder<GenericWoodStorageRecipe> recipeHolder, WoodType woodType,
			GenericWoodStorageHelper.GenericWoodInfo info) {
		GenericWoodStorageRecipe recipe = recipeHolder.value();
		ShapedRecipe compose = recipe.getCompose();
		List<Optional<Ingredient>> ingredients = specializeWoodIngredients(RecipeViewerRecipeHelper.getIngredients(compose), info);
		ItemStack result = ClientRecipeHelper.getResultItem(recipe).copy();
		WoodStorageBlockItem.setWoodType(result, woodType);

		ShapedRecipe displayRecipe = new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(recipe.category(), ""),
				new ShapedRecipePattern(compose.getWidth(), compose.getHeight(), ingredients, Optional.empty()), ItemStackTemplate.fromNonEmptyStack(result));
		return new RecipeHolder<>(ClientRecipeHelper.recipeKey(getRecipeId(recipeHolder.id().identifier(), woodType)), displayRecipe);
	}

	private static List<Optional<Ingredient>> specializeWoodIngredients(Collection<Optional<Ingredient>> ingredients,
			GenericWoodStorageHelper.GenericWoodInfo info) {
		List<Optional<Ingredient>> specializedIngredients = new ArrayList<>(ingredients.size());
		ItemStack planks = new ItemStack(info.planks());
		ItemStack slab = new ItemStack(info.slab());
		for (Optional<Ingredient> ingredient : ingredients) {
			specializedIngredients.add(ingredient.map(i -> specializeWoodIngredient(i, planks, slab, info)));
		}
		return specializedIngredients;
	}

	private static Ingredient specializeWoodIngredient(Ingredient ingredient, ItemStack planks, ItemStack slab, GenericWoodStorageHelper.GenericWoodInfo info) {
		if (ingredient.test(planks)) {
			return Ingredient.of(info.planks());
		} else if (ingredient.test(slab)) {
			return Ingredient.of(info.slab());
		}
		return ingredient;
	}

	private static Identifier getRecipeId(Identifier recipeId, WoodType woodType) {
		return recipeId.withPath(path -> "generic_wood_storage/" + woodType.name().toLowerCase(Locale.ROOT).replace(':', '/') + "/" + path);
	}
}
