package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
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
		private MockedStatic<Config> config;
		private MockedStatic<GenericWoodStorageHelper> woods;
		private WoodType genericWood;

		@BeforeEach
		void setUp() {
			genericWood = mock(WoodType.class);
			when(genericWood.name()).thenReturn("other_mod:willow");
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

			List<ItemStack> variants = DyeRecipesMaker.getWoodStorageStackList(ModBlocks.CHEST.get());

			assertEquals(enabled, variants.stream().anyMatch(stack -> WoodStorageBlockItem.getWoodType(stack).filter(genericWood::equals).isPresent()));
		}
	}

	@Nested
	class GenericWoodStorageRecipeTest {
		private MockedStatic<GenericWoodStorageHelper> woods;
		private GenericWoodStorageRecipe recipe;
		private CraftingInput input;
		private ItemStack defaultResult;

		@BeforeEach
		void setUp() {
			ItemStack planks = new ItemStack(Items.BAMBOO_MOSAIC);
			input = CraftingInput.of(1, 1, List.of(planks));
			defaultResult = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), WoodType.SPRUCE);
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
			CraftingInput customWoodInput = CraftingInput.of(1, 1, List.of(new ItemStack(Items.OAK_PLANKS)));

			boolean result = recipe.matches(customWoodInput, null);

			assertFalse(result);
		}

		@Test
		void assembleDoesNotModifyRecipeDefaultResult() {
			woods.when(() -> GenericWoodStorageHelper.isGenericWoodEnabled(WoodType.BIRCH)).thenReturn(true);

			recipe.assemble(input, null);

			assertEquals(Optional.of(WoodType.SPRUCE), WoodStorageBlockItem.getWoodType(defaultResult));
		}

		private static GenericWoodStorageRecipe createRecipe(ItemStack defaultResult) {
			Ingredient planks = Ingredient.of(Items.BAMBOO_MOSAIC, Items.OAK_PLANKS);
			ShapedRecipePattern pattern = new ShapedRecipePattern(1, 1, NonNullList.of(Ingredient.EMPTY, planks), Optional.empty());
			return new GenericWoodStorageRecipe(new ShapedRecipe("", CraftingBookCategory.MISC, pattern, defaultResult));
		}
	}
}
