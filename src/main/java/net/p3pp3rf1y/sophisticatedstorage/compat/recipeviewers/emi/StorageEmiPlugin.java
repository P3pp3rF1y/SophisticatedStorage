package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiGridMenuInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiSettingsGhostDragDropHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiStorageGhostDragDropHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.comparison.EmiSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.DyeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.FlatBarrelRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.ShulkerBoxFromChestRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.TierUpgradeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromVanillaShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.Map;
import java.util.function.Consumer;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreter;
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
		registry.addExclusionArea(StorageScreen.class, (screen, consumer) -> {
			//noinspection ConstantValue
			if (screen == null || screen.getUpgradeSettingsControl() == null) {
				return;
			}
			screen.getUpgradeSlotsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
			screen.getUpgradeSettingsControl().getTabRectangles().forEach(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
			screen.getSortButtonsRectangle().ifPresent(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		});
		registry.addExclusionArea(StorageSettingsScreen.class, (screen, consumer) -> {
			//noinspection ConstantValue
			if (screen == null || screen.getSettingsTabControl() == null) { // Due to how Emi collects the exclusion area this can be null
				return;
			}
			screen.getSettingsTabControl().getTabRectangles().forEach(r -> consumer.accept(new Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
		});

		registry.addDragDropHandler(StorageScreen.class, new EmiStorageGhostDragDropHandler<>());
		registry.addDragDropHandler(SettingsScreen.class, new EmiSettingsGhostDragDropHandler<>());
	}

	private void registerRecipes(EmiRegistry registry) {
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();

		DyeRecipesMaker.getRecipes(
						stack -> getSubtypeInterpreter(subtypeInterpreters, stack),
						EmiClientRecipeHelper::wrapSyntheticShapedRecipe
				)
				.forEach(registry::addRecipe);

		TierUpgradeRecipesMaker.getShapedCraftingRecipes(
						stack -> getSubtypeInterpreter(subtypeInterpreters, stack),
						EmiClientRecipeHelper::wrapSyntheticShapedRecipe
				)
				.forEach(registry::addRecipe);

		TierUpgradeRecipesMaker.getShapelessCraftingRecipes(
						stack -> getSubtypeInterpreter(subtypeInterpreters, stack),
						EmiClientRecipeHelper::wrapSyntheticShapelessRecipe
				)
				.forEach(registry::addRecipe);

		ShulkerBoxFromChestRecipesMaker.getShapedRecipes(
						stack -> getSubtypeInterpreter(subtypeInterpreters, stack),
						EmiClientRecipeHelper::wrapSyntheticShapedRecipe
				)
				.forEach(registry::addRecipe);

		ClientRecipeHelper.transformAllRecipesOfType(
						RecipeType.CRAFTING,
						ShulkerBoxFromVanillaShapelessRecipe.class,
						EmiClientRecipeHelper::wrapSyntheticShapelessRecipe
				)
				.forEach(registry::addRecipe);

		FlatBarrelRecipesMaker.getShapelessRecipes(EmiClientRecipeHelper::wrapSyntheticShapelessRecipe)
				.forEach(registry::addRecipe);
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