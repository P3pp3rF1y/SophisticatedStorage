package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;

import java.util.Optional;
import java.util.UUID;

abstract class StackStorageWrapper extends StorageWrapper {
	private final ItemStack storageStack;

	public StackStorageWrapper(ItemStack storageStack) {
		super(() -> () -> {}, () -> {}, () -> {});
		this.storageStack = storageStack;
	}

	private UUID getNewUuid() {
		UUID newUuid = UUID.randomUUID();
		NBTHelper.setUniqueId(storageStack, "uuid", newUuid);
		CompoundTag mainTag = new CompoundTag();
		CompoundTag storageWrapperTag = new CompoundTag();
		storageWrapperTag.put("contents", new CompoundTag());
		mainTag.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, storageWrapperTag);
		ItemContentsStorage.get().setStorageContents(newUuid, mainTag);
		return newUuid;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	@Override
	protected CompoundTag getContentsNbt() {
		if (contentsUuid == null) {
			contentsUuid = getNewUuid();
		}
		return ItemContentsStorage.get().getOrCreateStorageContents(contentsUuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).getCompound("contents");
	}

	@Override
	protected void onUpgradeRefresh() {
		//noop - there should be no upgrade refresh happening here
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
	}

	@Override
	protected void loadSlotNumbers(CompoundTag tag) {
		numberOfInventorySlots = NBTHelper.getInt(storageStack, "numberOfInventorySlots").orElse(0);
		numberOfUpgradeSlots = NBTHelper.getInt(storageStack, "numberOfUpgradeSlots").orElse(0);
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
	}
}
