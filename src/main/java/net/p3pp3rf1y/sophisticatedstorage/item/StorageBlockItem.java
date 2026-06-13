package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.entity.StorageHolderBase;

import java.util.Optional;
import java.util.UUID;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

	public static final String ACCENT_COLOR_TAG = "accentColor";
	public static final String MAIN_COLOR_TAG = "mainColor";
	private static final String SHOWS_TIER_TAG = "showsTier";

	public StorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<CompoundTag> getEntityWrapperTagFromStack(ItemStack barrelStack) {
		return NBTHelper.getCompound(barrelStack, "BlockEntityTag").flatMap(tag -> NBTHelper.getCompound(tag, "storageWrapper"));
	}

	public static Optional<Integer> getMainColorFromStack(ItemStack barrelStack) {
		return getEntityWrapperTagFromStack(barrelStack).map(tag -> NBTHelper.getInt(tag, MAIN_COLOR_TAG)).orElse(NBTHelper.getInt(barrelStack, MAIN_COLOR_TAG));
	}

	public static Optional<Integer> getAccentColorFromStack(ItemStack barrelStack) {
		return getEntityWrapperTagFromStack(barrelStack).map(tag -> NBTHelper.getInt(tag, ACCENT_COLOR_TAG)).orElse(NBTHelper.getInt(barrelStack, ACCENT_COLOR_TAG));
	}

	public static void setNumberOfInventorySlots(ItemStack storageStack, int numberOfInventorySlots) {
		NBTHelper.setInteger(storageStack, StackStorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG, numberOfInventorySlots);
	}

	public static void setNumberOfUpgradeSlots(ItemStack storageStack, int numberOfUpgradeSlots) {
		NBTHelper.setInteger(storageStack, StackStorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, numberOfUpgradeSlots);
	}

	public static int getNumberOfInventorySlots(ItemStack storageStack) {
		int defaultNumberOfInventorySlots = getDefaultNumberOfInventorySlots(storageStack);
		int numberOfInventorySlots = Math.max(getStoredNumberOfInventorySlots(storageStack).orElse(defaultNumberOfInventorySlots), defaultNumberOfInventorySlots);
		Optional<Integer> storedNumberOfInventorySlots = NBTHelper.getInt(storageStack, StackStorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG);
		if (storedNumberOfInventorySlots.isEmpty() || storedNumberOfInventorySlots.get() < numberOfInventorySlots) {
			setNumberOfInventorySlots(storageStack, numberOfInventorySlots);
		}
		return numberOfInventorySlots;
	}

	public static int getDefaultNumberOfInventorySlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
	}

	private static Optional<Integer> getStoredNumberOfInventorySlots(ItemStack storageStack) {
		Optional<Integer> numberOfInventorySlotsFromWrapperTag = getEntityWrapperTagFromStack(storageStack)
				.flatMap(tag -> tag.contains(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG) ? Optional.of(tag.getInt(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG)) : Optional.empty());
		Optional<Integer> numberOfInventorySlots = NBTHelper.getInt(storageStack, StackStorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG);
		if (numberOfInventorySlotsFromWrapperTag.isEmpty()) {
			return numberOfInventorySlots;
		}

		int storedNumberOfInventorySlots = numberOfInventorySlotsFromWrapperTag.get();
		if (numberOfInventorySlots.isPresent()) {
			storedNumberOfInventorySlots = Math.max(storedNumberOfInventorySlots, numberOfInventorySlots.get());
		}
		return Optional.of(storedNumberOfInventorySlots);
	}

	public static int getNumberOfUpgradeSlots(ItemStack storageStack) {
		int defaultNumberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots(storageStack);
		int numberOfUpgradeSlots = Math.max(getStoredNumberOfUpgradeSlots(storageStack).orElse(defaultNumberOfUpgradeSlots), defaultNumberOfUpgradeSlots);
		Optional<Integer> storedNumberOfUpgradeSlots = NBTHelper.getInt(storageStack, StackStorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG);
		if (storedNumberOfUpgradeSlots.isEmpty() || storedNumberOfUpgradeSlots.get() < numberOfUpgradeSlots) {
			setNumberOfUpgradeSlots(storageStack, numberOfUpgradeSlots);
		}
		return numberOfUpgradeSlots;
	}

	public static int getDefaultNumberOfUpgradeSlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
	}

	private static Optional<Integer> getStoredNumberOfUpgradeSlots(ItemStack storageStack) {
		Optional<Integer> numberOfUpgradeSlotsFromWrapperTag = getEntityWrapperTagFromStack(storageStack)
				.flatMap(tag -> tag.contains(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG) ? Optional.of(tag.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG)) : Optional.empty());
		Optional<Integer> numberOfUpgradeSlots = NBTHelper.getInt(storageStack, StackStorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG);
		if (numberOfUpgradeSlotsFromWrapperTag.isEmpty()) {
			return numberOfUpgradeSlots;
		}

		int storedNumberOfUpgradeSlots = numberOfUpgradeSlotsFromWrapperTag.get();
		if (numberOfUpgradeSlots.isPresent()) {
			storedNumberOfUpgradeSlots = Math.max(storedNumberOfUpgradeSlots, numberOfUpgradeSlots.get());
		}
		return Optional.of(storedNumberOfUpgradeSlots);
	}

	public static boolean isLocked(ItemStack stack) {
		return NBTHelper.getBoolean(stack, StorageHolderBase.LOCKED_TAG).orElse(false);
	}

	public static void setLocked(ItemStack stack, boolean locked) {
		if (locked) {
			stack.getOrCreateTag().putBoolean(StorageHolderBase.LOCKED_TAG, true);
		} else {
			NBTHelper.removeTag(stack, StorageHolderBase.LOCKED_TAG);
		}
	}

	@Override
	public void setMainColor(ItemStack storageStack, int mainColor) {
		storageStack.getOrCreateTag().putInt(MAIN_COLOR_TAG, mainColor);
	}

	@Override
	public Optional<Integer> getMainColor(ItemStack storageStack) {
		return StorageBlockItem.getMainColorFromStack(storageStack);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		storageStack.getOrCreateTag().putInt(ACCENT_COLOR_TAG, accentColor);
	}

	@Override
	public void removeMainColor(ItemStack stack) {
		NBTHelper.removeTag(stack, MAIN_COLOR_TAG);
	}

	@Override
	public void removeAccentColor(ItemStack stack) {
		NBTHelper.removeTag(stack, ACCENT_COLOR_TAG);
	}

	@Override
	public Optional<Integer> getAccentColor(ItemStack stack) {
		return StorageBlockItem.getAccentColorFromStack(stack);
	}

	public static boolean showsTier(ItemStack stack) {
		return NBTHelper.getBoolean(stack, SHOWS_TIER_TAG).orElse(true);
	}

	public static void setShowsTier(ItemStack stack, boolean showsTier) {
		if (showsTier) {
			NBTHelper.removeTag(stack, SHOWS_TIER_TAG);
		} else {
			stack.getOrCreateTag().putBoolean(SHOWS_TIER_TAG, false);
		}
	}

	public static Optional<UUID> getContentsUuid(ItemStack stack) {
		return NBTHelper.getUniqueId(stack, "uuid");
	}
}
