package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
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
	public boolean isCompatible(ServerLevel level, ItemStack stack, LinkedStorageHostDescriptor descriptor) {
		return getCompatibility(level, stack, descriptor) == Compatibility.COMPATIBLE;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ItemStack stack, LinkedStorageHostDescriptor descriptor) {
		if (!StorageLinkedStorageHostWrapper.getCompatibilityKey(stack)
				.equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(descriptor.virtualCarrier()))) {
			return Compatibility.INCOMPATIBLE;
		}
		CompoundTag contents = readWrapperData(stack).getCompound(StorageWrapper.CONTENTS_TAG);
		return hasItems(contents, InventoryHandler.INVENTORY_TAG) || hasItems(contents, UpgradeHandler.UPGRADE_INVENTORY_TAG)
				? Compatibility.HAS_CONTENTS
				: Compatibility.COMPATIBLE;
	}

	private static boolean hasItems(CompoundTag contents, String key) {
		return !contents.getCompound(key).getList("Items", Tag.TAG_COMPOUND).isEmpty();
	}

	private static CompoundTag readWrapperData(ItemStack stack) {
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
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
		UUID previousUuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		CustomData blockData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockData != null) {
			CompoundTag endpointData = blockData.copyTag();
			StorageWrapper wrapper = createCarrier(stack).getStorageWrapper();
			CompoundTag localData = wrapper.saveEndpointData(new CompoundTag());
			CompoundTag renderInfo = localData.getCompound(StorageWrapper.RENDER_INFO_TAG);
			renderInfo.remove("tanks");
			renderInfo.remove("battery");
			endpointData.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, localData);
			stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(endpointData));
		}
		stack.remove(ModCoreDataComponents.STORAGE_UUID);
		stack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, profile.inventorySlots());
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, profile.upgradeSlots());
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpoint);
		stack.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
		if (previousUuid != null) {
			ItemContentsStorage.get().removeStorageContents(previousUuid);
		}
	}
}
