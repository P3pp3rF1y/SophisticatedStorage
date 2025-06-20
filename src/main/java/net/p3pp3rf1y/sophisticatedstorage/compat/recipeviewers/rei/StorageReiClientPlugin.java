package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiCraftingContainerTransferHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiSettingsGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiStorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.DyeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.FlatBarrelRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.ShulkerBoxFromChestRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.TierUpgradeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreter;
import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@SuppressWarnings("unused")
@REIPluginClient
public class StorageReiClientPlugin implements REIClientPlugin {
	private static Consumer<WorkstationRegistration> additionalWorkstations = registration -> {
	};

	public static void addAdditionalWorkstations(Consumer<WorkstationRegistration> additionalWorkstations) {
		StorageReiClientPlugin.additionalWorkstations = StorageReiClientPlugin.additionalWorkstations.andThen(additionalWorkstations);
	}

	public static class WorkstationRegistration {
		private final CategoryRegistry registry;

		private WorkstationRegistration(CategoryRegistry registry) {
			this.registry = registry;
		}

		public void addWorkstations(CategoryIdentifier<? extends Display> id, Item... workstations) {
			registry.addWorkstations(id, Arrays.stream(workstations).map(EntryStacks::of).toArray(EntryStack[]::new));
		}
	}

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(StorageScreen.class, screen -> {
			List<Rect2i> ret = new ArrayList<>();
			screen.getUpgradeSlotsRectangle().ifPresent(ret::add);
			ret.addAll(screen.getUpgradeSettingsControl().getTabRectangles());
			screen.getSortButtonsRectangle().ifPresent(ret::add);
			return ret.stream().map(r -> new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight())).toList();
		});

		zones.register(StorageSettingsScreen.class, screen -> screen.getExtendedControlsRectangles().stream().map(r -> new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight())).toList());
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerDraggableStackVisitor(new ReiStorageGhostIngredientHandler<>(StorageScreen.class));
		registry.registerDraggableStackVisitor(new ReiSettingsGhostIngredientHandler<>(StorageSettingsScreen.class));
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
		ReiRecipeDisplayGenerator generator = new ReiRecipeDisplayGenerator(registry);

		DyeRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		TierUpgradeRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		ShulkerBoxFromChestRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		FlatBarrelRecipesMaker.addRecipes(generator);
	}

	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(ModItems.CRAFTING_UPGRADE.get()));
		registry.addWorkstations(BuiltinPlugin.STONE_CUTTING, EntryStacks.of(ModItems.STONECUTTER_UPGRADE.get()));
		additionalWorkstations.accept(new WorkstationRegistration(registry));
	}

	@Override
	public void registerTransferHandlers(TransferHandlerRegistry registry) {
		registry.register(ReiCraftingContainerTransferHandler.crafting(StorageContainerMenu.class));
	}
}