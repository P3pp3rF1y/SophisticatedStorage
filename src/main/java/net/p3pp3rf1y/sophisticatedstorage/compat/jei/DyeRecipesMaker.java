package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DyeRecipesMaker {
	private DyeRecipesMaker() {}

	public static List<CraftingRecipe> getRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();
		Map<Block, ItemStack[]> blocks = Map.of(
				ModBlocks.BARREL.get(), getBarrelWoodStacks(ModBlocks.BARREL.get()),
				ModBlocks.IRON_BARREL.get(), getBarrelWoodStacks(ModBlocks.IRON_BARREL.get()),
				ModBlocks.GOLD_BARREL.get(), getBarrelWoodStacks(ModBlocks.GOLD_BARREL.get()),
				ModBlocks.DIAMOND_BARREL.get(), getBarrelWoodStacks(ModBlocks.DIAMOND_BARREL.get()),
				ModBlocks.NETHERITE_BARREL.get(), getBarrelWoodStacks(ModBlocks.NETHERITE_BARREL.get())
		);
		addSingleColorRecipes(recipes, blocks);
		addMultipleColorsRecipe(recipes, blocks);

		return recipes;
	}

	private static ItemStack[] getBarrelWoodStacks(BarrelBlock barrelBlock) {
		Set<ItemStack> ret = new HashSet<>();
		BarrelBlock.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> ret.add(BarrelBlockItem.setWoodType(new ItemStack(barrelBlock), woodType)));
		return ret.toArray(new ItemStack[0]);
	}

	private static void addMultipleColorsRecipe(List<CraftingRecipe> recipes, Map<Block, ItemStack[]> blocks) {
		blocks.forEach((block, stacks) -> {
			if (block instanceof ITintableBlock tintableBlock) {
				NonNullList<Ingredient> ingredients = NonNullList.create();
				ingredients.add(Ingredient.of(DyeColor.YELLOW.getTag()));
				ingredients.add(Ingredient.of(stacks));
				ingredients.add(Ingredient.of(DyeColor.LIME.getTag()));

				ItemStack result = new ItemStack(block);
				tintableBlock.setMainColor(result, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
				tintableBlock.setAccentColor(result, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "multiple_colors");
				recipes.add(new ShapedRecipe(id, "", 3, 1, ingredients, result));
			}
		});
	}

	private static void addSingleColorRecipes(List<CraftingRecipe> recipes, Map<Block, ItemStack[]> blocks) {
		for (DyeColor color : DyeColor.values()) {
			blocks.forEach((block, stacks) -> {
				if (block instanceof ITintableBlock tintableBlock) {
					NonNullList<Ingredient> ingredients = NonNullList.create();
					ingredients.add(Ingredient.of(stacks));
					ingredients.add(Ingredient.of(color.getTag()));
					ItemStack result = new ItemStack(block);
					tintableBlock.setMainColor(result, ColorHelper.getColor(color.getTextureDiffuseColors()));
					tintableBlock.setAccentColor(result, ColorHelper.getColor(color.getTextureDiffuseColors()));
					ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "single_color_" + color.getSerializedName());
					recipes.add(new ShapedRecipe(id, "", 1, 2, ingredients, result));
				}
			});
		}
	}
}
