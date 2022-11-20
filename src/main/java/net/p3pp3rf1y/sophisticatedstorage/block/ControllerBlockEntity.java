package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.CapabilityItemHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.HashSet;
import java.util.Set;

public class ControllerBlockEntity extends ControllerBlockEntityBase implements ILockable {
	private long lastDepositTime = -100;

	public ControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(ControllerBlockEntityBase.SEARCH_RANGE);
	}

	public void depositPlayerItems(Player player, InteractionHand hand) {
		if (getLevel() == null) {
			return;
		}
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;
		if (doubleClick) {
			player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(
					playerInventory -> getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
							.ifPresent(controllerInventory -> InventoryHelper.transfer(playerInventory, controllerInventory, s -> {}, this::canDepositStack))
			);
			return;
		}

		ItemStack itemInHand = player.getItemInHand(hand);
		if (!itemInHand.isEmpty() && canDepositStack(itemInHand)) {
			player.setItemInHand(hand, insertItem(0, itemInHand, false));
		}
	}

	private boolean canDepositStack(ItemStack stack) {
		return hasStack(stack) || isMemorizedItem(stack);
	}

	@Override
	public void toggleLock() {
		Set<ILockable> unlockedStorages = new HashSet<>();
		Set<ILockable> lockedStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, ILockable.class).ifPresent(lockable -> {
			if (lockable.isLocked()) {
				lockedStorages.add(lockable);
			} else {
				unlockedStorages.add(lockable);
			}
		}));

		if (unlockedStorages.isEmpty()) {
			lockedStorages.forEach(ILockable::toggleLock);
		} else {
			unlockedStorages.forEach(ILockable::toggleLock);
		}
	}

	@Override
	public boolean isLocked() {
		return false;
	}
}
