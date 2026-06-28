package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.*;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("recipeViewerRegression")
class StorageRecipeViewerDisplaySpecTest {
	@Test
	void tierUpgradeUsagePreservesFocusedBarrelComponentsAndDoesNotDuplicate() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack tintedBasicBarrel = tintedStack(ModBlocks.BARREL_ITEM.get());

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, tintedBasicBarrel);

		assertEquals(2, usages.size());
		assertTrue(usages.stream().allMatch(usage -> ItemStack.isSameItemSameComponents(tintedBasicBarrel, usage.inputs().get(4))));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameComponents(tintedStack(ModBlocks.COPPER_BARREL_ITEM.get()), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameComponents(tintedStack(ModBlocks.IRON_BARREL_ITEM.get()), usage.firstOutput())));
	}

	@Test
	void tierUpgradeRecipePreservesFocusedBarrelResultComponentsAndDoesNotDuplicate() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack tintedIronBarrel = tintedStack(ModBlocks.IRON_BARREL_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, tintedIronBarrel);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItemSameComponents(tintedStack(ModBlocks.BARREL_ITEM.get()), recipe.inputs().get(4))));
		assertTrue(recipes.stream()
				.anyMatch(recipe -> ItemStack.isSameItemSameComponents(tintedStack(ModBlocks.COPPER_BARREL_ITEM.get()), recipe.inputs().get(4))));
		assertTrue(recipes.stream().allMatch(recipe -> ItemStack.isSameItemSameComponents(tintedIronBarrel, recipe.firstOutput())));
	}

	@Test
	void tierUpgradeUsagePreservesFocusedWoodBarrelComponents() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack spruceBasicBarrel = woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE);

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, spruceBasicBarrel);
		List<CraftingDisplayVariant> tierUsages = usages.stream().filter(usage -> usage.inputs().size() > 4).toList();

		assertEquals(3, usages.size());
		assertEquals(2, tierUsages.size());
		assertTrue(tierUsages.stream().allMatch(usage -> ItemStack.isSameItemSameComponents(spruceBasicBarrel, usage.inputs().get(4))));
		assertTrue(tierUsages.stream().anyMatch(
				usage -> ItemStack.isSameItemSameComponents(woodStorageStack(ModBlocks.COPPER_BARREL_ITEM.get(), WoodType.SPRUCE), usage.firstOutput())));
		assertTrue(tierUsages.stream().anyMatch(
				usage -> ItemStack.isSameItemSameComponents(woodStorageStack(ModBlocks.IRON_BARREL_ITEM.get(), WoodType.SPRUCE), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameComponents(flatTop(spruceBasicBarrel), usage.firstOutput())));
	}

	@Test
	void flatBarrelRecipeCanBeFocusedByFlatResultWithSourceIngredient() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		CraftingDisplaySpec flatBarrelSpec = getFlatBarrelSpec(catalog);
		ItemStack spruceBasicBarrel = woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE);
		ItemStack flatSpruceBasicBarrel = flatTop(spruceBasicBarrel);

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, flatSpruceBasicBarrel);

		assertEquals(1, recipes.size());
		assertSameStack(spruceBasicBarrel, recipes.getFirst().inputs().getFirst());
		assertSameStack(flatSpruceBasicBarrel, recipes.getFirst().firstOutput());
		assertFalse(flatBarrelSpec.getInputSlots(recipes).getFirst().isEmpty());
	}

	@Test
	void flatBarrelUsageShowsUnflattenRecipe() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack spruceBasicBarrel = woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE);
		ItemStack flatSpruceBasicBarrel = flatTop(spruceBasicBarrel);

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, flatSpruceBasicBarrel).stream()
				.filter(usage -> ItemStack.isSameItemSameComponents(spruceBasicBarrel, usage.firstOutput())).toList();

		assertEquals(1, usages.size());
		assertSameStack(flatSpruceBasicBarrel, usages.getFirst().inputs().getFirst());
		assertSameStack(spruceBasicBarrel, usages.getFirst().firstOutput());
	}

	@Test
	void tintedFlatBarrelUsageHasSourceIngredientSlot() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		CraftingDisplaySpec flatBarrelSpec = getFlatBarrelSpec(catalog);
		ItemStack tintedBarrel = yellowLimeBarrel();

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, tintedBarrel).stream()
				.filter(usage -> ItemStack.isSameItemSameComponents(flatTop(tintedBarrel), usage.firstOutput())).toList();

		assertEquals(1, usages.size());
		assertSameStack(tintedBarrel, usages.getFirst().inputs().getFirst());
		assertFalse(flatBarrelSpec.getInputSlots(usages).getFirst().isEmpty());
	}

	@Test
	void tierUpgradeRecipePreservesFocusedWoodBarrelResultComponents() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack spruceIronBarrel = woodStorageStack(ModBlocks.IRON_BARREL_ITEM.get(), WoodType.SPRUCE);

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, spruceIronBarrel);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(
				recipe -> ItemStack.isSameItemSameComponents(woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE), recipe.inputs().get(4))));
		assertTrue(recipes.stream().anyMatch(
				recipe -> ItemStack.isSameItemSameComponents(woodStorageStack(ModBlocks.COPPER_BARREL_ITEM.get(), WoodType.SPRUCE), recipe.inputs().get(4))));
		assertTrue(recipes.stream().allMatch(recipe -> ItemStack.isSameItemSameComponents(spruceIronBarrel, recipe.firstOutput())));
	}

	@Test
	void fullyTintedWoodStorageKeepsWoodTypeButHidesItInName() {
		ItemStack spruceChest = woodStorageStack(ModBlocks.CHEST_ITEM.get(), WoodType.SPRUCE);
		StorageBlockItem storageBlockItem = (StorageBlockItem) spruceChest.getItem();

		storageBlockItem.setMainColor(spruceChest, 0x336699);
		storageBlockItem.setAccentColor(spruceChest, 0x99CC33);

		assertTrue(WoodStorageBlockItem.getWoodType(spruceChest).filter(WoodType.SPRUCE::equals).isPresent());
		assertEquals(WoodStorageBlockItem.getDisplayName(ModBlocks.CHEST_ITEM.get().getDescriptionId(), null), spruceChest.getHoverName());
	}

	@Test
	void materialDecoratedBarrelKeepsWoodTypeButHidesItInName() {
		ItemStack spruceBarrel = woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE);
		Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
		materials.put(BarrelMaterial.ALL, ResourceLocation.parse("minecraft:oak_planks"));

		ItemStack decoratedBarrel = DecorationTableBlockEntity.STORAGE_DECORATOR.decorateWithMaterials(spruceBarrel, materials);

		assertTrue(WoodStorageBlockItem.getWoodType(decoratedBarrel).filter(WoodType.SPRUCE::equals).isPresent());
		assertFalse(BarrelBlockItem.getMaterials(decoratedBarrel).isEmpty());
		assertEquals(WoodStorageBlockItem.getDisplayName(ModBlocks.BARREL_ITEM.get().getDescriptionId(), null), decoratedBarrel.getHoverName());
	}

	@Test
	void ironTierRecipeCanComeFromBasicAndCopperBarrels() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack ironBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, ironBarrel);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItem(new ItemStack(ModBlocks.BARREL_ITEM.get()), recipe.inputs().get(4))));
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItem(new ItemStack(ModBlocks.COPPER_BARREL_ITEM.get()), recipe.inputs().get(4))));
	}

	@Test
	void untintedStorageTierUpgradeRecipesAndUsagesAreAvailable() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack basicBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
		ItemStack ironBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());

		List<CraftingDisplayVariant> basicBarrelUsages = getCraftingUsagesFor(catalog, basicBarrel);
		List<CraftingDisplayVariant> ironBarrelRecipes = getCraftingRecipesFor(catalog, ironBarrel);

		assertEquals(2, basicBarrelUsages.size());
		assertTrue(basicBarrelUsages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.COPPER_BARREL_ITEM.get()), usage.firstOutput())));
		assertTrue(basicBarrelUsages.stream().anyMatch(usage -> ItemStack.isSameItem(ironBarrel, usage.firstOutput())));
		assertEquals(2, ironBarrelRecipes.size());
		assertTrue(ironBarrelRecipes.stream().anyMatch(recipe -> ItemStack.isSameItem(basicBarrel, recipe.inputs().get(4))));
		assertTrue(ironBarrelRecipes.stream().allMatch(recipe -> ItemStack.isSameItem(ironBarrel, recipe.firstOutput())));
	}

	@Test
	void baseShulkerBoxUsagesOnlyShowCopperAndIronTierUpgrades() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack shulkerBox = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, shulkerBox);

		assertEquals(2, usages.size());
		assertTrue(usages.stream().allMatch(usage -> ItemStack.isSameItem(shulkerBox, usage.inputs().get(4))));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.COPPER_SHULKER_BOX_ITEM.get()), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get()), usage.firstOutput())));
	}

	@Test
	void higherTierShulkerBoxUsagesOnlyShowNextTierUpgrade() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack ironShulkerBox = new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get());

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, ironShulkerBox);

		assertEquals(1, usages.size());
		assertTrue(ItemStack.isSameItem(ironShulkerBox, usages.getFirst().inputs().get(4)));
		assertTrue(ItemStack.isSameItem(new ItemStack(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()), usages.getFirst().firstOutput()));
	}

	@Test
	void ironShulkerBoxRecipesOnlyShowBaseAndCopperTierUpgrades() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack shulkerBox = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		ItemStack copperShulkerBox = new ItemStack(ModBlocks.COPPER_SHULKER_BOX_ITEM.get());
		ItemStack ironShulkerBox = new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, ironShulkerBox);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItem(shulkerBox, recipe.inputs().get(4))));
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItem(copperShulkerBox, recipe.inputs().get(4))));
		assertTrue(recipes.stream().allMatch(recipe -> ItemStack.isSameItem(ironShulkerBox, recipe.firstOutput())));
	}

	@Test
	void higherTierShulkerBoxRecipesOnlyShowPreviousTierUpgrade() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack ironShulkerBox = new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get());
		ItemStack goldShulkerBox = new ItemStack(ModBlocks.GOLD_SHULKER_BOX_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, goldShulkerBox);

		assertEquals(1, recipes.size());
		assertTrue(ItemStack.isSameItem(ironShulkerBox, recipes.getFirst().inputs().get(4)));
		assertTrue(ItemStack.isSameItem(goldShulkerBox, recipes.getFirst().firstOutput()));
	}

	@Test
	void globalTierUpgradeDisplaysDoNotIncludeGeneratedTintedVariants() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();

		List<CraftingDisplayVariant> globalVariants = catalog.getGlobalCraftingDisplays().stream().flatMap(view -> view.variants().stream()).toList();

		assertFalse(globalVariants.isEmpty());
		assertTrue(globalVariants.stream().noneMatch(variant -> isTinted(variant.firstOutput())));
		assertTrue(globalVariants.stream().noneMatch(variant -> variant.inputs().stream().anyMatch(StorageRecipeViewerDisplaySpecTest::isTinted)));
	}

	@Test
	void catalogOwnsExactTierUpgradeRecipeReplacement() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		CraftingDisplaySpec spec = catalog.getCraftingSpecs().getFirst();
		RecipeHolder<?> replacedRecipe = new RecipeHolder<>(
				ClientRecipeHelper.recipeKey(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "iron_barrel")),
				spec.recipeHolder(spec.getGlobalDisplays().getFirst()).value());
		RecipeHolder<?> unrelatedRecipe = new RecipeHolder<>(ClientRecipeHelper.recipeKey(ResourceLocation.parse("test:unrelated")),
				spec.recipeHolder(spec.getGlobalDisplays().getFirst()).value());

		assertTrue(catalog.replacesCraftingRecipe(replacedRecipe));
		assertTrue(catalog.getCraftingDisplaySpecReplacing(replacedRecipe).isPresent());
		assertFalse(catalog.replacesCraftingRecipe(unrelatedRecipe));
	}

	@Test
	void tierUsageChainCanReachNetheriteUpgrade() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();

		ItemStack ironBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());
		ItemStack goldBarrel = getCraftingUsagesFor(catalog, ironBarrel).stream().map(CraftingDisplayVariant::firstOutput)
				.filter(stack -> stack.is(ModBlocks.GOLD_BARREL_ITEM.get())).findFirst().orElseThrow();
		ItemStack diamondBarrel = getCraftingUsagesFor(catalog, goldBarrel).stream().map(CraftingDisplayVariant::firstOutput)
				.filter(stack -> stack.is(ModBlocks.DIAMOND_BARREL_ITEM.get())).findFirst().orElseThrow();
		List<CraftingDisplayVariant> netheriteUsages = getCraftingUsagesFor(catalog, diamondBarrel);

		assertEquals(1, netheriteUsages.size());
		assertTrue(ItemStack.isSameItem(new ItemStack(ModBlocks.NETHERITE_BARREL_ITEM.get()), netheriteUsages.getFirst().firstOutput()),
				"Expected netherite barrel but got " + netheriteUsages.getFirst().firstOutput());
	}

	@Test
	void focusedHigherTierSingleColorDyeRecipeNarrowsDyeInputAndResult() {
		SingleColorDyeRecipeSpec ironBarrelDyeSpec = createCatalog().getGroupedCraftingSpecs().stream()
				.filter(spec -> spec.sourceStacks().stream().anyMatch(stack -> stack.is(ModBlocks.IRON_BARREL_ITEM.get()))).findFirst().orElseThrow();
		ItemStack redIronBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());
		if (ModBlocks.IRON_BARREL_ITEM.get() instanceof StorageBlockItem storageBlockItem) {
			storageBlockItem.setMainColor(redIronBarrel, DyeColor.RED.getTextureDiffuseColor());
			storageBlockItem.setAccentColor(redIronBarrel, DyeColor.RED.getTextureDiffuseColor());
		}

		List<RecipeHolder<GroupedCraftingRecipe>> focusedRecipes = ironBarrelDyeSpec.getRecipesFor(redIronBarrel);

		assertEquals(1, focusedRecipes.size());
		GroupedCraftingRecipe focusedRecipe = focusedRecipes.getFirst().value();
		assertEquals(1, focusedRecipe.getVariants().size());
		assertEquals(2, focusedRecipe.getInputSlots().size());
		assertEquals(1, focusedRecipe.getInputSlots().get(1).size());
		assertSameStack(new ItemStack(DyeItem.byColor(DyeColor.RED)), focusedRecipe.getInputSlots().get(1).getFirst());
		assertSameStack(redIronBarrel, focusedRecipe.getResultStacks().getFirst());
	}

	@Test
	void singleChestUsesDoNotShowDoubleChestTierUpgradeRecipes() {
		IRecipeViewerDisplayCatalog catalog = createChestCatalog();

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, singleChest(ModBlocks.CHEST_ITEM.get()));

		assertEquals(2, usages.size());
		assertTrue(
				usages.stream().allMatch(usage -> !ChestBlockItem.isDoubleChest(usage.inputs().get(4)) && !ChestBlockItem.isDoubleChest(usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.COPPER_CHEST_ITEM.get()), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.IRON_CHEST_ITEM.get()), usage.firstOutput())));
	}

	@Test
	void doubleChestUsesOnlyShowDoubleChestTierUpgradeRecipes() {
		IRecipeViewerDisplayCatalog catalog = createChestCatalog();

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, doubleChest(ModBlocks.CHEST_ITEM.get()));

		assertEquals(2, usages.size());
		assertTrue(usages.stream().allMatch(usage -> ChestBlockItem.isDoubleChest(usage.inputs().get(4)) && ChestBlockItem.isDoubleChest(usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.COPPER_CHEST_ITEM.get()), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItem(new ItemStack(ModBlocks.IRON_CHEST_ITEM.get()), usage.firstOutput())));
	}

	@Test
	void upgradeIngredientUsesShowSingleAndDoubleChestTierUpgradeRecipesWithCorrectSources() {
		IRecipeViewerDisplayCatalog catalog = createChestCatalog();

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, new ItemStack(Items.IRON_INGOT));
		List<RecipeHolder<CraftingRecipe>> recipeHolders = catalog.getCraftingUsagesFor(new ItemStack(Items.IRON_INGOT)).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder)).toList();

		assertEquals(16, usages.size());
		assertEquals(16, recipeHolders.size());
		assertTrue(usages.stream()
				.anyMatch(variant -> !ChestBlockItem.isDoubleChest(variant.inputs().get(4)) && !ChestBlockItem.isDoubleChest(variant.firstOutput())));
		assertTrue(usages.stream()
				.anyMatch(variant -> ChestBlockItem.isDoubleChest(variant.inputs().get(4)) && ChestBlockItem.isDoubleChest(variant.firstOutput())));
	}

	private static ItemStack tintedStack(Item item) {
		ItemStack stack = new ItemStack(item);
		if (item instanceof StorageBlockItem storageBlockItem) {
			storageBlockItem.setMainColor(stack, 0x336699);
			storageBlockItem.setAccentColor(stack, 0x99CC33);
		}
		return stack;
	}

	private static ItemStack woodStorageStack(Item item, WoodType woodType) {
		return WoodStorageBlockItem.setWoodType(new ItemStack(item), woodType);
	}

	private static ItemStack flatTop(ItemStack stack) {
		ItemStack flatTopStack = stack.copy();
		BarrelBlockItem.toggleFlatTop(flatTopStack);
		return flatTopStack;
	}

	private static ItemStack yellowLimeBarrel() {
		ItemStack stack = new ItemStack(ModBlocks.BARREL_ITEM.get());
		if (stack.getItem() instanceof StorageBlockItem storageBlockItem) {
			storageBlockItem.setMainColor(stack, DyeColor.YELLOW.getTextureDiffuseColor());
			storageBlockItem.setAccentColor(stack, DyeColor.LIME.getTextureDiffuseColor());
		}
		return stack;
	}

	private static CraftingDisplaySpec getFlatBarrelSpec(IRecipeViewerDisplayCatalog catalog) {
		return catalog.getCraftingSpecs().stream().filter(spec -> spec.id().equals(SophisticatedStorage.getRL("flat_top_barrel_toggle"))).findFirst()
				.orElseThrow();
	}

	private static IRecipeViewerDisplayCatalog createCatalog() {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		try (TestRecipeResources.LoadedResources resources = TestRecipeResources.load();
				MockedStatic<ClientRecipeHelper> clientRecipeHelper = Mockito.mockStatic(ClientRecipeHelper.class, Mockito.CALLS_REAL_METHODS)) {
			mockClientRecipeHelper(clientRecipeHelper, resources);
			StorageRecipeViewerDisplays.register(catalog, IRecipeViewerDisplayContext.empty());
		}
		return catalog;
	}

	private static IRecipeViewerDisplayCatalog createChestCatalog() {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		try (TestRecipeResources.LoadedResources resources = TestRecipeResources.load();
				MockedStatic<ClientRecipeHelper> clientRecipeHelper = Mockito.mockStatic(ClientRecipeHelper.class, Mockito.CALLS_REAL_METHODS)) {
			mockClientRecipeHelper(clientRecipeHelper, resources);
			StorageRecipeViewerDisplays.register(catalog, IRecipeViewerDisplayContext.empty());
		}
		return catalog;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void mockClientRecipeHelper(MockedStatic<ClientRecipeHelper> clientRecipeHelper, TestRecipeResources.LoadedResources resources) {
		RecipeManager recipeManager = resources.recipeManager();
		clientRecipeHelper
				.when(() -> ClientRecipeHelper.transformAllRecipeHoldersOfTypeIntoMultiple(Mockito.eq(RecipeType.CRAFTING), Mockito.any(), Mockito.any()))
				.thenAnswer(invocation -> {
					RecipeType recipeType = invocation.getArgument(0);
					Class recipeClass = invocation.getArgument(1);
					return ClientRecipeHelper.transformAllRecipeHoldersOfTypeIntoMultiple(recipeManager, recipeType, recipeClass, invocation.getArgument(2));
				});
		clientRecipeHelper.when(() -> ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(Mockito.eq(RecipeType.CRAFTING), Mockito.any(), Mockito.any()))
				.thenAnswer(invocation -> {
					RecipeType recipeType = invocation.getArgument(0);
					Class recipeClass = invocation.getArgument(1);
					return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(recipeManager, recipeType, recipeClass, invocation.getArgument(2));
				});
		clientRecipeHelper.when(() -> ClientRecipeHelper.transformAllRecipeHoldersOfType(Mockito.eq(RecipeType.CRAFTING), Mockito.any(), Mockito.any()))
				.thenAnswer(invocation -> {
					RecipeType recipeType = invocation.getArgument(0);
					Class recipeClass = invocation.getArgument(1);
					return ClientRecipeHelper.transformAllRecipeHoldersOfType(recipeManager, recipeType, recipeClass, invocation.getArgument(2));
				});
		clientRecipeHelper.when(() -> ClientRecipeHelper.assemble(Mockito.any(), Mockito.any()))
				.thenAnswer(invocation -> assembleRecipe(invocation.getArgument(0), invocation.getArgument(1), resources.registryLookup()));
		clientRecipeHelper.when(() -> ClientRecipeHelper.getResultItem(Mockito.any()))
				.thenAnswer(invocation -> ClientRecipeHelper.getResultItem(invocation.getArgument(0), resources.registryLookup()));
	}

	private static ItemStack assembleRecipe(Recipe<CraftingInput> recipe, CraftingInput input, HolderLookup.Provider registryLookup) {
		if (recipe instanceof StorageTierUpgradeRecipe || recipe instanceof StorageTierUpgradeShapelessRecipe || recipe instanceof DoubleChestTierUpgradeRecipe
				|| recipe instanceof DoubleChestTierUpgradeShapelessRecipe) {
			ItemStack result = recipe.assemble(input, registryLookup);
			for (int slot = 0; slot < input.size(); slot++) {
				ItemStack slotStack = input.getItem(slot);
				if (slotStack.getItem() instanceof StorageBlockItem) {
					result.applyComponents(slotStack.getComponents());
					return result;
				}
			}
		}
		return ClientRecipeHelper.assemble(recipe, input, registryLookup);
	}

	private static List<CraftingDisplayVariant> getCraftingUsagesFor(IRecipeViewerDisplayCatalog catalog, ItemStack stack) {
		return catalog.getCraftingUsagesFor(stack).stream().flatMap(view -> view.variants().stream()).toList();
	}

	private static List<CraftingDisplayVariant> getCraftingRecipesFor(IRecipeViewerDisplayCatalog catalog, ItemStack stack) {
		return catalog.getCraftingRecipesFor(stack).stream().flatMap(view -> view.variants().stream()).toList();
	}

	private static boolean isTinted(ItemStack stack) {
		return StorageBlockItem.getMainColorFromComponentHolder(stack).isPresent() || StorageBlockItem.getAccentColorFromComponentHolder(stack).isPresent();
	}

	private static ItemStack singleChest(Item item) {
		return new ItemStack(item);
	}

	private static ItemStack doubleChest(Item item) {
		ItemStack stack = new ItemStack(item);
		ChestBlockItem.setDoubleChest(stack, true);
		return stack;
	}

	private static void assertSameStack(ItemStack expected, ItemStack actual) {
		assertTrue(ItemStack.isSameItemSameComponents(expected, actual), "Expected " + expected + " but got " + actual);
	}

	private final static class TestRecipeResources {
		private static LoadedResources load() {
			SharedConstants.tryDetectVersion();
			Bootstrap.bootStrap();

			ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
			Executor gameExecutor = Runnable::run;
			try {
				PackRepository packRepository = ServerPacksSource.createVanillaTrustedRepository();
				WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, WorldDataConfiguration.DEFAULT, false, false);
				WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 0);

				return WorldLoader.load(initConfig, context -> new WorldLoader.DataLoadOutput<>(UnitCookie.INSTANCE, context.datapackDimensions()),
						(resourceManager, resources, registries, cookie) -> new LoadedResources(resourceManager, resources, registries), backgroundExecutor,
						gameExecutor).join();
			} finally {
				backgroundExecutor.shutdown();
			}
		}

		private enum UnitCookie {
			INSTANCE
		}

		private record LoadedResources(CloseableResourceManager resourceManager, ReloadableServerResources serverResources,
				LayeredRegistryAccess<RegistryLayer> registries) implements AutoCloseable {
			private RecipeManager recipeManager() {
				return serverResources.getRecipeManager();
			}

			private HolderLookup.Provider registryLookup() {
				return serverResources.getRegistryLookup();
			}

			@Override
			public void close() {
				resourceManager.close();
			}
		}
	}
}
