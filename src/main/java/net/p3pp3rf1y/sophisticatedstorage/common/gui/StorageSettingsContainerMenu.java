package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.UUID;

public class StorageSettingsContainerMenu extends SettingsContainerMenu<IStorageWrapper> {
	private final BlockPos pos;
	private final boolean doubleChest;
	private boolean stopOpenersOnRemove;
	private ContainerContents.SettingsData lastSettingsData;

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
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be.getMenuStorageWrapper())
				.orElse(NoopStorageWrapper.INSTANCE);
	}

	@Override
	public void detectSettingsChangeAndReload() {
		if (!player.level().isClientSide()) {
			return;
		}
		WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).filter(StorageBlockEntity::isLinkedStorage).ifPresent(storage -> {
			UUID groupId = storage.getLinkedStorageEndpointData().groupId();
			if (!ClientLinkedStorageContents.removeUpdatedGroup(groupId)) {
				return;
			}
			ClientLinkedStorageContents.getContents(groupId).ifPresent(contents -> {
				storage.updateClientLinkedStorageContents(groupId);
				storageWrapper.getSettingsHandler().reloadFrom(contents.contents().settings());
			});
		});
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).filter(StorageBlockEntity::isLinkedStorage).ifPresent(storage -> {
			ContainerContents.SettingsData settingsData = storageWrapper.getSettingsHandler().getSettingsData();
			if (lastSettingsData == null || !lastSettingsData.equals(settingsData)) {
				lastSettingsData = settingsData.copy();
				PacketDistributor.sendToPlayer(serverPlayer,
						LinkedStorageContentsPayload.createSnapshot(serverPlayer.level(), storage.getLinkedStorageEndpointData().groupId()));
			}
		});
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
		BlockEntity blockEntity = player.level().getBlockEntity(pos);
		return blockEntity instanceof StorageBlockEntity && player.isWithinBlockInteractionRange(pos, 4.0F)
				&& (!(blockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity) || !woodStorageBlockEntity.isPacked());
	}

	@Override
	public boolean supportsItemDisplaySideSelection() {
		return doubleChest;
	}
}
