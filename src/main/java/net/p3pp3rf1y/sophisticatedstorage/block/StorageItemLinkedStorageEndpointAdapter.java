package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.UUID;

public final class StorageItemLinkedStorageEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
	@Override
	public boolean supports(ItemStack stack) {
		return stack.getItem() instanceof StorageBlockItem item && item.getBlock() instanceof StorageBlockBase;
	}

	@Override
	public Identifier factoryId() {
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
		ContainerContents contents = readContents(level, stack);
		return contents.inventory().stacks().stream().anyMatch(item -> !item.isEmpty())
				|| contents.upgrades().stacks().stream().anyMatch(item -> !item.isEmpty()) ? Compatibility.HAS_CONTENTS : Compatibility.COMPATIBLE;
	}

	private static CompoundTag readWrapperData(ItemStack stack) {
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			return ItemContentsStorage.get().findAdditionalBeData(uuid).map(data -> data.getCompoundOrEmpty(StorageBlockEntity.STORAGE_WRAPPER).copy())
					.orElseGet(CompoundTag::new);
		}
		TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		return data == null ? new CompoundTag() : data.copyTagWithoutId().getCompoundOrEmpty(StorageBlockEntity.STORAGE_WRAPPER).copy();
	}

	private static ContainerContents readContents(ServerLevel level, ItemStack stack) {
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			return ItemContentsStorage.get().findContents(uuid).map(ContainerContents::copy).orElseGet(ContainerContents::new);
		}
		StorageBlockEntity carrier = createCarrier(level, stack);
		carrier.getStorageWrapper().getInventoryHandler();
		carrier.getStorageWrapper().getUpgradeHandler();
		return carrier.getStorageWrapper().copyContentsForLinkedStorage();
	}

	private static StorageBlockEntity createCarrier(ServerLevel level, ItemStack stack) {
		StorageBlockBase block = (StorageBlockBase) ((StorageBlockItem) stack.getItem()).getBlock();
		StorageBlockEntity carrier = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
		if (carrier == null) {
			throw new IllegalStateException("Storage item has no native block entity");
		}
		carrier.getStorageWrapper().deserialize(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), readWrapperData(stack)));
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			ItemContentsStorage.get().findContents(uuid).ifPresent(contents -> carrier.getStorageWrapper().replaceContents(contents.copy()));
		}
		return carrier;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ItemStack stack) {
		return new LinkedStorageHostDescriptor(factoryId(),
				StorageLinkedStorageHostWrapper.createVirtualCarrier(stack, createCarrier(level, stack).getStorageWrapper()));
	}

	@Override
	public ContainerContents copyCanonicalContents(ServerLevel level, ItemStack stack) {
		return readContents(level, stack);
	}

	@Override
	public SortBy getSortBy(ServerLevel level, ItemStack stack) {
		return createCarrier(level, stack).getStorageWrapper().getSortBy();
	}

	@Override
	public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(endpoint, false).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Storage endpoint requires a native canonical profile"));
		UUID previousUuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		TypedEntityData<BlockEntityType<?>> blockData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockData != null) {
			CompoundTag endpointData = blockData.copyTagWithoutId();
			CompoundTag localData = readWrapperData(stack);
			localData.remove(StorageWrapper.CONTENTS);
			endpointData.put(StorageBlockEntity.STORAGE_WRAPPER, localData);
			stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockData.type(), endpointData));
		}
		stack.remove(ModCoreDataComponents.STORAGE_UUID);
		stack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, profile.inventorySlots());
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, profile.upgradeSlots());
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpoint);
		stack.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
		if (previousUuid != null) {
			ItemContentsStorage.get().removeContents(previousUuid);
			ItemContentsStorage.get().removeAddtionalBeData(previousUuid);
		}
	}
}
