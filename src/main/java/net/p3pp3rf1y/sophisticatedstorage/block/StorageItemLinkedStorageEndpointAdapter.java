package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.UUID;

public final class StorageItemLinkedStorageEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
	@Override
	public boolean supports(ItemStack stack) {
		return stack.getItem() instanceof StorageBlockItem item && item.getBlock() instanceof StorageBlockBase;
	}

	@Override
	public ResourceLocation factoryId() {
		return StorageLinkedStorageHostWrapper.FACTORY_ID;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ItemStack stack, LinkedStorageHostDescriptor descriptor) {
		if (!StorageLinkedStorageHostWrapper.getCompatibilityKey(stack)
				.equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(descriptor.virtualCarrier()))) {
			return Compatibility.INCOMPATIBLE;
		}
		CompoundTag contents = readWrapperData(stack).getCompound(StorageWrapper.CONTENTS_TAG);
		return hasItems(contents, "inventory") || hasItems(contents, "upgradeInventory") ? Compatibility.HAS_CONTENTS : Compatibility.COMPATIBLE;
	}

	private static boolean hasItems(CompoundTag contents, String key) {
		return !contents.getCompound(key).getList("Items", Tag.TAG_COMPOUND).isEmpty();
	}

	private static CompoundTag readWrapperData(ItemStack stack) {
		UUID uuid = StorageBlockItem.getContentsUuid(stack).orElse(null);
		return uuid == null
				? StorageBlockItem.getEntityWrapperTagFromStack(stack).map(CompoundTag::copy).orElseGet(CompoundTag::new)
				: ItemContentsStorage.get().getStorageContents(uuid).map(data -> data.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).copy())
						.orElseGet(CompoundTag::new);
	}

	private static StorageBlockEntity createCarrier(ItemStack stack) {
		StorageBlockBase block = (StorageBlockBase) ((StorageBlockItem) stack.getItem()).getBlock();
		StorageBlockEntity carrier = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
		if (carrier == null) {
			throw new IllegalStateException("Storage item has no native block entity");
		}
		carrier.getStorageWrapper().load(readWrapperData(stack));
		return carrier;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ItemStack stack) {
		return new LinkedStorageHostDescriptor(factoryId(),
				StorageLinkedStorageHostWrapper.createVirtualCarrier(stack, createCarrier(stack).getStorageWrapper()));
	}

	@Override
	public CompoundTag copyCanonicalContents(ServerLevel level, ItemStack stack) {
		return createCarrier(stack).getStorageWrapper().saveCanonicalData(new CompoundTag());
	}

	@Override
	public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(endpoint, false).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Storage endpoint requires a native canonical profile"));
		UUID previousUuid = StorageBlockItem.getContentsUuid(stack).orElse(null);
		CompoundTag blockData = stack.getTagElement("BlockEntityTag");
		if (blockData != null) {
			CompoundTag localData = createCarrier(stack).getStorageWrapper().saveEndpointData(new CompoundTag());
			CompoundTag renderInfo = localData.getCompound(StorageWrapper.RENDER_INFO_TAG);
			renderInfo.remove("tanks");
			renderInfo.remove("battery");
			blockData.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, localData);
		}
		NBTHelper.removeTag(stack, "uuid");
		StorageBlockItem.setNumberOfInventorySlots(stack, profile.inventorySlots());
		StorageBlockItem.setNumberOfUpgradeSlots(stack, profile.upgradeSlots());
		LinkedStorageStackData.setEndpoint(stack, endpoint);
		LinkedStorageStackData.setPrimaryEndpoint(stack, manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
		if (previousUuid != null) {
			ItemContentsStorage.get().removeStorageContents(previousUuid);
		}
	}
}
