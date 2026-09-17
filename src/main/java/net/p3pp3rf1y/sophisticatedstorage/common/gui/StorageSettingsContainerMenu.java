package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class StorageSettingsContainerMenu extends SettingsContainerMenu<IStorageWrapper> {
	private final BlockPos pos;
	private final boolean doubleChest;
	private boolean stopOpenersOnRemove;
	private CompoundTag lastSettingsNbt = null;

	protected StorageSettingsContainerMenu(int windowId, Player player, BlockPos pos) {
		this(windowId, player, pos, false);
	}

	protected StorageSettingsContainerMenu(int windowId, Player player, BlockPos pos, boolean stopOpenersOnRemove) {
		this(ModBlocks.SETTINGS_CONTAINER_TYPE.get(), windowId, player, pos, stopOpenersOnRemove);
	}
	protected StorageSettingsContainerMenu(MenuType<?> menuType, int windowId, Player player, BlockPos pos) {
		this(menuType, windowId, player, pos, false);
	}

	protected StorageSettingsContainerMenu(MenuType<?> menuType, int windowId, Player player, BlockPos pos, boolean stopOpenersOnRemove) {
		super(menuType, windowId, player, getWrapper(player.level(), pos));
		this.pos = pos;
		this.stopOpenersOnRemove = stopOpenersOnRemove;
		BlockState blockState = player.level().getBlockState(pos);
		doubleChest = blockState.getBlock() instanceof ChestBlock && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
	}

	private static IStorageWrapper getWrapper(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be.getStorageWrapper())
				.orElse(NoopStorageWrapper.INSTANCE);
	}

	@Override
	public void detectSettingsChangeAndReload() {
		if (!player.level().isClientSide) {
			return;
		}
		WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).ifPresent(storageBlockEntity -> {
			LinkedStorageEndpointData endpoint = storageBlockEntity.getLinkedStorageEndpointData();
			if (endpoint != null && ClientLinkedStorageContents.removeUpdatedGroup(endpoint.groupId())) {
				ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(endpoint.groupId())
						.orElseThrow(() -> new IllegalStateException("Updated linked storage group has no snapshot: " + endpoint.groupId()));
				storageBlockEntity.updateClientLinkedStorageContents(endpoint.groupId());
				storageWrapper.getSettingsHandler().reloadFrom(contents.contents().getCompound("settings"));
			}
		});
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		CompoundTag settingsNbt = storageWrapper.getSettingsHandler().getNbt();
		if (lastSettingsNbt == null || !lastSettingsNbt.equals(settingsNbt)) {
			lastSettingsNbt = settingsNbt.copy();
			WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).map(StorageBlockEntity::getLinkedStorageEndpointData)
					.ifPresent(endpoint -> PacketDistributor.sendToPlayer(serverPlayer,
							LinkedStorageContentsPayload.createSnapshot(serverPlayer.serverLevel(), endpoint.groupId())));
		}
	}

	public static StorageSettingsContainerMenu fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
		return new StorageSettingsContainerMenu(windowId, playerInventory.player, StorageContainerMenu.readMenuData(buffer, playerInventory.player));
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (!player.level().isClientSide() && stopOpenersOnRemove) {
			WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).ifPresent(storageBlockEntity -> storageBlockEntity.stopOpen(player));
		}
	}

	public void transferOpenersToStorageMenu() {
		stopOpenersOnRemove = false;
	}

	@Override
	public BlockPos getBlockPosition() {
		return pos;
	}

	@Override
	public boolean stillValid(Player player) {
		return WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).filter(storageBlockEntity -> player.canInteractWithBlock(pos, 4.0F))
				.filter(storageBlockEntity -> !(storageBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity)
						|| !woodStorageBlockEntity.isPacked())
				.isPresent();
	}

	@Override
	public boolean supportsItemDisplaySideSelection() {
		return doubleChest;
	}
}
