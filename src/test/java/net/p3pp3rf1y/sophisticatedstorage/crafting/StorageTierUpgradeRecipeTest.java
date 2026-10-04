package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Requires loaded NeoForge item components; covered by DevClient assert.storageTierUpgradeRecipe matrix.")
class StorageTierUpgradeRecipeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void singleStorageRecipesOnlyMatchUnlinkedOrPrimaryStorage() {
		ItemStack storage = new ItemStack(ModBlocks.DIAMOND_CHEST_ITEM.get());
		assertUpgradeEligibility(storage, new StorageTierUpgradeRecipe(shaped(storage)), new StorageTierUpgradeShapelessRecipe(shapeless(storage)));
	}

	@Test
	void doubleChestRecipesOnlyMatchUnlinkedOrPrimaryStorage() {
		ItemStack storage = new ItemStack(ModBlocks.DIAMOND_CHEST_ITEM.get());
		ChestBlockItem.setDoubleChest(storage, true);
		assertUpgradeEligibility(storage, new DoubleChestTierUpgradeRecipe(shaped(storage)), new DoubleChestTierUpgradeShapelessRecipe(shapeless(storage)));
	}

	private static void assertUpgradeEligibility(ItemStack storage, Recipe<CraftingInput> shaped, Recipe<CraftingInput> shapeless) {
		CraftingInput input = CraftingInput.of(1, 1, List.of(storage));
		boolean shapedUnlinkedMatches = shaped.matches(input, null);
		boolean shapelessUnlinkedMatches = shapeless.matches(input, null);

		storage.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		boolean shapedSecondaryMatches = shaped.matches(input, null);
		boolean shapelessSecondaryMatches = shapeless.matches(input, null);
		ItemStack shapedSecondaryResult = shaped.assemble(input);
		ItemStack shapelessSecondaryResult = shapeless.assemble(input);

		storage.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, true);
		boolean shapedPrimaryMatches = shaped.matches(input, null);
		boolean shapelessPrimaryMatches = shapeless.matches(input, null);

		assertTrue(shapedUnlinkedMatches);
		assertTrue(shapelessUnlinkedMatches);
		assertFalse(shapedSecondaryMatches);
		assertFalse(shapelessSecondaryMatches);
		assertTrue(shapedSecondaryResult.isEmpty());
		assertTrue(shapelessSecondaryResult.isEmpty());
		assertTrue(shapedPrimaryMatches);
		assertTrue(shapelessPrimaryMatches);
	}

	private static ShapedRecipe shaped(ItemStack storage) {
		return new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				new ShapedRecipePattern(1, 1, List.of(Optional.of(Ingredient.of(storage.getItem()))), Optional.empty()),
				ItemStackTemplate.fromNonEmptyStack(new ItemStack(ModBlocks.NETHERITE_CHEST_ITEM.get())));
	}

	private static ShapelessRecipe shapeless(ItemStack storage) {
		return new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				ItemStackTemplate.fromNonEmptyStack(new ItemStack(ModBlocks.NETHERITE_CHEST_ITEM.get())), List.of(Ingredient.of(storage.getItem())));
	}
}
