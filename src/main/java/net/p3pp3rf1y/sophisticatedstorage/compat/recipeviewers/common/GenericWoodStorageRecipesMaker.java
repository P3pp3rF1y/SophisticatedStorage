package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.ArrayList;
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
		NonNullList<Ingredient> ingredients = specializeWoodIngredients(recipe.getIngredients(), info);
		ItemStack result = ClientRecipeHelper.getResultItem(recipe).copy();
		WoodStorageBlockItem.setWoodType(result, woodType);

		ShapedRecipe displayRecipe = new ShapedRecipe("", recipe.category(),
				new ShapedRecipePattern(recipe.getWidth(), recipe.getHeight(), ingredients, Optional.empty()), result);
		return new RecipeHolder<>(getRecipeId(recipeHolder.id(), woodType), displayRecipe);
	}

	private static NonNullList<Ingredient> specializeWoodIngredients(NonNullList<Ingredient> ingredients, GenericWoodStorageHelper.GenericWoodInfo info) {
		NonNullList<Ingredient> specializedIngredients = NonNullList.createWithCapacity(ingredients.size());
		ItemStack planks = new ItemStack(info.planks());
		ItemStack slab = new ItemStack(info.slab());
		for (Ingredient ingredient : ingredients) {
			if (ingredient.test(planks)) {
				specializedIngredients.add(Ingredient.of(info.planks()));
			} else if (ingredient.test(slab)) {
				specializedIngredients.add(Ingredient.of(info.slab()));
			} else {
				specializedIngredients.add(ingredient);
			}
		}
		return specializedIngredients;
	}

	private static ResourceLocation getRecipeId(ResourceLocation recipeId, WoodType woodType) {
		return recipeId.withPath(path -> "generic_wood_storage/" + woodType.name().toLowerCase(Locale.ROOT).replace(':', '/') + "/" + path);
	}
}
