package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.comparison.EmiSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.StorageRecipeViewerDisplays;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@EmiEntrypoint
public class StorageEmiPlugin implements EmiPlugin {
	private static Consumer<WorkstationRegistration> additionalWorkstations = registrar -> {};
	public static void addAdditionalWorkstations(Consumer<WorkstationRegistration> additionalWorkstations) {
		StorageEmiPlugin.additionalWorkstations = StorageEmiPlugin.additionalWorkstations.andThen(additionalWorkstations);
	}

	public static class WorkstationRegistration {
		private final EmiRegistry registry;

		private WorkstationRegistration(EmiRegistry registry) {
			this.registry = registry;
		}

		public void addWorkstation(ResourceLocation id, Block icon, Item workstation) {
			addWorkstation(new EmiRecipeCategory(id, EmiStack.of(icon)), workstation);
		}

		public void addWorkstation(EmiRecipeCategory category, Item workstation) {
			addWorkstation(category, EmiStack.of(workstation));
		}

		public void addWorkstation(EmiRecipeCategory category, EmiStack workstation) {
			registry.addWorkstation(category, workstation);
		}
	}

	@Override
	public void register(EmiRegistry registry) {
		registerGuiHandlers(registry);
		registerRecipes(registry);
		registerDefaultComparisons(registry);
		registerRecipeHandlers(registry);
		registerWorkstations(registry);
	}

	private void registerDefaultComparisons(EmiRegistry registry) {
		getSubtypeInterpreters()
				.forEach((item, comparator) -> registry.setDefaultComparison(item, EmiSubtypeInterpreter.of(comparator)));
	}

    private void registerGuiHandlers(EmiRegistry registry) {
		registry.addExclusionArea(StorageScreen.class, StorageEmiPlugin::addStorageExclusionArea);
		registry.addExclusionArea(LimitedBarrelScreen.class, StorageEmiPlugin::addStorageExclusionArea);
		registry.addExclusionArea(StorageSettingsScreen.class, StorageEmiPlugin::addSettingsExclusionArea);
		registry.addExclusionArea(LimitedBarrelSettingsScreen.class, StorageEmiPlugin::addSettingsExclusionArea);

		registry.addDragDropHandler(StorageScreen.class, new EmiStorageGhostDragDropHandler<>());
		registry.addDragDropHandler(LimitedBarrelScreen.class, new EmiStorageGhostDragDropHandler<>());
		registry.addDragDropHandler(StorageSettingsScreen.class, new EmiSettingsGhostDragDropHandler<>());
		registry.addDragDropHandler(LimitedBarrelSettingsScreen.class, new EmiSettingsGhostDragDropHandler<>());
	}

	private static void addSettingsExclusionArea(StorageSettingsScreen screen, Consumer<Bounds> consumer) {
		if (screen == null) { // Due to how Emi collects the exclusion area this can be null
			return;
		}
		screen.getExtendedControlsRectangles().forEach(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
	}

	private static void addStorageExclusionArea(StorageScreen screen, Consumer<Bounds> consumer) {
		//noinspection ConstantValue
		if (screen == null || screen.getUpgradeSettingsControl() == null) {
			return;
		}
		screen.getUpgradeSlotsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		screen.getUpgradeSettingsControl().getTabRectangles().forEach(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		screen.getSortButtonsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
	}

	private void registerRecipes(EmiRegistry registry) {
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
		IRecipeViewerDisplayCatalog catalog = createCatalog(subtypeInterpreters);
		Set<CraftingRecipe> craftingRecipes = catalog.getCraftingRecipes().stream().collect(Collectors.toSet());
		registry.removeRecipes(recipe -> recipe.getBackingRecipe() instanceof CraftingRecipe craftingRecipe && (catalog.replacesCraftingRecipe(craftingRecipe) || craftingRecipes.contains(craftingRecipe)));

		catalog.getGroupedCraftingSpecs().stream()
				.flatMap(spec -> spec.getAllDisplays().stream())
				.flatMap(recipe -> GroupedCraftingEmiRecipe.ofGroupedUsageAndFocusedRecipes(recipe).stream())
				.forEach(registry::addRecipe);
		catalog.getCraftingRecipes().stream()
				.filter(recipe -> !catalog.replacesCraftingRecipe(recipe))
				.map(StorageEmiPlugin::wrapSyntheticCraftingRecipe)
				.forEach(registry::addRecipe);

		catalog.getCraftingSpecs().stream()
				.flatMap(spec -> CraftingSpecEmiRecipe.ofGroupedUsageAndFocusedRecipes(spec).stream())
				.forEach(registry::addRecipe);

	}

	private static IRecipeViewerDisplayCatalog createCatalog(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters) {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
		StorageRecipeViewerDisplays.register(catalog, context);
		return catalog;
	}

	private static EmiCraftingRecipe wrapSyntheticCraftingRecipe(CraftingRecipe recipe) {
		if (recipe instanceof ShapelessRecipe) {
			return EmiClientRecipeHelper.wrapSyntheticShapelessRecipe(recipe);
		}
		return EmiClientRecipeHelper.wrapSyntheticShapedRecipe(recipe);
	}

	private void registerWorkstations(EmiRegistry registry) {
		registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(ModItems.CRAFTING_UPGRADE.get()));
		registry.addWorkstation(VanillaEmiRecipeCategories.STONECUTTING, EmiStack.of(ModItems.STONECUTTER_UPGRADE.get()));

		additionalWorkstations.accept(new WorkstationRegistration(registry));
	}

	private void registerRecipeHandlers(EmiRegistry registry) {
		registry.addRecipeHandler(ModBlocks.STORAGE_CONTAINER_TYPE.get(), EmiGridMenuInfo.crafting());
	}
}
