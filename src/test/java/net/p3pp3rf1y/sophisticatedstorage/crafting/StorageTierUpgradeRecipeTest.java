package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StorageTierUpgradeRecipeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Stream.of(ModBlocks.TANK_ITEM.get(), ModBlocks.COPPER_TANK_ITEM.get(), ModBlocks.IRON_TANK_ITEM.get(), ModBlocks.GOLD_TANK_ITEM.get(),
				ModBlocks.DIAMOND_TANK_ITEM.get(), ModBlocks.NETHERITE_TANK_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(),
				ModBlocks.NETHERITE_CHEST_ITEM.get(), ModItems.PUMP_UPGRADE.get()).map(Item.class::cast)
				.forEach(item -> item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY));
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
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

	@ParameterizedTest
	@MethodSource("tankUpgradeTargets")
	void assembleShapedTankUpgradePreservesCanonicalContentsAndComponents(TankBlockItem target) {
		ItemStack original = tankWithCanonicalContents();
		ItemStack snapshot = original.copy();
		CraftingInput input = CraftingInput.of(1, 1, List.of(original));
		StorageTierUpgradeRecipe recipe = new StorageTierUpgradeRecipe(
				new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
						new ShapedRecipePattern(1, 1, List.of(Optional.of(Ingredient.of(original.getItem()))), Optional.empty()),
						ItemStackTemplate.fromNonEmptyStack(new ItemStack(target))));

		assertTrue(recipe.matches(input, null));
		assertTankUpgradePreservesCanonicalContentsAndComponents(snapshot, recipe.assemble(input), target);
		assertTrue(ItemStack.isSameItemSameComponents(snapshot, original));
	}

	@ParameterizedTest
	@MethodSource("tankUpgradeTargets")
	void assembleShapelessTankUpgradePreservesCanonicalContentsAndComponents(TankBlockItem target) {
		ItemStack original = tankWithCanonicalContents();
		ItemStack snapshot = original.copy();
		CraftingInput input = CraftingInput.of(1, 1, List.of(original));
		StorageTierUpgradeShapelessRecipe recipe = new StorageTierUpgradeShapelessRecipe(
				new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
						ItemStackTemplate.fromNonEmptyStack(new ItemStack(target)), List.of(Ingredient.of(original.getItem()))));

		assertTrue(recipe.matches(input, null));
		assertTankUpgradePreservesCanonicalContentsAndComponents(snapshot, recipe.assemble(input), target);
		assertTrue(ItemStack.isSameItemSameComponents(snapshot, original));
	}

	@ParameterizedTest
	@MethodSource("tankEligibilityCases")
	void matchesTankRecipesOnlyWhenUnlinkedOrPrimary(TankEligibility params) {
		ItemStack tank = new ItemStack(ModBlocks.DIAMOND_TANK_ITEM.get());
		if (params.state != EndpointState.UNLINKED) {
			tank.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
			tank.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, params.state == EndpointState.PRIMARY);
		}
		Recipe<CraftingInput> recipe = params.shapeless ? new StorageTierUpgradeShapelessRecipe(shapeless(tank)) : new StorageTierUpgradeRecipe(shaped(tank));
		assertEquals(params.state != EndpointState.SECONDARY, recipe.matches(CraftingInput.of(1, 1, List.of(tank)), null));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void assembleRejectsSecondaryTankEndpoints(boolean shapeless) {
		ItemStack tank = new ItemStack(ModBlocks.DIAMOND_TANK_ITEM.get());
		tank.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		Recipe<CraftingInput> recipe = shapeless ? new StorageTierUpgradeShapelessRecipe(shapeless(tank)) : new StorageTierUpgradeRecipe(shaped(tank));
		assertTrue(recipe.assemble(CraftingInput.of(1, 1, List.of(tank))).isEmpty());
	}

	private static Stream<TankEligibility> tankEligibilityCases() {
		return Stream.of(EndpointState.values()).flatMap(state -> Stream.of(new TankEligibility(false, state), new TankEligibility(true, state)));
	}

	private enum EndpointState {
		UNLINKED, SECONDARY, PRIMARY
	}

	private record TankEligibility(boolean shapeless, EndpointState state) {
	}

	private static Stream<TankBlockItem> tankUpgradeTargets() {
		return Stream.of(ModBlocks.COPPER_TANK_ITEM.get(), ModBlocks.IRON_TANK_ITEM.get(), ModBlocks.GOLD_TANK_ITEM.get(), ModBlocks.DIAMOND_TANK_ITEM.get(),
				ModBlocks.NETHERITE_TANK_ITEM.get()).map(TankBlockItem.class::cast);
	}

	private static ItemStack tankWithCanonicalContents() {
		ItemStack tank = new ItemStack(ModBlocks.TANK_ITEM.get());
		FluidStack fluid = new FluidStack(Fluids.WATER, 17000);
		fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Component-bearing water"));
		tank.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(fluid));
		tank.set(ModDataComponents.TANK_UPGRADES, ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.PUMP_UPGRADE.get()))));
		tank.set(ModDataComponents.LOCKED, true);
		tank.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		tank.set(ModDataComponents.TANK_HEIGHT, 3);
		tank.set(DataComponents.CUSTOM_NAME, Component.literal("Stored fluid"));
		return tank;
	}

	private static void assertTankUpgradePreservesCanonicalContentsAndComponents(ItemStack original, ItemStack result, TankBlockItem target) {
		assertTrue(result.is(target));
		assertEquals(original.get(ModDataComponents.TANK_HEIGHT), result.get(ModDataComponents.TANK_HEIGHT));
		assertEquals(original.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
		assertEquals(original.get(ModCoreDataComponents.STORAGE_UUID), result.get(ModCoreDataComponents.STORAGE_UUID));
		assertTrue(result.get(ModDataComponents.LOCKED));
		FluidStack fluid = original.get(ModCoreDataComponents.FLUID_CONTENTS).copy();
		FluidStack restoredFluid = result.get(ModCoreDataComponents.FLUID_CONTENTS).copy();
		assertTrue(FluidStack.isSameFluidSameComponents(fluid, restoredFluid));
		assertEquals(fluid.getAmount(), restoredFluid.getAmount());
		assertEquals(original.get(ModDataComponents.TANK_UPGRADES), result.get(ModDataComponents.TANK_UPGRADES));
		assertTrue(result.get(ModDataComponents.TANK_UPGRADES).getStackInSlot(0).is(ModItems.PUMP_UPGRADE));
		assertEquals(((TankBlock) target.getBlock()).getTier().upgradeSlots(), result.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS));
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
