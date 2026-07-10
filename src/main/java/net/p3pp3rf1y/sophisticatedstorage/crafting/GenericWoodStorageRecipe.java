package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.data.BlockFamily;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GenericWoodStorageRecipe implements CraftingRecipe, IWrapperRecipe<ShapedRecipe> {
	public static final RecipeSerializer<GenericWoodStorageRecipe> SERIALIZER = RecipeWrapperSerializer.create(GenericWoodStorageRecipe::new,
			ShapedRecipe.SERIALIZER);
	private final ShapedRecipe compose;

	public GenericWoodStorageRecipe(ShapedRecipe compose) {
		this.compose = compose;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return compose.matches(input, level) && hasMixedOrNonCustomWood(input);
	}

	private record TopLeftCornerCoords(int left, int top) {
	}

	private TopLeftCornerCoords getTopLeftCornerCoords(CraftingInput input) {
		if (compose.getHeight() * compose.getWidth() == input.size()) {
			return new TopLeftCornerCoords(0, 0);
		}

		int minRow = Integer.MAX_VALUE;
		int minCol = Integer.MAX_VALUE;

		for (int row = 0; row <= input.height() - compose.getHeight(); row++) {
			for (int col = 0; col <= input.width() - compose.getWidth(); col++) {
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
		for (int row = topLeftCorner.top; row < topLeftCorner.top + compose.getHeight(); row++) {
			for (int col = topLeftCorner.left; col < topLeftCorner.left + compose.getWidth(); col++) {
				int inputSlot = col + row * input.width();
				int recipeSlot = col - topLeftCorner.left + (row - topLeftCorner.top) * compose.getWidth();
				ItemStack itemStack = input.getItem(inputSlot);
				if (itemStack.isEmpty() || compose.pattern.ingredients().get(recipeSlot).map(i -> i.getValues().size() < 2).orElse(true)) {
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
		for (int row = topLeftCorner.top; row < topLeftCorner.top + compose.getHeight(); row++) {
			for (int col = topLeftCorner.left; col < topLeftCorner.left + compose.getWidth(); col++) {
				int inputSlot = col + row * input.width();
				int recipeSlot = col - topLeftCorner.left + (row - topLeftCorner.top) * compose.getWidth();
				ItemStack itemStack = input.getItem(inputSlot);
				if (itemStack.isEmpty() || compose.pattern.ingredients().get(recipeSlot).map(i -> i.getValues().size() < 2).orElse(true)) {
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

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack result = compose.assemble(input);
		getGenericWoodType(input).ifPresent(woodType -> WoodStorageBlockItem.setWoodType(result, woodType));
		return result;
	}

	@Override
	public boolean showNotification() {
		return compose.showNotification();
	}

	@Override
	public String group() {
		return compose.group();
	}

	@Override
	public CraftingBookCategory category() {
		return compose.category();
	}

	@Override
	public PlacementInfo placementInfo() {
		return compose.placementInfo();
	}

	@Override
	public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
		return compose.display();
	}

}
