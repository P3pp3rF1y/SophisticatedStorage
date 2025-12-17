package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiSettingsGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiStorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.subtypes.JeiSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.DyeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.FlatBarrelRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.ShulkerBoxFromChestRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.TierUpgradeRecipesMaker;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromVanillaShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreter;
import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@SuppressWarnings("unused")
@JeiPlugin
public class StorageJeiPlugin implements IModPlugin {
	private static Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar = registration -> {};
	public static void addAdditionalCatalystRegistrar(Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar) {
		StorageJeiPlugin.additionalCatalystRegistrar = StorageJeiPlugin.additionalCatalystRegistrar.andThen(additionalCatalystRegistrar);
	}

	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "default");
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		getSubtypeInterpreters()
				.forEach((item, subtypeInterpreter) -> registration.registerSubtypeInterpreter(item, JeiSubtypeInterpreter.of(subtypeInterpreter)));
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
		Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
		JeiRecipeDisplayGenerator generator = new JeiRecipeDisplayGenerator();

		DyeRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		TierUpgradeRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		ShulkerBoxFromChestRecipesMaker.addRecipes(generator, stack -> getSubtypeInterpreter(subtypeInterpreters, stack));
		ClientRecipeHelper.addAllRecipesOfType(generator, RecipeType.CRAFTING, ShulkerBoxFromVanillaShapelessRecipe.class);
		FlatBarrelRecipesMaker.addRecipes(generator);

		registration.addRecipes(RecipeTypes.CRAFTING, generator.getCraftingRecipes());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(RecipeTypes.CRAFTING, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
		registration.addCraftingStation(RecipeTypes.STONECUTTING, new ItemStack(ModItems.STONECUTTER_UPGRADE.get()));
		additionalCatalystRegistrar.accept(registration);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		IRecipeTransferHandlerHelper handlerHelper = registration.getTransferHelper();
		IStackHelper stackHelper = registration.getJeiHelpers().getStackHelper();
		registration.addRecipeTransferHandler(new JeiCraftingContainerRecipeTransferHandlerBase<StorageContainerMenu, RecipeHolder<CraftingRecipe>>(handlerHelper, stackHelper) {
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
