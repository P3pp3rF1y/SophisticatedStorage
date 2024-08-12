package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.Optional;

public class StorageTierUpgradeShapelessRecipe extends ShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	private final ShapelessRecipe compose;

	public StorageTierUpgradeShapelessRecipe(ShapelessRecipe compose) {
		super(compose.getGroup(), compose.category(), compose.result, compose.getIngredients());
		this.compose = compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && getOriginalStorage(input).map(storage -> !(storage.getItem() instanceof WoodStorageBlockItem) || !WoodStorageBlockItem.isPacked(storage)).orElse(false);
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack upgradedStorage = super.assemble(input, registries);
		getOriginalStorage(input).ifPresent(originalStorage -> upgradedStorage.applyComponents(originalStorage.getComponents()));
		if (upgradedStorage.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
			StackStorageWrapper wrapper = StackStorageWrapper.fromStack(registries, upgradedStorage);
			shulkerBoxItem.setNumberOfInventorySlots(upgradedStorage, wrapper.getDefaultNumberOfInventorySlots());
			shulkerBoxItem.setNumberOfUpgradeSlots(upgradedStorage, wrapper.getDefaultNumberOfUpgradeSlots());
		}
		return upgradedStorage;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getOriginalStorage(CraftingInput inv) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof IStorageBlock) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.STORAGE_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapelessRecipe, StorageTierUpgradeShapelessRecipe> {
		public Serializer() {
			super(StorageTierUpgradeShapelessRecipe::new, RecipeSerializer.SHAPELESS_RECIPE);
		}
	}
}
