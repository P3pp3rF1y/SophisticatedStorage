package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;

record StorageLinkedStorageVirtualCarrier(String storageType, String compatibilityKey, String displayName, int inventorySlots, int upgradeSlots,
		int baseStackSizeMultiplier, RenderData renderData) {
	private static final String STORAGE_TYPE_TAG = "storageType";
	private static final String COMPATIBILITY_KEY_TAG = "compatibilityKey";
	private static final String DISPLAY_NAME_TAG = "displayName";
	private static final String INVENTORY_SLOTS_TAG = "inventorySlots";
	private static final String UPGRADE_SLOTS_TAG = "upgradeSlots";
	private static final String BASE_STACK_SIZE_MULTIPLIER_TAG = "baseStackSizeMultiplier";
	private static final String RENDER_INFO_TAG = "renderInfo";
	private static final Codec<StorageLinkedStorageVirtualCarrier> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.STRING.optionalFieldOf(STORAGE_TYPE_TAG, "").forGetter(StorageLinkedStorageVirtualCarrier::storageType),
					Codec.STRING.optionalFieldOf(COMPATIBILITY_KEY_TAG, StorageLinkedStorageHostWrapper.STANDARD_COMPATIBILITY_KEY)
							.forGetter(StorageLinkedStorageVirtualCarrier::compatibilityKey),
					Codec.STRING.optionalFieldOf(DISPLAY_NAME_TAG, "").forGetter(StorageLinkedStorageVirtualCarrier::displayName),
					Codec.INT.optionalFieldOf(INVENTORY_SLOTS_TAG, 0).forGetter(StorageLinkedStorageVirtualCarrier::inventorySlots),
					Codec.INT.optionalFieldOf(UPGRADE_SLOTS_TAG, 0).forGetter(StorageLinkedStorageVirtualCarrier::upgradeSlots),
					Codec.INT.optionalFieldOf(BASE_STACK_SIZE_MULTIPLIER_TAG, 0).forGetter(StorageLinkedStorageVirtualCarrier::baseStackSizeMultiplier),
					RenderData.CODEC.optionalFieldOf(RENDER_INFO_TAG, RenderData.EMPTY).forGetter(StorageLinkedStorageVirtualCarrier::renderData))
			.apply(instance, StorageLinkedStorageVirtualCarrier::new));

	static StorageLinkedStorageVirtualCarrier from(StorageBlockEntity storageBlockEntity) {
		StorageWrapper storageWrapper = storageBlockEntity.getStorageWrapper();
		return new StorageLinkedStorageVirtualCarrier(storageWrapper.getStorageType(), StorageLinkedStorageHostWrapper.getCompatibilityKey(storageBlockEntity),
				storageBlockEntity.getDisplayName().getString(), storageWrapper.getInventoryHandler().size(), storageWrapper.getUpgradeHandler().size(),
				storageWrapper.getBaseStackSizeMultiplier(), storageWrapper.getRenderDataHandler().getData());
	}

	static StorageLinkedStorageVirtualCarrier fromTag(CompoundTag tag) {
		return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
	}

	CompoundTag toTag() {
		return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	StorageLinkedStorageVirtualCarrier withDisplayName(Component name) {
		return new StorageLinkedStorageVirtualCarrier(storageType, compatibilityKey, name.getString(), inventorySlots, upgradeSlots, baseStackSizeMultiplier,
				renderData);
	}

	StorageLinkedStorageVirtualCarrier withSnapshotProfile(Component groupName, int inventorySlots, int upgradeSlots) {
		return new StorageLinkedStorageVirtualCarrier(storageType, compatibilityKey, groupName.getString(), inventorySlots, upgradeSlots,
				baseStackSizeMultiplier, renderData);
	}

	StorageLinkedStorageVirtualCarrier withRenderData(RenderData renderData) {
		return new StorageLinkedStorageVirtualCarrier(storageType, compatibilityKey, displayName, inventorySlots, upgradeSlots, baseStackSizeMultiplier,
				renderData);
	}

	int numberOfDisplayItems() {
		return storageType.equals(BarrelBlockEntity.STORAGE_TYPE) || storageType.equals(LimitedBarrelBlockEntity.STORAGE_TYPE) ? 4 : 1;
	}

	boolean tracksCountsAndFillRatios() {
		return storageType.equals(LimitedBarrelBlockEntity.STORAGE_TYPE);
	}
}
