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
import java.util.List;
import java.util.Set;

public class ControllerBlockEntity extends ControllerBlockEntityBase implements ILockable, ICountDisplay {
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
					playerInventory -> InventoryHelper.iterate(playerInventory, (slot, stack) -> {
								if (canDepositStack(stack)) {
									ItemStack resultStack = insertItem(stack, true, false);
									int countToExtract = stack.getCount() - resultStack.getCount();
									if (countToExtract > 0 && playerInventory.extractItem(slot, countToExtract, true).getCount() == countToExtract) {
										insertItem(playerInventory.extractItem(slot, countToExtract, false), false, false);
									}
								}
							}
					));
			return;
		}

		ItemStack itemInHand = player.getItemInHand(hand);
		if (!itemInHand.isEmpty() && canDepositStack(itemInHand)) {
			player.setItemInHand(hand, insertItem(itemInHand, false, false));
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

	@Override
	public boolean shouldShowLock() {
		return false;
	}

	@Override
	public void toggleLockVisibility() {
		Set<ILockable> invisibleLockStorages = new HashSet<>();
		Set<ILockable> visibleLockStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, ILockable.class).ifPresent(lockable -> {
			if (lockable.isLocked()) {
				if (lockable.shouldShowLock()) {
					visibleLockStorages.add(lockable);
				} else {
					invisibleLockStorages.add(lockable);
				}
			}
		}));

		if (invisibleLockStorages.isEmpty()) {
			visibleLockStorages.forEach(ILockable::toggleLockVisibility);
		} else {
			invisibleLockStorages.forEach(ILockable::toggleLockVisibility);
		}
	}

	@Override
	public boolean shouldShowCounts() {
		return false;
	}

	@Override
	public void toggleCountVisibility() {
		Set<ICountDisplay> invisibleCountStorages = new HashSet<>();
		Set<ICountDisplay> visibleCountStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, ICountDisplay.class).ifPresent(countDisplay -> {
			if (countDisplay.shouldShowCounts()) {
				visibleCountStorages.add(countDisplay);
			} else {
				invisibleCountStorages.add(countDisplay);
			}
		}));

		if (invisibleCountStorages.isEmpty()) {
			visibleCountStorages.forEach(ICountDisplay::toggleCountVisibility);
		} else {
			invisibleCountStorages.forEach(ICountDisplay::toggleCountVisibility);
		}
	}

	@Override
	public List<Integer> getSlotCounts() {
		return List.of();
	}
}
