package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
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
import net.p3pp3rf1y.sophisticatedstorage.crafting.GenericWoodStorageRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@EmiEntrypoint
@SuppressWarnings("PMD.UnnecessaryImport")
public class StorageEmiPlugin implements EmiPlugin {
	private static Consumer<WorkstationRegistration> additionalWorkstations = registrar -> {
	};
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
		EmiCraftingRecipeHandler craftingRecipeHandler = new EmiCraftingRecipeHandler();
		registerRecipes(registry, craftingRecipeHandler);
		registry.addRecipeHandler(MenuType.CRAFTING, craftingRecipeHandler);
		registerDefaultComparisons(registry);
		registerRecipeHandlers(registry);
		registerWorkstations(registry);
	}

	private void registerDefaultComparisons(EmiRegistry registry) {
		getSubtypeInterpreters().forEach((item, comparator) -> registry.setDefaultComparison(item, EmiSubtypeInterpreter.of(comparator)));
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
		// noinspection ConstantValue
		if (screen == null || screen.getUpgradeSettingsControl() == null) {
			return;
		}
		screen.getUpgradeSlotsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		screen.getUpgradeSettingsControl().getTabRectangles().forEach(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		screen.getSortButtonsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
	}

	private void registerRecipes(EmiRegistry registry, EmiCraftingRecipeHandler craftingRecipeHandler) {
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
		IRecipeViewerDisplayCatalog catalog = createCatalog(subtypeInterpreters);
		Set<CraftingRecipe> craftingRecipes = catalog.getCraftingRecipes().stream().collect(Collectors.toSet());
		registry.removeRecipes(recipe -> recipe.getBackingRecipe() instanceof CraftingRecipe craftingRecipe
				&& (catalog.replacesCraftingRecipe(craftingRecipe) || craftingRecipes.contains(craftingRecipe)));

		catalog.getGroupedCraftingSpecs().stream().flatMap(spec -> spec.getAllDisplays().stream())
				.flatMap(groupedRecipe -> GroupedCraftingEmiRecipe.ofGroupedUsageAndFocusedRecipes(groupedRecipe).stream()).forEach(recipe -> {
					craftingRecipeHandler.addRecipe(recipe);
					registry.addRecipe(recipe);
				});
		catalog.getCraftingRecipes().stream().filter(recipe -> !catalog.replacesCraftingRecipe(recipe))
				.flatMap(recipe -> wrapSyntheticCraftingRecipe(recipe).stream())
				.forEach(recipe -> registerSyntheticCraftingRecipe(registry, craftingRecipeHandler, recipe));

		catalog.getCraftingSpecs().stream().flatMap(spec -> CraftingSpecEmiRecipe.ofGroupedUsageAndFocusedRecipes(spec).stream()).forEach(recipe -> {
			craftingRecipeHandler.addRecipe(recipe);
			registry.addRecipe(recipe);
		});

	}

	private static void registerSyntheticCraftingRecipe(EmiRegistry registry, EmiCraftingRecipeHandler craftingRecipeHandler, EmiRecipe recipe) {
		if (recipe instanceof SyntheticCraftingRecipe syntheticRecipe) {
			craftingRecipeHandler.addRecipe(syntheticRecipe, syntheticRecipe.shapeless, syntheticRecipe.width, syntheticRecipe.displayInputSlots,
					syntheticRecipe.output.getItemStack());
		}
		registry.addRecipe(recipe);
	}

	private static IRecipeViewerDisplayCatalog createCatalog(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters) {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
		StorageRecipeViewerDisplays.register(catalog, context);
		return catalog;
	}

	private static List<EmiRecipe> wrapSyntheticCraftingRecipe(CraftingRecipe recipe) {
		List<Ingredient> ingredients = recipe.getIngredients();
		List<EmiRecipe> recipes = new ArrayList<>();
		if (hasBroadIngredient(ingredients)) {
			recipes.add(new SyntheticCraftingRecipe(recipe.getId(), recipe, getInputIngredients(ingredients),
					ingredientIndex -> !isBroadIngredient(ingredients, ingredientIndex), true));
		} else {
			recipes.add(recipe instanceof ShapelessRecipe
					? EmiClientRecipeHelper.wrapSyntheticShapelessRecipe(recipe)
					: EmiClientRecipeHelper.wrapSyntheticShapedRecipe(recipe));
		}
		addFocusedInputSyntheticRecipes(recipe, recipes);
		return recipes;
	}

	private static boolean hasBroadIngredient(List<Ingredient> ingredients) {
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			if (isBroadIngredient(ingredients, ingredientIndex)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBroadIngredient(List<Ingredient> ingredients, int ingredientIndex) {
		return ingredients.get(ingredientIndex).getItems().length > 1;
	}

	private static void addFocusedInputSyntheticRecipes(CraftingRecipe recipe, List<EmiRecipe> recipes) {
		ResourceLocation baseId = recipe.getId();
		if (recipe instanceof GenericWoodStorageRecipe) {
			return;
		}
		List<Ingredient> ingredients = recipe.getIngredients();
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			ItemStack[] stacks = ingredients.get(ingredientIndex).getItems();
			if (stacks.length <= 1) {
				continue;
			}
			for (int stackIndex = 0; stackIndex < stacks.length; stackIndex++) {
				int focusedIngredientIndex = ingredientIndex;
				int focusedStackIndex = stackIndex;
				ResourceLocation id = baseId.withPath(path -> path + "/input/" + focusedIngredientIndex + "/" + focusedStackIndex);
				recipes.add(new SyntheticCraftingRecipe(id, recipe, getFocusedInputIngredients(ingredients, ingredientIndex, stacks[stackIndex]),
						inputIndex -> true, false));
			}
		}
	}

	private static List<EmiIngredient> getInputIngredients(List<Ingredient> ingredients) {
		List<EmiIngredient> inputIngredients = new ArrayList<>(ingredients.size());
		for (Ingredient ingredient : ingredients) {
			inputIngredients.add(EmiIngredient.of(Arrays.stream(ingredient.getItems()).map(EmiStack::of).toList()));
		}
		return inputIngredients;
	}

	private static List<EmiIngredient> getFocusedInputIngredients(List<Ingredient> ingredients, int focusedIngredientIndex, ItemStack focusedStack) {
		List<EmiIngredient> focusedIngredients = new ArrayList<>(ingredients.size());
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			if (ingredientIndex == focusedIngredientIndex) {
				focusedIngredients.add(EmiStack.of(focusedStack));
			} else {
				focusedIngredients.add(EmiIngredient.of(Arrays.stream(ingredients.get(ingredientIndex).getItems()).map(EmiStack::of).toList()));
			}
		}
		return focusedIngredients;
	}

	private static class SyntheticCraftingRecipe extends BasicEmiRecipe {
		private final CraftingRecipe recipe;
		private final List<EmiIngredient> displayInputs;
		private final List<List<ItemStack>> displayInputSlots;
		private final EmiStack output;
		private final boolean shapeless;
		private final int width;

		private SyntheticCraftingRecipe(ResourceLocation id, CraftingRecipe recipe, List<EmiIngredient> displayInputs, IntPredicate inputIndexFilter,
				boolean indexOutput) {
			super(VanillaEmiRecipeCategories.CRAFTING, id.withPath(path -> path.startsWith("/") ? path : "/" + path), 118, 54);
			this.recipe = recipe;
			this.displayInputs = displayInputs;
			displayInputSlots = displayInputs.stream().map(EmiIngredient::getEmiStacks).map(stacks -> stacks.stream().map(EmiStack::getItemStack).toList())
					.toList();
			output = EmiStack.of(ClientRecipeHelper.getResultItem(recipe));
			shapeless = recipe instanceof ShapelessRecipe;
			width = shapeless ? displayInputs.size() : ((net.minecraft.world.item.crafting.ShapedRecipe) recipe).getWidth();
			for (int inputIndex = 0; inputIndex < displayInputs.size(); inputIndex++) {
				if (inputIndexFilter.test(inputIndex)) {
					inputs.add(displayInputs.get(inputIndex));
				}
			}
			if (indexOutput) {
				outputs.add(output);
			}
		}

		@Override
		public void addWidgets(WidgetHolder widgets) {
			widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
			if (shapeless) {
				widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
			}
			for (int i = 0; i < displayInputs.size(); i++) {
				widgets.addSlot(displayInputs.get(i), i % 3 * 18, i / 3 * 18);
			}
			widgets.addSlot(output, 92, 14).large(true).recipeContext(this);
		}

		@Override
		public Recipe<?> getBackingRecipe() {
			return recipe;
		}

		@Override
		public boolean supportsRecipeTree() {
			// Lookup indexes intentionally omit broad inputs or outputs; the handler owns the complete fill view.
			return false;
		}
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
