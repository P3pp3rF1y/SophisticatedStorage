package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemContents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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

	@ParameterizedTest
	@MethodSource("tankUpgradeTargets")
	void assembleShapedTankUpgradePreservesCanonicalContentsAndComponents(TankBlockItem target) {
		ItemStack original = tankWithCanonicalContents();
		ItemStack snapshot = original.copy();
		CraftingContainer input = input(original);
		StorageTierUpgradeRecipe recipe = new StorageTierUpgradeRecipe(new ShapedRecipe(id(), "", CraftingBookCategory.MISC, 1, 1,
				NonNullList.of(Ingredient.EMPTY, Ingredient.of(original.getItem())), new ItemStack(target)));

		assertTrue(recipe.matches(input, null));
		ItemStack result = recipe.assemble(input, null);

		assertTankUpgradePreservesCanonicalContentsAndComponents(snapshot, result, target);
		assertTrue(ItemStack.isSameItemSameTags(snapshot, original));
	}

	@ParameterizedTest
	@MethodSource("tankUpgradeTargets")
	void assembleShapelessTankUpgradePreservesCanonicalContentsAndComponents(TankBlockItem target) {
		ItemStack original = tankWithCanonicalContents();
		ItemStack snapshot = original.copy();
		CraftingContainer input = input(original);
		StorageTierUpgradeShapelessRecipe recipe = new StorageTierUpgradeShapelessRecipe(new ShapelessRecipe(id(), "", CraftingBookCategory.MISC,
				new ItemStack(target), NonNullList.of(Ingredient.EMPTY, Ingredient.of(original.getItem()))));

		assertTrue(recipe.matches(input, null));
		ItemStack result = recipe.assemble(input, null);

		assertTankUpgradePreservesCanonicalContentsAndComponents(snapshot, result, target);
		assertTrue(ItemStack.isSameItemSameTags(snapshot, original));
	}

	@ParameterizedTest
	@MethodSource("tankEligibilityCases")
	void matchesTankRecipesOnlyWhenUnlinkedOrPrimary(TankEligibility params) {
		ItemStack tank = new ItemStack(ModBlocks.DIAMOND_TANK_ITEM.get());
		if (params.state != EndpointState.UNLINKED) {
			LinkedStorageStackData.setEndpoint(tank, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
			LinkedStorageStackData.setPrimaryEndpoint(tank, params.state == EndpointState.PRIMARY);
		}
		Recipe<CraftingContainer> recipe = params.shapeless
				? new StorageTierUpgradeShapelessRecipe(shapeless(tank))
				: new StorageTierUpgradeRecipe(shaped(tank));

		assertEquals(params.state != EndpointState.SECONDARY, recipe.matches(input(tank), null));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void assembleRejectsSecondaryTankEndpoints(boolean shapeless) {
		ItemStack tank = new ItemStack(ModBlocks.DIAMOND_TANK_ITEM.get());
		LinkedStorageStackData.setEndpoint(tank, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		Recipe<CraftingContainer> recipe = shapeless ? new StorageTierUpgradeShapelessRecipe(shapeless(tank)) : new StorageTierUpgradeRecipe(shaped(tank));

		assertTrue(recipe.assemble(input(tank), null).isEmpty());
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
				ModBlocks.NETHERITE_TANK_ITEM.get()).map(item -> (TankBlockItem) item);
	}

	private static ItemStack tankWithCanonicalContents() {
		ItemStack tank = new ItemStack(ModBlocks.TANK_ITEM.get());
		FluidStack fluid = new FluidStack(Fluids.WATER, 17000);
		CompoundTag identity = new CompoundTag();
		identity.putString("identity", "Component-bearing water");
		fluid.setTag(identity);
		tank.getOrCreateTag().put("fluidContents", fluid.writeToNBT(new CompoundTag()));
		ItemStackHandler upgrades = new ItemStackHandler(3);
		upgrades.setStackInSlot(2, new ItemStack(ModItems.PUMP_UPGRADE.get()));
		tank.getTag().put("tankUpgrades", upgrades.serializeNBT());
		ItemStackHandler containers = new ItemStackHandler(4);
		containers.setStackInSlot(3, new ItemStack(Items.BUCKET, 2));
		tank.getTag().put("tankContainers", containers.serializeNBT());
		TankItemContents.setFlag(tank, "locked", true);
		tank.getTag().putUUID(StorageWrapper.UUID_TAG, UUID.randomUUID());
		TankItemContents.height(tank, 3);
		tank.setHoverName(Component.literal("Stored fluid"));
		return tank;
	}

	private static void assertTankUpgradePreservesCanonicalContentsAndComponents(ItemStack original, ItemStack result, TankBlockItem target) {
		assertTrue(result.is(target));
		assertEquals(TankItemContents.height(original), TankItemContents.height(result));
		assertEquals(original.getHoverName(), result.getHoverName());
		assertEquals(original.getTag().getUUID(StorageWrapper.UUID_TAG), result.getTag().getUUID(StorageWrapper.UUID_TAG));
		assertTrue(TankItemContents.flag(result, "locked", false));
		FluidStack fluid = FluidStack.loadFluidStackFromNBT(original.getTag().getCompound("fluidContents"));
		FluidStack restored = FluidStack.loadFluidStackFromNBT(result.getTag().getCompound("fluidContents"));
		assertTrue(fluid.isFluidEqual(restored));
		assertEquals(fluid.getAmount(), restored.getAmount());
		assertEquals(fluid.getTag(), restored.getTag());
		assertEquals(original.getTag().getCompound("tankUpgrades"), result.getTag().getCompound("tankUpgrades"));
		assertEquals(original.getTag().getCompound("tankContainers"), result.getTag().getCompound("tankContainers"));
		assertTrue(TankItemContents.hasContents(result));
		int slots = ((TankBlock) target.getBlock()).getTier().upgradeSlots();
		assertEquals(slots, result.getTag().getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG));
	}

	private static void assertUpgradeEligibility(ItemStack storage, Recipe<CraftingContainer> shaped, Recipe<CraftingContainer> shapeless) {
		CraftingContainer input = input(storage);
		boolean shapedUnlinkedMatches = shaped.matches(input, null);
		boolean shapelessUnlinkedMatches = shapeless.matches(input, null);
		LinkedStorageStackData.setEndpoint(storage, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		boolean shapedSecondaryMatches = shaped.matches(input, null);
		boolean shapelessSecondaryMatches = shapeless.matches(input, null);
		ItemStack shapedSecondaryResult = shaped.assemble(input, null);
		ItemStack shapelessSecondaryResult = shapeless.assemble(input, null);
		LinkedStorageStackData.setPrimaryEndpoint(storage, true);
		assertTrue(shapedUnlinkedMatches);
		assertTrue(shapelessUnlinkedMatches);
		assertFalse(shapedSecondaryMatches);
		assertFalse(shapelessSecondaryMatches);
		assertTrue(shapedSecondaryResult.isEmpty());
		assertTrue(shapelessSecondaryResult.isEmpty());
		assertTrue(shaped.matches(input, null));
		assertTrue(shapeless.matches(input, null));
	}

	private static CraftingContainer input(ItemStack storage) {
		return new TransientCraftingContainer(null, 1, 1, NonNullList.of(ItemStack.EMPTY, storage));
	}

	private static ResourceLocation id() {
		return new ResourceLocation("test", "tank_upgrade");
	}

	private static ShapedRecipe shaped(ItemStack storage) {
		return new ShapedRecipe(id(), "", CraftingBookCategory.MISC, 1, 1, NonNullList.of(Ingredient.EMPTY, Ingredient.of(storage.getItem())),
				new ItemStack(ModBlocks.NETHERITE_CHEST_ITEM.get()));
	}

	private static ShapelessRecipe shapeless(ItemStack storage) {
		return new ShapelessRecipe(id(), "", CraftingBookCategory.MISC, new ItemStack(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				NonNullList.of(Ingredient.EMPTY, Ingredient.of(storage.getItem())));
	}
}
