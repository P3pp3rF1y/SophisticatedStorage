package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.SmithingStorageUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {}

	public static List<UpgradeRecipe> getSmithingRecipes() {
		List<UpgradeRecipe> recipes = new ArrayList<>();
		addItemSmithingUpgradeRecipes(recipes, ModBlocks.DIAMOND_BARREL_ITEM.get(), "netherite_barrel");
		addItemSmithingUpgradeRecipes(recipes, ModBlocks.DIAMOND_CHEST_ITEM.get(), "netherite_chest");

		return recipes;
	}

	public static List<CraftingRecipe> getCraftingRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();

		addItemUpgradeRecipes(recipes, ModBlocks.BARREL_ITEM.get(), "iron_barrel");
		addItemUpgradeRecipes(recipes, ModBlocks.IRON_BARREL_ITEM.get(), "gold_barrel");
		addItemUpgradeRecipes(recipes, ModBlocks.GOLD_BARREL_ITEM.get(), "diamond_barrel");
		addItemUpgradeRecipes(recipes, ModBlocks.CHEST_ITEM.get(), "iron_chest");
		addItemUpgradeRecipes(recipes, ModBlocks.IRON_CHEST_ITEM.get(), "gold_chest");
		addItemUpgradeRecipes(recipes, ModBlocks.GOLD_CHEST_ITEM.get(), "diamond_chest");

		return recipes;
	}

	private static void addItemUpgradeRecipes(List<CraftingRecipe> recipes, Item storageItem, String recipeName) {
		//noinspection ConstantConditions
		RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

		NonNullList<ItemStack> items = NonNullList.create();
		//noinspection ConstantConditions
		storageItem.fillItemCategory(storageItem.getItemCategory(), items);

		recipeManager.byKey(SophisticatedStorage.getRL(recipeName)).ifPresent((Consumer<Recipe<?>>) r -> {
			if (!(r instanceof StorageTierUpgradeRecipe recipe)) {
				return;
			}
			NonNullList<Ingredient> ingredients = recipe.getIngredients();

			items.forEach(item -> {
				CraftingContainer craftinginventory = new CraftingContainer(new AbstractContainerMenu(null, -1) {
					public boolean stillValid(Player playerIn) {
						return false;
					}
				}, 3, 3);
				NonNullList<Ingredient> ingredientsCopy = NonNullList.create();
				int i = 0;
				for (Ingredient ingredient : ingredients) {
					ItemStack[] ingredientItems = ingredient.getItems();
					if (ingredientItems.length == 1) {
						if (storageItem.getClass().isInstance(ingredientItems[0].getItem())) {
							ingredientsCopy.add(i, Ingredient.of(item));
							craftinginventory.setItem(i, item.copy());
						} else {
							ingredientsCopy.add(i, ingredient);
							craftinginventory.setItem(i, ingredientItems[0]);
						}
					}
					i++;
				}
				ItemStack result = recipe.assemble(craftinginventory);
				//noinspection ConstantConditions
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "tier_upgrade_" + item.getItem().getRegistryName().getPath() + result.getOrCreateTag().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:\s]", "_"));

				recipes.add(new ShapedRecipe(id, "", recipe.getRecipeWidth(), recipe.getRecipeHeight(), ingredientsCopy, result));
			});
		});
	}

	private static void addItemSmithingUpgradeRecipes(List<UpgradeRecipe> recipes, Item storageItem, String recipeName) {
		//noinspection ConstantConditions
		RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

		NonNullList<ItemStack> items = NonNullList.create();
		//noinspection ConstantConditions
		storageItem.fillItemCategory(storageItem.getItemCategory(), items);

		recipeManager.byKey(SophisticatedStorage.getRL(recipeName)).ifPresent((Consumer<Recipe<?>>) r -> {
			if (!(r instanceof SmithingStorageUpgradeRecipe recipe)) {
				return;
			}

			items.forEach(item -> {
				SimpleContainer container = new SimpleContainer(2);
				container.setItem(0, item);
				ItemStack[] additionItems = recipe.addition.getItems();
				container.setItem(1, additionItems[0]);

				ItemStack result = recipe.assemble(container);
				//noinspection ConstantConditions
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "tier_upgrade_" + item.getItem().getRegistryName().getPath() + result.getOrCreateTag().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:\s]", "_"));

				recipes.add(new UpgradeRecipe(id, Ingredient.of(item), recipe.addition, result));
			});
		});
	}
}
