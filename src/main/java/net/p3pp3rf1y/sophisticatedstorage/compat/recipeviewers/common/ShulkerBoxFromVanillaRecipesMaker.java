package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IFocusBehavior;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromVanillaShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.List;
import java.util.Set;

public class ShulkerBoxFromVanillaRecipesMaker {
	private ShulkerBoxFromVanillaRecipesMaker() {
	}

	public static List<CraftingDisplaySpec> getShapelessRecipeSpecs() {
		return ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, ShulkerBoxFromVanillaShapelessRecipe.class,
				ShulkerBoxFromVanillaRecipesMaker::toSpec);
	}

	private static CraftingDisplaySpec toSpec(ShulkerBoxFromVanillaShapelessRecipe recipe) {
		NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(recipe.getIngredients().size());
		ingredients.addAll(recipe.getIngredients());
		ResourceLocation id = recipe.getId().withPath(path -> "shulker_from_vanilla/" + path);
		ItemStack result = ClientRecipeHelper.getResultItem(recipe);
		CraftingDisplayVariant variant = new CraftingDisplayVariant(List.of(), List.of(result));
		List<CraftingDisplayVariant> globalVariants = StorageBlockItem.getMainColorFromStack(result).isEmpty()
				&& StorageBlockItem.getAccentColorFromStack(result).isEmpty() ? List.of(variant) : List.of();
		return new CraftingDisplaySpec(id, true, 0, 0, ingredients, List.of(variant), globalVariants, Set.of(recipe.getId()),
				new ShulkerBoxFromVanillaFocusBehavior());
	}

	private static class ShulkerBoxFromVanillaFocusBehavior implements IFocusBehavior<CraftingDisplayVariant> {
		@Override
		public List<CraftingDisplayVariant> allDisplays(List<CraftingDisplayVariant> variants) {
			return variants;
		}

		@Override
		public List<CraftingDisplayVariant> recipesFor(List<CraftingDisplayVariant> variants, ItemStack focusedOutput) {
			// JEI already supplies the original special recipe; returning a synthetic display would duplicate it.
			return List.of();
		}

		@Override
		public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
			return List.of();
		}
	}
}
