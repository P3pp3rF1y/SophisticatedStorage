package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SingleColorDyeRecipeSpec;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

public class StorageRecipeViewerDisplays {
	private StorageRecipeViewerDisplays() {
	}

	public static void register(IRecipeViewerDisplayCatalog catalog, IRecipeViewerDisplayContext context) {
		registerDyeRecipes(catalog, context);
		catalog.addCraftingSpecExtensionRecipeClass(StorageTierUpgradeRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(StorageTierUpgradeShapelessRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(DoubleChestTierUpgradeRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(DoubleChestTierUpgradeShapelessRecipe.class);
		catalog.addCraftingSpecExtensionRecipeClass(ShulkerBoxFromChestRecipe.class);
		TierUpgradeRecipesMaker.getGroupedShapedCraftingRecipes(context::getSubtypeInterpreter).stream()
				.map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		TierUpgradeRecipesMaker.getGroupedShapelessCraftingRecipes(context::getSubtypeInterpreter).stream()
				.map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		ShulkerBoxFromChestRecipesMaker.getShapedRecipeSpecs(context::getSubtypeInterpreter).forEach(catalog::addCraftingSpec);
		ClientRecipeHelper.transformAllRecipeHoldersOfType(RecipeType.CRAFTING, CraftingRecipe.class, (id, recipeHolder) -> recipeHolder).stream()
				.filter(StorageRecipeViewerDisplays::isBaseStorageRecipe)
				.forEach(catalog::addCraftingRecipe);
		FlatBarrelRecipesMaker.getShapelessSpecs().forEach(catalog::addCraftingSpec);
		ClientRecipeHelper.transformAllRecipeHoldersOfType(RecipeType.CRAFTING, ShulkerBoxFromVanillaShapelessRecipe.class,
				(id, recipeHolder) -> new RecipeHolder<CraftingRecipe>(ClientRecipeHelper.recipeKey(id), recipeHolder.value()))
				.forEach(catalog::addCraftingRecipe);
	}

	private static boolean isBaseStorageRecipe(RecipeHolder<CraftingRecipe> recipeHolder) {
		CraftingRecipe recipe = recipeHolder.value();
		ItemStack result = ClientRecipeHelper.getResultItem(recipe);
		return recipeHolder.id().identifier().getNamespace().equals(SophisticatedStorage.MOD_ID)
				&& result.getItem() instanceof StorageBlockItem
				&& !(recipe instanceof StorageTierUpgradeRecipe)
				&& !(recipe instanceof StorageTierUpgradeShapelessRecipe)
				&& !(recipe instanceof DoubleChestTierUpgradeRecipe)
				&& !(recipe instanceof DoubleChestTierUpgradeShapelessRecipe)
				&& !(recipe instanceof ShulkerBoxFromChestRecipe)
				&& !(recipe instanceof ShulkerBoxFromVanillaShapelessRecipe);
	}

	public static void registerDyeRecipes(IRecipeViewerDisplayCatalog catalog, IRecipeViewerDisplayContext context) {
		DyeRecipesMaker.getSingleColorRecipeSpecs(context::getSubtypeInterpreter).stream()
				.map(spec -> new SingleColorDyeRecipeSpec(spec.id(), spec.sourceStacks(), spec.variantPairs(), (recipeResult, focusedOutput) -> context.getSubtypeInterpreter(focusedOutput)
						.map(interpreter -> recipeResult.is(focusedOutput.getItem()) && interpreter.getComparableData(recipeResult).equals(interpreter.getComparableData(focusedOutput)))
						.orElse(ItemStack.isSameItemSameComponents(recipeResult, focusedOutput))))
				.forEach(catalog::addGroupedCraftingSpec);
		DyeRecipesMaker.getMultipleColorsRecipes(context::getSubtypeInterpreter).forEach(catalog::addCraftingRecipe);
	}

}
