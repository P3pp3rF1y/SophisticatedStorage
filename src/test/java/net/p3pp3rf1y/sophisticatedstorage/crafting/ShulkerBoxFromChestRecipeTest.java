package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.StorageRecipeViewerDisplaySpecTest;
import net.p3pp3rf1y.sophisticatedstorage.entity.StorageHolderBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShulkerBoxFromChestRecipeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		StorageRecipeViewerDisplaySpecTest.initializeStorageRegistriesForTests();
	}

	@Test
	void matchesAndAssembleRejectTierMismatch() {
		ItemStack chest = new ItemStack(ModBlocks.DIAMOND_CHEST_ITEM.get());
		ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
		CraftingContainer input = input(chest);

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input, null);

		assertFalse(matches);
		assertTrue(result.isEmpty());
	}

	@Test
	void matchesAndAssembleRejectPackedDoubleChest() {
		ItemStack chest = packedChest(ModBlocks.CHEST_ITEM.get());
		ChestBlockItem.setDoubleChest(chest, true);
		ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
		CraftingContainer input = input(chest);

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input, null);

		assertFalse(matches);
		assertTrue(result.isEmpty());
	}

	@Test
	void assemblePreservesStorageUntilResultSlotPickup() {
		ItemStack chest = packedChest(ModBlocks.CHEST_ITEM.get());
		UUID storageUuid = StorageBlockItem.getContentsUuid(chest).orElseThrow();
		ItemContentsStorage storage = ItemContentsStorage.get();
		storage.setStorageContents(storageUuid, backendContents());
		try {
			CompoundTag backendSnapshot = storage.getStorageContents(storageUuid).orElseThrow().copy();
			ItemStack chestSnapshot = chest.copy();
			ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
			CraftingContainer craftingContainer = new TransientCraftingContainer(mock(AbstractContainerMenu.class), 1, 1);
			craftingContainer.setItem(0, chest);
			ItemStack preview = recipe.assemble(craftingContainer, null);

			assertEquals(storageUuid, StorageBlockItem.getContentsUuid(preview).orElseThrow());
			assertTrue(ItemStack.isSameItemSameTags(chestSnapshot, craftingContainer.getItem(0)));
			assertEquals(backendSnapshot, storage.getStorageContents(storageUuid).orElseThrow());
			assertBackendContents(chest);

			ResultContainer resultContainer = new ResultContainer();
			resultContainer.setItem(0, preview);
			Player player = mock(Player.class);
			Level level = mock(Level.class);
			RecipeManager recipeManager = mock(RecipeManager.class);
			when(player.level()).thenReturn(level);
			when(level.getRecipeManager()).thenReturn(recipeManager);
			when(recipeManager.getRemainingItemsFor(eq(RecipeType.CRAFTING), any(CraftingContainer.class), same(level)))
					.thenReturn(NonNullList.withSize(1, ItemStack.EMPTY));
			ResultSlot resultSlot = new ResultSlot(player, craftingContainer, resultContainer, 0, 0, 0);
			ItemStack craftedShulker = resultSlot.safeTake(1, 1, player);

			assertTrue(craftedShulker.is(ModBlocks.SHULKER_BOX_ITEM.get()));
			assertEquals(storageUuid, StorageBlockItem.getContentsUuid(craftedShulker).orElseThrow());
			assertTrue(craftingContainer.getItem(0).isEmpty());
			assertTrue(resultContainer.getItem(0).isEmpty());
			assertEquals(backendSnapshot, storage.getStorageContents(storageUuid).orElseThrow());
			assertBackendContents(craftedShulker);
		} finally {
			storage.removeStorageContents(storageUuid);
		}
	}

	private static void assertBackendContents(ItemStack stack) {
		UUID uuid = StorageBlockItem.getContentsUuid(stack).orElseThrow();
		CompoundTag contents = ItemContentsStorage.get().getStorageContents(uuid).orElseThrow().getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG)
				.getCompound(StorageWrapper.CONTENTS_TAG);
		ItemStackHandler inventory = new ItemStackHandler();
		inventory.deserializeNBT(contents.getCompound(InventoryHandler.INVENTORY_TAG));
		ItemStackHandler upgrades = new ItemStackHandler();
		upgrades.deserializeNBT(contents.getCompound(UpgradeHandler.UPGRADE_INVENTORY_TAG));

		assertEquals(7, inventory.getStackInSlot(0).getCount());
		assertTrue(inventory.getStackInSlot(0).is(Items.DIAMOND));
		assertTrue(upgrades.getStackInSlot(0).is(ModItems.HOPPER_UPGRADE.get()));
	}

	private static CompoundTag backendContents() {
		ItemStackHandler inventory = new ItemStackHandler(37);
		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
		ItemStackHandler upgrades = new ItemStackHandler(3);
		upgrades.setStackInSlot(0, new ItemStack(ModItems.HOPPER_UPGRADE.get()));
		CompoundTag contents = new CompoundTag();
		contents.put(InventoryHandler.INVENTORY_TAG, inventory.serializeNBT());
		contents.put(UpgradeHandler.UPGRADE_INVENTORY_TAG, upgrades.serializeNBT());
		CompoundTag wrapper = new CompoundTag();
		wrapper.put(StorageWrapper.CONTENTS_TAG, contents);
		CompoundTag backend = new CompoundTag();
		backend.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, wrapper);
		return backend;
	}

	@ParameterizedTest
	@MethodSource("conversions")
	void assemblePreservesPackedChestDataForMatchingTier(Conversion conversion) {
		ItemStack chest = packedChest(conversion.chest());
		ItemStack snapshot = chest.copy();
		ShulkerBoxFromChestRecipe recipe = recipe(conversion.chest(), conversion.shulker());
		CraftingContainer input = input(chest);

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input, null);

		assertTrue(matches);
		assertTrue(result.is(conversion.shulker()));
		CompoundTag expected = chest.getTag().copy();
		expected.remove(WoodStorageBlockItem.PACKED_TAG);
		expected.remove(WoodStorageBlockItem.WOOD_TYPE_TAG);
		expected.remove(ChestBlockItem.DOUBLE_CHEST_TAG);
		assertEquals(expected, result.getTag());
		assertEquals(StorageBlockItem.getContentsUuid(chest), StorageBlockItem.getContentsUuid(result));
		assertEquals(chest.getTag().getInt(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG), result.getTag().getInt(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG));
		assertEquals(chest.getTag().getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG), result.getTag().getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG));
		assertEquals(chest.getHoverName(), result.getHoverName());
		assertEquals(StorageBlockItem.getMainColorFromStack(chest), StorageBlockItem.getMainColorFromStack(result));
		assertEquals(StorageBlockItem.getAccentColorFromStack(chest), StorageBlockItem.getAccentColorFromStack(result));
		assertTrue(StorageBlockItem.isLocked(result));
		assertFalse(WoodStorageBlockItem.isPacked(result));
		assertTrue(ItemStack.isSameItemSameTags(snapshot, chest));
	}

	private static ItemStack packedChest(Item chestItem) {
		ItemStack chest = new ItemStack(chestItem);
		chest.getOrCreateTag().putUUID(StorageWrapper.UUID_TAG, UUID.randomUUID());
		StorageBlockItem.setNumberOfInventorySlots(chest, 81);
		StorageBlockItem.setNumberOfUpgradeSlots(chest, 5);
		chest.setHoverName(Component.literal("Packed storage"));
		((StorageBlockItem) chestItem).setMainColor(chest, 0x123456);
		((StorageBlockItem) chestItem).setAccentColor(chest, 0x654321);
		chest.getTag().putBoolean(StorageHolderBase.LOCKED_TAG, true);
		WoodStorageBlockItem.setPacked(chest, true);
		WoodStorageBlockItem.setWoodType(chest, WoodType.SPRUCE);
		ChestBlockItem.setDoubleChest(chest, false);
		return chest;
	}

	private static CraftingContainer input(ItemStack chest) {
		return new TransientCraftingContainer(null, 1, 1, NonNullList.of(ItemStack.EMPTY, chest));
	}

	private static ShulkerBoxFromChestRecipe recipe(Item chest, Item shulker) {
		return new ShulkerBoxFromChestRecipe(new ShapedRecipe(new ResourceLocation("test", "shulker_from_chest"), "", CraftingBookCategory.MISC, 1, 1,
				NonNullList.of(Ingredient.EMPTY, Ingredient.of(chest)), new ItemStack(shulker)));
	}

	private static Stream<Conversion> conversions() {
		return Stream.of(new Conversion(ModBlocks.CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get()),
				new Conversion(ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.COPPER_SHULKER_BOX_ITEM.get()),
				new Conversion(ModBlocks.IRON_CHEST_ITEM.get(), ModBlocks.IRON_SHULKER_BOX_ITEM.get()),
				new Conversion(ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
				new Conversion(ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				new Conversion(ModBlocks.NETHERITE_CHEST_ITEM.get(), ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()));
	}

	private record Conversion(Item chest, Item shulker) {
	}
}
