package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
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

import java.util.List;
import java.util.Optional;

public class DoubleChestTierUpgradeRecipe implements CraftingRecipe, IWrapperRecipe<ShapedRecipe> {
	public static final RecipeSerializer<DoubleChestTierUpgradeRecipe> SERIALIZER = RecipeWrapperSerializer.create(DoubleChestTierUpgradeRecipe::new,
			ShapedRecipe.SERIALIZER);
	private final ShapedRecipe compose;

	public DoubleChestTierUpgradeRecipe(ShapedRecipe compose) {
		this.compose = compose;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return compose.matches(input, level) && getDoubleChest(input).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack upgradedStorage = compose.assemble(input);
		getDoubleChest(input).ifPresent(originalStorage -> {
			upgradedStorage.applyComponents(originalStorage.getComponentsPatch());
		});
		if (upgradedStorage.has(ModCoreDataComponents.STORAGE_UUID)) {
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
