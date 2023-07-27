package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.SmithingStorageUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {}

	public static List<CraftingRecipe> getCraftingRecipes() {
		return ClientRecipeHelper.getAndTransformAvailableItemGroupRecipes(StorageTierUpgradeRecipe.REGISTERED_RECIPES, StorageTierUpgradeRecipe.class, recipe -> {
			List<CraftingRecipe> itemGroupRecipes = new ArrayList<>();
			getStorageItems(recipe).forEach(storageItem -> {
				NonNullList<Ingredient> ingredients = recipe.getIngredients();
				CraftingContainer craftinginventory = new CraftingContainer(new AbstractContainerMenu(null, -1) {
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
						craftinginventory.setItem(i, ingredientItems[0]);
					}
					i++;
				}
				ItemStack result = recipe.assemble(craftinginventory);
				//noinspection ConstantConditions
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "tier_upgrade_" + ForgeRegistries.ITEMS.getKey(storageItem.getItem()).getPath() + result.getOrCreateTag().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:\s]", "_"));

				itemGroupRecipes.add(new ShapedRecipe(id, "", recipe.getRecipeWidth(), recipe.getRecipeHeight(), ingredientsCopy, result));
			});
			return itemGroupRecipes;
		});
	}

	private static List<ItemStack> getStorageItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> storageItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof StorageBlockItem) {
					item.fillItemCategory(SophisticatedStorage.CREATIVE_TAB, storageItems);
				}
			}
		}

		return storageItems;
	}

	public static List<UpgradeRecipe> getSmithingRecipes() {
		return ClientRecipeHelper.getAndTransformAvailableItemGroupRecipes(SmithingStorageUpgradeRecipe.REGISTERED_RECIPES, SmithingStorageUpgradeRecipe.class, recipe -> {
			List<UpgradeRecipe> itemGroupRecipes = new ArrayList<>();
			getStorageItems(recipe).forEach(storageItem -> {
				SimpleContainer container = new SimpleContainer(2);
				container.setItem(0, storageItem);
				ItemStack[] additionItems = recipe.addition.getItems();
				container.setItem(1, additionItems[0]);

				ItemStack result = recipe.assemble(container);
				//noinspection ConstantConditions
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "tier_upgrade_" + ForgeRegistries.ITEMS.getKey(storageItem.getItem()).getPath() + result.getOrCreateTag().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:\s]", "_"));

				itemGroupRecipes.add(new UpgradeRecipe(id, Ingredient.of(storageItem), recipe.addition, result));
			});
			return itemGroupRecipes;
		});
	}

	private static List<ItemStack> getStorageItems(UpgradeRecipe recipe) {
		NonNullList<ItemStack> storageItems = NonNullList.create();

		for (ItemStack ingredientItem : recipe.base.getItems()) {
			Item item = ingredientItem.getItem();
			if (item instanceof StorageBlockItem) {
				item.fillItemCategory(SophisticatedStorage.CREATIVE_TAB, storageItems);
			}
		}

		return storageItems;
	}
}
