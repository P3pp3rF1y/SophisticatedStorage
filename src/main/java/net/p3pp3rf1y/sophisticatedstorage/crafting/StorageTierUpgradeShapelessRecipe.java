package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
	public boolean matches(CraftingContainer inv, Level level) {
		return super.matches(inv, level) && getOriginalStorage(inv).map(storage -> !(storage.getItem() instanceof WoodStorageBlockItem) || !WoodStorageBlockItem.isPacked(storage)).orElse(false);
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
		ItemStack upgradedStorage = super.assemble(inv, registryAccess);
		getOriginalStorage(inv).ifPresent(originalStorage -> upgradedStorage.setTag(originalStorage.getTag()));
		if (upgradedStorage.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
			StackStorageWrapper wrapper = StackStorageWrapper.fromData(upgradedStorage);
			shulkerBoxItem.setNumberOfInventorySlots(upgradedStorage, wrapper.getDefaultNumberOfInventorySlots());
			shulkerBoxItem.setNumberOfUpgradeSlots(upgradedStorage, wrapper.getDefaultNumberOfUpgradeSlots());
		}
		return upgradedStorage;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getOriginalStorage(CraftingContainer inv) {
		for (int slot = 0; slot < inv.getContainerSize(); slot++) {
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
