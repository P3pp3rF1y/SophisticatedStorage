package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity.STORAGE_WRAPPER;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

	public StorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<CompoundTag> getEntityWrapperTagFromStack(DataComponentGetter componentHolder) {
		CustomData customData = componentHolder.get(() -> DataComponents.BLOCK_ENTITY_DATA);
		if (customData == null) {
			return Optional.empty();
		}
		return customData.copyTag().getCompound(STORAGE_WRAPPER);
	}

	public static Optional<Integer> getMainColorFromComponentHolder(DataComponentGetter componentHolder) {
		return getEntityWrapperTagFromStack(componentHolder).flatMap(tag -> tag.getInt(StorageWrapper.MAIN_COLOR))
				.or(() -> Optional.ofNullable(componentHolder.get(ModCoreDataComponents.MAIN_COLOR)));
	}

	public static Optional<Integer> getAccentColorFromComponentHolder(DataComponentGetter componentHolder) {
		return getEntityWrapperTagFromStack(componentHolder).flatMap(tag -> tag.getInt(StorageWrapper.ACCENT_COLOR))
				.or(() -> Optional.ofNullable(componentHolder.get(ModCoreDataComponents.ACCENT_COLOR)));
	}

	public static void setNumberOfInventorySlots(ItemStack storageStack, int numberOfInventorySlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
	}

	public static void setNumberOfUpgradeSlots(ItemStack storageStack, int numberOfUpgradeSlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
	}

	public static int getNumberOfInventorySlots(ItemStack storageStack) {
		int defaultNumberOfInventorySlots = getDefaultNumberOfInventorySlots(storageStack);
		int numberOfInventorySlots = Math.max(getStoredNumberOfInventorySlots(storageStack).orElse(defaultNumberOfInventorySlots),
				defaultNumberOfInventorySlots);
		Integer storedNumberOfInventorySlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
		if (storedNumberOfInventorySlots == null || storedNumberOfInventorySlots < numberOfInventorySlots) {
			storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		}
		return numberOfInventorySlots;
	}

	public static int getDefaultNumberOfInventorySlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock
				? storageBlock.getNumberOfInventorySlots()
				: 0;
	}

	private static Optional<Integer> getStoredNumberOfInventorySlots(ItemStack storageStack) {
		Optional<Integer> numberOfInventorySlotsFromWrapperTag = getEntityWrapperTagFromStack(storageStack)
				.flatMap(tag -> tag.getInt(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS));
		Integer numberOfInventorySlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
		if (numberOfInventorySlotsFromWrapperTag.isEmpty() && numberOfInventorySlots == null) {
			return Optional.empty();
		}

		int storedNumberOfInventorySlots = numberOfInventorySlotsFromWrapperTag.orElse(numberOfInventorySlots == null ? 0 : numberOfInventorySlots);
		if (numberOfInventorySlots != null) {
			storedNumberOfInventorySlots = Math.max(storedNumberOfInventorySlots, numberOfInventorySlots);
		}
		return Optional.of(storedNumberOfInventorySlots);
	}

	public static int getNumberOfUpgradeSlots(ItemStack storageStack) {
		int defaultNumberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots(storageStack);
		int numberOfUpgradeSlots = Math.max(getStoredNumberOfUpgradeSlots(storageStack).orElse(defaultNumberOfUpgradeSlots), defaultNumberOfUpgradeSlots);
		Integer storedNumberOfUpgradeSlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);
		if (storedNumberOfUpgradeSlots == null || storedNumberOfUpgradeSlots < numberOfUpgradeSlots) {
			storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
		}
		return numberOfUpgradeSlots;
	}

	public static int getDefaultNumberOfUpgradeSlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock
				? storageBlock.getNumberOfUpgradeSlots()
				: 0;
	}

	private static Optional<Integer> getStoredNumberOfUpgradeSlots(ItemStack storageStack) {
		Optional<Integer> numberOfUpgradeSlotsFromWrapperTag = getEntityWrapperTagFromStack(storageStack)
				.flatMap(tag -> tag.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS));
		Integer numberOfUpgradeSlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);
		if (numberOfUpgradeSlotsFromWrapperTag.isEmpty() && numberOfUpgradeSlots == null) {
			return Optional.empty();
		}

		int storedNumberOfUpgradeSlots = numberOfUpgradeSlotsFromWrapperTag.orElse(numberOfUpgradeSlots == null ? 0 : numberOfUpgradeSlots);
		if (numberOfUpgradeSlots != null) {
			storedNumberOfUpgradeSlots = Math.max(storedNumberOfUpgradeSlots, numberOfUpgradeSlots);
		}
		return Optional.of(storedNumberOfUpgradeSlots);
	}

	public static boolean isLocked(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.LOCKED, false);
	}

	public static void setLocked(ItemStack stack, boolean locked) {
		if (locked) {
			stack.set(ModDataComponents.LOCKED, true);
		} else {
			stack.remove(ModDataComponents.LOCKED);
		}
	}

	@Override
	public void setMainColor(ItemStack storageStack, int mainColor) {
		storageStack.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
	}

	@Override
	public Optional<Integer> getMainColor(ItemStack storageStack) {
		return getMainColorFromComponentHolder(storageStack);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		storageStack.set(ModCoreDataComponents.ACCENT_COLOR, accentColor);
	}

	@Override
	public void removeMainColor(ItemStack stack) {
		stack.remove(ModCoreDataComponents.MAIN_COLOR);
	}

	@Override
	public void removeAccentColor(ItemStack stack) {
		stack.remove(ModCoreDataComponents.ACCENT_COLOR);
	}

	@Override
	public Optional<Integer> getAccentColor(ItemStack stack) {
		return getAccentColorFromComponentHolder(stack);
	}

	public static boolean showsTier(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.TIER_VISIBLE, true);
	}

	public static void setShowsTier(ItemStack stack, boolean showsTier) {
		if (showsTier) {
			stack.remove(ModDataComponents.TIER_VISIBLE);
		} else {
			stack.set(ModDataComponents.TIER_VISIBLE, false);
		}
	}
}
