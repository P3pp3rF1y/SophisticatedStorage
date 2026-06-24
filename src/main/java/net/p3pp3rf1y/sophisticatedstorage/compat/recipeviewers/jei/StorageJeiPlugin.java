package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.CraftingDisplayCatalogRecipeManagerPlugin;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.GroupedCraftingRecipeCategoryExtension;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.GroupedCraftingRecipeManagerPlugin;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingSpecExtensionRegistrar;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiSettingsGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiStorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.subtypes.JeiSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.StorageRecipeViewerDisplays;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@SuppressWarnings("unused")
@JeiPlugin
public class StorageJeiPlugin implements IModPlugin {
	private static Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar = registration -> {
	};
	private IRecipeViewerDisplayCatalog catalog = null;

	public static void addAdditionalCatalystRegistrar(Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar) {
		StorageJeiPlugin.additionalCatalystRegistrar = StorageJeiPlugin.additionalCatalystRegistrar.andThen(additionalCatalystRegistrar);
	}

	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "default");
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		getSubtypeInterpreters().forEach((item, subtypeInterpreter) -> registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item,
				JeiSubtypeInterpreter.of(subtypeInterpreter)));
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(StorageScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(StorageScreen gui) {
				List<Rect2i> ret = new ArrayList<>();
				gui.getUpgradeSlotsRectangle().ifPresent(ret::add);
				ret.addAll(gui.getUpgradeSettingsControl().getTabRectangles());
				gui.getSortButtonsRectangle().ifPresent(ret::add);
				return ret;
			}
		});

		registration.addGuiContainerHandler(StorageSettingsScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(StorageSettingsScreen gui) {
				return new ArrayList<>(gui.getExtendedControlsRectangles());
			}
		});

		registration.addGhostIngredientHandler(StorageScreen.class, new JeiStorageGhostIngredientHandler<>());
		registration.addGhostIngredientHandler(SettingsScreen.class, new JeiSettingsGhostIngredientHandler<>());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		IRecipeViewerDisplayCatalog catalog = getCatalog();
		registration.addRecipes(RecipeTypes.CRAFTING,
				catalog.getGroupedCraftingSpecs().stream().map(spec -> castCraftingRecipeHolder(spec.recipeHolder())).toList());
		registration.addRecipes(RecipeTypes.CRAFTING, catalog.getCraftingRecipes());
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		GroupedCraftingRecipeCategoryExtension.registerOnce(registration);
		JeiCraftingSpecExtensionRegistrar.registerCraftingSpecExtensions(registration, this::getCatalog, stack -> stack.getItem() instanceof StorageBlockItem,
				List.of(StorageTierUpgradeRecipe.class, StorageTierUpgradeShapelessRecipe.class, DoubleChestTierUpgradeRecipe.class,
						DoubleChestTierUpgradeShapelessRecipe.class, ShulkerBoxFromChestRecipe.class));
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addTypedRecipeManagerPlugin(RecipeTypes.CRAFTING, new GroupedCraftingRecipeManagerPlugin(() -> getCatalog().getGroupedCraftingSpecs(),
				stack -> stack.getItem() instanceof StorageBlockItem, StorageJeiPlugin::needsComponentSensitiveCraftingDisplay));
		registration.addTypedRecipeManagerPlugin(RecipeTypes.CRAFTING, new CraftingDisplayCatalogRecipeManagerPlugin(this::getCatalog,
				stack -> stack.getItem() instanceof StorageBlockItem || stack.is(Items.SHULKER_SHELL)));
	}

	private static boolean needsComponentSensitiveCraftingDisplay(ItemStack stack) {
		return stack.getItem() instanceof StorageBlockItem
				&& (StorageBlockItem.getMainColorFromComponentHolder(stack).isPresent() || StorageBlockItem.getAccentColorFromComponentHolder(stack).isPresent()
						|| WoodStorageBlockItem.getWoodType(stack).isPresent() || ChestBlockItem.isDoubleChest(stack) || BarrelBlockItem.isFlatTop(stack));
	}

	private IRecipeViewerDisplayCatalog getCatalog() {
		if (catalog == null) {
			catalog = createCatalog(getSubtypeInterpreters());
		}
		return catalog;
	}

	private static IRecipeViewerDisplayCatalog createCatalog(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters) {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
		StorageRecipeViewerDisplays.register(catalog, context);
		return catalog;
	}

	@SuppressWarnings("unchecked")
	private static RecipeHolder<CraftingRecipe> castCraftingRecipeHolder(RecipeHolder<? extends CraftingRecipe> recipeHolder) {
		return (RecipeHolder<CraftingRecipe>) recipeHolder;
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModItems.CRAFTING_UPGRADE.get()), RecipeTypes.CRAFTING);
		registration.addRecipeCatalyst(new ItemStack(ModItems.STONECUTTER_UPGRADE.get()), RecipeTypes.STONECUTTING);
		additionalCatalystRegistrar.accept(registration);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		IRecipeTransferHandlerHelper handlerHelper = registration.getTransferHelper();
		IStackHelper stackHelper = registration.getJeiHelpers().getStackHelper();
		registration.addRecipeTransferHandler(
				new JeiCraftingContainerRecipeTransferHandlerBase<StorageContainerMenu, RecipeHolder<CraftingRecipe>>(handlerHelper, stackHelper) {
					@Override
					public Class<StorageContainerMenu> getContainerClass() {
						return StorageContainerMenu.class;
					}

					@Override
					public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
						return RecipeTypes.CRAFTING;
					}
				}, RecipeTypes.CRAFTING);
	}

}
