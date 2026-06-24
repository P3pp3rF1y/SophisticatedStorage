package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Optional;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

	public StorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<Integer> getMainColorFromComponentHolder(DataComponentGetter componentHolder) {
		return Optional.ofNullable(componentHolder.get(ModCoreDataComponents.MAIN_COLOR));
	}

	public static Optional<Integer> getAccentColorFromComponentHolder(DataComponentGetter componentHolder) {
		return Optional.ofNullable(componentHolder.get(ModCoreDataComponents.ACCENT_COLOR));
	}

	public static void setNumberOfInventorySlots(ItemStack storageStack, int numberOfInventorySlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
	}

	public static void setNumberOfUpgradeSlots(ItemStack storageStack, int numberOfUpgradeSlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
	}

	public static int getNumberOfInventorySlots(ItemStack storageStack) {
		int defaultNumberOfInventorySlots = getDefaultNumberOfInventorySlots(storageStack);
		Integer storedNumberOfInventorySlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
		int numberOfInventorySlots = Math.max(storedNumberOfInventorySlots == null ? defaultNumberOfInventorySlots : storedNumberOfInventorySlots,
				defaultNumberOfInventorySlots);
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

	public static int getNumberOfUpgradeSlots(ItemStack storageStack) {
		int defaultNumberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots(storageStack);
		Integer storedNumberOfUpgradeSlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);
		int numberOfUpgradeSlots = Math.max(storedNumberOfUpgradeSlots == null ? defaultNumberOfUpgradeSlots : storedNumberOfUpgradeSlots,
				defaultNumberOfUpgradeSlots);
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
