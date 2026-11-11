package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenericWoodStorageModFilterTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Nested
	class GenericWoodStorageHelperTest {
		private static final WoodType REGISTERED_GENERIC_WOOD = WoodType.register(new WoodType("other_mod:willow", BlockSetType.OAK));
		private MockedStatic<Config> config;
		private MockedStatic<GenericWoodStorageHelper> woods;
		private WoodType genericWood;

		@BeforeEach
		void setUp() {
			genericWood = REGISTERED_GENERIC_WOOD;
			GenericWoodStorageHelper.GenericWoodInfo info = new GenericWoodStorageHelper.GenericWoodInfo(genericWood, Blocks.OAK_PLANKS, Blocks.OAK_SLAB);
			config = mockStatic(Config.class);
			woods = mockStatic(GenericWoodStorageHelper.class, CALLS_REAL_METHODS);
			woods.when(GenericWoodStorageHelper::getGenericWoodTypes).thenReturn(List.of(genericWood));
			woods.when(() -> GenericWoodStorageHelper.getGenericWoodInfo(genericWood)).thenReturn(Optional.of(info));
		}

		@AfterEach
		void tearDown() {
			woods.close();
			config.close();
		}

		@ParameterizedTest
		@ValueSource(booleans = {false, true})
		void isGenericWoodEnabledUsesResolvedPlanksNamespaceWhenNamespacesDiffer(boolean enabled) {
			config.when(() -> Config.isGenericWoodStorageModEnabled("minecraft")).thenReturn(enabled);
			config.when(() -> Config.isGenericWoodStorageModEnabled("other_mod")).thenReturn(!enabled);

			boolean result = GenericWoodStorageHelper.isGenericWoodEnabled(genericWood);

			assertEquals(enabled, result);
		}

		@Test
		void getEnabledGenericWoodTypesIncludesEnabledWood() {
			config.when(() -> Config.isGenericWoodStorageModEnabled("minecraft")).thenReturn(true);

			var result = GenericWoodStorageHelper.getEnabledGenericWoodTypes();

			assertEquals(List.of(genericWood), List.copyOf(result));
		}

		@Test
		void getEnabledGenericWoodTypesExcludesDisabledWood() {
			config.when(() -> Config.isGenericWoodStorageModEnabled("minecraft")).thenReturn(false);

			var result = GenericWoodStorageHelper.getEnabledGenericWoodTypes();

			assertTrue(result.isEmpty());
		}

		@Test
		void isGenericWoodRecognizesDisabledWood() {
			config.when(() -> Config.isGenericWoodStorageModEnabled("minecraft")).thenReturn(false);

			boolean result = GenericWoodStorageHelper.isGenericWood(genericWood);

			assertTrue(result);
		}

		@ParameterizedTest
		@ValueSource(booleans = {false, true})
		void getWoodStorageStackListIncludesOnlyEnabledWood(boolean enabled) {
			config.when(() -> Config.isGenericWoodStorageModEnabled("minecraft")).thenReturn(enabled);

			ChestBlock chest = mock(ChestBlock.class);
			when(chest.asItem()).thenReturn(Items.CHEST);
			List<ItemStack> variants = DyeRecipesMaker.getWoodStorageStackList(chest);

			assertEquals(enabled, variants.stream().anyMatch(stack -> WoodStorageBlockItem.getWoodType(stack).filter(genericWood::equals).isPresent()));
		}
	}

	@Nested
	class GenericWoodStorageRecipeTest {
		private MockedStatic<GenericWoodStorageHelper> woods;
		private GenericWoodStorageRecipe recipe;
		private CraftingContainer input;
		private ItemStack defaultResult;

		@BeforeEach
		void setUp() {
			ItemStack planks = new ItemStack(Items.BAMBOO_MOSAIC);
			input = createCraftingInput(planks);
			defaultResult = WoodStorageBlockItem.setWoodType(new ItemStack(Items.CHEST), WoodType.SPRUCE);
			recipe = createRecipe(defaultResult);
			woods = mockStatic(GenericWoodStorageHelper.class);
			woods.when(() -> GenericWoodStorageHelper.getWoodTypeForPlanks(planks)).thenReturn(Optional.of(WoodType.BIRCH));
		}

		@AfterEach
		void tearDown() {
			woods.close();
		}

		@Test
		void assembleReturnsResolvedWoodVariantWhenEnabled() {
			woods.when(() -> GenericWoodStorageHelper.isGenericWoodEnabled(WoodType.BIRCH)).thenReturn(true);

			ItemStack result = recipe.assemble(input, null);

			assertEquals(Optional.of(WoodType.BIRCH), WoodStorageBlockItem.getWoodType(result));
		}

		@Test
		void assembleReturnsDefaultWoodVariantWhenDisabled() {
			woods.when(() -> GenericWoodStorageHelper.isGenericWoodEnabled(WoodType.BIRCH)).thenReturn(false);

			ItemStack result = recipe.assemble(input, null);

			assertEquals(Optional.of(WoodType.SPRUCE), WoodStorageBlockItem.getWoodType(result));
		}

		@Test
		void matchesAcceptsDisabledWood() {
			woods.when(() -> GenericWoodStorageHelper.isGenericWoodEnabled(WoodType.BIRCH)).thenReturn(false);

			boolean result = recipe.matches(input, null);

			assertTrue(result);
		}

		@Test
		void matchesRejectsWoodWithCustomTextures() {
			CraftingContainer customWoodInput = createCraftingInput(new ItemStack(Items.OAK_PLANKS));

			boolean result = recipe.matches(customWoodInput, null);

			assertFalse(result);
		}

		@Test
		void assembleDoesNotModifyRecipeDefaultResult() {
			woods.when(() -> GenericWoodStorageHelper.isGenericWoodEnabled(WoodType.BIRCH)).thenReturn(true);

			recipe.assemble(input, null);

			assertEquals(Optional.of(WoodType.SPRUCE), WoodStorageBlockItem.getWoodType(defaultResult));
		}

		private static CraftingContainer createCraftingInput(ItemStack stack) {
			CraftingContainer craftingInput = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
				@Override
				public ItemStack quickMoveStack(Player player, int index) {
					return ItemStack.EMPTY;
				}

				@Override
				public boolean stillValid(Player player) {
					return false;
				}
			}, 1, 1);
			craftingInput.setItem(0, stack);
			return craftingInput;
		}

		private static GenericWoodStorageRecipe createRecipe(ItemStack defaultResult) {
			Ingredient planks = Ingredient.of(Items.BAMBOO_MOSAIC, Items.OAK_PLANKS);
			return new GenericWoodStorageRecipe(new ShapedRecipe(new ResourceLocation("test", "generic_wood_storage"), "", CraftingBookCategory.MISC, 1, 1,
					NonNullList.of(Ingredient.EMPTY, planks), defaultResult));
		}
	}
}
