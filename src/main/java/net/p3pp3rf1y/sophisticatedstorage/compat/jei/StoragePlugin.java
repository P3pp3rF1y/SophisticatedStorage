package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.CraftingContainerRecipeTransferHandlerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SettingsGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.StorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings("unused")
@JeiPlugin
public class StoragePlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(SophisticatedStorage.MOD_ID, "default");
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		IIngredientSubtypeInterpreter<ItemStack> woodStorageNbtInterpreter = (itemStack, context) -> {
			StringJoiner result = new StringJoiner(",");
			WoodStorageBlockItem.getWoodType(itemStack).ifPresent(woodName -> result.add("woodName:" + woodName));
			StorageBlockItem.getMainColorFromStack(itemStack).ifPresent(mainColor -> result.add("mainColor:" + mainColor));
			StorageBlockItem.getAccentColorFromStack(itemStack).ifPresent(accentColor -> result.add("accentColor:" + accentColor));
			return "{" + result + "}";
		};
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.BARREL_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.IRON_BARREL_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.GOLD_BARREL_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.DIAMOND_BARREL_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.NETHERITE_BARREL_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.IRON_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.GOLD_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.DIAMOND_CHEST_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.NETHERITE_CHEST_ITEM.get(), woodStorageNbtInterpreter);

		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_BARREL_1_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_BARREL_2_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_BARREL_3_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_BARREL_4_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get(), woodStorageNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get(), woodStorageNbtInterpreter);

		IIngredientSubtypeInterpreter<ItemStack> shulkerBoxNbtInterpreter = (itemStack, context) -> {
			StringJoiner result = new StringJoiner(",");
			StorageBlockItem.getMainColorFromStack(itemStack).ifPresent(mainColor -> result.add("mainColor:" + mainColor));
			StorageBlockItem.getAccentColorFromStack(itemStack).ifPresent(accentColor -> result.add("accentColor:" + accentColor));
			return "{" + result + "}";
		};
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.IRON_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
		registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), shulkerBoxNbtInterpreter);
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
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
		ClientRecipeHelper.getRecipeByKey(ModBlocks.CONTROLLER_ITEM.getId()).ifPresent(controllerRecipe -> {
			if (controllerRecipe instanceof CraftingRecipe craftingRecipe) {
				recipeManager.hideRecipes(RecipeTypes.CRAFTING, Collections.singleton(craftingRecipe));
			}
		});
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(RecipeTypes.CRAFTING, DyeRecipesMaker.getRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, TierUpgradeRecipesMaker.getCraftingRecipes());
		registration.addRecipes(RecipeTypes.SMITHING, TierUpgradeRecipesMaker.getSmithingRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, ControllerRecipesMaker.getRecipes());
		registration.addRecipes(RecipeTypes.CRAFTING, ShulkerBoxFromChestRecipesMaker.getRecipes());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModItems.CRAFTING_UPGRADE.get()), RecipeTypes.CRAFTING);
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
