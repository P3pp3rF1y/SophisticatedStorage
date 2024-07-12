package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

abstract class StackStorageWrapper extends StorageWrapper {
	private static final String CONTENTS_TAG = "contents";
	private final ItemStack storageStack;

	public StackStorageWrapper(ItemStack storageStack) {
		super(() -> () -> {}, () -> {}, () -> {});
		this.storageStack = storageStack;
	}

	private UUID getNewUuid() {
		UUID newUuid = UUID.randomUUID();
		setContentsUuid(newUuid);
		return newUuid;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	@Override
	public void setContentsUuid(@Nullable UUID contentsUuid) {
		super.setContentsUuid(contentsUuid);
		if (contentsUuid != null) {
			NBTHelper.setUniqueId(storageStack, "uuid", contentsUuid);
			ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
			CompoundTag storageContents = itemContentsStorage.getOrCreateStorageContents(contentsUuid);
			if (!storageContents.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG)) {
				CompoundTag storageWrapperTag = new CompoundTag();
				storageWrapperTag.put(CONTENTS_TAG, new CompoundTag());
				storageContents.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, storageWrapperTag);
			}

			onContentsNbtUpdated();
		}
	}

	@Override
	protected CompoundTag getContentsNbt() {
		if (contentsUuid == null) {
			contentsUuid = getNewUuid();
		}
		return ItemContentsStorage.get().getOrCreateStorageContents(contentsUuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).getCompound(CONTENTS_TAG);
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
