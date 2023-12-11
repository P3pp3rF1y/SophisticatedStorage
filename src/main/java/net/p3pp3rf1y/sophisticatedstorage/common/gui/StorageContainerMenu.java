package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Optional;

public class StorageContainerMenu extends StorageContainerMenuBase<IStorageWrapper> implements ISyncedContainer {
	private final StorageBlockEntity storageBlockEntity;

	public StorageContainerMenu(int containerId, Player player, BlockPos pos) {
		this(ModBlocks.STORAGE_CONTAINER_TYPE.get(), containerId, player, pos);
	}
	public StorageContainerMenu(MenuType<?> menuType, int containerId, Player player, BlockPos pos) {
		super(menuType, containerId, player, getWrapper(player.level, pos), NoopStorageWrapper.INSTANCE, -1, false);
		storageBlockEntity = WorldHelper.getBlockEntity(player.level, pos, StorageBlockEntity.class).orElseThrow(() -> new IllegalArgumentException("Incorrect block entity at " + pos + " exptected to find StorageBlockEntity"));
		storageBlockEntity.startOpen(player);
	}

	public StorageBlockEntity getStorageBlockEntity() {
		return storageBlockEntity;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		storageBlockEntity.stopOpen(player);
	}

	private static IStorageWrapper getWrapper(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be.getStorageWrapper()).orElse(NoopStorageWrapper.INSTANCE);
	}

	public static StorageContainerMenu fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
		return new StorageContainerMenu(windowId, playerInventory.player, packetBuffer.readBlockPos());
	}

	@Override
	public Optional<BlockPos> getBlockPosition() {
		return Optional.of(storageBlockEntity.getBlockPos());
	}

	@Override
	protected StorageContainerMenuBase<IStorageWrapper>.StorageUpgradeSlot instantiateUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex) {
		return new StorageUpgradeSlot(upgradeHandler, slotIndex) {
			@Override
			protected void onUpgradeChanged() {
				if (player.getLevel().isClientSide()) {
					return;
				}
				storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
			}
		};
	}

	@Override
	public void openSettings() {
		if (isClientSide()) {
			sendToServer(data -> data.putString(ACTION_TAG, "openSettings"));
			return;
		}
		getBlockPosition().ifPresent(pos -> NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> instantiateSettingsContainerMenu(w, pl, pos),
				Component.translatable(StorageTranslationHelper.INSTANCE.translGui("settings.title"))), storageBlockEntity.getBlockPos()));
	}

	protected StorageSettingsContainerMenu instantiateSettingsContainerMenu(int windowId, Player player, BlockPos pos) {
		return new StorageSettingsContainerMenu(windowId, player, pos);
	}

	@Override
	protected boolean storageItemHasChanged() {
		return false; //storage blocks never have the issue of needing to close gui when item has moved in inventory
	}

	@Override
	public boolean detectSettingsChangeAndReload() {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		BlockPos pos = storageBlockEntity.getBlockPos();
		BlockEntity be = player.level.getBlockEntity(pos);
		return be instanceof StorageBlockEntity
				&& (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D)
				&& (!(be instanceof WoodStorageBlockEntity woodStorageBlockEntity) || !woodStorageBlockEntity.isPacked());
	}

	@Override
	protected void onStorageInventorySlotSet(int slotIndex) {
		super.onStorageInventorySlotSet(slotIndex);

		if (getStorageBlockEntity().isLocked() && getStorageBlockEntity().memorizesItemsWhenLocked() && !getSlot(slotIndex).getItem().isEmpty()) {
			MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			if (!memorySettings.isSlotSelected(slotIndex)) {
				memorySettings.selectSlot(slotIndex);
			}
		}
	}

	public float getSlotFillPercentage(int slot) {
		return storageBlockEntity.getSlotFillPercentage(slot);
	}
}
