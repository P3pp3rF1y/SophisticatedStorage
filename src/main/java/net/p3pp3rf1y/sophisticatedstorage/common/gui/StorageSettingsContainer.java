package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class StorageSettingsContainer extends SettingsContainer<IStorageWrapper> {
	private final BlockPos pos;

	protected StorageSettingsContainer(int windowId, Player player, BlockPos pos) {
		super(ModBlocks.SETTINGS_CONTAINER_TYPE.get(), windowId, player, getWrapper(player.level, pos));
		this.pos = pos;
	}

	private static IStorageWrapper getWrapper(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be).orElse(NoopStorageWrapper.INSTANCE);
	}

	@Override
	public void detectSettingsChangeAndReload() {
		//noop
	}

	public static StorageSettingsContainer fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
		return new StorageSettingsContainer(windowId, playerInventory.player, packetBuffer.readBlockPos());
	}

	@Override
	public BlockPos getBlockPosition() {
		return pos;
	}
}
