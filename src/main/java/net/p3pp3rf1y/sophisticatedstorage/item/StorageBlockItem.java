package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity.STORAGE_WRAPPER_TAG;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

	public StorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static Optional<CompoundTag> getEntityWrapperTagFromStack(IDataComponentHolderExtension componentHolder) {
		if (componentHolder instanceof ItemStack storageStack) {
			Optional<CompoundTag> legacyWrapperTag = LegacyStorageBlockDataMigration.getEntityWrapperTagFromStack(storageStack);
			if (legacyWrapperTag.isPresent()) {
				return legacyWrapperTag;
			}
		}

		CustomData customData = componentHolder.get(() -> DataComponents.BLOCK_ENTITY_DATA);
		if (customData == null) {
			return Optional.empty();
		}
		return Optional.of(customData.copyTag().getCompound(STORAGE_WRAPPER_TAG));
	}

	public static Optional<Integer> getMainColorFromComponentHolder(IDataComponentHolderExtension componentHolder) {
		return getEntityWrapperTagFromStack(componentHolder)
				.flatMap(tag -> tag.contains(StorageWrapper.MAIN_COLOR_TAG) ? Optional.of(tag.getInt(StorageWrapper.MAIN_COLOR_TAG)) : Optional.empty())
				.or(() -> Optional.ofNullable(componentHolder.get(ModCoreDataComponents.MAIN_COLOR)))
				.or(() -> componentHolder instanceof ItemStack storageStack ? LegacyStorageBlockDataMigration.getMainColor(storageStack) : Optional.empty());
	}

	public static Optional<Integer> getAccentColorFromComponentHolder(IDataComponentHolderExtension componentHolder) {
		return getEntityWrapperTagFromStack(componentHolder)
				.flatMap(tag -> tag.contains(StorageWrapper.ACCENT_COLOR_TAG) ? Optional.of(tag.getInt(StorageWrapper.ACCENT_COLOR_TAG)) : Optional.empty())
				.or(() -> Optional.ofNullable(componentHolder.get(ModCoreDataComponents.ACCENT_COLOR)))
				.or(() -> componentHolder instanceof ItemStack storageStack ? LegacyStorageBlockDataMigration.getAccentColor(storageStack) : Optional.empty());
	}

	public static void setNumberOfInventorySlots(ItemStack storageStack, int numberOfInventorySlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
	}

	public static  void setNumberOfUpgradeSlots(ItemStack storageStack, int numberOfUpgradeSlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
	}

	public static int getNumberOfInventorySlots(ItemStack storageStack) {
		Integer numberOfInventorySlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
		if (numberOfInventorySlots != null) {
			return numberOfInventorySlots;
		}
		return LegacyStorageBlockDataMigration.getNumberOfInventorySlots(storageStack).map(slots -> {
			storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, slots);
			return slots;
		}).orElse(0);
	}

	public static int getNumberOfUpgradeSlots(ItemStack storageStack) {
		Integer numberOfUpgradeSlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);
		if (numberOfUpgradeSlots != null) {
			return numberOfUpgradeSlots;
		}
		return LegacyStorageBlockDataMigration.getNumberOfUpgradeSlots(storageStack).map(slots -> {
			storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, slots);
			return slots;
		}).orElse(0);
	}

	public static boolean isLocked(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.LOCKED, LegacyStorageBlockDataMigration.getLocked(stack).orElse(false));
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
		LegacyStorageBlockDataMigration.normalizeLegacyData(storageStack);
		return StorageBlockItem.getMainColorFromComponentHolder(storageStack);
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
		LegacyStorageBlockDataMigration.normalizeLegacyData(stack);
		return StorageBlockItem.getAccentColorFromComponentHolder(stack);
	}

	public static boolean showsTier(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.SHOWS_TIER, LegacyStorageBlockDataMigration.getShowsTier(stack).orElse(true));
	}

	public static void setShowsTier(ItemStack stack, boolean showsTier) {
		if (showsTier) {
			stack.remove(ModDataComponents.SHOWS_TIER);
		} else {
			stack.set(ModDataComponents.SHOWS_TIER, false);
		}
	}
}
