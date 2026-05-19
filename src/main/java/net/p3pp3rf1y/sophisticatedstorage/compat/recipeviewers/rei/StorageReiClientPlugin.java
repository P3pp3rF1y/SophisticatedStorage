package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.rei;

import dev.architectury.event.EventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.*;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.StorageRecipeViewerDisplays;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.*;
import java.util.function.Consumer;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@SuppressWarnings("unused")
@REIPluginClient
public class StorageReiClientPlugin implements REIClientPlugin {
	private static Consumer<WorkstationRegistration> additionalWorkstations = registration -> {};
	private IRecipeViewerDisplayCatalog catalog = null;

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
	public void preStage(PluginManager<REIClientPlugin> manager, ReloadStage stage) {
		if (stage == ReloadStage.START) {
			catalog = null;
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
	public void registerEntries(EntryRegistry registry) {
		ModBlocks.ITEMS.getEntries().stream()
				.map(holder -> holder.get())
				.filter(StorageBlockItem.class::isInstance)
				.map(StorageBlockItem.class::cast)
				.forEach(storageItem -> getCreativeVariants(storageItem).stream()
						.filter(stack -> !registry.alreadyContain(EntryStacks.of(stack)))
						.forEach(stack -> registry.addEntry(EntryStacks.of(stack))));
	}

	@Override
	public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
		ModBlocks.ITEMS.getEntries().stream()
				.map(holder -> holder.get())
				.filter(StorageBlockItem.class::isInstance)
				.map(StorageBlockItem.class::cast)
				.forEach(storageItem -> {
					List<ItemStack> variants = getCreativeVariants(storageItem);
					if (variants.size() > 1) {
						registry.group(getCollapseId(storageItem), storageItem.getName(storageItem.getDefaultInstance()), variants.stream().map(EntryStacks::of).toList());
					}
				});
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		registry.registerGlobalDisplayGenerator(new GroupedCraftingReiDisplayGenerator(this::getCatalog, stack -> true));
		registry.registerGlobalDisplayGenerator(new CraftingSpecReiDisplayGenerator(this::getCatalog, stack -> true));
		registry.registerVisibilityPredicate((category, display) -> {
			if (display instanceof CraftingSpecReiDisplay) {
				return EventResult.pass();
			}
			if (display instanceof DefaultCraftingDisplay<?> craftingDisplay && craftingDisplay.getOptionalRecipe().isPresent()
					&& getCatalog().replacesCraftingRecipe(craftingDisplay.getOptionalRecipe().get())) {
				return EventResult.interruptFalse();
			}
			return EventResult.pass();
		});

		getCatalog().getCraftingRecipes().forEach(registry::add);
	}

	private IRecipeViewerDisplayCatalog getCatalog() {
		if (catalog == null) {
			catalog = createCatalog(getSubtypeInterpreters());
		}
		return catalog;
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

	private static List<ItemStack> getCreativeVariants(StorageBlockItem storageItem) {
		List<ItemStack> variants = new ArrayList<>();
		storageItem.addCreativeTabItems(variants::add);
		return variants;
	}

	private static ResourceLocation getCollapseId(StorageBlockItem storageItem) {
		return new ResourceLocation(SophisticatedStorage.MOD_ID, "rei_group/" + BuiltInRegistries.ITEM.getKey(storageItem).getPath());
	}

	private static IRecipeViewerDisplayCatalog createCatalog(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters) {
		IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
		IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
		StorageRecipeViewerDisplays.register(catalog, context);
		return catalog;
	}
}
