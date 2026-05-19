package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedShapedCraftingRecipes(Function<ItemStack, Optional<T>> subtypeInterpreterGetter) {
		List<TierUpgradeDisplayRecipe> recipes = getGroupedCraftingRecipes(StorageTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getStorageItems, subtypeInterpreterGetter, false);
		recipes.addAll(getGroupedCraftingRecipes(DoubleChestTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, false));
		return recipes;
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedShapelessCraftingRecipes(Function<ItemStack, Optional<T>> subtypeInterpreterGetter) {
		List<TierUpgradeDisplayRecipe> recipes = getGroupedCraftingRecipes(StorageTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getStorageItems, subtypeInterpreterGetter, true);
		recipes.addAll(getGroupedCraftingRecipes(DoubleChestTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, true));
		return recipes;
	}

	private static <T extends CraftingRecipe, U extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedCraftingRecipes(Class<T> originalRecipeClass,
															 Function<CraftingRecipe, List<ItemStack>> getStorageItems,
															 Function<ItemStack, Optional<U>> getSubtypeInterpreter,
															 boolean shapeless) {
		return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(RecipeType.CRAFTING, originalRecipeClass, recipe -> {
			TierUpgradeDisplayRecipe displayRecipe = createDisplayRecipe(recipe, getStorageItems, getSubtypeInterpreter, shapeless);
			return List.of(displayRecipe);
		});
	}

	private static <T extends CraftingRecipe, U extends PropertyBasedSubtypeInterpreter> TierUpgradeDisplayRecipe createDisplayRecipe(T recipe,
			Function<CraftingRecipe, List<ItemStack>> getStorageItems,
			Function<ItemStack, Optional<U>> getSubtypeInterpreter,
			boolean shapeless) {
		CraftingContainer craftingInventory = createCraftingInventory();
		int storageIngredientIndex = findStorageIngredientIndex(recipe.getIngredients());
		NonNullList<Ingredient> ingredientsCopy = copyIngredients(recipe.getIngredients());
		Map<String, TierUpgradeVariantPair> variantPairs = new LinkedHashMap<>();
		for (ItemStack storageItem : getStorageItems.apply(recipe)) {
			populateCraftingInventory(recipe.getIngredients(), craftingInventory, storageIngredientIndex, storageItem);
			ItemStack result = ClientRecipeHelper.assemble(recipe, craftingInventory);
			TierUpgradeVariantPair pair = new TierUpgradeVariantPair(storageItem.copy(), result.copy());
			variantPairs.putIfAbsent(getPairKey(pair, getSubtypeInterpreter), pair);
		}
		ResourceLocation id = recipe.getId().withPath(path -> "tier_upgrade_grouped/" + path);
		int width = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : 0;
		int height = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : 0;
		CraftingRecipe displayRecipe = shapeless ? new ShapelessRecipe(recipe.getId(), "", CraftingBookCategory.MISC, ClientRecipeHelper.getResultItem(recipe), ingredientsCopy)
				: new ShapedRecipe(recipe.getId(), "", CraftingBookCategory.MISC, width, height, ingredientsCopy, ClientRecipeHelper.getResultItem(recipe));
		return new TierUpgradeDisplayRecipe(id, displayRecipe, shapeless, width, height, ingredientsCopy, storageIngredientIndex, List.copyOf(variantPairs.values()));
	}

	private static CraftingContainer createCraftingInventory() {
		return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, 3, 3);
	}

	private static NonNullList<Ingredient> copyIngredients(NonNullList<Ingredient> ingredients) {
		NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
		ingredientsCopy.addAll(ingredients);
		return ingredientsCopy;
	}

	private static int findStorageIngredientIndex(NonNullList<Ingredient> ingredients) {
		for (int i = 0; i < ingredients.size(); i++) {
			for (ItemStack ingredientItem : ingredients.get(i).getItems()) {
				if (ingredientItem.getItem() instanceof StorageBlockItem) {
					return i;
				}
			}
		}
		throw new IllegalStateException("Tier upgrade recipe missing storage ingredient");
	}

	private static void populateCraftingInventory(NonNullList<Ingredient> ingredients, CraftingContainer craftingInventory, int storageIngredientIndex, ItemStack storageItem) {
		for (int i = 0; i < ingredients.size(); i++) {
			if (i == storageIngredientIndex) {
				craftingInventory.setItem(i, storageItem.copy());
				continue;
			}
			Ingredient ingredient = ingredients.get(i);
			ItemStack[] ingredientItems = ingredient.getItems();
			craftingInventory.setItem(i, ingredient.isEmpty() ? ItemStack.EMPTY : ingredientItems[0]);
		}
	}

	private static <U extends PropertyBasedSubtypeInterpreter> String getPairKey(TierUpgradeVariantPair pair, Function<ItemStack, Optional<U>> getSubtypeInterpreter) {
		return getSubtypeInterpreter.apply(pair.source()).map(interpreter -> interpreter.getRegistrySanitizedItemString(pair.source())).orElse(pair.source().toString())
				+ "->"
				+ getSubtypeInterpreter.apply(pair.result()).map(interpreter -> interpreter.getRegistrySanitizedItemString(pair.result())).orElse(pair.result().toString());
	}

	private static List<ItemStack> getDoubleChestItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> doubleChestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem chestBlockItem) {
					addRecipeViewerVariants(chestBlockItem).forEach(stack -> {
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
					storageItems.addAll(addRecipeViewerVariants(storageBlockItem));
				}
			}
		}

		return storageItems;
	}

	private static List<ItemStack> addRecipeViewerVariants(StorageBlockItem storageBlockItem) {
		List<ItemStack> variants = new ArrayList<>();
		variants.add(new ItemStack(storageBlockItem));
		if (storageBlockItem instanceof WoodStorageBlockItem) {
			List<ItemStack> woodStorageStacks = DyeRecipesMaker.getWoodStorageStackList((StorageBlockBase) storageBlockItem.getBlock());
			variants.addAll(woodStorageStacks);
			if (storageBlockItem instanceof BarrelBlockItem) {
				ItemStack acaciaStack = WoodStorageBlockItem.setWoodType(new ItemStack(storageBlockItem), WoodType.ACACIA);
				variants.add(acaciaStack);
				ItemStack flatAcaciaStack = acaciaStack.copy();
				BarrelBlockItem.setFlatTop(flatAcaciaStack, true);
				variants.add(flatAcaciaStack);
				woodStorageStacks.forEach(stack -> {
					ItemStack flatTopStack = stack.copy();
					BarrelBlockItem.setFlatTop(flatTopStack, true);
					variants.add(flatTopStack);
				});
			}
		}
		for (DyeColor color : DyeColor.values()) {
			ItemStack storageStack = new ItemStack(storageBlockItem);
			int colorValue = ColorHelper.getColor(color.getTextureDiffuseColors());
			storageBlockItem.setMainColor(storageStack, colorValue);
			storageBlockItem.setAccentColor(storageStack, colorValue);
			variants.add(storageStack);
		}
		ItemStack storageStack = new ItemStack(storageBlockItem);
		storageBlockItem.setMainColor(storageStack, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
		storageBlockItem.setAccentColor(storageStack, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
		variants.add(storageStack);
		return variants;
	}

}
