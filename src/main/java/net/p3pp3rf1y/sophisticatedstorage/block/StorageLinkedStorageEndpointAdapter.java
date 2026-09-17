package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;

public class StorageLinkedStorageEndpointAdapter implements ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> {
	@Override
	public Identifier factoryId() {
		return StorageLinkedStorageHostWrapper.FACTORY_ID;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageHostDescriptor hostDescriptor) {
		if (!(endpoint instanceof StorageBlockEntity storageBlockEntity) || !storageBlockEntity.isLinkedStorageCandidate()) {
			return Compatibility.INCOMPATIBLE;
		}
		if (!StorageLinkedStorageHostWrapper.getCompatibilityKey(storageBlockEntity)
				.equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(hostDescriptor.virtualCarrier()))) {
			return Compatibility.INCOMPATIBLE;
		}
		StorageWrapper storageWrapper = storageBlockEntity.getStorageWrapper();
		return ResourceHandlerUtil.isEmpty(storageWrapper.getInventoryHandler()) && ResourceHandlerUtil.isEmpty(storageWrapper.getUpgradeHandler())
				? Compatibility.COMPATIBLE
				: Compatibility.HAS_CONTENTS;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		StorageBlockEntity storageBlockEntity = requireCandidate(endpoint);
		return new LinkedStorageHostDescriptor(factoryId(), StorageLinkedStorageHostWrapper.createVirtualCarrier(storageBlockEntity));
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
		if (endpoint instanceof StorageBlockEntity storageBlockEntity) {
			storageBlockEntity.onLinkedStorageEndpointLinked();
		}
	}

	public static void synchronizePrimaryCarrier(ServerLevel level, ItemStack stack) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null || !Boolean.TRUE.equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))) {
			return;
		}

		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		if (!manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			return;
		}

		manager.getHostDescriptor(endpoint.groupId()).filter(hostDescriptor -> hostDescriptor.factoryId().equals(StorageLinkedStorageHostWrapper.FACTORY_ID))
				.filter(hostDescriptor -> !hostDescriptor.virtualCarrier().getStringOr("displayName", "").equals(stack.getHoverName().getString()))
				.ifPresent(hostDescriptor -> manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(), new LinkedStorageHostDescriptor(
						hostDescriptor.factoryId(), StorageLinkedStorageHostWrapper.withDisplayName(hostDescriptor.virtualCarrier(), stack.getHoverName()))));
	}

	private static StorageBlockEntity requireCandidate(ILinkedStorageBlockEndpoint endpoint) {
		if (endpoint instanceof StorageBlockEntity storageBlockEntity && storageBlockEntity.isLinkedStorageCandidate()) {
			return storageBlockEntity;
		}
		throw new IllegalArgumentException("Unsupported linked storage endpoint");
	}
}
