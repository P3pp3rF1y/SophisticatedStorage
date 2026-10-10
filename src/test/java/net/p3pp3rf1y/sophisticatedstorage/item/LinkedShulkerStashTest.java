package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkedShulkerStashTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void stashUsesCanonicalHandlerForSimulationAndExecutionWithoutCreatingStackStorage() {
		ItemStack shulker = endpoint();
		ItemStack incoming = new ItemStack(Items.DIAMOND, 7);
		IStorageWrapper host = mock(IStorageWrapper.class);
		ITrackedContentsItemHandler inventory = mock(ITrackedContentsItemHandler.class);
		when(host.getInventoryForUpgradeProcessing()).thenReturn(inventory);
		when(inventory.insertItem(incoming, true)).thenReturn(ItemStack.EMPTY);
		when(inventory.insertItem(incoming, false)).thenReturn(ItemStack.EMPTY);
		try (MockedStatic<StorageLinkedStorageResolver> resolver = mockStatic(StorageLinkedStorageResolver.class);
				MockedStatic<StackStorageWrapper> stackStorage = mockStatic(StackStorageWrapper.class)) {
			resolver.when(() -> StorageLinkedStorageResolver.resolveServerCanonicalHost(shulker)).thenReturn(Optional.of(host));

			assertTrue(((ShulkerBoxItem) shulker.getItem()).stash(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), shulker, incoming, true)
					.isEmpty());
			assertTrue(((ShulkerBoxItem) shulker.getItem()).stash(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), shulker, incoming, false)
					.isEmpty());

			verify(inventory).insertItem(incoming, true);
			verify(inventory).insertItem(incoming, false);
			assertFalse(shulker.has(ModCoreDataComponents.STORAGE_UUID));
			stackStorage.verifyNoInteractions();
		}
	}

	@Test
	void stashRejectsStaleEndpointWithoutFallingBackToOrdinaryContents() {
		ItemStack shulker = endpoint();
		ItemStack incoming = new ItemStack(Items.DIAMOND, 7);
		try (MockedStatic<StorageLinkedStorageResolver> resolver = mockStatic(StorageLinkedStorageResolver.class);
				MockedStatic<StackStorageWrapper> stackStorage = mockStatic(StackStorageWrapper.class)) {
			resolver.when(() -> StorageLinkedStorageResolver.resolveServerCanonicalHost(shulker)).thenReturn(Optional.empty());

			ItemStack remainder = ((ShulkerBoxItem) shulker.getItem()).stash(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), shulker,
					incoming, false);

			assertEquals(7, remainder.getCount());
			assertFalse(shulker.has(ModCoreDataComponents.STORAGE_UUID));
			stackStorage.verifyNoInteractions();
		}
	}

	private static ItemStack endpoint() {
		ItemStack shulker = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		shulker.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		return shulker;
	}
}
