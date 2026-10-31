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
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.CraftingSpecEmiRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiCraftingRecipeHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiGridMenuInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiSettingsGhostDragDropHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiStorageGhostDragDropHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.GroupedCraftingEmiRecipe;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

		public void addWorkstation(Identifier id, Block icon, Item workstation) {
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
		Set<Identifier> craftingRecipeIds = catalog.getCraftingRecipes().stream().map(holder -> holder.id().identifier()).collect(Collectors.toSet());
		registry.removeRecipes(recipe -> recipe.getBackingRecipe() != null
				&& (catalog.replacesCraftingRecipe(recipe.getBackingRecipe()) || craftingRecipeIds.contains(recipe.getBackingRecipe().id().identifier())));

		catalog.getGroupedCraftingSpecs().stream().flatMap(spec -> spec.getAllDisplays().stream())
				.flatMap(recipeHolder -> GroupedCraftingEmiRecipe.ofGroupedUsageAndFocusedRecipes(recipeHolder).stream()).forEach(recipe -> {
					craftingRecipeHandler.addRecipe(recipe);
					registry.addRecipe(recipe);
				});
		catalog.getCraftingRecipes().stream().filter(recipeHolder -> !catalog.replacesCraftingRecipe(recipeHolder))
				.flatMap(recipeHolder -> wrapSyntheticCraftingRecipe(recipeHolder).stream())
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

	private static List<EmiRecipe> wrapSyntheticCraftingRecipe(RecipeHolder<CraftingRecipe> recipeHolder) {
		CraftingRecipe recipe = recipeHolder.value();
		List<Optional<Ingredient>> ingredients = getIngredients(recipe);
		List<EmiRecipe> recipes = new ArrayList<>();
		if (hasBroadIngredient(ingredients)) {
			recipes.add(new SyntheticCraftingRecipe(recipeHolder.id().identifier(), recipeHolder, getInputIngredients(ingredients),
					ingredientIndex -> !isBroadIngredient(ingredients, ingredientIndex), true));
		} else {
			recipes.add(recipe instanceof ShapelessRecipe
					? EmiClientRecipeHelper.wrapSyntheticShapelessRecipe(recipeHolder.id().identifier(), recipe)
					: EmiClientRecipeHelper.wrapSyntheticShapedRecipe(recipeHolder.id().identifier(), recipe));
		}
		addFocusedInputSyntheticRecipes(recipeHolder, recipes);
		return recipes;
	}

	private static List<Optional<Ingredient>> getIngredients(CraftingRecipe recipe) {
		if (recipe instanceof GenericWoodStorageRecipe genericWoodRecipe) {
			return genericWoodRecipe.getCompose().getIngredients();
		}
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			return shapedRecipe.getIngredients();
		}
		return recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
	}

	private static ItemStack[] getItems(Optional<Ingredient> ingredient) {
		return ingredient.map(value -> value.items().map(ItemStack::new).toArray(ItemStack[]::new)).orElseGet(() -> new ItemStack[0]);
	}

	private static boolean hasBroadIngredient(List<Optional<Ingredient>> ingredients) {
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			if (isBroadIngredient(ingredients, ingredientIndex)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBroadIngredient(List<Optional<Ingredient>> ingredients, int ingredientIndex) {
		return getItems(ingredients.get(ingredientIndex)).length > 1;
	}

	private static void addFocusedInputSyntheticRecipes(RecipeHolder<CraftingRecipe> recipeHolder, List<EmiRecipe> recipes) {
		Identifier baseId = recipeHolder.id().identifier();
		CraftingRecipe recipe = recipeHolder.value();
		if (recipe instanceof GenericWoodStorageRecipe) {
			return;
		}
		List<Optional<Ingredient>> ingredients = getIngredients(recipe);
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			ItemStack[] stacks = getItems(ingredients.get(ingredientIndex));
			if (stacks.length <= 1) {
				continue;
			}
			for (int stackIndex = 0; stackIndex < stacks.length; stackIndex++) {
				int focusedIngredientIndex = ingredientIndex;
				int focusedStackIndex = stackIndex;
				Identifier id = baseId.withPath(path -> path + "/input/" + focusedIngredientIndex + "/" + focusedStackIndex);
				recipes.add(new SyntheticCraftingRecipe(id, recipeHolder, getFocusedInputIngredients(ingredients, ingredientIndex, stacks[stackIndex]),
						inputIndex -> true, false));
			}
		}
	}

	private static List<EmiIngredient> getInputIngredients(List<Optional<Ingredient>> ingredients) {
		List<EmiIngredient> inputIngredients = new ArrayList<>(ingredients.size());
		for (Optional<Ingredient> ingredient : ingredients) {
			inputIngredients.add(EmiIngredient.of(java.util.Arrays.stream(getItems(ingredient)).map(EmiStack::of).toList()));
		}
		return inputIngredients;
	}

	private static List<EmiIngredient> getFocusedInputIngredients(List<Optional<Ingredient>> ingredients, int focusedIngredientIndex, ItemStack focusedStack) {
		List<EmiIngredient> focusedIngredients = new ArrayList<>(ingredients.size());
		for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
			if (ingredientIndex == focusedIngredientIndex) {
				focusedIngredients.add(EmiStack.of(focusedStack));
			} else {
				focusedIngredients.add(EmiIngredient.of(java.util.Arrays.stream(getItems(ingredients.get(ingredientIndex))).map(EmiStack::of).toList()));
			}
		}
		return focusedIngredients;
	}

	private static class SyntheticCraftingRecipe extends BasicEmiRecipe {
		private final RecipeHolder<CraftingRecipe> recipeHolder;
		private final List<EmiIngredient> displayInputs;
		private final List<List<ItemStack>> displayInputSlots;
		private final EmiStack output;
		private final boolean shapeless;
		private final int width;

		private SyntheticCraftingRecipe(Identifier id, RecipeHolder<CraftingRecipe> recipeHolder, List<EmiIngredient> displayInputs,
				IntPredicate inputIndexFilter, boolean indexOutput) {
			super(VanillaEmiRecipeCategories.CRAFTING, id.withPath(path -> path.startsWith("/") ? path : "/" + path), 118, 54);
			this.recipeHolder = recipeHolder;
			this.displayInputs = displayInputs;
			displayInputSlots = displayInputs.stream().map(EmiIngredient::getEmiStacks).map(stacks -> stacks.stream().map(EmiStack::getItemStack).toList())
					.toList();
			output = EmiStack.of(ClientRecipeHelper.getResultItem(recipeHolder.value()));
			shapeless = recipeHolder.value() instanceof ShapelessRecipe;
			width = shapeless
					? displayInputs.size()
					: recipeHolder.value() instanceof GenericWoodStorageRecipe genericWoodRecipe
							? genericWoodRecipe.getCompose().getWidth()
							: ((ShapedRecipe) recipeHolder.value()).getWidth();
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
