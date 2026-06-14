package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.*;

public class ControllerBlockEntity extends ControllerBlockEntityBase implements ILockable, ICountDisplay, ITierDisplay, IUpgradeDisplay, IFillLevelDisplay {
	private long lastDepositTime = -100;

	private List<VoxelOutliner.Edge> cachedStorageEdges = null;
	private List<VoxelOutliner.Edge> cachedLinkedBlockEdges = null;
	private List<VoxelOutliner.Edge> cachedControllerEdges = null;

	public ControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(getSearchRange());
	}

	public void depositPlayerItems(Player player, InteractionHand hand) {
		if (getLevel() == null) {
			return;
		}
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;
		if (doubleClick) {
			player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(
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
		return hasItem(stack.getItem()) || isMemorizedItem(stack) || isFilterItem(stack.getItem()) || hasMatchingFilter(stack);
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

	@Override
	protected int getSearchRange() {
		return Config.SERVER.controllerRange.get();
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		cachedStorageEdges = null;
		cachedLinkedBlockEdges = null;
	}

	public List<VoxelOutliner.Edge> getStorageBlockEdges() {
		if (cachedStorageEdges == null) {
			Set<BlockPos> positions = new HashSet<>();
			getStoragePositions().stream().filter(pos -> !getLinkedBlocks().contains(pos)).forEach(pos -> positions.addAll(StoragePositionGroups.getGroup(level, pos).memberPositions()));
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

	public List<BlockPos> getStackStorages(ItemStackKey stackKey) {
		List<BlockPos> result = new ArrayList<>();
		result.addAll(stackStorages.getOrDefault(stackKey, Collections.emptySet()));
		result.addAll(memorizedStackStorages.getOrDefault(stackKey.hashCode(), Collections.emptySet()));
		return result;
	}

	public List<BlockPos> getItemStorages(ItemStackKey stackKey) {
		Set<BlockPos> positions = new HashSet<>();
		Item item = stackKey.getStack().getItem();
		if (itemStackKeys.containsKey(item)) {
			itemStackKeys.get(item).forEach(sk -> {
				if (sk.equals(stackKey)) {
					return;
				}
				if (stackStorages.containsKey(sk)) {
					positions.addAll(stackStorages.get(sk));
				}
			});
		}
		positions.addAll(memorizedItemStorages.getOrDefault(item, Collections.emptySet()));
		positions.addAll(filterItemStorages.getOrDefault(item, Collections.emptySet()));
		getStackStorages(stackKey).forEach(positions::remove);
		return new ArrayList<>(positions);
	}

	public List<BlockPos> getEmptyTargetSlotStorages(ItemStackKey stackKey) {
		Set<BlockPos> positions = new HashSet<>(emptySlotsStorages);
		getStackStorages(stackKey).forEach(positions::remove);
		getItemStorages(stackKey).forEach(positions::remove);
		ItemStack copy = stackKey.getStack().copyWithCount(1);
		positions.removeIf(p -> !insertIntoStorage(p, copy, true).isEmpty());
		return new ArrayList<>(positions);
	}
}
