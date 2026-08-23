package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SingleColorDyeRecipeSpec;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromVanillaShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

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
		catalog.addCraftingSpecExtensionRecipeClass(ShulkerBoxFromVanillaShapelessRecipe.class);
		TierUpgradeRecipesMaker.getGroupedShapedCraftingRecipes(context::getSubtypeInterpreter).stream().map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		TierUpgradeRecipesMaker.getGroupedShapelessCraftingRecipes(context::getSubtypeInterpreter).stream().map(TierUpgradeDisplayRecipe::toSpec)
				.forEach(catalog::addCraftingSpec);
		ShulkerBoxFromChestRecipesMaker.getShapedRecipeSpecs(context::getSubtypeInterpreter).forEach(catalog::addCraftingSpec);
		ShulkerBoxFromVanillaRecipesMaker.getShapelessRecipeSpecs().forEach(catalog::addCraftingSpec);
		ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, CraftingRecipe.class, recipe -> recipe).stream()
				.filter(StorageRecipeViewerDisplays::isBaseStorageRecipe).forEach(catalog::addCraftingRecipe);
		GenericWoodStorageRecipesMaker.getRecipes().forEach(catalog::addCraftingRecipe);
		FlatBarrelRecipesMaker.getShapelessRecipes().forEach(catalog::addCraftingRecipe);
	}

	private static boolean isBaseStorageRecipe(CraftingRecipe recipe) {
		ItemStack result = ClientRecipeHelper.getResultItem(recipe);
		return recipe.getId().getNamespace().equals("sophisticatedstorage") && result.getItem() instanceof StorageBlockItem
				&& WoodStorageBlockItem.getWoodType(result).isPresent() && !(recipe instanceof StorageTierUpgradeRecipe)
				&& !(recipe instanceof StorageTierUpgradeShapelessRecipe) && !(recipe instanceof DoubleChestTierUpgradeRecipe)
				&& !(recipe instanceof DoubleChestTierUpgradeShapelessRecipe) && !(recipe instanceof ShulkerBoxFromChestRecipe)
				&& !(recipe instanceof ShulkerBoxFromVanillaShapelessRecipe) && !(recipe instanceof GenericWoodStorageRecipe);
	}

	public static void registerDyeRecipes(IRecipeViewerDisplayCatalog catalog, IRecipeViewerDisplayContext context) {
		DyeRecipesMaker.getSingleColorRecipeSpecs(context::getSubtypeInterpreter).stream()
				.map(spec -> new SingleColorDyeRecipeSpec(spec.id(), spec.sourceStacks(), spec.variantPairs(),
						(recipeResult, focusedOutput) -> context.getSubtypeInterpreter(focusedOutput)
								.map(interpreter -> recipeResult.is(focusedOutput.getItem())
										&& interpreter.getComparableData(recipeResult).equals(interpreter.getComparableData(focusedOutput)))
								.orElse(ItemStack.isSameItemSameTags(recipeResult, focusedOutput))))
				.forEach(catalog::addGroupedCraftingSpec);
		DyeRecipesMaker.getMultipleColorsRecipes(context::getSubtypeInterpreter).forEach(catalog::addCraftingRecipe);
	}

	public static boolean needsComponentSensitiveCraftingDisplay(ItemStack stack) {
		return stack.getItem() instanceof StorageBlockItem
				&& (StorageBlockItem.getMainColorFromStack(stack).isPresent() || StorageBlockItem.getAccentColorFromStack(stack).isPresent()
						|| WoodStorageBlockItem.getWoodType(stack).isPresent() || ChestBlockItem.isDoubleChest(stack) || BarrelBlockItem.isFlatTop(stack));
	}

}
