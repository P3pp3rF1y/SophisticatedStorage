package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
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

import java.util.List;
import java.util.Optional;

public class StorageTierUpgradeShapelessRecipe extends CustomShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	public static final RecipeSerializer<StorageTierUpgradeShapelessRecipe> SERIALIZER = RecipeWrapperSerializer.create(StorageTierUpgradeShapelessRecipe::new, ShapelessRecipe.SERIALIZER);
	private final ShapelessRecipe compose;

	public StorageTierUpgradeShapelessRecipe(ShapelessRecipe compose) {
		super("", CraftingBookCategory.MISC, compose.result, compose.ingredients);
		this.compose = compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && getOriginalStorage(input).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack upgradedStorage = super.assemble(input);
		getOriginalStorage(input).ifPresent(originalStorage -> upgradedStorage.applyComponents(originalStorage.getComponentsPatch()));
		if (upgradedStorage.has(ModCoreDataComponents.STORAGE_UUID)) {
			StackStorageWrapper storageWrapper = new StackStorageWrapper(upgradedStorage);
			StorageBlockItem.setNumberOfInventorySlots(upgradedStorage, storageWrapper.getDefaultNumberOfInventorySlots());
			StorageBlockItem.setNumberOfUpgradeSlots(upgradedStorage, storageWrapper.getDefaultNumberOfUpgradeSlots());
		}
		return upgradedStorage;
	}

	private Optional<ItemStack> getOriginalStorage(CraftingInput inv) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof StorageBlockItem
					&& (!(slotStack.getItem() instanceof ChestBlockItem) || !ChestBlockItem.isDoubleChest(slotStack))) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<StorageTierUpgradeShapelessRecipe> getSerializer() {
		return ModBlocks.STORAGE_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
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
