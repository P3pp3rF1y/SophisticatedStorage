package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.BlockSide;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HopperUpgradeWrapper extends UpgradeWrapperBase<HopperUpgradeWrapper, HopperUpgradeItem>
		implements
			ITickableUpgrade,
			INeighborChangeListenerUpgrade {

	private final Set<Direction> pullDirections = new LinkedHashSet<>();
	private final Set<Direction> pushDirections = new LinkedHashSet<>();
	private final Map<Direction, ItemHandlerHolder> handlerCache = new EnumMap<>(Direction.class);

	private final ContentsFilterLogic inputFilterLogic;
	private final TargetContentsFilterLogic outputFilterLogic;
	private long coolDownTime = 0;

	protected HopperUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inputFilterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getInputFilterSlotCount(), storageWrapper::getInventoryHandler,
				storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES);
		outputFilterLogic = new TargetContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getOutputFilterSlotCount(),
				storageWrapper::getInventoryHandler, storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
				ModDataComponents.OUTPUT_FILTER_ATTRIBUTES);

		deserialize();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		initDirections(level, pos);

		if (coolDownTime > level.getGameTime()) {
			return;
		}

		for (Direction pushDirection : pushDirections) {
			if (runOnItemHandlers(level, pos, pushDirection, this::pushItems, entity)) {
				break;
			}
		}

		for (Direction pullDirection : pullDirections) {
			if (runOnItemHandlers(level, pos, pullDirection, this::pullItems, entity)) {
				break;
			}
		}

		coolDownTime = level.getGameTime() + upgradeItem.getTransferSpeedTicks();
	}

	private void initDirections(Level level, BlockPos pos) {
		if (upgrade.has(ModDataComponents.PUSH_DIRECTIONS) || upgrade.has(ModDataComponents.PULL_DIRECTIONS)) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof StorageBlockBase storageBlock) {
			Direction horizontalDirection = storageBlock.getHorizontalDirection(state);
			VerticalFacing verticalFacing = storageBlock.getVerticalFacing(state);
			pullDirections.clear();
			pushDirections.clear();
			initDirections(BlockSide.BOTTOM.toDirection(horizontalDirection, verticalFacing), BlockSide.TOP.toDirection(horizontalDirection, verticalFacing));
		} else {
			initDirections(Direction.DOWN, Direction.UP);
		}
	}

	private boolean pullItems(List<IItemHandler> fromHandlers) {
		for (IItemHandler fromHandler : fromHandlers) {
			if (moveItems(fromHandler, storageWrapper.getInventoryForUpgradeProcessing(), inputFilterLogic)) {
				return true;
			}
		}
		return false;
	}

	private boolean pushItems(List<IItemHandler> toHandlers) {
		for (IItemHandler toHandler : toHandlers) {
			outputFilterLogic.setInventory(toHandler);
			if (moveItems(storageWrapper.getInventoryForUpgradeProcessing(), toHandler, outputFilterLogic)) {
				return true;
			}
		}
		return false;
	}

	private boolean moveItems(IItemHandler fromHandler, IItemHandler toHandler, FilterLogic filterLogic) {
		for (int slot = 0; slot < fromHandler.getSlots(); slot++) {
			ItemStack slotStack = fromHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && filterLogic.matchesFilter(slotStack)) {
				ItemStack extractedStack = fromHandler.extractItem(slot, upgradeItem.getMaxTransferStackSize(), true);
				if (!extractedStack.isEmpty()) {
					ItemStack remainder = InventoryHelper.insertIntoInventory(extractedStack, toHandler, true);
					if (remainder.getCount() < extractedStack.getCount()) {
						InventoryHelper.insertIntoInventory(fromHandler.extractItem(slot, extractedStack.getCount() - remainder.getCount(), false), toHandler,
								false);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onNeighborChange(Level level, BlockPos pos, Direction direction) {
		if (!level.isClientSide() && (pushDirections.contains(direction) || pullDirections.contains(direction)) && needsCacheUpdate(level, pos, direction)) {
			handlerCache.remove(direction);
		}
	}

	private boolean needsCacheUpdate(Level level, BlockPos pos, Direction direction) {
		ItemHandlerHolder holder = handlerCache.get(direction);
		if (holder == null || holder.handlers().isEmpty()) {
			return !level.getBlockState(pos).isAir();
		} else if (holder.refreshOnEveryNeighborChange()) {
			return true;
		}

		for (ItemHandlerTarget handler : holder.handlers()) {
			if (handler.getCapability() == null) {
				return true;
			}
		}

		return false;
	}

	public void updateCacheOnSide(Level level, BlockPos pos, Direction direction) {
		if (!level.isLoaded(pos) || !level.isLoaded(pos.relative(direction)) || !(level instanceof ServerLevel serverLevel)) {
			handlerCache.remove(direction);
			return;
		}

		ItemHandlerHolder itemHandlers = getItemHandlerHolder(level, pos, direction, serverLevel);
		handlerCache.put(direction, itemHandlers);
	}

	private ItemHandlerHolder getItemHandlerHolder(Level level, BlockPos pos, Direction direction, ServerLevel serverLevel) {
		WeakReference<IStorageWrapper> storageWrapperRef = new WeakReference<>(storageWrapper);
		BooleanSupplier validityCheck = () -> {
			IStorageWrapper sw = storageWrapperRef.get();
			if (sw != null) {
				return sw.getUpgradeHandler().getSlotWrappers().containsValue(this);
			}
			return false;
		};

		BlockState storageState = level.getBlockState(pos);
		List<BlockPos> offsetPositions = storageState.getBlock() instanceof StorageBlockBase storageBlock
				? storageBlock.getNeighborPos(storageState, pos, direction)
				: List.of(pos.relative(direction));

		List<ItemHandlerTarget> itemHandlerTargets = new ArrayList<>();

		AtomicBoolean refreshOnEveryNeighborChange = new AtomicBoolean(false);
		offsetPositions.forEach(offsetPos -> {
			Optional<BlockPos> controllerPos = level.getBlockEntity(offsetPos, ModBlocks.STORAGE_INPUT_BLOCK_ENTITY_TYPE.get())
					.flatMap(storageInputBlockEntity -> {
						refreshOnEveryNeighborChange.set(true);
						return storageInputBlockEntity.getControllerPos();
					});
			BlockPos targetPos = controllerPos.orElse(offsetPos);

			itemHandlerTargets.add(new ItemHandlerTarget(BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, targetPos,
					direction.getOpposite(), validityCheck, () -> handlerCache.remove(direction)), controllerPos.isPresent()));
		});
		return new ItemHandlerHolder(itemHandlerTargets, refreshOnEveryNeighborChange.get());
	}

	private boolean runOnItemHandlers(Level level, BlockPos pos, Direction direction, Predicate<List<IItemHandler>> run, @Nullable Entity entity) {
		ItemHandlerHolder holder = getItemHandlerHolder(level, pos, direction, entity == null);
		if (holder == null) {
			return runOnAutomationEntityItemHandlers(level, pos, direction, run, entity);
		}

		List<IItemHandler> handler = holder.handlers().stream().map(ItemHandlerTarget::getCapability).filter(Objects::nonNull).toList();

		return handler.isEmpty() ? runOnAutomationEntityItemHandlers(level, pos, direction, run, entity) : run.test(handler);
	}

	private boolean runOnAutomationEntityItemHandlers(Level level, BlockPos pos, Direction direction, Predicate<List<IItemHandler>> run,
			@Nullable Entity entity) {
		BlockState storageState = level.getBlockState(pos);
		List<BlockPos> offsetPositions = entity == null && storageState.getBlock() instanceof StorageBlockBase storageBlock
				? storageBlock.getNeighborPos(storageState, pos, direction)
				: List.of(pos.relative(direction));

		List<Entity> entities = new ArrayList<>();
		for (BlockPos offsetPosition : offsetPositions) {
			entities.addAll(level.getEntities((Entity) null, new AABB(offsetPosition), e -> e != entity && EntitySelector.ENTITY_STILL_ALIVE.test(e)));
		}
		if (!entities.isEmpty()) {
			Collections.shuffle(entities);
			for (Entity e : entities) {
				IItemHandler entityCap = e.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, direction.getOpposite());
				if (entityCap != null) {
					return run.test(List.of(entityCap));
				}
			}
		}

		return false;
	}

	@Nullable
	private ItemHandlerHolder getItemHandlerHolder(Level level, BlockPos pos, Direction direction, boolean useCache) {
		if (useCache) {
			if (!handlerCache.containsKey(direction)) {
				updateCacheOnSide(level, pos, direction);
			}
			return handlerCache.get(direction);
		}

		return getItemHandlerHolder(level, pos, direction, (ServerLevel) level);
	}

	public ContentsFilterLogic getInputFilterLogic() {
		return inputFilterLogic;
	}

	public ContentsFilterLogic getOutputFilterLogic() {
		return outputFilterLogic;
	}

	public boolean isPullingFrom(Direction direction) {
		return pullDirections.contains(direction);
	}

	public boolean isPushingTo(Direction direction) {
		return pushDirections.contains(direction);
	}

	public void setPullingFrom(Direction direction, boolean shouldPull) {
		if (shouldPull) {
			pullDirections.add(direction);
		} else {
			pullDirections.remove(direction);
		}
		serializePullDirections();
	}

	public void setPushingTo(Direction direction, boolean isPushing) {
		if (isPushing) {
			pushDirections.add(direction);
		} else {
			pushDirections.remove(direction);
		}
		serializePushDirections();
	}

	private void serializePullDirections() {
		upgrade.set(ModDataComponents.PULL_DIRECTIONS, Set.copyOf(pullDirections));
		save();
	}

	private void serializePushDirections() {
		upgrade.set(ModDataComponents.PUSH_DIRECTIONS, Set.copyOf(pushDirections));
		save();
	}

	public void deserialize() {
		pullDirections.clear();
		pushDirections.clear();
		Set<Direction> directions = upgrade.get(ModDataComponents.PULL_DIRECTIONS);
		if (directions != null) {
			pullDirections.addAll(directions);
		}
		directions = upgrade.get(ModDataComponents.PUSH_DIRECTIONS);
		if (directions != null) {
			pushDirections.addAll(directions);
		}
	}

	public void initDirections(Direction pushDirection, Direction pullDirection) {
		setPushingTo(pushDirection, true);
		setPullingFrom(pullDirection, true);
	}

	private static class ItemHandlerTarget {
		private final BlockCapabilityCache<IItemHandler, Direction> handlerCache;
		private final boolean blockExtraction;
		@Nullable
		private IItemHandler noExtractHandler;

		private ItemHandlerTarget(BlockCapabilityCache<IItemHandler, Direction> handlerCache, boolean blockExtraction) {
			this.handlerCache = handlerCache;
			this.blockExtraction = blockExtraction;
		}

		@Nullable
		private IItemHandler getCapability() {
			IItemHandler itemHandler = handlerCache.getCapability();
			if (itemHandler == null) {
				return null;
			}

			if (!blockExtraction) {
				return itemHandler;
			}

			if (noExtractHandler == null) {
				noExtractHandler = new NoExtractItemHandler(handlerCache);
			}
			return noExtractHandler;
		}
	}

	private static class NoExtractItemHandler implements IItemHandler {
		private final BlockCapabilityCache<IItemHandler, Direction> handlerCache;

		private NoExtractItemHandler(BlockCapabilityCache<IItemHandler, Direction> handlerCache) {
			this.handlerCache = handlerCache;
		}

		@Override
		public int getSlots() {
			IItemHandler itemHandler = handlerCache.getCapability();
			return itemHandler == null ? 0 : itemHandler.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			IItemHandler itemHandler = handlerCache.getCapability();
			return itemHandler == null ? ItemStack.EMPTY : itemHandler.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			IItemHandler itemHandler = handlerCache.getCapability();
			return itemHandler == null ? stack : itemHandler.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			IItemHandler itemHandler = handlerCache.getCapability();
			return itemHandler == null ? 0 : itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			IItemHandler itemHandler = handlerCache.getCapability();
			return itemHandler != null && itemHandler.isItemValid(slot, stack);
		}
	}

	private record ItemHandlerHolder(List<ItemHandlerTarget> handlers, boolean refreshOnEveryNeighborChange) {
	}
}
