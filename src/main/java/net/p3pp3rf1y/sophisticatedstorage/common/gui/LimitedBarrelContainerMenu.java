package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.List;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_CONTAINER_TYPE;

public class LimitedBarrelContainerMenu extends StorageContainerMenu {
	public LimitedBarrelContainerMenu(int containerId, Player player, BlockPos pos) {
		this(containerId, player, pos, false);
	}

	public LimitedBarrelContainerMenu(int containerId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		super(LIMITED_BARREL_CONTAINER_TYPE.get(), containerId, player, pos, openersAlreadyActive);
	}

	public static LimitedBarrelContainerMenu fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
		return new LimitedBarrelContainerMenu(windowId, playerInventory.player, buffer.readBlockPos());
	}

	@Override
	protected StorageSettingsContainerMenu instantiateSettingsContainerMenu(int windowId, Player player, BlockPos pos) {
		return instantiateSettingsContainerMenu(windowId, player, pos, false);
	}

	@Override
	protected StorageSettingsContainerMenu instantiateSettingsContainerMenu(int windowId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		return new LimitedBarrelSettingsContainerMenu(windowId, player, pos, openersAlreadyActive);
	}

	@Override
	public List<Integer> getSlotOverlayColors(int slot) {
		return List.of();
	}
}
