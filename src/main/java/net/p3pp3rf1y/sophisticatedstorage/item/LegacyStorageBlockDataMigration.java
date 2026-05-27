package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Optional;
import java.util.UUID;

public final class LegacyStorageBlockDataMigration {
	private static final String UUID_TAG = "uuid";
	private static final String MAIN_COLOR_TAG = "mainColor";
	private static final String ACCENT_COLOR_TAG = "accentColor";
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	private static final String SORT_BY_TAG = "sortBy";
	private static final String NUMBER_OF_INVENTORY_SLOTS_TAG = "numberOfInventorySlots";
	private static final String NUMBER_OF_UPGRADE_SLOTS_TAG = "numberOfUpgradeSlots";
	private static final String RENDER_INFO_TAG = "renderInfo";
	private static final String PACKED_TAG = "packed";
	private static final String WOOD_TYPE_TAG = "woodType";
	private static final String LOCKED_TAG = "locked";
	private static final String SHOWS_TIER_TAG = "showsTier";
	private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
	private static final String REAL_COUNT_TAG = "realCount";

	private LegacyStorageBlockDataMigration() {}

	public static void normalizeLegacyData(ItemStack storageStack) {
		if (!storageStack.has(ModCoreDataComponents.STORAGE_UUID)) {
			getContentsUuid(storageStack).ifPresent(uuid -> storageStack.set(ModCoreDataComponents.STORAGE_UUID, uuid));
		}
		if (!storageStack.has(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS)) {
			getNumberOfInventorySlots(storageStack).ifPresent(slots -> storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, slots));
		}
		if (!storageStack.has(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS)) {
			getNumberOfUpgradeSlots(storageStack).ifPresent(slots -> storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, slots));
		}
		if (!storageStack.has(ModCoreDataComponents.MAIN_COLOR)) {
			getMainColor(storageStack).ifPresent(color -> storageStack.set(ModCoreDataComponents.MAIN_COLOR, color));
		}
		if (!storageStack.has(ModCoreDataComponents.ACCENT_COLOR)) {
			getAccentColor(storageStack).ifPresent(color -> storageStack.set(ModCoreDataComponents.ACCENT_COLOR, color));
		}
		if (!storageStack.has(ModCoreDataComponents.OPEN_TAB_ID)) {
			getOpenTabId(storageStack).ifPresent(openTabId -> storageStack.set(ModCoreDataComponents.OPEN_TAB_ID, openTabId));
		}
		if (!storageStack.has(ModCoreDataComponents.SORT_BY)) {
			getSortBy(storageStack).ifPresent(sortBy -> storageStack.set(ModCoreDataComponents.SORT_BY, sortBy));
		}
		if (!storageStack.has(ModCoreDataComponents.RENDER_INFO_TAG)) {
			getRenderInfo(storageStack).ifPresent(renderInfo -> storageStack.set(ModCoreDataComponents.RENDER_INFO_TAG, CustomData.of(renderInfo)));
		}
		if (!storageStack.has(ModDataComponents.PACKED)) {
			getPacked(storageStack).ifPresent(packed -> storageStack.set(ModDataComponents.PACKED, packed));
		}
		if (!storageStack.has(ModDataComponents.WOOD_TYPE)) {
			getWoodType(storageStack).ifPresent(woodType -> storageStack.set(ModDataComponents.WOOD_TYPE, woodType));
		}
		if (!storageStack.has(ModDataComponents.LOCKED)) {
			getLocked(storageStack).ifPresent(locked -> storageStack.set(ModDataComponents.LOCKED, locked));
		}
		if (!storageStack.has(ModDataComponents.SHOWS_TIER)) {
			getShowsTier(storageStack).ifPresent(showsTier -> storageStack.set(ModDataComponents.SHOWS_TIER, showsTier));
		}
	}

	public static Optional<CompoundTag> getEntityWrapperTagFromStack(ItemStack storageStack) {
		return getBlockEntityData(storageStack).map(blockEntityData -> {
			CompoundTag storageWrapper = blockEntityData.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG);
			normalizeStorageWrapperTag(storageWrapper);
			return storageWrapper;
		});
	}

	public static void normalizeStorageContents(CompoundTag storageContents) {
		normalizeBlockEntityTag(storageContents);
	}

	public static void normalizeBlockEntityTag(CompoundTag blockEntityTag) {
		if (blockEntityTag.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG)) {
			normalizeStorageWrapperTag(blockEntityTag.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG));
		}
	}

	private static void normalizeStorageWrapperTag(CompoundTag storageWrapper) {
		if (storageWrapper.contains(StorageWrapper.CONTENTS_TAG)) {
			normalizeContentsNbt(storageWrapper.getCompound(StorageWrapper.CONTENTS_TAG));
		}
	}

	private static void normalizeContentsNbt(CompoundTag contentsNbt) {
		normalizeInventory(contentsNbt, InventoryHandler.INVENTORY_TAG);
		normalizeInventory(contentsNbt, UpgradeHandler.UPGRADE_INVENTORY_TAG);
	}

	private static void normalizeInventory(CompoundTag contentsNbt, String inventoryTag) {
		if (!contentsNbt.contains(inventoryTag)) {
			return;
		}

		CompoundTag inventoryNbt = contentsNbt.getCompound(inventoryTag);
		if (!inventoryNbt.contains("Items")) {
			return;
		}

		ListTag items = inventoryNbt.getList("Items", Tag.TAG_COMPOUND);
		for (Tag item : items) {
			normalizeItemStackCount((CompoundTag) item);
		}
	}

	private static void normalizeItemStackCount(CompoundTag itemTag) {
		if (itemTag.contains("count")) {
			return;
		}

		if (itemTag.contains(REAL_COUNT_TAG)) {
			itemTag.putInt("count", itemTag.getInt(REAL_COUNT_TAG));
		} else if (itemTag.contains("Count")) {
			itemTag.putInt("count", itemTag.getByte("Count"));
		}
	}

	public static Optional<UUID> getContentsUuid(ItemStack storageStack) {
		return getUuid(storageStack, UUID_TAG);
	}

	public static Optional<Integer> getNumberOfInventorySlots(ItemStack storageStack) {
		return getInt(storageStack, NUMBER_OF_INVENTORY_SLOTS_TAG);
	}

	public static Optional<Integer> getNumberOfUpgradeSlots(ItemStack storageStack) {
		return getInt(storageStack, NUMBER_OF_UPGRADE_SLOTS_TAG);
	}

	public static Optional<Integer> getMainColor(ItemStack storageStack) {
		return getInt(storageStack, MAIN_COLOR_TAG);
	}

	public static Optional<Integer> getAccentColor(ItemStack storageStack) {
		return getInt(storageStack, ACCENT_COLOR_TAG);
	}

	public static Optional<Integer> getOpenTabId(ItemStack storageStack) {
		return getInt(storageStack, OPEN_TAB_ID_TAG);
	}

	public static Optional<SortBy> getSortBy(ItemStack storageStack) {
		return getString(storageStack, SORT_BY_TAG).map(SortBy::fromName);
	}

	public static Optional<CompoundTag> getRenderInfo(ItemStack storageStack) {
		return getCompound(storageStack, RENDER_INFO_TAG);
	}

	public static Optional<Boolean> getPacked(ItemStack storageStack) {
		return getBoolean(storageStack, PACKED_TAG);
	}

	public static Optional<WoodType> getWoodType(ItemStack storageStack) {
		return getString(storageStack, WOOD_TYPE_TAG).flatMap(woodType -> Optional.ofNullable(WoodType.TYPES.get(woodType)));
	}

	public static Optional<Boolean> getLocked(ItemStack storageStack) {
		return getBoolean(storageStack, LOCKED_TAG);
	}

	public static Optional<Boolean> getShowsTier(ItemStack storageStack) {
		return getBoolean(storageStack, SHOWS_TIER_TAG);
	}

	private static Optional<UUID> getUuid(ItemStack storageStack, String key) {
		return getLegacyCustomData(storageStack).flatMap(tag -> getUuid(tag, key))
				.or(() -> getBlockEntityWrapperTag(storageStack).flatMap(tag -> getUuid(tag, key)));
	}

	private static Optional<UUID> getUuid(CompoundTag tag, String key) {
		Tag uuidTag = tag.get(key);
		if (uuidTag == null) {
			return Optional.empty();
		}
		return Optional.of(NbtUtils.loadUUID(uuidTag));
	}

	private static Optional<Integer> getInt(ItemStack storageStack, String key) {
		return getLegacyCustomData(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getInt(key)) : Optional.empty())
				.or(() -> getBlockEntityWrapperTag(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getInt(key)) : Optional.empty()));
	}

	private static Optional<String> getString(ItemStack storageStack, String key) {
		return getLegacyCustomData(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty())
				.or(() -> getBlockEntityWrapperTag(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty()));
	}

	private static Optional<Boolean> getBoolean(ItemStack storageStack, String key) {
		return getLegacyCustomData(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getBoolean(key)) : Optional.empty())
				.or(() -> getBlockEntityWrapperTag(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getBoolean(key)) : Optional.empty()));
	}

	private static Optional<CompoundTag> getCompound(ItemStack storageStack, String key) {
		return getLegacyCustomData(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getCompound(key)) : Optional.empty())
				.or(() -> getBlockEntityWrapperTag(storageStack).flatMap(tag -> tag.contains(key) ? Optional.of(tag.getCompound(key)) : Optional.empty()));
	}

	private static Optional<CompoundTag> getBlockEntityWrapperTag(ItemStack storageStack) {
		return getBlockEntityData(storageStack).flatMap(tag -> tag.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG) ? Optional.of(tag.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG)) : Optional.empty());
	}

	private static Optional<CompoundTag> getBlockEntityData(ItemStack storageStack) {
		CustomData blockEntityData = storageStack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockEntityData != null) {
			return Optional.of(blockEntityData.copyTag());
		}
		return getLegacyCustomData(storageStack).flatMap(tag -> tag.contains(BLOCK_ENTITY_TAG) ? Optional.of(tag.getCompound(BLOCK_ENTITY_TAG)) : Optional.empty());
	}

	private static Optional<CompoundTag> getLegacyCustomData(ItemStack storageStack) {
		return Optional.ofNullable(storageStack.get(DataComponents.CUSTOM_DATA)).map(CustomData::copyTag);
	}
}
