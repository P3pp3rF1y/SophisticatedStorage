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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ShulkerBoxFromChestRecipesMaker {
	private ShulkerBoxFromChestRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<CraftingDisplaySpec> getShapedRecipeSpecs(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return ClientRecipeHelper.transformAllRecipeHoldersOfTypeIntoMultiple(RecipeType.CRAFTING, ShulkerBoxFromChestRecipe.class, recipeHolder -> {
			ShulkerBoxFromChestRecipeDisplayRecipe displayRecipe = createDisplayRecipe(recipeHolder);
			return List.of(displayRecipe.toSpec());
		});
	}

	private static ShulkerBoxFromChestRecipeDisplayRecipe createDisplayRecipe(RecipeHolder<ShulkerBoxFromChestRecipe> recipeHolder) {
		ShulkerBoxFromChestRecipe recipe = recipeHolder.value();
		List<Optional<Ingredient>> recipeIngredients = new ArrayList<>(RecipeHelper.getIngredients(recipe));
		int chestIngredientIndex = findChestIngredientIndex(recipeIngredients);
		NonNullList<Ingredient> ingredientsCopy = copyIngredients(recipeIngredients);
		List<CraftingDisplayVariant> variants = new ArrayList<>();
		List<CraftingDisplayVariant> globalVariants = new ArrayList<>();
		for (ItemStack chestItem : getChestItems(recipe)) {
			CraftingDisplayVariant variant = createVariant(recipe, chestIngredientIndex, chestItem);
			variants.add(variant);
			if (!isTinted(chestItem)) {
				globalVariants.add(variant);
			}
		}

		Identifier id = recipeHolder.id().identifier().withPath(path -> "shulker_from_chest_grouped/" + path);
		RecipeHolder<CraftingRecipe> displayRecipeHolder = new RecipeHolder<>(recipeHolder.id(),
				new ShapedRecipe("", recipe.category(), new ShapedRecipePattern(recipe.getWidth(), recipe.getHeight(), recipeIngredients, Optional.empty()), ClientRecipeHelper.getResultItem(recipe)));
		return new ShulkerBoxFromChestRecipeDisplayRecipe(id, displayRecipeHolder, recipe.getWidth(), recipe.getHeight(), ingredientsCopy, chestIngredientIndex, variants, globalVariants);
	}

	private static CraftingDisplayVariant createVariant(ShulkerBoxFromChestRecipe recipe, int chestIngredientIndex, ItemStack chestItem) {
		CraftingContainer craftingInventory = createCraftingInventory();
		List<Optional<Ingredient>> ingredients = new ArrayList<>(RecipeHelper.getIngredients(recipe));
		List<ItemStack> inputs = new ArrayList<>(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++) {
			if (i == chestIngredientIndex) {
				craftingInventory.setItem(i, chestItem.copy());
				inputs.add(chestItem.copy());
				continue;
			}
			List<ItemStack> ingredientItems = getIngredientItems(ingredients.get(i));
			craftingInventory.setItem(i, ingredientItems.isEmpty() ? ItemStack.EMPTY : ingredientItems.getFirst());
			inputs.add(ItemStack.EMPTY);
		}
		return new CraftingDisplayVariant(inputs, List.of(ClientRecipeHelper.assemble(recipe, craftingInventory.asCraftInput())));
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

	private static NonNullList<Ingredient> copyIngredients(List<Optional<Ingredient>> ingredients) {
		NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
		ingredients.forEach(ingredient -> ingredientsCopy.add(ingredient.orElseGet(ClientRecipeHelper::emptyDisplayIngredient)));
		return ingredientsCopy;
	}

	private static int findChestIngredientIndex(List<Optional<Ingredient>> ingredients) {
		for (int i = 0; i < ingredients.size(); i++) {
			for (ItemStack ingredientItem : getIngredientItems(ingredients.get(i))) {
				if (ingredientItem.getItem() instanceof ChestBlockItem) {
					return i;
				}
			}
		}
		throw new IllegalStateException("Shulker box from chest recipe missing chest ingredient");
	}

	private static List<ItemStack> getChestItems(ShapedRecipe recipe) {
		NonNullList<ItemStack> chestItems = NonNullList.create();
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			for (ItemStack ingredientItem : getIngredientItems(ingredient)) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem chestBlockItem) {
					chestItems.add(new ItemStack(chestBlockItem));
					chestItems.addAll(DyeRecipesMaker.getWoodStorageStackList((StorageBlockBase) chestBlockItem.getBlock()));
					addTintedChestItems(chestItems, chestBlockItem);
				}
			}
		}

		return chestItems;
	}

	private static List<ItemStack> getIngredientItems(Optional<Ingredient> ingredient) {
		return ingredient.map(value -> value.items().map(ItemStack::new).toList()).orElse(List.of());
	}

	private static void addTintedChestItems(List<ItemStack> chestItems, ChestBlockItem chestBlockItem) {
		for (DyeColor color : DyeColor.values()) {
			ItemStack chestStack = new ItemStack(chestBlockItem);
			chestBlockItem.setMainColor(chestStack, color.getTextureDiffuseColor());
			chestBlockItem.setAccentColor(chestStack, color.getTextureDiffuseColor());
			chestItems.add(chestStack);
		}
		ItemStack chestStack = new ItemStack(chestBlockItem);
		chestBlockItem.setMainColor(chestStack, DyeColor.YELLOW.getTextureDiffuseColor());
		chestBlockItem.setAccentColor(chestStack, DyeColor.LIME.getTextureDiffuseColor());
		chestItems.add(chestStack);
	}

	private static boolean isTinted(ItemStack stack) {
		return StorageBlockItem.getMainColorFromComponentHolder(stack).isPresent() || StorageBlockItem.getAccentColorFromComponentHolder(stack).isPresent();
	}

	private record ShulkerBoxFromChestRecipeDisplayRecipe(Identifier id, RecipeHolder<CraftingRecipe> recipeHolder, int width, int height, NonNullList<Ingredient> ingredients,
			int chestIngredientIndex, List<CraftingDisplayVariant> variants, List<CraftingDisplayVariant> globalVariants) {
		private CraftingDisplaySpec toSpec() {
			return new CraftingDisplaySpec(id, false, width, height, ingredients, variants, globalVariants, Set.of(recipeHolder.id().identifier()), new ShulkerBoxFromChestFocusBehavior(chestIngredientIndex));
		}
	}

	private static class ShulkerBoxFromChestFocusBehavior extends SourceResultFocusBehavior implements IGroupedOutputFocusBehavior {
		private final int chestIngredientIndex;

		private ShulkerBoxFromChestFocusBehavior(int chestIngredientIndex) {
			super(chestIngredientIndex, (variant, focusedInput) -> Optional.empty(), (variant, focusedOutput) -> Optional.empty());
			this.chestIngredientIndex = chestIngredientIndex;
		}

		@Override
		public List<CraftingDisplayVariant> allDisplays(List<CraftingDisplayVariant> variants) {
			return variants;
		}

		@Override
		public List<CraftingDisplayVariant> recipesFor(List<CraftingDisplayVariant> variants, ItemStack focusedOutput) {
			if (isTinted(focusedOutput)) {
				return variants.stream().filter(variant -> ItemStack.isSameItemSameComponents(variant.firstOutput(), focusedOutput)).toList();
			}
			return variants.stream().filter(variant -> ItemStack.isSameItem(variant.firstOutput(), focusedOutput) && !isTinted(variant.firstOutput())).toList();
		}

		@Override
		public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
			if (focusedInput.getItem() instanceof ChestBlockItem) {
				return variants.stream().filter(variant -> chestIngredientIndex < variant.inputs().size() && ItemStack.isSameItemSameComponents(variant.inputs().get(chestIngredientIndex), focusedInput)).toList();
			}
			return focusedInput.is(Items.SHULKER_SHELL) ? variants : List.of();
		}
	}
}
