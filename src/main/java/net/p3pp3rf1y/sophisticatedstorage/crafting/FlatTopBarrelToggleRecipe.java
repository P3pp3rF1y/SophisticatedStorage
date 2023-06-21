package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

public class FlatTopBarrelToggleRecipe extends CustomRecipe {
	public FlatTopBarrelToggleRecipe(ResourceLocation registryName, CraftingBookCategory category) {
		super(registryName, category);
	}

	@Override
	public boolean matches(CraftingContainer container, Level level) {
		boolean barrelFound = false;
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack item = container.getItem(i);
			if (item.isEmpty()) {
				continue;
			}

			if (!barrelFound && item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
				barrelFound = true;
			} else {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack item = container.getItem(i);
			if (item.isEmpty()) {
				continue;
			}

			if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
				ItemStack result = item.copy();
				result.setCount(1);
				BarrelBlockItem.toggleFlatTop(result);
				return result;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.FLAT_TOP_BARREL_TOGGLE_RECIPE_SERIALIZER.get();
	}
}
