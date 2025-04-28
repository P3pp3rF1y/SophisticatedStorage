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
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<CraftingRecipe> getShapedCraftingRecipes(Function<ItemStack, Optional<T>> subtypeInterpreterGetter) {
		return getShapedCraftingRecipes(subtypeInterpreterGetter, r -> r);
	}

	public static <R, T extends PropertyBasedSubtypeInterpreter> List<R> getShapedCraftingRecipes(Function<ItemStack, Optional<T>> subtypeInterpreterGetter, Function<CraftingRecipe, R> transformRecipe) {
		RecipeConstructor<StorageTierUpgradeRecipe> constructRecipe = (originalRecipe, id, ingredients, result) -> {
			return new ShapedRecipe(id, "", CraftingBookCategory.MISC, originalRecipe.getWidth(), originalRecipe.getHeight(), originalRecipe.getIngredients(), result);
		};
		List<R> craftingRecipes = getCraftingRecipes(constructRecipe, StorageTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getStorageItems, subtypeInterpreterGetter, transformRecipe);
		RecipeConstructor<DoubleChestTierUpgradeRecipe> constructDoubleChestRecipe = (originalRecipe, id, ingredients, result) -> {
			return new ShapedRecipe(id, "", CraftingBookCategory.MISC, originalRecipe.getWidth(), originalRecipe.getHeight(), originalRecipe.getIngredients(), result);
		};
		craftingRecipes.addAll(getCraftingRecipes(constructDoubleChestRecipe, DoubleChestTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, transformRecipe));
		return craftingRecipes;
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<CraftingRecipe> getShapelessCraftingRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return getShapelessCraftingRecipes(getSubtypeInterpreter, r -> r);
	}

	public static <R, T extends PropertyBasedSubtypeInterpreter> List<R> getShapelessCraftingRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter, Function<CraftingRecipe, R> transformRecipe) {
		RecipeConstructor<StorageTierUpgradeShapelessRecipe> constructRecipe = (originalRecipe, id, ingredients, result) -> new ShapelessRecipe(id, "", CraftingBookCategory.MISC, result, ingredients);
		List<R> craftingRecipes = getCraftingRecipes(constructRecipe, StorageTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getStorageItems, getSubtypeInterpreter, transformRecipe);
		RecipeConstructor<DoubleChestTierUpgradeShapelessRecipe> constructDoubleChestRecipe = (originalRecipe, id, ingredients, result) -> new ShapelessRecipe(id, "", CraftingBookCategory.MISC, result, ingredients);
		craftingRecipes.addAll(getCraftingRecipes(constructDoubleChestRecipe, DoubleChestTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, getSubtypeInterpreter, transformRecipe));
		return craftingRecipes;
	}

	@NotNull
	private static <R, T extends CraftingRecipe, U extends PropertyBasedSubtypeInterpreter> List<R> getCraftingRecipes(RecipeConstructor<T> constructRecipe, Class<T> originalRecipeClass,
																													   Function<CraftingRecipe, List<ItemStack>> getStorageItems,
																													   Function<ItemStack, Optional<U>> getSubtypeInterpreter,
																													   Function<CraftingRecipe, R> transformRecipe) {
		return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(RecipeType.CRAFTING, originalRecipeClass, recipe -> {
			List<R> itemGroupRecipes = new ArrayList<>();
			getStorageItems.apply(recipe).forEach(storageItem -> {
				NonNullList<Ingredient> ingredients = recipe.getIngredients();
				CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
					@Override
					public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
						return ItemStack.EMPTY;
					}

					public boolean stillValid(Player playerIn) {
						return false;
					}
				}, 3, 3);
				NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
				int i = 0;
				for (Ingredient ingredient : ingredients) {
					ItemStack[] ingredientItems = ingredient.getItems();
					if (ingredientItems.length == 1 && storageItem.getItem() == ingredientItems[0].getItem()) {
						ingredientsCopy.add(i, Ingredient.of(storageItem));
						craftinginventory.setItem(i, storageItem.copy());
					} else {
						ingredientsCopy.add(i, ingredient);
						if (!ingredient.isEmpty()) {
							craftinginventory.setItem(i, ingredientItems[0]);
						}
					}
					i++;
				}
				ItemStack result = ClientRecipeHelper.assemble(recipe, craftinginventory);
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID,
						"tier_upgrade_"
								+ getSubtypeInterpreter.apply(storageItem).map(interpreter -> interpreter.getRegistrySanitizedItemString(storageItem)).orElse("")
								+ "_to_"
								+ getSubtypeInterpreter.apply(result).map(interpreter -> interpreter.getRegistrySanitizedItemString(result)).orElse("")
				);
				itemGroupRecipes.add(transformRecipe.apply(constructRecipe.construct(recipe, id, ingredientsCopy, result)));
			});
			return itemGroupRecipes;
		});
	}

	private static List<ItemStack> getDoubleChestItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> doubleChestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem chestBlockItem) {
					chestBlockItem.addCreativeTabItems(stack -> {
						ChestBlockItem.setDoubleChest(stack, true);
						doubleChestItems.add(stack);
					});
				}
			}
		}

		return doubleChestItems;
	}

	private static List<ItemStack> getStorageItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> storageItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof StorageBlockItem storageBlockItem) {
					storageBlockItem.addCreativeTabItems(storageItems::add);
				}
			}
		}

		return storageItems;
	}

	private interface RecipeConstructor<T extends Recipe<?>> {
		CraftingRecipe construct(T originalRecipe, ResourceLocation id, NonNullList<Ingredient> ingredients, ItemStack result);
	}
}
