package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
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
		List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
		GenericWoodStorageHelper.getGenericWoodTypes()
				.forEach(woodType -> GenericWoodStorageHelper.getGenericWoodInfo(woodType).ifPresent(info -> addRecipes(recipes, woodType, info)));
		return recipes;
	}

	private static void addRecipes(List<RecipeHolder<CraftingRecipe>> recipes, WoodType woodType, GenericWoodStorageHelper.GenericWoodInfo info) {
		recipes.add(createRecipe("chest", woodType, ModBlocks.CHEST_ITEM.get(), info.planks(), null, "PPP", "PLP", "PPP"));
		recipes.add(createRecipe("barrel", woodType, ModBlocks.BARREL_ITEM.get(), info.planks(), info.slab(), "PSP", "PLP", "PSP"));
		recipes.add(createRecipe("limited_barrel_1", woodType, ModBlocks.LIMITED_BARREL_1_ITEM.get(), info.planks(), info.slab(), "PSP", "PLP", "PPP"));
		recipes.add(createRecipe("limited_barrel_2", woodType, ModBlocks.LIMITED_BARREL_2_ITEM.get(), info.planks(), info.slab(), "PPP", "SLS", "PPP"));
		recipes.add(createRecipe("limited_barrel_3", woodType, ModBlocks.LIMITED_BARREL_3_ITEM.get(), info.planks(), info.slab(), "PSP", "PLP", "SPS"));
		recipes.add(createRecipe("limited_barrel_4", woodType, ModBlocks.LIMITED_BARREL_4_ITEM.get(), info.planks(), info.slab(), "SPS", "PLP", "SPS"));
	}

	private static RecipeHolder<CraftingRecipe> createRecipe(String storageName, WoodType woodType, Item item, Block planks, Block slab, String... pattern) {
		List<Optional<Ingredient>> ingredients = new ArrayList<>();
		for (String row : pattern) {
			for (int i = 0; i < row.length(); i++) {
				ingredients.add(getIngredient(row.charAt(i), planks, slab));
			}
		}

		ItemStack result = WoodStorageBlockItem.setWoodType(new ItemStack(item), woodType);
		ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients, Optional.empty()), result);
		return new RecipeHolder<>(ClientRecipeHelper.recipeKey(getRecipeId(storageName, woodType)), recipe);
	}

	private static Optional<Ingredient> getIngredient(char key, Block planks, Block slab) {
		return switch (key) {
			case 'P' -> Optional.of(Ingredient.of(planks));
			case 'S' -> Optional.of(Ingredient.of(slab));
			case 'L' -> Optional.of(Ingredient.of(Items.LEVER));
			default -> Optional.empty();
		};
	}

	private static Identifier getRecipeId(String storageName, WoodType woodType) {
		return SophisticatedStorage.getIdentifier("generic_wood_storage/" + woodType.name().toLowerCase(Locale.ROOT).replace(':', '/') + "/" + storageName);
	}
}
