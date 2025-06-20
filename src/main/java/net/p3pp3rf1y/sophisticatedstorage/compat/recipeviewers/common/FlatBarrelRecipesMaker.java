package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class FlatBarrelRecipesMaker {
	private FlatBarrelRecipesMaker() {
	}

	public static void addRecipes(IRecipeDisplayGenerator<?> generator) {
		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL.get()), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.toggleFlatTop(flatBarrel);

		generator.shapeless(flatBarrel).requires(barrel)
				.save(ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getRL("flatten_barrel")));

		generator.shapeless(barrel).requires(flatBarrel)
				.save(ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getRL("unflatten_barrel")));
	}
}
