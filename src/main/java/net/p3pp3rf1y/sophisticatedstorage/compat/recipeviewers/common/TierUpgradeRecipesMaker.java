package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.*;
import java.util.function.Function;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedShapedCraftingRecipes(
			Function<ItemStack, Optional<T>> subtypeInterpreterGetter) {
		List<TierUpgradeDisplayRecipe> recipes = getGroupedCraftingRecipes(StorageTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getStorageItems,
				subtypeInterpreterGetter, false);
		recipes.addAll(
				getGroupedCraftingRecipes(DoubleChestTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems, subtypeInterpreterGetter, false));
		return recipes;
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedShapelessCraftingRecipes(
			Function<ItemStack, Optional<T>> subtypeInterpreterGetter) {
		List<TierUpgradeDisplayRecipe> recipes = getGroupedCraftingRecipes(StorageTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getStorageItems,
				subtypeInterpreterGetter, true);
		recipes.addAll(getGroupedCraftingRecipes(DoubleChestTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems,
				subtypeInterpreterGetter, true));
		return recipes;
	}

	private static <T extends CraftingRecipe, U extends PropertyBasedSubtypeInterpreter> List<TierUpgradeDisplayRecipe> getGroupedCraftingRecipes(
			Class<T> originalRecipeClass, Function<CraftingRecipe, List<ItemStack>> getStorageItems, Function<ItemStack, Optional<U>> getSubtypeInterpreter,
			boolean shapeless) {
		return ClientRecipeHelper.transformAllRecipeHoldersOfTypeIntoMultiple(RecipeType.CRAFTING, originalRecipeClass, recipeHolder -> {
			TierUpgradeDisplayRecipe displayRecipe = createDisplayRecipe(recipeHolder, getStorageItems, getSubtypeInterpreter, shapeless);
			return List.of(displayRecipe);
		});
	}

	private static <T extends CraftingRecipe, U extends PropertyBasedSubtypeInterpreter> TierUpgradeDisplayRecipe createDisplayRecipe(
			RecipeHolder<T> recipeHolder, Function<CraftingRecipe, List<ItemStack>> getStorageItems, Function<ItemStack, Optional<U>> getSubtypeInterpreter,
			boolean shapeless) {
		T recipe = recipeHolder.value();
		CraftingContainer craftingInventory = createCraftingInventory();
		List<Optional<Ingredient>> recipeIngredients = new ArrayList<>(RecipeHelper.getIngredients(recipe));
		int storageIngredientIndex = findStorageIngredientIndex(recipeIngredients);
		NonNullList<Ingredient> ingredientsCopy = copyIngredients(recipeIngredients);
		Map<String, TierUpgradeVariantPair> variantPairs = new LinkedHashMap<>();
		for (ItemStack storageItem : getStorageItems.apply(recipe)) {
			populateCraftingInventory(recipeIngredients, craftingInventory, storageIngredientIndex, storageItem);
			ItemStack result = ClientRecipeHelper.assemble(recipe, craftingInventory.asCraftInput());
			TierUpgradeVariantPair pair = new TierUpgradeVariantPair(storageItem.copy(), result.copy());
			variantPairs.putIfAbsent(getPairKey(pair, getSubtypeInterpreter), pair);
		}
		Identifier id = recipeHolder.id().identifier().withPath(path -> "tier_upgrade_grouped/" + path);
		int width = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : 0;
		int height = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : 0;
		List<Optional<Ingredient>> shapedIngredients = ingredientsCopy.stream().map(Optional::of).toList();
		RecipeHolder<CraftingRecipe> displayRecipeHolder = new RecipeHolder<>(recipeHolder.id(),
				shapeless
						? new ShapelessRecipe("", CraftingBookCategory.MISC, ClientRecipeHelper.getResultItem(recipe), ingredientsCopy)
						: new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(width, height, shapedIngredients, Optional.empty()),
								ClientRecipeHelper.getResultItem(recipe)));
		return new TierUpgradeDisplayRecipe(id, displayRecipeHolder, shapeless, width, height, ingredientsCopy, storageIngredientIndex,
				List.copyOf(variantPairs.values()));
	}

	private static NonNullList<Ingredient> getIngredients(CraftingRecipe recipe) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		RecipeHelper.getIngredients(recipe).forEach(ingredient -> ingredients.add(ingredient.orElseThrow()));
		return ingredients;
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

	private static NonNullList<Ingredient> copyIngredients(Collection<Optional<Ingredient>> ingredients) {
		NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
		ingredients.forEach(ingredient -> ingredientsCopy.add(ingredient.orElseGet(ClientRecipeHelper::emptyDisplayIngredient)));
		return ingredientsCopy;
	}

	private static int findStorageIngredientIndex(List<Optional<Ingredient>> ingredients) {
		for (int i = 0; i < ingredients.size(); i++) {
			for (ItemStack ingredientItem : getIngredientItems(ingredients.get(i))) {
				if (ingredientItem.getItem() instanceof StorageBlockItem) {
					return i;
				}
			}
		}
		throw new IllegalStateException("Tier upgrade recipe missing storage ingredient");
	}

	private static void populateCraftingInventory(List<Optional<Ingredient>> ingredients, CraftingContainer craftingInventory, int storageIngredientIndex,
			ItemStack storageItem) {
		for (int i = 0; i < ingredients.size(); i++) {
			if (i == storageIngredientIndex) {
				craftingInventory.setItem(i, storageItem.copy());
				continue;
			}
			List<ItemStack> ingredientItems = getIngredientItems(ingredients.get(i));
			craftingInventory.setItem(i, ingredientItems.isEmpty() ? ItemStack.EMPTY : ingredientItems.getFirst());
		}
	}

	private static <U extends PropertyBasedSubtypeInterpreter> String getPairKey(TierUpgradeVariantPair pair,
			Function<ItemStack, Optional<U>> getSubtypeInterpreter) {
		return getSubtypeInterpreter.apply(pair.source()).map(interpreter -> interpreter.getRegistrySanitizedItemString(pair.source()))
				.orElse(pair.source().toString()) + "->"
				+ getSubtypeInterpreter.apply(pair.result()).map(interpreter -> interpreter.getRegistrySanitizedItemString(pair.result()))
						.orElse(pair.result().toString());
	}

	private static List<ItemStack> getDoubleChestItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> doubleChestItems = NonNullList.create();
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			for (ItemStack ingredientItem : getIngredientItems(ingredient)) {
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
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			for (ItemStack ingredientItem : getIngredientItems(ingredient)) {
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
			variants.addAll(DyeRecipesMaker.getWoodStorageStackList((StorageBlockBase) storageBlockItem.getBlock()));
		}
		for (DyeColor color : DyeColor.values()) {
			ItemStack storageStack = new ItemStack(storageBlockItem);
			storageBlockItem.setMainColor(storageStack, color.getTextureDiffuseColor());
			storageBlockItem.setAccentColor(storageStack, color.getTextureDiffuseColor());
			variants.add(storageStack);
		}
		ItemStack storageStack = new ItemStack(storageBlockItem);
		storageBlockItem.setMainColor(storageStack, DyeColor.YELLOW.getTextureDiffuseColor());
		storageBlockItem.setAccentColor(storageStack, DyeColor.LIME.getTextureDiffuseColor());
		variants.add(storageStack);
		return variants;
	}

	private static List<ItemStack> getIngredientItems(Optional<Ingredient> ingredient) {
		return ingredient.map(value -> value.items().map(ItemStack::new).toList()).orElse(List.of());
	}

}
