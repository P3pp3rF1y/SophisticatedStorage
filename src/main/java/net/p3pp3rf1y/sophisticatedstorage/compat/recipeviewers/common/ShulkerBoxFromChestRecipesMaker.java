package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ShulkerBoxFromChestRecipesMaker {
	private ShulkerBoxFromChestRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<RecipeHolder<CraftingRecipe>> getShapedRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(RecipeType.CRAFTING, ShulkerBoxFromChestRecipe.class, originalRecipe -> {
			List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
			getChestItems(originalRecipe).forEach(chestItem -> {
				CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
					@Override
					public ItemStack quickMoveStack(Player player, int index) {
						return ItemStack.EMPTY;
					}

					public boolean stillValid(Player playerIn) {
						return false;
					}
				}, 3, 3);

				NonNullList<Ingredient> ingredients = originalRecipe.getIngredients();
				NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
				int i = 0;
				for (Ingredient ingredient : ingredients) {
					ItemStack[] ingredientItems = ingredient.getItems();

					boolean isChestIngredient = false;
					for (ItemStack ingredientItem : ingredientItems) {
						if (ingredientItem.getItem() instanceof ChestBlockItem) {
							isChestIngredient = true;
							break;
						}
					}
					if (isChestIngredient) {
						ingredientsCopy.add(i, Ingredient.of(chestItem));
						craftinginventory.setItem(i, chestItem.copy());
					} else {
						ingredientsCopy.add(i, ingredient);
						craftinginventory.setItem(i, ingredientItems[0]);
					}
					i++;
				}
				ItemStack result = ClientRecipeHelper.assemble(originalRecipe, craftinginventory.asCraftInput());
				ResourceLocation newId = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "shulker_from_"
						+ getSubtypeInterpreter.apply(chestItem).map(interpreter -> interpreter.getRegistrySanitizedItemString(chestItem)).orElse(""));
				ShapedRecipePattern pattern = new ShapedRecipePattern(originalRecipe.getWidth(), originalRecipe.getHeight(), ingredientsCopy, Optional.empty());

				recipes.add(new RecipeHolder<>(newId, new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result)));
			});
			return recipes;
		});
	}

	private static List<ItemStack> getChestItems(ShapedRecipe recipe) {
		NonNullList<ItemStack> chestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem chestBlockItem) {
					chestBlockItem.addCreativeTabItems(chestItems::add);
				}
			}
		}

		return chestItems;
	}
}
