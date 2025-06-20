package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class DyeRecipesMaker {
	private DyeRecipesMaker() {
	}

	public static void addRecipes(IRecipeDisplayGenerator<?> generator, Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter) {
		Map<Item, List<ItemStack>> blocks = new HashMap<>();

		getItemsOfClass(WoodStorageBlockItem.class).forEach(item -> blocks.put(item, getWoodStorageStacks(item.getBlock())));
		getItemsOfClass(ShulkerBoxItem.class).forEach(item -> blocks.put(item, Collections.singletonList(new ItemStack(item))));

		addSingleColorRecipes(generator, blocks, getSubtypeInterpreter);
		addMultipleColorsRecipe(generator, blocks, getSubtypeInterpreter);
	}

	private static Stream<BlockItem> getItemsOfClass(Class<? extends BlockItem> clazz) {
		return ModBlocks.ITEMS.getEntries().stream()
				.filter(holder -> clazz.isInstance(holder.get()))
				.map(holder -> clazz.cast(holder.get().asItem()));
	}

	private static List<ItemStack> getWoodStorageStacks(Block woodStorageBlock) {
		List<ItemStack> ret = new ArrayList<>();
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> ret.add(WoodStorageBlockItem.setWoodType(new ItemStack(woodStorageBlock), woodType)));
		return ret;
	}

	private static void addMultipleColorsRecipe(IRecipeDisplayGenerator<?> generator, Map<Item, List<ItemStack>> items, Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter) {
		items.forEach((block, stacks) -> {
			ItemStack result = new ItemStack(block);
			if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.setMainColor(result, DyeColor.YELLOW.getTextureDiffuseColor());
				tintableBlockItem.setAccentColor(result, DyeColor.LIME.getTextureDiffuseColor());
			}
			generator.shaped(result)
					.pattern("YSL")
					.define('Y', DyeColor.YELLOW.getTag())
					.define('S', stacks)
					.define('L', DyeColor.LIME.getTag())
					.save(ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(result).map(i -> i.getRegistrySanitizedItemString(result)).orElse("multiple_color"))));
		});
	}

	private static void addSingleColorRecipes(IRecipeDisplayGenerator<?> generator, Map<Item, List<ItemStack>> items, Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter) {
		for (DyeColor color : DyeColor.values()) {
			items.forEach((block, stacks) -> {
				ItemStack result = new ItemStack(block);
				if (result.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(result, color.getTextureDiffuseColor());
					tintableBlockItem.setAccentColor(result, color.getTextureDiffuseColor());
				}
				generator.shaped(result)
						.pattern("S")
						.pattern("C")
						.define('C', color.getTag())
						.define('S', stacks)
						.save(ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, getSubtypeInterpreter.apply(result).map(i -> i.getRegistrySanitizedItemString(result)).orElse("single_color_" + color.getSerializedName()))));
			});
		}
	}
}
