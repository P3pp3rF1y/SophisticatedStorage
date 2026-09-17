package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageBlockEndpoint;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;

public class StorageLinkedStorageEndpointAdapter implements ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> {
	@Override
	public ResourceLocation factoryId() {
		return StorageLinkedStorageHostWrapper.FACTORY_ID;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageHostDescriptor descriptor) {
		if (!(endpoint instanceof StorageBlockEntity storage) || !storage.isLinkedStorageCandidate() || !StorageLinkedStorageHostWrapper
				.getCompatibilityKey(storage).equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(descriptor.virtualCarrier()))) {
			return Compatibility.INCOMPATIBLE;
		}
		StorageWrapper wrapper = storage.getStorageWrapper();
		return ResourceHandlerUtil.isEmpty(wrapper.getInventoryHandler()) && ResourceHandlerUtil.isEmpty(wrapper.getUpgradeHandler())
				? Compatibility.COMPATIBLE
				: Compatibility.HAS_CONTENTS;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return new LinkedStorageHostDescriptor(factoryId(), StorageLinkedStorageHostWrapper.createVirtualCarrier(requireCandidate(endpoint)));
	}

	@Override
	public ContainerContents copyCanonicalContents(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return requireCandidate(endpoint).getStorageWrapper().copyContentsForLinkedStorage();
	}

	@Override
	public void bindEndpoint(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageEndpointData endpointData) {
		requireCandidate(endpoint).bindLinkedStorage(level, endpointData);
	}

	@Override
	public void onEndpointLinked(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		if (endpoint instanceof StorageBlockEntity storage) {
			storage.onLinkedStorageEndpointLinked();
		}
	}

	public static void synchronizePrimaryCarrier(ServerLevel level, ItemStack stack) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null || !Boolean.TRUE.equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))) {
			return;
		}
		synchronizePrimaryCarrier(level, endpoint, stack.getHoverName());
	}

	static void synchronizePrimaryCarrier(ServerLevel level, LinkedStorageEndpointData endpoint, net.minecraft.network.chat.Component displayName) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		if (!manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			return;
		}
		manager.getHostDescriptor(endpoint.groupId()).filter(descriptor -> descriptor.factoryId().equals(StorageLinkedStorageHostWrapper.FACTORY_ID))
				.filter(descriptor -> !descriptor.virtualCarrier().getStringOr("displayName", "").equals(displayName.getString()))
				.ifPresent(descriptor -> manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(), new LinkedStorageHostDescriptor(
						descriptor.factoryId(), StorageLinkedStorageHostWrapper.withDisplayName(descriptor.virtualCarrier(), displayName))));
	}

	private static StorageBlockEntity requireCandidate(ILinkedStorageBlockEndpoint endpoint) {
		if (endpoint instanceof StorageBlockEntity storage && storage.isLinkedStorageCandidate()) {
			return storage;
		}
		throw new IllegalArgumentException("Unsupported linked storage endpoint");
	}
}
