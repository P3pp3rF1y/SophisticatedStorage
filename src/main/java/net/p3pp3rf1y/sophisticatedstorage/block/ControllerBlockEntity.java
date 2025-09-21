package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.util.VoxelOutliner;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerBlockEntity extends ControllerBlockEntityBase implements ILockable, ICountDisplay, ITierDisplay, IUpgradeDisplay, IFillLevelDisplay {
	private long lastDepositTime = -100;

	@Nullable
	private IItemHandler cachedFailedInsertItemHandler;
	private List<VoxelOutliner.Edge> cachedStorageEdges = null;
	private List<VoxelOutliner.Edge> cachedLinkedBlockEdges = null;
	private List<VoxelOutliner.Edge> cachedControllerEdges = null;

	public ControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	public void depositPlayerItems(Player player, InteractionHand hand) {
		if (getLevel() == null) {
			return;
		}
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;
		if (doubleClick) {
			CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null,
					playerInventory -> InventoryHelper.iterate(playerInventory, (slot, stack) -> {
						if (canDepositStack(stack)) {
							ItemStack resultStack = insertItem(stack, true, false);
							int countToExtract = stack.getCount() - resultStack.getCount();
							if (countToExtract > 0 && playerInventory.extractItem(slot, countToExtract, true).getCount() == countToExtract) {
								insertItem(playerInventory.extractItem(slot, countToExtract, false), false, false);
							}
						}
					}));
			return;
		}

		ItemStack itemInHand = player.getItemInHand(hand);
		if (!itemInHand.isEmpty() && canDepositStack(itemInHand)) {
			player.setItemInHand(hand, insertItem(itemInHand, false, false));
		}
	}

	private boolean canDepositStack(ItemStack stack) {
		return hasItem(stack.getItem()) || isMemorizedItem(stack) || isFilterItem(stack.getItem());
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

	@Override
	public boolean shouldShowTier() {
		return false;
	}

	@Override
	public void toggleTierVisiblity() {
		Set<ITierDisplay> invisibleTierStorages = new HashSet<>();
		Set<ITierDisplay> visibleTierStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, ITierDisplay.class).ifPresent(tierDisplay -> {
			if (tierDisplay.shouldShowTier()) {
				visibleTierStorages.add(tierDisplay);
			} else {
				invisibleTierStorages.add(tierDisplay);
			}
		}));

		if (invisibleTierStorages.isEmpty()) {
			visibleTierStorages.forEach(ITierDisplay::toggleTierVisiblity);
		} else {
			invisibleTierStorages.forEach(ITierDisplay::toggleTierVisiblity);
		}
	}

	@Override
	public boolean shouldShowUpgrades() {
		return false;
	}

	@Override
	public void toggleUpgradesVisiblity() {
		Set<IUpgradeDisplay> invisibleUpgradeStorages = new HashSet<>();
		Set<IUpgradeDisplay> visibleUpgradeStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, IUpgradeDisplay.class).ifPresent(upgradeDisplay -> {
			if (upgradeDisplay.shouldShowUpgrades()) {
				visibleUpgradeStorages.add(upgradeDisplay);
			} else {
				invisibleUpgradeStorages.add(upgradeDisplay);
			}
		}));

		if (invisibleUpgradeStorages.isEmpty()) {
			visibleUpgradeStorages.forEach(IUpgradeDisplay::toggleUpgradesVisiblity);
		} else {
			invisibleUpgradeStorages.forEach(IUpgradeDisplay::toggleUpgradesVisiblity);
		}
	}

	@Override
	public boolean shouldShowFillLevels() {
		return false;
	}

	@Override
	public void toggleFillLevelVisibility() {
		Set<IFillLevelDisplay> invisibleFillLevelStorages = new HashSet<>();
		Set<IFillLevelDisplay> visibleFillLevelStorages = new HashSet<>();
		getStoragePositions().forEach(storagePosition -> WorldHelper.getLoadedBlockEntity(level, storagePosition, IFillLevelDisplay.class).ifPresent(fillLevelDisplay -> {
			if (fillLevelDisplay.shouldShowFillLevels()) {
				visibleFillLevelStorages.add(fillLevelDisplay);
			} else {
				invisibleFillLevelStorages.add(fillLevelDisplay);
			}
		}));

		if (invisibleFillLevelStorages.isEmpty()) {
			visibleFillLevelStorages.forEach(IFillLevelDisplay::toggleFillLevelVisibility);
		} else {
			invisibleFillLevelStorages.forEach(IFillLevelDisplay::toggleFillLevelVisibility);
		}
	}

	@Override
	public List<Float> getSlotFillLevels() {
		return List.of();
	}

	public IItemHandler getExternalItemHandler(@Nullable Direction side) {
		if (side == null) {
			return this;
		} else {
			if (cachedFailedInsertItemHandler == null) {
				cachedFailedInsertItemHandler = new CachedFailedInsertInventoryHandler(() -> this, () -> level != null ? level.getGameTime() : 0);
			}
			return cachedFailedInsertItemHandler;
		}
	}

	@Override
	protected int getSearchRange() {
		return Config.SERVER.controllerRange.getAsInt();
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		cachedStorageEdges = null;
		cachedLinkedBlockEdges = null;
	}

	public List<VoxelOutliner.Edge> getStorageBlockEdges() {
		if (cachedStorageEdges == null) {
			Set<BlockPos> positions = new HashSet<>(getStoragePositions());
			positions.removeIf(getLinkedBlocks()::contains);
			List<BlockPos> extraPositions = new ArrayList<>();
			positions.forEach(pos -> {
						BlockState state = level.getBlockState(pos);
						if (state.getBlock() instanceof StorageBlockBase storageBlock) {
							storageBlock.getExtraPosition(state, pos).ifPresent(extraPositions::add);
						}
					});
			positions.addAll(extraPositions);
			cachedStorageEdges = VoxelOutliner.computeRenderableEdges(positions);
		}
		return cachedStorageEdges;
	}

	public List<VoxelOutliner.Edge> getLinkedBlockEdges() {
		if (cachedLinkedBlockEdges == null) {
			cachedLinkedBlockEdges = new ArrayList<>();
			getLinkedBlocks().forEach(linkedPos -> cachedLinkedBlockEdges.addAll(VoxelOutliner.computeShapeRenderableEdges(level, List.of(linkedPos))));
		}
		return cachedLinkedBlockEdges;
	}

	public List<VoxelOutliner.Edge> getControllerEdges() {
		if (cachedControllerEdges == null) {
			cachedControllerEdges = VoxelOutliner.computeRenderableEdges(List.of(worldPosition));
		}
		return cachedControllerEdges;
	}
}
