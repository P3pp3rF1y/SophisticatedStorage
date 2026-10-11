package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageItemLinkedStorageEndpointAdapterTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.SHULKER_BOX_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void getCompatibilityReadsExistingOrdinaryContentsWithoutCreatingBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		UUID uuid = UUID.randomUUID();
		stack.set(ModCoreDataComponents.STORAGE_UUID, uuid);
		ItemContentsStorage storage = mock(ItemContentsStorage.class);
		ContainerContents contents = new ContainerContents();
		contents.inventory().stacks().add(new ItemStack(Items.DIAMOND));
		when(storage.findContents(uuid)).thenReturn(Optional.of(contents));
		try (MockedStatic<ItemContentsStorage> mockedStorage = mockStatic(ItemContentsStorage.class)) {
			mockedStorage.when(ItemContentsStorage::get).thenReturn(storage);

			Compatibility compatibility = adapter.getCompatibility(mock(ServerLevel.class), stack,
					new LinkedStorageHostDescriptor(StorageLinkedStorageHostWrapper.FACTORY_ID, new net.minecraft.nbt.CompoundTag()));

			assertEquals(Compatibility.HAS_CONTENTS, compatibility);
			verify(storage).findContents(uuid);
			verifyNoMoreInteractions(storage);
		}
	}

	@Test
	void createHostDescriptorKeepsNewStorageItemOutOfOrdinaryBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		try (MockedStatic<ItemContentsStorage> storage = mockStatic(ItemContentsStorage.class)) {

			LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, stack);

			assertEquals(ShulkerBoxBlockEntity.STORAGE_TYPE, descriptor.virtualCarrier().getString("storageType").orElseThrow());
			assertEquals(27, descriptor.virtualCarrier().getInt("inventorySlots").orElseThrow());
			assertFalse(stack.has(ModCoreDataComponents.STORAGE_UUID));
			storage.verifyNoInteractions();
		}
	}

	@Test
	void copyCanonicalContentsKeepsNewStorageItemOutOfOrdinaryBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		try (MockedStatic<ItemContentsStorage> storage = mockStatic(ItemContentsStorage.class)) {

			ContainerContents contents = adapter.copyCanonicalContents(level, stack);

			assertEquals(27, contents.inventory().stacks().size());
			assertFalse(stack.has(ModCoreDataComponents.STORAGE_UUID));
			storage.verifyNoInteractions();
		}
	}
}
