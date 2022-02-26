package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.CraftingContainerRecipeTransferHandlerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.StorageGhostIngredientHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.ArrayList;
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
		IIngredientSubtypeInterpreter<ItemStack> barrelNbtInterpreter = (itemStack, context) -> {
			StringJoiner result = new StringJoiner(",");
			BarrelBlock.getWoodType(itemStack).ifPresent(woodName -> result.add("woodName:" + woodName));
			BarrelBlock.getMaincolorFromStack(itemStack).ifPresent(mainColor -> result.add("mainColor:" + mainColor));
			BarrelBlock.getAccentColorFromStack(itemStack).ifPresent(accentColor -> result.add("accentColor:" + accentColor));
			return "{" + result + "}";
		};
		registration.registerSubtypeInterpreter(ModBlocks.BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.IRON_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.GOLD_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.DIAMOND_BARREL_ITEM.get(), barrelNbtInterpreter);
		registration.registerSubtypeInterpreter(ModBlocks.NETHERITE_BARREL_ITEM.get(), barrelNbtInterpreter);
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
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(DyeRecipesMaker.getRecipes(), VanillaRecipeCategoryUid.CRAFTING);
		registration.addRecipes(TierUpgradeRecipesMaker.getCraftingRecipes(), VanillaRecipeCategoryUid.CRAFTING);
		registration.addRecipes(TierUpgradeRecipesMaker.getSmithingRecipes(), VanillaRecipeCategoryUid.SMITHING);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModItems.CRAFTING_UPGRADE.get()), VanillaRecipeCategoryUid.CRAFTING);
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
		}, VanillaRecipeCategoryUid.CRAFTING);
	}

}
