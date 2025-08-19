package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DyeRecipesMaker {
	private DyeRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<RecipeHolder<CraftingRecipe>> getRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return getRecipes(getSubtypeInterpreter, RecipeHolder::new);
	}

	public static <R, T extends PropertyBasedSubtypeInterpreter> List<R> getRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter, BiFunction<ResourceLocation, ShapedRecipe, R> transformRecipe) {
		List<R> recipes = new ArrayList<>();

		Map<Item, ItemStack[]> blocks = new HashMap<>();
		blocks.put(ModBlocks.BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.BARREL.get()));
		blocks.put(ModBlocks.COPPER_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.COPPER_BARREL.get()));
		blocks.put(ModBlocks.IRON_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.IRON_BARREL.get()));
		blocks.put(ModBlocks.GOLD_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.GOLD_BARREL.get()));
		blocks.put(ModBlocks.DIAMOND_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.DIAMOND_BARREL.get()));
		blocks.put(ModBlocks.NETHERITE_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.NETHERITE_BARREL.get()));

		blocks.put(ModBlocks.CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.CHEST.get()));
		blocks.put(ModBlocks.COPPER_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.COPPER_CHEST.get()));
		blocks.put(ModBlocks.IRON_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.IRON_CHEST.get()));
		blocks.put(ModBlocks.GOLD_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.GOLD_CHEST.get()));
		blocks.put(ModBlocks.DIAMOND_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.DIAMOND_CHEST.get()));
		blocks.put(ModBlocks.NETHERITE_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.NETHERITE_CHEST.get()));

		blocks.put(ModBlocks.SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.COPPER_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.IRON_SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.GOLD_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), new ItemStack[]{new ItemStack(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get())});

		blocks.put(ModBlocks.LIMITED_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_COPPER_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_COPPER_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_COPPER_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_COPPER_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()));

		addSingleColorRecipes(recipes, blocks, getSubtypeInterpreter, transformRecipe);
		addMultipleColorsRecipe(recipes, blocks, getSubtypeInterpreter, transformRecipe);

		return recipes;
	}

	private static ItemStack[] getWoodStorageStacks(StorageBlockBase woodStorageBlock) {
		Set<ItemStack> ret = new HashSet<>();
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> ret.add(WoodStorageBlockItem.setWoodType(new ItemStack(woodStorageBlock), woodType)));
		return ret.toArray(new ItemStack[0]);
	}

	private static <R, T extends PropertyBasedSubtypeInterpreter> void addMultipleColorsRecipe(List<R> recipes, Map<Item, ItemStack[]> items, Function<ItemStack, Optional<T>> getSubtypeInterpreter, BiFunction<ResourceLocation, ShapedRecipe, R> transformRecipe) {
		items.forEach((block, stacks) -> {
			NonNullList<Ingredient> ingredients = NonNullList.create();
			ingredients.add(Ingredient.of(DyeColor.YELLOW.getTag()));
			ingredients.add(Ingredient.of(stacks));
			ingredients.add(Ingredient.of(DyeColor.LIME.getTag()));

			ItemStack result = new ItemStack(block);
			if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.setMainColor(result, DyeColor.YELLOW.getTextureDiffuseColor());
				tintableBlockItem.setAccentColor(result, DyeColor.LIME.getTextureDiffuseColor());
			}
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(result).map(i -> i.getRegistrySanitizedItemString(result)).orElse("multiple_color"));
			ShapedRecipePattern pattern = new ShapedRecipePattern(3, 1, ingredients, Optional.empty());
			recipes.add(transformRecipe.apply(id, new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result)));
		});
	}

	private static <R, T extends PropertyBasedSubtypeInterpreter> void addSingleColorRecipes(List<R> recipes, Map<Item, ItemStack[]> items, Function<ItemStack, Optional<T>> getSubtypeInterpreter, BiFunction<ResourceLocation, ShapedRecipe, R> transformRecipe) {
		for (DyeColor color : DyeColor.values()) {
			items.forEach((block, stacks) -> {
				NonNullList<Ingredient> ingredients = NonNullList.create();
				ingredients.add(Ingredient.of(stacks));
				ingredients.add(Ingredient.of(color.getTag()));
				ItemStack result = new ItemStack(block);
				if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(result, color.getTextureDiffuseColor());
					tintableBlockItem.setAccentColor(result, color.getTextureDiffuseColor());
				}
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(result).map(i -> i.getRegistrySanitizedItemString(result)).orElse("single_color_" + color.getSerializedName()));
				ShapedRecipePattern pattern = new ShapedRecipePattern(1, 2, ingredients, Optional.empty());
				recipes.add(transformRecipe.apply(id, new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result)));
			});
		}
	}
}
