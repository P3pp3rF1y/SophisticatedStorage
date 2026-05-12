package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.DyeVariantPair;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SingleColorDyeRecipeSpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.*;
import java.util.function.Function;

public class DyeRecipesMaker {
	private DyeRecipesMaker() {
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<CraftingRecipe> getMultipleColorsRecipes(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		List<CraftingRecipe> recipes = new ArrayList<>();

		Map<Item, ItemStack[]> blocks = getDyeableItems();
		addMultipleColorsRecipe(recipes, blocks, getSubtypeInterpreter);

		return recipes;
	}

	public static <T extends PropertyBasedSubtypeInterpreter> List<SingleColorDyeRecipeSpec> getSingleColorRecipeSpecs(Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		return getSingleColorRecipeSpecs(getDyeableItems(), getSubtypeInterpreter);
	}

	private static Map<Item, ItemStack[]> getDyeableItems() {
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
		return blocks;
	}

	private static ItemStack[] getWoodStorageStacks(StorageBlockBase woodStorageBlock) {
		return getWoodStorageStackList(woodStorageBlock).toArray(new ItemStack[0]);
	}

	static List<ItemStack> getWoodStorageStackList(StorageBlockBase woodStorageBlock) {
		Set<ItemStack> ret = new HashSet<>();
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> ret.add(WoodStorageBlockItem.setWoodType(new ItemStack(woodStorageBlock), woodType)));
		return List.copyOf(ret);
	}

	private static <T extends PropertyBasedSubtypeInterpreter> void addMultipleColorsRecipe(List<CraftingRecipe> recipes, Map<Item, ItemStack[]> items, Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
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
			ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(result).map(i -> i.getRegistrySanitizedItemString(result)).orElse("multiple_color"));
			recipes.add(new ShapedRecipe(id, "", CraftingBookCategory.MISC, 3, 1, ingredients, result));
		});
	}

	private static <T extends PropertyBasedSubtypeInterpreter> List<SingleColorDyeRecipeSpec> getSingleColorRecipeSpecs(Map<Item, ItemStack[]> items, Function<ItemStack, Optional<T>> getSubtypeInterpreter) {
		List<SingleColorDyeRecipeSpec> recipes = new ArrayList<>();
		items.forEach((block, stacks) -> {
			List<DyeVariantPair> variants = new ArrayList<>();
			for (DyeColor color : DyeColor.values()) {
				ItemStack result = new ItemStack(block);
				if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					int colorValue = ColorHelper.getColor(color.getTextureDiffuseColors());
					tintableBlockItem.setMainColor(result, colorValue);
					tintableBlockItem.setAccentColor(result, colorValue);
				}
				variants.add(new DyeVariantPair(new ItemStack(DyeItem.byColor(color)), result));
			}
			ItemStack idStack = variants.get(0).result();
			ResourceLocation id = new ResourceLocation(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(idStack).map(i -> i.getRegistrySanitizedItemString(idStack)).orElse("single_color_" + BuiltInRegistries.ITEM.getKey(block).getPath()));
			recipes.add(new SingleColorDyeRecipeSpec(id.withSuffix("_grouped"), List.of(stacks), variants));
		});
		return recipes;
	}
}
