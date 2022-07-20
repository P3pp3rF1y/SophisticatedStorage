package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShulkerBoxFromChestRecipesMaker {
	private ShulkerBoxFromChestRecipesMaker() {}

	public static List<CraftingRecipe> getRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();
		ShulkerBoxFromChestRecipe.REGISTERED_RECIPES.forEach(id -> ClientRecipeHelper.getRecipeByKey(id).ifPresent(r -> {
			if (!(r instanceof ShapedRecipe originalRecipe)) {
				return;
			}

			getChestItems(originalRecipe).forEach(chestItem -> {
				CraftingContainer craftinginventory = new CraftingContainer(new AbstractContainerMenu(null, -1) {
					public boolean stillValid(Player playerIn) {
						return false;
					}
				}, 3, 3);

				NonNullList<Ingredient> ingredientsCopy = NonNullList.create();
				int i = 0;
				for (Ingredient ingredient : r.getIngredients()) {
					ItemStack[] ingredientItems = ingredient.getItems();

					boolean isChestIngredient = false;
					for (ItemStack ingredientItem : ingredientItems) {
						if (ingredientItem.getItem() instanceof ChestBlockItem) {
							isChestIngredient = true;
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
				ItemStack result = originalRecipe.assemble(craftinginventory);
				//noinspection ConstantConditions
				ResourceLocation newId = new ResourceLocation(SophisticatedStorage.MOD_ID, "shulker_from_" + chestItem.getItem().getRegistryName().getPath() + result.getOrCreateTag().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:\s]", "_"));

				recipes.add(new ShapedRecipe(newId, "", originalRecipe.getRecipeWidth(), originalRecipe.getRecipeHeight(), ingredientsCopy, result));
			});
		}));

		return recipes;
	}

	private static List<ItemStack> getChestItems(ShapedRecipe recipe) {
		NonNullList<ItemStack> chestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem) {
					item.fillItemCategory(SophisticatedStorage.CREATIVE_TAB, chestItems);
				}
			}
		}

		return chestItems;
	}
}
