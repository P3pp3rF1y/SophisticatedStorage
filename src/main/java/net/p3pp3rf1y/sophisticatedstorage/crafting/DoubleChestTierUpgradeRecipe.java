package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.Optional;

public class DoubleChestTierUpgradeRecipe extends ShapedRecipe implements IWrapperRecipe<ShapedRecipe> {
	private final ShapedRecipe compose;

	public DoubleChestTierUpgradeRecipe(ShapedRecipe compose) {
		super(compose.group(), compose.category(), compose.pattern, compose.result);
		this.compose = compose;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && getDoubleChest(input).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack upgradedStorage = super.assemble(input, registries);
		getDoubleChest(input).ifPresent(originalStorage -> {
			upgradedStorage.applyComponents(originalStorage.getComponentsPatch());
		});
		if (upgradedStorage.has(ModCoreDataComponents.STORAGE_UUID)) {
			StackStorageWrapper storageWrapper = StackStorageWrapper.fromStack(registries, upgradedStorage);
			StorageBlockItem.setNumberOfInventorySlots(upgradedStorage, storageWrapper.getDefaultNumberOfInventorySlots() * 2);
			StorageBlockItem.setNumberOfUpgradeSlots(upgradedStorage, storageWrapper.getDefaultNumberOfUpgradeSlots() * 2);
		}
		return upgradedStorage;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getDoubleChest(CraftingInput input) {
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack slotStack = input.getItem(slot);
			if (slotStack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(slotStack)) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<DoubleChestTierUpgradeRecipe> getSerializer() {
		return ModBlocks.DOUBLE_CHEST_TIER_UPGRADE_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapedRecipe, DoubleChestTierUpgradeRecipe> {
		public Serializer() {
			super(DoubleChestTierUpgradeRecipe::new, RecipeSerializer.SHAPED_RECIPE);
		}
	}
}
