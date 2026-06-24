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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@EmiEntrypoint
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
		registerRecipes(registry);
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

	private void registerRecipes(EmiRegistry registry) {
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
		IRecipeViewerDisplayCatalog catalog = createCatalog(subtypeInterpreters);
		Set<ResourceLocation> craftingRecipeIds = catalog.getCraftingRecipes().stream().map(RecipeHolder::id).collect(Collectors.toSet());
		registry.removeRecipes(recipe -> recipe.getBackingRecipe() != null
				&& (catalog.replacesCraftingRecipe(recipe.getBackingRecipe()) || craftingRecipeIds.contains(recipe.getBackingRecipe().id())));

		catalog.getGroupedCraftingSpecs().stream().flatMap(spec -> spec.getAllDisplays().stream())
				.flatMap(recipeHolder -> GroupedCraftingEmiRecipe.ofGroupedUsageAndFocusedRecipes(recipeHolder).stream()).forEach(registry::addRecipe);
		catalog.getCraftingRecipes().stream().filter(recipeHolder -> !catalog.replacesCraftingRecipe(recipeHolder))
				.flatMap(recipeHolder -> wrapSyntheticCraftingRecipe(recipeHolder).stream()).forEach(registry::addRecipe);

		catalog.getCraftingSpecs().stream().flatMap(spec -> CraftingSpecEmiRecipe.ofGroupedUsageAndFocusedRecipes(spec).stream()).forEach(registry::addRecipe);

	}

	private static IRecipeViewerDisplayCatalog createCatalog(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters) {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
		StorageRecipeViewerDisplays.register(catalog, context);
		return catalog;
	}

	private static List<EmiRecipe> wrapSyntheticCraftingRecipe(RecipeHolder<CraftingRecipe> recipeHolder) {
		CraftingRecipe recipe = recipeHolder.value();
		List<Ingredient> ingredients = recipe.getIngredients();
		List<EmiRecipe> recipes = new ArrayList<>();
		if (hasBroadIngredient(ingredients)) {
			recipes.add(new SyntheticCraftingRecipe(recipeHolder.id(), recipeHolder, getInputIngredients(ingredients),
					ingredientIndex -> !isBroadIngredient(ingredients, ingredientIndex), true));
		} else {
			recipes.add(recipe instanceof ShapelessRecipe
					? EmiClientRecipeHelper.wrapSyntheticShapelessRecipe(recipeHolder.id(), recipe)
					: EmiClientRecipeHelper.wrapSyntheticShapedRecipe(recipeHolder.id(), recipe));
		}
		addFocusedInputSyntheticRecipes(recipeHolder, recipes);
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

	private static void addFocusedInputSyntheticRecipes(RecipeHolder<CraftingRecipe> recipeHolder, List<EmiRecipe> recipes) {
		ResourceLocation baseId = recipeHolder.id();
		CraftingRecipe recipe = recipeHolder.value();
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
				recipes.add(new SyntheticCraftingRecipe(id, recipeHolder, getFocusedInputIngredients(ingredients, ingredientIndex, stacks[stackIndex]),
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
		private final RecipeHolder<CraftingRecipe> recipeHolder;
		private final List<EmiIngredient> displayInputs;
		private final EmiStack output;
		private final boolean shapeless;

		private SyntheticCraftingRecipe(ResourceLocation id, RecipeHolder<CraftingRecipe> recipeHolder, List<EmiIngredient> displayInputs,
				IntPredicate inputIndexFilter, boolean indexOutput) {
			super(VanillaEmiRecipeCategories.CRAFTING, id.withPath(path -> path.startsWith("/") ? path : "/" + path), 118, 54);
			this.recipeHolder = recipeHolder;
			this.displayInputs = displayInputs;
			output = EmiStack.of(ClientRecipeHelper.getResultItem(recipeHolder.value()));
			shapeless = recipeHolder.value() instanceof ShapelessRecipe;
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
		public RecipeHolder<?> getBackingRecipe() {
			return recipeHolder;
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
