package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LinkedShulkerStashTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.SHULKER_BOX_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void stashUsesCanonicalHandlerForSimulationAndExecutionWithoutCreatingStackStorage() {
		ItemStack shulker = endpoint();
		ItemResource incoming = ItemResource.of(Items.DIAMOND);
		IStorageWrapper host = mock(IStorageWrapper.class);
		ITrackedContentsItemResourceHandler inventory = mock(ITrackedContentsItemResourceHandler.class);
		when(host.getInventoryForUpgradeProcessing()).thenReturn(inventory);
		when(inventory.insert(eq(incoming), eq(7), any())).thenReturn(7);
		try (MockedStatic<StorageLinkedStorageResolver> resolver = mockStatic(StorageLinkedStorageResolver.class);
				MockedStatic<StackStorageWrapper> stackStorage = mockStatic(StackStorageWrapper.class)) {
			resolver.when(() -> StorageLinkedStorageResolver.resolveServerCanonicalHost(shulker)).thenReturn(Optional.of(host));

			try (Transaction simulation = Transaction.openRoot()) {
				assertEquals(7, ((ShulkerBoxItem) shulker.getItem()).stash(registries(), shulker, incoming, 7, simulation));
			}
			try (Transaction execution = Transaction.openRoot()) {
				assertEquals(7, ((ShulkerBoxItem) shulker.getItem()).stash(registries(), shulker, incoming, 7, execution));
				execution.commit();
			}

			verify(inventory, times(2)).insert(eq(incoming), eq(7), any());
			assertFalse(shulker.has(ModCoreDataComponents.STORAGE_UUID));
			stackStorage.verifyNoInteractions();
		}
	}

	@Test
	void stashRejectsStaleEndpointWithoutFallingBackToOrdinaryContents() {
		ItemStack shulker = endpoint();
		try (MockedStatic<StorageLinkedStorageResolver> resolver = mockStatic(StorageLinkedStorageResolver.class);
				MockedStatic<StackStorageWrapper> stackStorage = mockStatic(StackStorageWrapper.class)) {
			resolver.when(() -> StorageLinkedStorageResolver.resolveServerCanonicalHost(shulker)).thenReturn(Optional.empty());

			try (Transaction tx = Transaction.openRoot()) {
				assertEquals(0, ((ShulkerBoxItem) shulker.getItem()).stash(registries(), shulker, ItemResource.of(Items.DIAMOND), 7, tx));
			}
			assertFalse(shulker.has(ModCoreDataComponents.STORAGE_UUID));
			stackStorage.verifyNoInteractions();
		}
	}

	private static RegistryAccess registries() {
		return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
	}

	private static ItemStack endpoint() {
		ItemStack shulker = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
		shulker.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		return shulker;
	}
}
