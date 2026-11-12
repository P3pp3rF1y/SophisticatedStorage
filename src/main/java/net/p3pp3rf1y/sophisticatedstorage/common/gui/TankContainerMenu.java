package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.TankFluidHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;
import net.p3pp3rf1y.sophisticatedstorage.network.TankMenuUpdateMessage;

import java.util.List;

public class TankContainerMenu extends StorageContainerMenu {
	private static final String CONTAINER_INTERACTION_ACTION = "tankContainer";
	private int amount;
	private int capacity;
	private int lastSentAmount = -1;
	private int lastSentCapacity = -1;
	private UpgradeHandler boundUpgradeHandler;

	public TankContainerMenu(int id, Player player, BlockPos pos) {
		super(ModBlocks.TANK_CONTAINER_TYPE.get(), id, player, pos, false, createContainerSlots(player, pos));
		boundUpgradeHandler = getStorageWrapper().getUpgradeHandler();
	}

	private static List<Slot> createContainerSlots(Player player, BlockPos pos) {
		List<Slot> slots = TankUpgradeContainer.createContainerSlots(
				() -> WorldHelper.getBlockEntity(player.level(), pos, TankBlockEntity.class).orElseThrow().getStorageWrapper().getContainerInventory());
		for (int index = 0; index < slots.size(); index++) {
			slots.get(index).x = 32 + index % 2 * 21;
			slots.get(index).y = 58 + index / 2 * 32;
		}
		return slots;
	}

	public void interactWithFluidContainer() {
		if (isClientSide()) {
			sendToServer(data -> data.putString(ACTION_TAG, CONTAINER_INTERACTION_ACTION));
			return;
		}
		if (!stillValid(player) || getCarried().getCount() > 1) {
			return;
		}
		((TankBlockEntity) getStorageBlockEntity()).getStorageWrapper().getContainerHandler().interactWithCursorStack(getCarried(), stack -> {
			setCarried(stack);
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(-1, incrementStateId(), -1, stack));
			}
		});
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (CONTAINER_INTERACTION_ACTION.equals(data.getString(ACTION_TAG))) {
			interactWithFluidContainer();
		} else {
			super.handleMessage(data);
		}
	}

	public FluidStack getFluid() {
		FluidStack fluid = ((TankBlockEntity) getStorageBlockEntity()).getStorageWrapper().getTankFluidHandler().getFluidInTank(0);
		FluidStack result = fluid.copy();
		if (player.level().isClientSide && !result.isEmpty()) {
			result.setAmount(amount);
		}
		return result;
	}

	public int getCapacity() {
		return player.level().isClientSide ? capacity : ((TankBlockEntity) getStorageBlockEntity()).getStorageWrapper().getCapacity();
	}

	public void setFluidData(int amount, int capacity) {
		this.amount = amount;
		this.capacity = capacity;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player instanceof ServerPlayer serverPlayer) {
			TankFluidHandler handler = ((TankBlockEntity) getStorageBlockEntity()).getStorageWrapper().getTankFluidHandler();
			int currentAmount = handler.getFluidInTank(0).getAmount();
			int currentCapacity = handler.getTankCapacity(0);
			if (lastSentAmount != currentAmount || lastSentCapacity != currentCapacity) {
				lastSentAmount = currentAmount;
				lastSentCapacity = currentCapacity;
				StoragePacketHandler.INSTANCE.sendToClient(serverPlayer, new TankMenuUpdateMessage(containerId, currentAmount, currentCapacity));
			}
		}
	}

	public static TankContainerMenu fromBuffer(int id, Inventory inventory, FriendlyByteBuf buffer) {
		return new TankContainerMenu(id, inventory.player, readMenuData(buffer, inventory.player));
	}

	@Override
	public boolean detectSettingsChangeAndReload() {
		boolean changed = super.detectSettingsChangeAndReload();
		UpgradeHandler current = getStorageWrapper().getUpgradeHandler();
		if (isClientSide() && current != boundUpgradeHandler) {
			boundUpgradeHandler = current;
			if (!changed) {
				refreshAllSlots();
				onUpgradesChanged();
			}
			return true;
		}
		return changed;
	}

	@Override
	public void openSettings() {
	}

	@Override
	public boolean stillValid(Player player) {
		return super.stillValid(player) && getStorageBlockEntity() instanceof TankBlockEntity tank && tank.isPrimary() && !tank.isPacked();
	}
}
