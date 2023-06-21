package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class StorageSettingsContainerMenu extends SettingsContainerMenu<IStorageWrapper> {
	private final BlockPos pos;

	protected StorageSettingsContainerMenu(int windowId, Player player, BlockPos pos) {
		this(ModBlocks.SETTINGS_CONTAINER_TYPE.get(), windowId, player, pos);
	}
	protected StorageSettingsContainerMenu(MenuType<?> menuType, int windowId, Player player, BlockPos pos) {
		super(menuType, windowId, player, getWrapper(player.level(), pos));
		this.pos = pos;
	}

	private static IStorageWrapper getWrapper(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be.getStorageWrapper()).orElse(NoopStorageWrapper.INSTANCE);
	}

	@Override
	public void detectSettingsChangeAndReload() {
		//noop
	}

	public static StorageSettingsContainerMenu fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
		return new StorageSettingsContainerMenu(windowId, playerInventory.player, packetBuffer.readBlockPos());
	}

	@Override
	public BlockPos getBlockPosition() {
		return pos;
	}
}
