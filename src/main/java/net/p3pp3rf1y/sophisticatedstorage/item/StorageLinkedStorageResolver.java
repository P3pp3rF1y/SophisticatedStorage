package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;

import java.util.Optional;

public final class StorageLinkedStorageResolver {
	private StorageLinkedStorageResolver() {
	}

	public static Optional<StorageWrapper> resolveServerCanonicalHost(ItemStack stack) {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
			return Optional.empty();
		}
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return Optional.empty();
		}
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		return overworld == null ? Optional.empty() : resolveCanonicalHost(overworld, stack);
	}

	public static boolean isPrimary(ServerLevel level, ItemStack stack) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		return endpoint != null && LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId());
	}

	private static Optional<StorageWrapper> resolveCanonicalHost(ServerLevel level, ItemStack stack) {
		if (LinkedStorageStackLifecycle.classifyEndpoint(stack) != LinkedStorageEndpointStackState.ENDPOINT) {
			return Optional.empty();
		}
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		return manager.resolveVirtualHost(endpoint, false).filter(StorageWrapper.class::isInstance).map(StorageWrapper.class::cast);
	}
}
