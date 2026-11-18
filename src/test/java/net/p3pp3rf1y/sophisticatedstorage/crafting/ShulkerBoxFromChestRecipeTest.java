package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.event.EventHooks;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ShulkerBoxFromChestRecipeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Stream.concat(conversions().flatMap(conversion -> Stream.of(conversion.chest(), conversion.shulker())),
				Stream.of(ModItems.HOPPER_UPGRADE.get(), Items.DIAMOND)).map(Item.class::cast).distinct()
				.forEach(item -> item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY));
	}

	@Test
	void matchesAndAssembleRejectTierMismatch() {
		ItemStack chest = new ItemStack(ModBlocks.DIAMOND_CHEST_ITEM.get());
		ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
		CraftingInput input = CraftingInput.of(1, 1, List.of(chest));

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input);

		assertFalse(matches);
		assertTrue(result.isEmpty());
	}

	@Test
	void matchesAndAssembleRejectPackedDoubleChest() {
		ItemStack chest = packedChest(ModBlocks.CHEST_ITEM.get());
		ChestBlockItem.setDoubleChest(chest, true);
		ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
		CraftingInput input = CraftingInput.of(1, 1, List.of(chest));

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input);

		assertFalse(matches);
		assertTrue(result.isEmpty());
	}

	@Test
	void assemblePreservesStorageUntilResultSlotPickup() {
		ItemStack chest = packedChest(ModBlocks.CHEST_ITEM.get());
		ItemStack chestSnapshot = chest.copy();
		UUID storageUuid = chest.get(ModCoreDataComponents.STORAGE_UUID);
		ContainerContents contents = new ContainerContents();
		contents.inventory().resize(37);
		contents.inventory().stacks().set(0, new ItemStack(Items.DIAMOND, 7));
		NonNullList<ItemStack> upgrades = NonNullList.withSize(3, ItemStack.EMPTY);
		upgrades.set(0, new ItemStack(ModItems.HOPPER_UPGRADE.get()));
		contents.upgrades().setStacks(upgrades);
		ItemContentsStorage.get().setContents(storageUuid, contents);

		try {
			ShulkerBoxFromChestRecipe recipe = recipe(ModBlocks.CHEST_ITEM.get(), ModBlocks.SHULKER_BOX_ITEM.get());
			AbstractContainerMenu menu = mock(AbstractContainerMenu.class);
			TransientCraftingContainer craftingContainer = new TransientCraftingContainer(menu, 1, 1);
			craftingContainer.setItem(0, chest);
			ItemStack preview = recipe.assemble(craftingContainer.asCraftInput());

			assertTrue(ItemStack.isSameItemSameComponents(chestSnapshot, craftingContainer.getItem(0)));
			ContainerContents previewContents = ItemContentsStorage.get().findContents(storageUuid).orElseThrow();
			assertSame(contents, previewContents);
			assertBackendContents(previewContents);

			ResultContainer resultContainer = new ResultContainer();
			resultContainer.setItem(0, preview);
			Player player = mock(Player.class);
			when(player.level()).thenReturn(mock(Level.class));
			ResultSlot resultSlot = new ResultSlot(player, craftingContainer, resultContainer, 0, 0, 0);
			try (MockedStatic<EventHooks> ignored = mockStatic(EventHooks.class)) {
				ItemStack craftedShulker = resultSlot.safeTake(1, 1, player);

				assertTrue(craftedShulker.is(ModBlocks.SHULKER_BOX_ITEM.get()));
				assertEquals(storageUuid, craftedShulker.get(ModCoreDataComponents.STORAGE_UUID));
				assertTrue(craftingContainer.getItem(0).isEmpty());
				assertTrue(resultContainer.getItem(0).isEmpty());
				assertBackendContents(ItemContentsStorage.get().findContents(storageUuid).orElseThrow());
			}
		} finally {
			ItemContentsStorage.get().removeContents(storageUuid);
		}
	}

	private static void assertBackendContents(ContainerContents contents) {
		assertEquals(7, contents.inventory().stacks().get(0).getCount());
		assertTrue(contents.inventory().stacks().get(0).is(Items.DIAMOND));
		assertTrue(contents.upgrades().stacks().get(0).is(ModItems.HOPPER_UPGRADE.get()));
	}

	@ParameterizedTest
	@MethodSource("conversions")
	void assemblePreservesPackedChestDataForMatchingTier(Conversion conversion) {
		ItemStack chest = packedChest(conversion.chest());
		ItemStack snapshot = chest.copy();
		ShulkerBoxFromChestRecipe recipe = recipe(conversion.chest(), conversion.shulker());
		CraftingInput input = CraftingInput.of(1, 1, List.of(chest));

		boolean matches = recipe.matches(input, null);
		ItemStack result = recipe.assemble(input);

		assertTrue(matches);
		assertTrue(result.is(conversion.shulker()));
		assertEquals(chest.get(ModCoreDataComponents.STORAGE_UUID), result.get(ModCoreDataComponents.STORAGE_UUID));
		assertEquals(chest.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS), result.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS));
		assertEquals(chest.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS), result.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS));
		assertEquals(chest.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
		assertEquals(chest.get(ModCoreDataComponents.MAIN_COLOR), result.get(ModCoreDataComponents.MAIN_COLOR));
		assertEquals(chest.get(ModCoreDataComponents.ACCENT_COLOR), result.get(ModCoreDataComponents.ACCENT_COLOR));
		assertEquals(chest.get(ModDataComponents.LOCKED), result.get(ModDataComponents.LOCKED));
		assertFalse(result.has(ModDataComponents.PACKED));
		assertFalse(result.has(ModDataComponents.WOOD_TYPE));
		assertFalse(result.has(ModDataComponents.DOUBLE_CHEST));
		assertTrue(ItemStack.isSameItemSameComponents(snapshot, chest));
	}

	private static ItemStack packedChest(Item chestItem) {
		ItemStack chest = new ItemStack(chestItem);
		chest.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		chest.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 37);
		chest.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
		chest.set(DataComponents.CUSTOM_NAME, Component.literal("Packed storage"));
		chest.set(ModCoreDataComponents.MAIN_COLOR, 0x123456);
		chest.set(ModCoreDataComponents.ACCENT_COLOR, 0x654321);
		chest.set(ModDataComponents.LOCKED, true);
		chest.set(ModDataComponents.PACKED, true);
		chest.set(ModDataComponents.WOOD_TYPE, WoodType.SPRUCE);
		chest.set(ModDataComponents.DOUBLE_CHEST, false);
		return chest;
	}

	private static ShulkerBoxFromChestRecipe recipe(Item chest, Item shulker) {
		return new ShulkerBoxFromChestRecipe(new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				new ShapedRecipePattern(1, 1, List.of(Optional.of(Ingredient.of(chest))), Optional.empty()),
				ItemStackTemplate.fromNonEmptyStack(new ItemStack(shulker))));
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
