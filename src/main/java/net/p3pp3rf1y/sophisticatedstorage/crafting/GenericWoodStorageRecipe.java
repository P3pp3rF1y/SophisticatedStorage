package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class GenericWoodStorageRecipe extends ShapedRecipe implements IWrapperRecipe<ShapedRecipe> {
	private final ShapedRecipe compose;

	public GenericWoodStorageRecipe(ShapedRecipe compose) {
		super(compose.group(), compose.category(), compose.pattern, compose.result);
		this.compose = compose;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && hasMixedOrNonCustomWood(input);
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack result = super.assemble(input, registries);
		getGenericWoodType(input).ifPresent(woodType -> WoodStorageBlockItem.setWoodType(result, woodType));
		return result;
	}

	private record TopLeftCornerCoords(int left, int top) {
	}

	private TopLeftCornerCoords getTopLeftCornerCoords(CraftingInput input) {
		if (getHeight() * getWidth() == input.size()) {
			return new TopLeftCornerCoords(0, 0);
		}

		int minRow = Integer.MAX_VALUE;
		int minCol = Integer.MAX_VALUE;

		for (int row = 0; row <= input.height() - getHeight(); row++) {
			for (int col = 0; col <= input.width() - getWidth(); col++) {
				if (!input.getItem(col + row * input.width()).isEmpty()) {
					minRow = Math.min(minRow, row);
					minCol = Math.min(minCol, col);
				}
			}
		}
		return new TopLeftCornerCoords(minCol, minRow);
	}

	private boolean hasMixedOrNonCustomWood(CraftingInput input) {
		TopLeftCornerCoords topLeftCorner = getTopLeftCornerCoords(input);
		Set<BlockFamily> customFamilies = new LinkedHashSet<>();
		for (int row = topLeftCorner.top; row < topLeftCorner.top + getHeight(); row++) {
			for (int col = topLeftCorner.left; col < topLeftCorner.left + getWidth(); col++) {
				int inputSlot = col + row * input.width();
				int recipeSlot = col - topLeftCorner.left + (row - topLeftCorner.top) * getWidth();
				ItemStack itemStack = input.getItem(inputSlot);
				if (itemStack.isEmpty() || pattern.ingredients().get(recipeSlot).map(i -> i.getValues().size() < 2).orElse(true)) {
					continue;
				}

				Optional<BlockFamily> customBlockFamily = getCustomBlockFamily(itemStack.getItem());
				if (customBlockFamily.isEmpty()) {
					return true;
				} else {
					customFamilies.add(customBlockFamily.get());
				}
			}
		}

		return customFamilies.size() > 1;
	}

	private Optional<WoodType> getGenericWoodType(CraftingInput input) {
		TopLeftCornerCoords topLeftCorner = getTopLeftCornerCoords(input);
		Set<WoodType> woodTypes = new LinkedHashSet<>();
		for (int row = topLeftCorner.top; row < topLeftCorner.top + getHeight(); row++) {
			for (int col = topLeftCorner.left; col < topLeftCorner.left + getWidth(); col++) {
				int inputSlot = col + row * input.width();
				int recipeSlot = col - topLeftCorner.left + (row - topLeftCorner.top) * getWidth();
				ItemStack itemStack = input.getItem(inputSlot);
				if (itemStack.isEmpty() || pattern.ingredients().get(recipeSlot).map(i -> i.getValues().size() < 2).orElse(true)) {
					continue;
				}

				Optional<WoodType> woodType = GenericWoodStorageHelper.getWoodTypeForPlanks(itemStack)
						.or(() -> GenericWoodStorageHelper.getWoodTypeForSlab(itemStack));
				if (woodType.isEmpty()) {
					return Optional.empty();
				}
				woodTypes.add(woodType.get());
			}
		}
		return woodTypes.size() == 1 ? Optional.of(woodTypes.iterator().next()) : Optional.empty();
	}

	private Optional<BlockFamily> getCustomBlockFamily(Item item) {
		if (!(item instanceof BlockItem blockItem)) {
			return Optional.empty();
		}

		for (BlockFamily family : WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.values()) {
			if (blockItem.getBlock() == family.getBaseBlock() || blockItem.getBlock() == family.get(BlockFamily.Variant.SLAB)) {
				return Optional.of(family);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<GenericWoodStorageRecipe> getSerializer() {
		return ModBlocks.GENERIC_WOOD_STORAGE_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapedRecipe, GenericWoodStorageRecipe> {
		public Serializer() {
			super(GenericWoodStorageRecipe::new, SHAPED_RECIPE);
		}
	}
}
