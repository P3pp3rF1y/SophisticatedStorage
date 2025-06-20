package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.CustomShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.Optional;

public class DoubleChestTierUpgradeShapelessRecipe extends CustomShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	private final ShapelessRecipe compose;

	public DoubleChestTierUpgradeShapelessRecipe(ShapelessRecipe compose) {
		super(compose.group(), compose.category(), compose.result, compose.ingredients);
		this.compose = compose;
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && getOriginalStorage(input).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack upgradedStorage = super.assemble(input, registries);
		getOriginalStorage(input).ifPresent(originalStorage -> upgradedStorage.applyComponents(originalStorage.getComponentsPatch()));
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

	private Optional<ItemStack> getOriginalStorage(CraftingInput inv) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(slotStack)) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<DoubleChestTierUpgradeShapelessRecipe> getSerializer() {
		return ModBlocks.DOUBLE_CHEST_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapelessRecipe, DoubleChestTierUpgradeShapelessRecipe> {
		public Serializer() {
			super(DoubleChestTierUpgradeShapelessRecipe::new, RecipeSerializer.SHAPELESS_RECIPE);
		}
	}
}
