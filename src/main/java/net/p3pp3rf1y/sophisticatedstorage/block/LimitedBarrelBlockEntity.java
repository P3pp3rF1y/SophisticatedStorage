package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class LimitedBarrelBlockEntity extends BarrelBlockEntity {
	private long lastDepositTime = -100;

	public LimitedBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get());
	}

	public void depositItem(Player player, InteractionHand hand, ItemStack stackInHand, int slot) {
		//noinspection ConstantConditions
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;

		StorageWrapper storageWrapper = getStorageWrapper();
		InventoryHandler invHandler = storageWrapper.getInventoryHandler();
		ItemStack stackInSlot = invHandler.getStackInSlot(slot);
		ItemStack result = stackInHand;

		if (doubleClick) {
			player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(
					playerInventory -> InventoryHelper.transferIntoSlot(playerInventory, getStorageWrapper().getInventoryHandler(), slot, s -> ItemHandlerHelper.canItemStacksStack(stackInSlot, s)));
			return;
		}

		if (stackInSlot.isEmpty()) {
			result = result.copy();
			invHandler.setStackInSlot(slot, result.split(Math.min(result.getCount(), invHandler.getStackLimit(slot, result))));
		} else if (ItemHandlerHelper.canItemStacksStack(stackInSlot, result)) {
			result = invHandler.insertItem(slot, result, false);
		}
		if (result.getCount() != stackInHand.getCount()) {
			player.setItemInHand(hand, result);
		}
	}

	void tryToTakeItem(Player player, int slot) {
		InventoryHandler inventoryHandler = getStorageWrapper().getInventoryHandler();
		ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
		if (stackInSlot.isEmpty()) {
			return;
		}

		int countToTake = player.isShiftKeyDown() ? Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount()) : 1;
		ItemStack stackTaken = inventoryHandler.extractItem(slot, countToTake, false);

		if (player.getInventory().add(stackTaken)) {
			//noinspection ConstantConditions
			getLevel().playSound(null, getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f, (RandHelper.getRandomMinusOneToOne(getLevel().random) * .7f + 1) * 2);
		} else {
			player.drop(stackTaken, false);
		}
	}
}
