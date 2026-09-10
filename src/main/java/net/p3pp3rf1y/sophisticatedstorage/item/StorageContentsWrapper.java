package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StorageContentsWrapper extends StorageWrapper {
	private final IStorageBlock storageBlock;

	StorageContentsWrapper(IStorageBlock storageBlock, CompoundTag tag) {
		super(() -> () -> {}, () -> {}, () -> {}, storageBlock.getNumberOfInventorySlots());
		this.storageBlock = storageBlock;
		load(tag.copy());
		onInit();
	}

	public static Optional<IStorageWrapper> fromStack(ItemStack stack) {
		if (!WoodStorageBlockItem.isPacked(stack) || !(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof BarrelBlock barrel)) {
			return Optional.empty();
		}
		return NBTHelper.getUniqueId(stack, "uuid").flatMap(uuid -> {
			CompoundTag saved = ItemContentsStorage.get().getOrCreateStorageContents(uuid);
			CompoundTag wrapperTag = saved.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG);
			CompoundTag contents = wrapperTag.getCompound(CONTENTS_TAG);
			if (!contents.contains(InventoryHandler.INVENTORY_TAG, Tag.TAG_COMPOUND)
					&& !contents.contains(UpgradeHandler.UPGRADE_INVENTORY_TAG, Tag.TAG_COMPOUND)
					&& !wrapperTag.contains("numberOfInventorySlots", Tag.TAG_INT)
					&& !wrapperTag.contains("numberOfUpgradeSlots", Tag.TAG_INT)) {
				return Optional.empty();
			}
			return Optional.of(new StorageContentsWrapper(barrel, wrapperTag));
		});
	}

	public List<ItemStack> getContents() {
		List<ItemStack> contents = new ArrayList<>();
		InventoryHandler inventory = getInventoryHandler();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				contents.add(stack.copy());
			}
		}
		return contents;
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return storageBlock.getNumberOfInventorySlots();
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return storageBlock.getNumberOfUpgradeSlots();
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return storageBlock.getBaseStackSizeMultiplier();
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return false;
	}

	@Override
	protected void onUpgradeRefresh() {
		//noop
	}

	@Override
	public String getStorageType() {
		return "wood_storage";
	}

	@Override
	public Component getDisplayName() {
		return TextComponent.EMPTY;
	}
}
