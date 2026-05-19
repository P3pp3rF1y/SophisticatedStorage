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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IGroupedOutputFocusBehavior;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ShulkerBoxFromChestRecipesMaker {
	private ShulkerBoxFromChestRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<CraftingDisplaySpec> getShapedRecipeSpecs(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(RecipeType.CRAFTING, ShulkerBoxFromChestRecipe.class, recipe -> List.of(createDisplayRecipe(recipe).toSpec()));
	}

	private static ShulkerBoxFromChestRecipeDisplayRecipe createDisplayRecipe(ShulkerBoxFromChestRecipe recipe) {
		int chestIngredientIndex = findChestIngredientIndex(recipe.getIngredients());
		NonNullList<Ingredient> ingredientsCopy = copyIngredients(recipe.getIngredients());
		List<CraftingDisplayVariant> variants = new ArrayList<>();
		List<CraftingDisplayVariant> globalVariants = new ArrayList<>();
		for (ItemStack chestItem : getChestItems(recipe)) {
			CraftingDisplayVariant variant = createVariant(recipe, chestIngredientIndex, chestItem);
			variants.add(variant);
			if (!isTinted(chestItem)) {
				globalVariants.add(variant);
			}
		}

		ResourceLocation id = recipe.getId().withPath(path -> "shulker_from_chest_grouped/" + path);
		CraftingRecipe displayRecipe = new ShapedRecipe(recipe.getId(), "", CraftingBookCategory.MISC, recipe.getWidth(), recipe.getHeight(), ingredientsCopy, ClientRecipeHelper.getResultItem(recipe));
		return new ShulkerBoxFromChestRecipeDisplayRecipe(id, displayRecipe, recipe.getWidth(), recipe.getHeight(), ingredientsCopy, chestIngredientIndex, variants, globalVariants);
	}

	private static CraftingDisplayVariant createVariant(ShulkerBoxFromChestRecipe recipe, int chestIngredientIndex, ItemStack chestItem) {
		CraftingContainer craftingInventory = createCraftingInventory();
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		List<ItemStack> inputs = new ArrayList<>(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++) {
			if (i == chestIngredientIndex) {
				craftingInventory.setItem(i, chestItem.copy());
				inputs.add(chestItem.copy());
				continue;
			}
			Ingredient ingredient = ingredients.get(i);
			ItemStack[] ingredientItems = ingredient.getItems();
			craftingInventory.setItem(i, ingredient.isEmpty() ? ItemStack.EMPTY : ingredientItems[0]);
			inputs.add(ItemStack.EMPTY);
		}
		return new CraftingDisplayVariant(inputs, List.of(ClientRecipeHelper.assemble(recipe, craftingInventory)));
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

	private static int findChestIngredientIndex(NonNullList<Ingredient> ingredients) {
		for (int i = 0; i < ingredients.size(); i++) {
			for (ItemStack ingredientItem : ingredients.get(i).getItems()) {
				if (ingredientItem.getItem() instanceof ChestBlockItem) {
					return i;
				}
			}
		}
		throw new IllegalStateException("Shulker box from chest recipe missing chest ingredient");
	}

	private static List<ItemStack> getChestItems(ShapedRecipe recipe) {
		NonNullList<ItemStack> chestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			for (ItemStack ingredientItem : ingredient.getItems()) {
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

	private static void addTintedChestItems(List<ItemStack> chestItems, ChestBlockItem chestBlockItem) {
		for (DyeColor color : DyeColor.values()) {
			ItemStack chestStack = new ItemStack(chestBlockItem);
			int colorValue = ColorHelper.getColor(color.getTextureDiffuseColors());
			chestBlockItem.setMainColor(chestStack, colorValue);
			chestBlockItem.setAccentColor(chestStack, colorValue);
			chestItems.add(chestStack);
		}
		ItemStack chestStack = new ItemStack(chestBlockItem);
		chestBlockItem.setMainColor(chestStack, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
		chestBlockItem.setAccentColor(chestStack, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
		chestItems.add(chestStack);
	}

	private static boolean isTinted(ItemStack stack) {
		return StorageBlockItem.getMainColorFromStack(stack).isPresent() || StorageBlockItem.getAccentColorFromStack(stack).isPresent();
	}

	private record ShulkerBoxFromChestRecipeDisplayRecipe(ResourceLocation id, CraftingRecipe recipe, int width, int height, NonNullList<Ingredient> ingredients,
			int chestIngredientIndex, List<CraftingDisplayVariant> variants, List<CraftingDisplayVariant> globalVariants) {
		private CraftingDisplaySpec toSpec() {
			return new CraftingDisplaySpec(id, false, width, height, ingredients, variants, globalVariants, Set.of(recipe.getId()), new ShulkerBoxFromChestFocusBehavior(chestIngredientIndex));
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
				return variants.stream().filter(variant -> ItemStack.isSameItemSameTags(variant.firstOutput(), focusedOutput)).toList();
			}
			return variants.stream().filter(variant -> ItemStack.isSameItem(variant.firstOutput(), focusedOutput) && !isTinted(variant.firstOutput())).toList();
		}

		@Override
		public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
			if (focusedInput.getItem() instanceof ChestBlockItem) {
				return variants.stream().filter(variant -> chestIngredientIndex < variant.inputs().size() && ItemStack.isSameItemSameTags(variant.inputs().get(chestIngredientIndex), focusedInput)).toList();
			}
			return focusedInput.is(Items.SHULKER_SHELL) ? variants : List.of();
		}
	}
}
