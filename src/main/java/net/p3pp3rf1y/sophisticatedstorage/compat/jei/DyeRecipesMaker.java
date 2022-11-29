package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DyeRecipesMaker {
	private DyeRecipesMaker() {}

	public static List<CraftingRecipe> getRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();

		Map<Item, ItemStack[]> blocks = new HashMap<>();
		blocks.put(ModBlocks.BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.BARREL.get()));
		blocks.put(ModBlocks.IRON_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.IRON_BARREL.get()));
		blocks.put(ModBlocks.GOLD_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.GOLD_BARREL.get()));
		blocks.put(ModBlocks.DIAMOND_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.DIAMOND_BARREL.get()));
		blocks.put(ModBlocks.NETHERITE_BARREL_ITEM.get(), getWoodStorageStacks(ModBlocks.NETHERITE_BARREL.get()));
		blocks.put(ModBlocks.CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.CHEST.get()));
		blocks.put(ModBlocks.IRON_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.IRON_CHEST.get()));
		blocks.put(ModBlocks.GOLD_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.GOLD_CHEST.get()));
		blocks.put(ModBlocks.DIAMOND_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.DIAMOND_CHEST.get()));
		blocks.put(ModBlocks.NETHERITE_CHEST_ITEM.get(), getWoodStorageStacks(ModBlocks.NETHERITE_CHEST.get()));
		blocks.put(ModBlocks.SHULKER_BOX_ITEM.get(), new ItemStack[] {new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.IRON_SHULKER_BOX_ITEM.get(), new ItemStack[] {new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), new ItemStack[] {new ItemStack(ModBlocks.GOLD_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), new ItemStack[] {new ItemStack(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get())});
		blocks.put(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), new ItemStack[] {new ItemStack(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get())});

		blocks.put(ModBlocks.LIMITED_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()));

		blocks.put(ModBlocks.LIMITED_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_IRON_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_GOLD_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()));
		blocks.put(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get(), getWoodStorageStacks(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()));

		addSingleColorRecipes(recipes, blocks);
		addMultipleColorsRecipe(recipes, blocks);

		return recipes;
	}

	private static ItemStack[] getWoodStorageStacks(StorageBlockBase woodStorageBlock) {
		Set<ItemStack> ret = new HashSet<>();
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> ret.add(WoodStorageBlockItem.setWoodType(new ItemStack(woodStorageBlock), woodType)));
		return ret.toArray(new ItemStack[0]);
	}

	private static void addMultipleColorsRecipe(List<CraftingRecipe> recipes, Map<Item, ItemStack[]> items) {
		items.forEach((block, stacks) -> {
			NonNullList<Ingredient> ingredients = NonNullList.create();
			ingredients.add(Ingredient.of(DyeColor.YELLOW.getTag()));
			ingredients.add(Ingredient.of(stacks));
			ingredients.add(Ingredient.of(DyeColor.LIME.getTag()));

			ItemStack result = new ItemStack(block);
			if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.setMainColor(result, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
				tintableBlockItem.setAccentColor(result, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
			}
			ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "multiple_colors");
			recipes.add(new ShapedRecipe(id, "", 3, 1, ingredients, result));
		});
	}

	private static void addSingleColorRecipes(List<CraftingRecipe> recipes, Map<Item, ItemStack[]> items) {
		for (DyeColor color : DyeColor.values()) {
			items.forEach((block, stacks) -> {
				NonNullList<Ingredient> ingredients = NonNullList.create();
				ingredients.add(Ingredient.of(stacks));
				ingredients.add(Ingredient.of(color.getTag()));
				ItemStack result = new ItemStack(block);
				if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(result, ColorHelper.getColor(color.getTextureDiffuseColors()));
					tintableBlockItem.setAccentColor(result, ColorHelper.getColor(color.getTextureDiffuseColors()));
				}
				ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, "single_color_" + color.getSerializedName());
				recipes.add(new ShapedRecipe(id, "", 1, 2, ingredients, result));
			});
		}
	}
}
