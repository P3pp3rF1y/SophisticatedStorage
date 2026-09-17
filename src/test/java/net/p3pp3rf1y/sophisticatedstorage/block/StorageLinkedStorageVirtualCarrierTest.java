package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageLinkedStorageVirtualCarrierTest {
	@Test
	void serializesExistingVirtualCarrierSchema() {
		StorageLinkedStorageVirtualCarrier carrier = new StorageLinkedStorageVirtualCarrier(BarrelBlockEntity.STORAGE_TYPE, "limited:12", "Shared Barrel", 12,
				2, 4, RenderData.EMPTY);

		CompoundTag tag = carrier.toTag();
		StorageLinkedStorageVirtualCarrier decoded = StorageLinkedStorageVirtualCarrier.fromTag(tag);

		assertEquals(BarrelBlockEntity.STORAGE_TYPE, tag.getStringOr("storageType", ""));
		assertEquals("limited:12", tag.getStringOr("compatibilityKey", ""));
		assertEquals("Shared Barrel", tag.getStringOr("displayName", ""));
		assertEquals(12, tag.getIntOr("inventorySlots", 0));
		assertEquals(2, tag.getIntOr("upgradeSlots", 0));
		assertEquals(4, tag.getIntOr("baseStackSizeMultiplier", 0));
		assertEquals(carrier, decoded);
	}

	@Test
	void decodesMissingFieldsWithExistingDefaults() {
		StorageLinkedStorageVirtualCarrier carrier = StorageLinkedStorageVirtualCarrier.fromTag(new CompoundTag());

		assertEquals("", carrier.storageType());
		assertEquals(StorageLinkedStorageHostWrapper.STANDARD_COMPATIBILITY_KEY, carrier.compatibilityKey());
		assertEquals("", carrier.displayName());
		assertEquals(0, carrier.inventorySlots());
		assertEquals(0, carrier.upgradeSlots());
		assertEquals(0, carrier.baseStackSizeMultiplier());
		assertEquals(RenderData.EMPTY, carrier.renderData());
	}
}
