package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.registries.*;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("recipeViewerRegression")
class StorageRecipeViewerDisplaySpecTest {
	@Test
	void tierUpgradeUsagePreservesFocusedBarrelComponentsAndDoesNotDuplicate() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack tintedBasicBarrel = tintedStack(ModBlocks.BARREL_ITEM.get());

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, tintedBasicBarrel);

		assertEquals(2, usages.size());
		assertTrue(usages.stream().allMatch(usage -> ItemStack.isSameItemSameTags(tintedBasicBarrel, usage.inputs().get(4))));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameTags(tintedStack(ModBlocks.COPPER_BARREL_ITEM.get()), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameTags(tintedStack(ModBlocks.IRON_BARREL_ITEM.get()), usage.firstOutput())));
	}

	@Test
	void tierUpgradeRecipePreservesFocusedBarrelResultComponentsAndDoesNotDuplicate() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack tintedIronBarrel = tintedStack(ModBlocks.IRON_BARREL_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, tintedIronBarrel);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItemSameTags(tintedStack(ModBlocks.BARREL_ITEM.get()), recipe.inputs().get(4))));
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItemSameTags(tintedStack(ModBlocks.COPPER_BARREL_ITEM.get()), recipe.inputs().get(4))));
		assertTrue(recipes.stream().allMatch(recipe -> ItemStack.isSameItemSameTags(tintedIronBarrel, recipe.firstOutput())));
	}

	@Test
	void tierUpgradeUsagePreservesFocusedWoodBarrelComponents() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack spruceBasicBarrel = woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE);

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, spruceBasicBarrel);

		assertEquals(2, usages.size());
		assertTrue(usages.stream().allMatch(usage -> ItemStack.isSameItemSameTags(spruceBasicBarrel, usage.inputs().get(4))));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameTags(woodStorageStack(ModBlocks.COPPER_BARREL_ITEM.get(), WoodType.SPRUCE), usage.firstOutput())));
		assertTrue(usages.stream().anyMatch(usage -> ItemStack.isSameItemSameTags(woodStorageStack(ModBlocks.IRON_BARREL_ITEM.get(), WoodType.SPRUCE), usage.firstOutput())));
	}

	@Test
	void tierUpgradeRecipePreservesFocusedWoodBarrelResultComponents() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack spruceIronBarrel = woodStorageStack(ModBlocks.IRON_BARREL_ITEM.get(), WoodType.SPRUCE);

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, spruceIronBarrel);

		assertEquals(2, recipes.size());
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItemSameTags(woodStorageStack(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE), recipe.inputs().get(4))));
		assertTrue(recipes.stream().anyMatch(recipe -> ItemStack.isSameItemSameTags(woodStorageStack(ModBlocks.COPPER_BARREL_ITEM.get(), WoodType.SPRUCE), recipe.inputs().get(4))));
		assertTrue(recipes.stream().allMatch(recipe -> ItemStack.isSameItemSameTags(spruceIronBarrel, recipe.firstOutput())));
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
		assertTrue(ItemStack.isSameItem(ironShulkerBox, usages.get(0).inputs().get(4)));
		assertTrue(ItemStack.isSameItem(new ItemStack(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()), usages.get(0).firstOutput()));
	}

	@Test
	void ironShulkerBoxRecipesOnlyShowBaseAndCopperTierUpgrades() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();
		ItemStack shulkerBox = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		ItemStack copperShulkerBox = new ItemStack(ModBlocks.COPPER_SHULKER_BOX_ITEM.get());
		ItemStack ironShulkerBox = new ItemStack(ModBlocks.IRON_SHULKER_BOX_ITEM.get());

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, ironShulkerBox).stream()
				.filter(recipe -> recipe.inputs().size() > 4 && !(recipe.inputs().get(4).getItem() instanceof ChestBlockItem))
				.toList();

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

		List<CraftingDisplayVariant> recipes = getCraftingRecipesFor(catalog, goldShulkerBox).stream()
				.filter(recipe -> recipe.inputs().size() > 4 && !(recipe.inputs().get(4).getItem() instanceof ChestBlockItem))
				.toList();

		assertEquals(1, recipes.size());
		assertTrue(ItemStack.isSameItem(ironShulkerBox, recipes.get(0).inputs().get(4)));
		assertTrue(ItemStack.isSameItem(goldShulkerBox, recipes.get(0).firstOutput()));
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
		CraftingDisplaySpec spec = catalog.getCraftingSpecs().get(0);
		CraftingRecipe displayRecipe = spec.recipe(spec.getGlobalDisplays().get(0));
		CraftingRecipe replacedRecipe = recipeWithId(spec, displayRecipe, new ResourceLocation(SophisticatedStorage.MOD_ID, "iron_barrel"));
		CraftingRecipe unrelatedRecipe = recipeWithId(spec, displayRecipe, new ResourceLocation("test", "unrelated"));

		assertTrue(catalog.replacesCraftingRecipe(replacedRecipe));
		assertTrue(catalog.getCraftingDisplaySpecReplacing(replacedRecipe).isPresent());
		assertFalse(catalog.replacesCraftingRecipe(unrelatedRecipe));
	}

	@Test
	void tierUsageChainCanReachNetheriteUpgrade() {
		IRecipeViewerDisplayCatalog catalog = createCatalog();

		ItemStack ironBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());
		ItemStack goldBarrel = getCraftingUsagesFor(catalog, ironBarrel).get(0).firstOutput();
		ItemStack diamondBarrel = getCraftingUsagesFor(catalog, goldBarrel).get(0).firstOutput();
		List<CraftingDisplayVariant> netheriteUsages = getCraftingUsagesFor(catalog, diamondBarrel);

		assertEquals(1, netheriteUsages.size());
		assertTrue(ItemStack.isSameItem(new ItemStack(ModBlocks.NETHERITE_BARREL_ITEM.get()), netheriteUsages.get(0).firstOutput()));
	}

	@Test
	void focusedHigherTierSingleColorDyeRecipeNarrowsDyeInputAndResult() {
		SingleColorDyeRecipeSpec ironBarrelDyeSpec = createCatalog().getGroupedCraftingSpecs().stream()
				.filter(SingleColorDyeRecipeSpec.class::isInstance)
				.map(SingleColorDyeRecipeSpec.class::cast)
				.filter(spec -> spec.sourceStacks().stream().anyMatch(stack -> stack.is(ModBlocks.IRON_BARREL_ITEM.get())))
				.findFirst()
				.orElseThrow();
		ItemStack redIronBarrel = new ItemStack(ModBlocks.IRON_BARREL_ITEM.get());
		if (ModBlocks.IRON_BARREL_ITEM.get() instanceof StorageBlockItem storageBlockItem) {
			int color = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors());
			storageBlockItem.setMainColor(redIronBarrel, color);
			storageBlockItem.setAccentColor(redIronBarrel, color);
		}

		List<GroupedCraftingRecipe> focusedRecipes = ironBarrelDyeSpec.getRecipesFor(redIronBarrel);

		assertEquals(1, focusedRecipes.size());
		GroupedCraftingRecipe focusedRecipe = focusedRecipes.get(0);
		assertEquals(1, focusedRecipe.getVariants().size());
		assertEquals(2, focusedRecipe.getInputSlots().size());
		assertEquals(1, focusedRecipe.getInputSlots().get(1).size());
		assertSameStack(new ItemStack(DyeItem.byColor(DyeColor.RED)), focusedRecipe.getInputSlots().get(1).get(0));
		assertSameStack(redIronBarrel, focusedRecipe.getResultStacks().get(0));
	}

	@Test
	void singleChestUsesDoNotShowDoubleChestTierUpgradeRecipes() {
		IRecipeViewerDisplayCatalog catalog = createChestCatalog();

		List<CraftingDisplayVariant> usages = getCraftingUsagesFor(catalog, singleChest(ModBlocks.CHEST_ITEM.get()));

		assertTrue(usages.size() >= 2);
		assertTrue(usages.stream().allMatch(usage -> usage.inputs().stream().noneMatch(ChestBlockItem::isDoubleChest) && !ChestBlockItem.isDoubleChest(usage.firstOutput())));
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
		List<net.minecraft.world.item.crafting.CraftingRecipe> recipes = catalog.getCraftingUsagesFor(new ItemStack(Items.IRON_INGOT)).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe))
				.toList();

		assertEquals(16, usages.size());
		assertEquals(16, recipes.size());
		assertTrue(usages.stream().anyMatch(variant -> !ChestBlockItem.isDoubleChest(variant.inputs().get(4)) && !ChestBlockItem.isDoubleChest(variant.firstOutput())));
		assertTrue(usages.stream().anyMatch(variant -> ChestBlockItem.isDoubleChest(variant.inputs().get(4)) && ChestBlockItem.isDoubleChest(variant.firstOutput())));
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

	private static IRecipeViewerDisplayCatalog createCatalog() {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		try (TestRecipeResources.LoadedResources resources = TestRecipeResources.load(); MockedStatic<ClientRecipeHelper> clientRecipeHelper = Mockito.mockStatic(ClientRecipeHelper.class, Mockito.CALLS_REAL_METHODS)) {
			mockClientRecipeHelper(clientRecipeHelper, resources);
			StorageRecipeViewerDisplays.register(catalog, IRecipeViewerDisplayContext.empty());
		}
		return catalog;
	}

	private static IRecipeViewerDisplayCatalog createChestCatalog() {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		try (TestRecipeResources.LoadedResources resources = TestRecipeResources.load(); MockedStatic<ClientRecipeHelper> clientRecipeHelper = Mockito.mockStatic(ClientRecipeHelper.class, Mockito.CALLS_REAL_METHODS)) {
			mockClientRecipeHelper(clientRecipeHelper, resources);
			StorageRecipeViewerDisplays.register(catalog, IRecipeViewerDisplayContext.empty());
		}
		return catalog;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void mockClientRecipeHelper(MockedStatic<ClientRecipeHelper> clientRecipeHelper, TestRecipeResources.LoadedResources resources) {
		RecipeManager recipeManager = resources.recipeManager();
		clientRecipeHelper.when(() -> ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(Mockito.eq(RecipeType.CRAFTING), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
			RecipeType recipeType = invocation.getArgument(0);
			Class recipeClass = invocation.getArgument(1);
			return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(recipeManager, recipeType, recipeClass, invocation.getArgument(2));
		});
		clientRecipeHelper.when(() -> ClientRecipeHelper.transformAllRecipesOfType(Mockito.eq(RecipeType.CRAFTING), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
			RecipeType recipeType = invocation.getArgument(0);
			Class recipeClass = invocation.getArgument(1);
			return ClientRecipeHelper.transformAllRecipesOfType(recipeManager, recipeType, recipeClass, invocation.getArgument(2));
		});
		clientRecipeHelper.when(() -> ClientRecipeHelper.assemble(Mockito.any(), Mockito.any())).thenAnswer(invocation -> assembleRecipe(invocation.getArgument(0), invocation.getArgument(1), resources.registryLookup()));
		clientRecipeHelper.when(() -> ClientRecipeHelper.getResultItem(Mockito.any())).thenAnswer(invocation -> ClientRecipeHelper.getResultItem(invocation.getArgument(0), resources.registryLookup()));
	}

	private static ItemStack assembleRecipe(Recipe<CraftingContainer> recipe, CraftingContainer input, RegistryAccess registryLookup) {
		if (recipe instanceof StorageTierUpgradeRecipe || recipe instanceof StorageTierUpgradeShapelessRecipe || recipe instanceof DoubleChestTierUpgradeRecipe || recipe instanceof DoubleChestTierUpgradeShapelessRecipe) {
			ItemStack result = ClientRecipeHelper.getResultItem(recipe, registryLookup).copy();
			for (int slot = 0; slot < input.getContainerSize(); slot++) {
				ItemStack slotStack = input.getItem(slot);
				if (slotStack.getItem() instanceof StorageBlockItem) {
					if (slotStack.hasTag()) {
						result.setTag(slotStack.getTag().copy());
					}
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

	private static CraftingRecipe recipeWithId(CraftingDisplaySpec spec, CraftingRecipe displayRecipe, ResourceLocation id) {
		return spec.shapeless() ? new ShapelessRecipe(id, "", CraftingBookCategory.MISC, displayRecipe.getResultItem(null), spec.baseIngredients())
				: new ShapedRecipe(id, "", CraftingBookCategory.MISC, spec.width(), spec.height(), spec.baseIngredients(), displayRecipe.getResultItem(null));
	}

	private static boolean isTinted(ItemStack stack) {
		return StorageBlockItem.getMainColorFromStack(stack).isPresent() || StorageBlockItem.getAccentColorFromStack(stack).isPresent();
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
		assertTrue(ItemStack.isSameItemSameTags(expected, actual), "Expected " + expected + " but got " + actual);
	}

	private final static class TestRecipeResources {
		private TestRecipeResources() {
		}

		private static LoadedResources load() {
			SharedConstants.tryDetectVersion();
			Bootstrap.bootStrap();
			ForgeTestModList.install(SophisticatedCore.MOD_ID, SophisticatedStorage.MOD_ID);
			ForgeTestRegistries.installStorage();

			ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
			Executor gameExecutor = Runnable::run;
			try {
				PackRepository packRepository = new PackRepository(new ServerPacksSource());
				WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, WorldDataConfiguration.DEFAULT, false, false);
				WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 0);

				return WorldLoader.load(
						initConfig,
						context -> new WorldLoader.DataLoadOutput<>(UnitCookie.INSTANCE, context.datapackDimensions()),
						(resourceManager, resources, registries, cookie) -> new LoadedResources(resourceManager, resources, registries),
						backgroundExecutor,
						gameExecutor
				).join();
			} finally {
				backgroundExecutor.shutdown();
			}
		}

		private final static class ForgeTestModList {
			private static final Map<String, Path> MERGED_RESOURCE_ROOTS = new HashMap<>();

			private ForgeTestModList() {
			}

			private static void install(String... modIds) {
				List<IModFileInfo> modFiles = new ArrayList<>();
				List<IModInfo> mods = new ArrayList<>();
				Map<String, Object> indexedMods = new HashMap<>();

				for (String modId : modIds) {
					Path moduleRoot = moduleRoot(modId);
					Path testResources = moduleRoot.resolve(Path.of("src", "test", "resources"));
					Path mainResources = moduleRoot.resolve(Path.of("src", "main", "resources"));
					Path generatedResources = moduleRoot.resolve(Path.of("src", "generated", "resources"));
					Path mergedResources = mergedResourceRoot(modId, moduleRoot, List.of(mainResources, generatedResources, testResources));

					IModInfo modInfo = proxy(IModInfo.class, (proxy, method, args) -> switch (method.getName()) {
						case "getModId", "getNamespace" -> modId;
						case "getDisplayName" -> modId;
						default -> defaultValue(method);
					});
					IModFile modFile = proxy(IModFile.class, (proxy, method, args) -> switch (method.getName()) {
						case "getModInfos" -> List.of(modInfo);
						case "getFileName" -> moduleRoot.getFileName().toString();
						case "getFilePath" -> mergedResources;
						case "findResource" -> findResource(mergedResources, (String[]) args[0]);
						default -> defaultValue(method);
					});
					IModFileInfo modFileInfo = proxy(IModFileInfo.class, (proxy, method, args) -> switch (method.getName()) {
						case "getFile" -> modFile;
						case "getMods" -> List.of(modInfo);
						case "requiredLanguageLoaders" -> List.of();
						default -> defaultValue(method);
					});

					modFiles.add(modFileInfo);
					mods.add(modInfo);
					indexedMods.put(modId, new Object());
				}

				ModList modList = ModList.of(List.<ModFile>of(), List.<ModInfo>of());
				setField(modList, "modFiles", modFiles);
				setField(modList, "sortedList", mods);
				setField(modList, "fileById", Map.of());
				setField(modList, "mods", List.of());
				setField(modList, "indexedMods", indexedMods);
			}

			private static Path moduleRoot(String modId) {
				String moduleName = switch (modId) {
					case SophisticatedCore.MOD_ID -> "SophisticatedCore";
					case SophisticatedStorage.MOD_ID -> "SophisticatedStorage";
					default -> throw new IllegalArgumentException("Unknown test mod " + modId);
				};
				Path workingDir = Path.of("").toAbsolutePath();
				if (workingDir.getFileName().toString().equals(moduleName)) {
					return workingDir;
				}
				Path child = workingDir.resolve(moduleName);
				if (Files.isDirectory(child)) {
					return child;
				}
				Path sibling = workingDir.getParent().resolve(moduleName);
				if (Files.isDirectory(sibling)) {
					return sibling;
				}
				throw new IllegalStateException("Unable to locate module " + moduleName + " from " + workingDir);
			}

			private static synchronized Path mergedResourceRoot(String modId, Path moduleRoot, List<Path> roots) {
				Path existingRoot = MERGED_RESOURCE_ROOTS.get(modId);
				if (existingRoot != null) {
					return existingRoot;
				}

				String workerId = System.getProperty("org.gradle.test.worker", "main");
				Path mergedResources = moduleRoot.resolve(Path.of("build", "recipe-viewer-test-resources", StorageRecipeViewerDisplaySpecTest.class.getSimpleName() + "-" + workerId, modId));
				try {
					deleteRecursively(mergedResources);
					Files.createDirectories(mergedResources);
					for (Path root : roots) {
						copyResources(root, mergedResources);
					}
				} catch (IOException e) {
					throw new IllegalStateException("Unable to prepare merged test resources for " + modId, e);
				}
				MERGED_RESOURCE_ROOTS.put(modId, mergedResources);
				return mergedResources;
			}

			private static void copyResources(Path root, Path targetRoot) throws IOException {
				if (!Files.isDirectory(root)) {
					return;
				}
				try (Stream<Path> paths = Files.walk(root)) {
					paths.filter(Files::isRegularFile).forEach(source -> {
						Path target = targetRoot.resolve(root.relativize(source));
						try {
							Files.createDirectories(target.getParent());
							Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				} catch (UncheckedIOException e) {
					throw e.getCause();
				}
			}

			private static void deleteRecursively(Path path) throws IOException {
				if (!Files.exists(path)) {
					return;
				}
				try (Stream<Path> paths = Files.walk(path)) {
					for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) {
						Files.delete(child);
					}
				}
			}

			private static Path findResource(Path root, String[] path) {
				Path relativePath = Path.of(String.join("/", path));
				return root.resolve(relativePath);
			}

			@SuppressWarnings("unchecked")
			private static <T> T proxy(Class<T> type, InvocationHandler handler) {
				return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
							case "toString" -> type.getSimpleName() + " test proxy";
							case "hashCode" -> System.identityHashCode(proxy);
							case "equals" -> proxy == args[0];
							default -> defaultValue(method);
						};
					}
					return handler.invoke(proxy, method, args);
				});
			}

			private static Object defaultValue(Method method) {
				Class<?> returnType = method.getReturnType();
				if (returnType == boolean.class) {
					return false;
				}
				if (returnType == int.class) {
					return 0;
				}
				return null;
			}

			private static void setField(ModList modList, String name, Object value) {
				try {
					Field field = ModList.class.getDeclaredField(name);
					field.setAccessible(true);
					field.set(modList, value);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Unable to initialize Forge test mod list", e);
				}
			}
		}

		private final static class ForgeTestRegistries {
			private static boolean installed;

			private ForgeTestRegistries() {
			}

			private static void installStorage() {
				if (installed) {
					return;
				}
				installed = true;
				GameData.unfreezeData();

				registerDeferred(ModBlocks.class, "BLOCKS", ForgeRegistries.BLOCKS);
				registerDeferred(ModBlocks.class, "ITEMS", ForgeRegistries.ITEMS);
				registerDeferred(ModItems.class, "ITEMS", ForgeRegistries.ITEMS);
				registerDeferred(ModRecipes.class, "RECIPE_SERIALIZERS", ForgeRegistries.RECIPE_SERIALIZERS);
				registerDeferred(ModBlocks.class, "RECIPE_SERIALIZERS", ForgeRegistries.RECIPE_SERIALIZERS);
				registerAlwaysTrueCondition(SophisticatedCore.getRL("item_enabled"));
				registerAlwaysTrueCondition(SophisticatedStorage.getRL("drop_packed_disabled"));
				registerRecipeIngredient(new ResourceLocation("minecraft", "item"), VanillaIngredientSerializer.INSTANCE);
			}

			private static <T> void registerDeferred(Class<?> owner, String fieldName, IForgeRegistry<T> registry) {
				try {
					unfreeze(registry);
					Field field = owner.getDeclaredField(fieldName);
					field.setAccessible(true);
					DeferredRegister<T> deferredRegister = (DeferredRegister<T>) field.get(null);
					Field entriesField = DeferredRegister.class.getDeclaredField("entries");
					entriesField.setAccessible(true);
					Map<RegistryObject<T>, java.util.function.Supplier<? extends T>> entries = (Map<RegistryObject<T>, java.util.function.Supplier<? extends T>>) entriesField.get(deferredRegister);

					for (Map.Entry<RegistryObject<T>, java.util.function.Supplier<? extends T>> entry : entries.entrySet()) {
						RegistryObject<T> registryObject = entry.getKey();
						T value;
						if (!registry.containsKey(registryObject.getId())) {
							value = entry.getValue().get();
							registry.register(registryObject.getId(), value);
						} else {
							value = registry.getValue(registryObject.getId());
						}
						registerVanillaRegistry(registry, registryObject.getId(), value);
						setField(registryObject, "value", value);
						setField(registryObject, "holder", registry.getHolder(registryObject.getId()).orElse(null));
					}
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Unable to initialize test registry " + owner.getName() + "." + fieldName, e);
				}
			}

			private static void unfreeze(IForgeRegistry<?> registry) throws ReflectiveOperationException {
				Method method = registry.getClass().getDeclaredMethod("unfreeze");
				method.setAccessible(true);
				method.invoke(registry);
			}

			private static <T> void registerVanillaRegistry(IForgeRegistry<T> forgeRegistry, ResourceLocation id, T value) {
				if (forgeRegistry == ForgeRegistries.ITEMS && !BuiltInRegistries.ITEM.containsKey(id)) {
					Registry.register(BuiltInRegistries.ITEM, id, (Item) value);
				} else if (forgeRegistry == ForgeRegistries.BLOCKS && !BuiltInRegistries.BLOCK.containsKey(id)) {
					Registry.register(BuiltInRegistries.BLOCK, id, (Block) value);
				} else if (forgeRegistry == ForgeRegistries.RECIPE_SERIALIZERS && !BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id)) {
					Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, (RecipeSerializer<?>) value);
				}
			}

			private static void registerRecipeCondition(net.minecraftforge.common.crafting.conditions.IConditionSerializer<?> serializer) {
				try {
					CraftingHelper.register(serializer);
				} catch (IllegalStateException e) {
					if (!e.getMessage().startsWith("Duplicate recipe condition serializer:")) {
						throw e;
					}
				}
			}

			private static void registerAlwaysTrueCondition(ResourceLocation id) {
				registerRecipeCondition(new net.minecraftforge.common.crafting.conditions.IConditionSerializer<>() {
					@Override
					public void write(com.google.gson.JsonObject json, net.minecraftforge.common.crafting.conditions.ICondition value) {
					}

					@Override
					public net.minecraftforge.common.crafting.conditions.ICondition read(com.google.gson.JsonObject json) {
						return new net.minecraftforge.common.crafting.conditions.ICondition() {
							@Override
							public ResourceLocation getID() {
								return id;
							}

							@Override
							public boolean test(IContext context) {
								return true;
							}
						};
					}

					@Override
					public ResourceLocation getID() {
						return id;
					}
				});
			}

			private static void registerRecipeIngredient(ResourceLocation id, net.minecraftforge.common.crafting.IIngredientSerializer<?> serializer) {
				try {
					CraftingHelper.register(id, serializer);
				} catch (IllegalStateException e) {
					if (!e.getMessage().startsWith("Duplicate recipe ingredient serializer:")) {
						throw e;
					}
				}
			}

			private static void setField(Object target, String name, Object value) {
				try {
					Field field = target.getClass().getDeclaredField(name);
					field.setAccessible(true);
					field.set(target, value);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Unable to set " + name + " on " + target, e);
				}
			}
		}

		private enum UnitCookie {
			INSTANCE
		}

		private record LoadedResources(CloseableResourceManager resourceManager, ReloadableServerResources serverResources, LayeredRegistryAccess<RegistryLayer> registries) implements AutoCloseable {
			private RecipeManager recipeManager() {
				return serverResources.getRecipeManager();
			}

			private RegistryAccess registryLookup() {
				return registries.compositeAccess();
			}

			@Override
			public void close() {
				resourceManager.close();
			}
		}
	}
}
