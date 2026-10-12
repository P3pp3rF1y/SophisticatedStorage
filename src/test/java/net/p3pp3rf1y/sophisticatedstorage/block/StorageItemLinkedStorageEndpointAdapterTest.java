package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageItemLinkedStorageEndpointAdapterTest {
	private static ItemStack newStorageStack() {
		ItemStack stack = mock(ItemStack.class);
		when(stack.getTag()).thenReturn(new CompoundTag());
		return stack;
	}

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void getCompatibilityReadsExistingOrdinaryContentsWithoutCreatingBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = newStorageStack();
		UUID uuid = UUID.randomUUID();
		stack.getTag().putUUID("uuid", uuid);
		ItemContentsStorage storage = mock(ItemContentsStorage.class);
		CompoundTag inventory = new CompoundTag();
		ListTag items = new ListTag();
		items.add(new CompoundTag());
		inventory.put("Items", items);
		CompoundTag contents = new CompoundTag();
		contents.put(InventoryHandler.INVENTORY_TAG, inventory);
		CompoundTag wrapper = new CompoundTag();
		wrapper.put(StorageWrapper.CONTENTS_TAG, contents);
		CompoundTag root = new CompoundTag();
		root.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, wrapper);
		when(storage.getStorageContents(uuid)).thenReturn(Optional.of(root));
		try (MockedStatic<ItemContentsStorage> mockedStorage = mockStatic(ItemContentsStorage.class)) {
			mockedStorage.when(ItemContentsStorage::get).thenReturn(storage);

			Compatibility compatibility = adapter.getCompatibility(mock(ServerLevel.class), stack,
					new LinkedStorageHostDescriptor(new ResourceLocation("sophisticatedbackpacks:backpack"), new CompoundTag()));

			assertEquals(Compatibility.HAS_CONTENTS, compatibility);
			verify(storage).getStorageContents(uuid);
			verifyNoMoreInteractions(storage);
		}
	}

}
