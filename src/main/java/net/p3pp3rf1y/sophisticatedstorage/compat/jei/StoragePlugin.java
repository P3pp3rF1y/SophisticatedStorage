package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.CraftingContainerRecipeTransferHandlerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SettingsGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.StorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes.BarrelSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes.ShulkerBoxSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes.WoodStorageSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@JeiPlugin
public class StoragePlugin implements IModPlugin {
	private static Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar = registration -> {
	};

	public static void setAdditionalCatalystRegistrar(Consumer<IRecipeCatalystRegistration> additionalCatalystRegistrar) {
		StoragePlugin.additionalCatalystRegistrar = additionalCatalystRegistrar;
	}

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "default");
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		ISubtypeInterpreter<ItemStack> woodStorageNbtInterpreter = new WoodStorageSubtypeInterpreter();
		ISubtypeInterpreter<ItemStack> barrelNbtInterpreter = new BarrelSubtypeInterpreter();

		registration.registerSubtypeInterpreter(ModBlocks.BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.COPPER_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.IRON_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.GOLD_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.DIAMOND_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.NETHERITE_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.COPPER_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.IRON_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.GOLD_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.DIAMOND_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.NETHERITE_CHEST_ITEM.get(), woodStorageNbtInterpreter);

		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_BARREL_4_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get(), barrelNbtInterpreter);

		ISubtypeInterpreter<ItemStack> shulkerBoxNbtInterpreter = new ShulkerBoxSubtypeInterpreter();
		registration.registerSubtypeInterpreter(ModBlocks.SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.IRON_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
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
				return new ArrayList<>(gui.getSettingsTabControl().getTabRectangles());
			}
		});

		registration.addGhostIngredientHandler(StorageScreen.class, new StorageGhostIngredientHandler<>());
		registration.addGhostIngredientHandler(SettingsScreen.class, new SettingsGhostIngredientHandler<>());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(RecipeTypes.CRAFTING, DyeRecipesMaker.getRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, TierUpgradeRecipesMaker.getShapedCraftingRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, TierUpgradeRecipesMaker.getShapelessCraftingRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, ShulkerBoxFromChestRecipesMaker.getRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, FlatBarrelRecipesMaker.getRecipes());
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
		registration.addRecipeTransferHandler(new CraftingContainerRecipeTransferHandlerBase<StorageContainerMenu>(handlerHelper, stackHelper) {
			@Override
			public Class<StorageContainerMenu> getContainerClass() {
				return StorageContainerMenu.class;
			}
		}, RecipeTypes.CRAFTING);
	}

}
