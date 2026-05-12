package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SingleColorDyeRecipeSpec;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromVanillaShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;

public class StorageRecipeViewerDisplays {
	private StorageRecipeViewerDisplays() {
	}

	public static void register(IRecipeViewerDisplayCatalog catalog, IRecipeViewerDisplayContext context) {
		registerDyeRecipes(catalog, context);
		catalog.addCraftingSpecExtensionRecipeClass(StorageTierUpgradeRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(StorageTierUpgradeShapelessRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(DoubleChestTierUpgradeRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(DoubleChestTierUpgradeShapelessRecipe.class);
		TierUpgradeRecipesMaker.getGroupedShapedCraftingRecipes(context::getSubtypeInterpreter).stream()
				.map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		TierUpgradeRecipesMaker.getGroupedShapelessCraftingRecipes(context::getSubtypeInterpreter).stream()
				.map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		ShulkerBoxFromChestRecipesMaker.getShapedRecipes(context::getSubtypeInterpreter).forEach(catalog::addCraftingRecipe);
		FlatBarrelRecipesMaker.getShapelessRecipes().forEach(catalog::addCraftingRecipe);
		ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, ShulkerBoxFromVanillaShapelessRecipe.class, recipe -> recipe)
				.forEach(catalog::addCraftingRecipe);
	}

	public static void registerDyeRecipes(IRecipeViewerDisplayCatalog catalog, IRecipeViewerDisplayContext context) {
		DyeRecipesMaker.getSingleColorRecipeSpecs(context::getSubtypeInterpreter).stream()
				.map(spec -> new SingleColorDyeRecipeSpec(spec.id(), spec.sourceStacks(), spec.variantPairs(), (recipeResult, focusedOutput) -> context.getSubtypeInterpreter(focusedOutput)
						.map(interpreter -> recipeResult.is(focusedOutput.getItem()) && interpreter.getComparableData(recipeResult).equals(interpreter.getComparableData(focusedOutput)))
						.orElse(ItemStack.isSameItemSameTags(recipeResult, focusedOutput))))
				.forEach(catalog::addGroupedCraftingSpec);
		DyeRecipesMaker.getMultipleColorsRecipes(context::getSubtypeInterpreter).forEach(catalog::addCraftingRecipe);
	}

}
