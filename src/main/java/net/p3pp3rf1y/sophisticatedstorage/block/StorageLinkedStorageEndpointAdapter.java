package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public class StorageLinkedStorageEndpointAdapter implements ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> {
	@Override
	public ResourceLocation factoryId() {
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
		return InventoryHelper.isEmpty(storageWrapper.getInventoryHandler()) && InventoryHelper.isEmpty(storageWrapper.getUpgradeHandler())
				? Compatibility.COMPATIBLE
				: Compatibility.HAS_CONTENTS;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		StorageBlockEntity storageBlockEntity = requireCandidate(endpoint);
		return new LinkedStorageHostDescriptor(factoryId(), StorageLinkedStorageHostWrapper.createVirtualCarrier(storageBlockEntity));
	}

	@Override
	public CompoundTag copyCanonicalContents(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return requireCandidate(endpoint).getStorageWrapper().saveCanonicalData(new CompoundTag());
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
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
		if (endpoint == null || !LinkedStorageStackData.isPrimaryEndpoint(stack)) {
			return;
		}

		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		if (!manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			return;
		}

		manager.getHostDescriptor(endpoint.groupId()).filter(hostDescriptor -> hostDescriptor.factoryId().equals(StorageLinkedStorageHostWrapper.FACTORY_ID))
				.map(hostDescriptor -> {
					if (hostDescriptor.virtualCarrier().getString("displayName").equals(stack.getHoverName().getString())) {
						return true;
					}
					CompoundTag virtualCarrier = StorageLinkedStorageHostWrapper.withDisplayName(hostDescriptor.virtualCarrier(), stack.getHoverName());
					return manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(),
							new LinkedStorageHostDescriptor(hostDescriptor.factoryId(), virtualCarrier));
				});
	}

	public static boolean isLinkedStorageEndpoint(ItemStack stack) {
		return LinkedStorageStackData.getEndpoint(stack) != null;
	}

	private static StorageBlockEntity requireCandidate(ILinkedStorageBlockEndpoint endpoint) {
		if (endpoint instanceof StorageBlockEntity storageBlockEntity && storageBlockEntity.isLinkedStorageCandidate()) {
			return storageBlockEntity;
		}
		throw new IllegalArgumentException("Unsupported linked storage endpoint");
	}
}
