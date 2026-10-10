package net.p3pp3rf1y.sophisticatedstorage.block;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageItemLinkedStorageEndpointAdapterTest {
	@BeforeAll
	static void setup() throws ReflectiveOperationException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		if (!Config.SERVER_SPEC.isLoaded()) {
			CommentedConfig config = CommentedConfig.inMemory();
			Config.SERVER_SPEC.correct(config);
			Constructor<?> constructor = Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(CommentedConfig.class, Path.class,
					ModConfig.class);
			constructor.setAccessible(true);
			Config.SERVER_SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
		}
	}

	@Test
	void getCompatibilityReadsExistingOrdinaryContentsWithoutCreatingBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		UUID uuid = UUID.randomUUID();
		stack.set(ModCoreDataComponents.STORAGE_UUID, uuid);
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
					new LinkedStorageHostDescriptor(ResourceLocation.parse("sophisticatedbackpacks:backpack"), new CompoundTag()));

			assertEquals(Compatibility.HAS_CONTENTS, compatibility);
			verify(storage).getStorageContents(uuid);
			verifyNoMoreInteractions(storage);
		}
	}

	@Test
	void createHostDescriptorKeepsNewStorageItemOutOfOrdinaryBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		try (MockedStatic<ItemContentsStorage> storage = mockStatic(ItemContentsStorage.class)) {

			LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(mock(ServerLevel.class), stack);

			assertEquals(ShulkerBoxBlockEntity.STORAGE_TYPE, descriptor.virtualCarrier().getString("storageType"));
			assertEquals(27, descriptor.virtualCarrier().getInt("inventorySlots"));
			assertFalse(stack.has(ModCoreDataComponents.STORAGE_UUID));
			storage.verifyNoInteractions();
		}
	}

	@Test
	void copyCanonicalContentsKeepsNewStorageItemOutOfOrdinaryBackend() {
		StorageItemLinkedStorageEndpointAdapter adapter = new StorageItemLinkedStorageEndpointAdapter();
		ItemStack stack = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		try (MockedStatic<ItemContentsStorage> storage = mockStatic(ItemContentsStorage.class)) {

			CompoundTag contents = adapter.copyCanonicalContents(mock(ServerLevel.class), stack);

			assertEquals(27, contents.getInt(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG));
			assertFalse(stack.has(ModCoreDataComponents.STORAGE_UUID));
			storage.verifyNoInteractions();
		}
	}
}
