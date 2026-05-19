package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class DoubleChestTierUpgradeShapelessRecipe extends ShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	public static final Set<ResourceLocation> REGISTERED_RECIPES = new LinkedHashSet<>();
	private final ShapelessRecipe compose;

	public DoubleChestTierUpgradeShapelessRecipe(ShapelessRecipe compose) {
		super(compose.getId(), compose.getGroup(), compose.category(), compose.getResultItem(null), compose.getIngredients());
		this.compose = compose;
		REGISTERED_RECIPES.add(compose.getId());
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		return super.matches(inv, level) && getOriginalStorage(inv).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registries) {
		ItemStack upgradedStorage = super.assemble(inv, registries);
		getOriginalStorage(inv).ifPresent(originalStorage -> upgradedStorage.setTag(originalStorage.getTag()));
		if (StorageBlockItem.getContentsUuid(upgradedStorage).isPresent()) {
			StackStorageWrapper storageWrapper = new StackStorageWrapper(upgradedStorage);
			StorageBlockItem.setNumberOfInventorySlots(upgradedStorage, storageWrapper.getDefaultNumberOfInventorySlots() * 2);
			StorageBlockItem.setNumberOfUpgradeSlots(upgradedStorage, storageWrapper.getDefaultNumberOfUpgradeSlots() * 2);
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
			if (slotStack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(slotStack)) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.DOUBLE_CHEST_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapelessRecipe, DoubleChestTierUpgradeShapelessRecipe> {
		public Serializer() {
			super(DoubleChestTierUpgradeShapelessRecipe::new, RecipeSerializer.SHAPELESS_RECIPE);
		}
	}
}
