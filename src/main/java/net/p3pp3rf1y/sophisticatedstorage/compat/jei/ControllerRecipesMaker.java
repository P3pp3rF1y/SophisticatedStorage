package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import mezz.jei.library.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ControllerRecipesMaker {
	private ControllerRecipesMaker() {}

	public static List<RecipeHolder<CraftingRecipe>> getRecipes() {
		return ClientRecipeHelper.getRecipeByKey(ModBlocks.CONTROLLER_ITEM.getId()).map(r -> {
			if (!(r.value() instanceof ShapedRecipe originalRecipe)) {
				return Collections.<RecipeHolder<CraftingRecipe>>emptyList();
			}

			NonNullList<Ingredient> ingredients = originalRecipe.getIngredients();
			NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
			int i = 0;
			for (Ingredient ingredient : ingredients) {
				ingredientsCopy.add(i, getExpandedIngredient(ingredient));
				i++;
			}
			ShapedRecipePattern pattern = new ShapedRecipePattern(originalRecipe.getRecipeWidth(), originalRecipe.getRecipeHeight(), ingredientsCopy, Optional.empty());
			return Collections.singletonList(new RecipeHolder<CraftingRecipe>(new ResourceLocation(r.id().getNamespace(), r.id().getPath() + "_expanded_variants"), new ShapedRecipe("", CraftingBookCategory.MISC, pattern, RecipeUtil.getResultItem(originalRecipe))));
		}).orElse(Collections.emptyList());
	}

	private static Ingredient getExpandedIngredient(Ingredient ingredient) {
		ItemStack[] ingredientItems = ingredient.getItems();

		NonNullList<ItemStack> storages = NonNullList.create();
		for (ItemStack ingredientItem : ingredientItems) {
			Item item = ingredientItem.getItem();
			if (item instanceof BlockItemBase itemBase && (item == ModBlocks.BARREL_ITEM.get() || item == ModBlocks.CHEST_ITEM.get())) {
				itemBase.addCreativeTabItems(storages::add);
			}
		}
		if (!storages.isEmpty()) {
			return Ingredient.of(storages.toArray(new ItemStack[0]));
		} else {
			return ingredient;
		}
	}
}
