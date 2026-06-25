package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageInputBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.BlockSide;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class HopperUpgradeWrapper extends UpgradeWrapperBase<HopperUpgradeWrapper, HopperUpgradeItem>
		implements
			ITickableUpgrade,
			INeighborChangeListenerUpgrade {

	private Set<Direction> pullDirections = new LinkedHashSet<>();
	private Set<Direction> pushDirections = new LinkedHashSet<>();
	private boolean directionsInitialized = false;
	private final Map<Direction, ItemHandlerHolder> handlerCache = new EnumMap<>(Direction.class);

	private final ContentsFilterLogic inputFilterLogic;
	private final TargetContentsFilterLogic outputFilterLogic;
	private long coolDownTime = 0;

	protected HopperUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inputFilterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getInputFilterSlotCount(), storageWrapper::getInventoryHandler,
				storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), "inputFilter");
		outputFilterLogic = new TargetContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getOutputFilterSlotCount(),
				storageWrapper::getInventoryHandler, storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), "outputFilter");

		deserialize();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		initDirections(level, pos);

		if (coolDownTime > level.getGameTime()) {
			return;
		}

		for (Direction pushDirection : pushDirections) {
			boolean done = false;
			for (ItemHandlerTarget itemHandler : getItemHandlers(level, pos, pushDirection, entity == null)) {
				if (itemHandler.getCapability().map(this::pushItems).orElse(false)) {
					done = true;
					break;
				}
			}
			if (!done) {
				for (WorldlyContainer worldlyContainer : getWorldlyContainers(level, pos, pushDirection)) {
					if (pushItemsToContainer(worldlyContainer, pushDirection.getOpposite())) {
						break;
					}
				}
			}

			if (!done) {
				getEntityContainer(level, pos, pushDirection, entity).ifPresent(container -> {
					pushItemsToContainer(container, pushDirection.getOpposite());
				});
			}
		}

		for (Direction pullDirection : pullDirections) {
			boolean done = false;
			for (ItemHandlerTarget itemHandler : getItemHandlers(level, pos, pullDirection, entity == null)) {
				if (itemHandler.getCapability().map(this::pullItems).orElse(false)) {
					done = true;
					break;
				}
			}

			if (!done) {
				for (WorldlyContainer worldlyContainer : getWorldlyContainers(level, pos, pullDirection)) {
					if (pullItemsFromContainer(worldlyContainer, pullDirection.getOpposite())) {
						done = true;
						break;
					}
				}
			}

			if (!done) {
				getEntityContainer(level, pos, pullDirection, entity).ifPresent(container -> {
					pullItemsFromContainer(container, pullDirection.getOpposite());
				});
			}
		}

		coolDownTime = level.getGameTime() + upgradeItem.getTransferSpeedTicks();
	}

	private Optional<Container> getEntityContainer(Level level, BlockPos pos, Direction direction, @Nullable Entity entity) {
		BlockState storageState = level.getBlockState(pos);
		List<BlockPos> offsetPositions = entity == null && storageState.getBlock() instanceof StorageBlockBase storageBlock
				? storageBlock.getNeighborPos(storageState, pos, direction)
				: List.of(pos.relative(direction));

		List<Entity> entities = new ArrayList<>();
		for (BlockPos offsetPosition : offsetPositions) {
			entities.addAll(level.getEntities((Entity) null, new AABB(offsetPosition), e -> e != entity && EntitySelector.CONTAINER_ENTITY_SELECTOR.test(e)));
		}
		if (!entities.isEmpty()) {
			Collections.shuffle(entities);
			return Optional.of((Container) entities.get(0));
		}
		return Optional.empty();
	}

	private boolean pushItemsToContainer(Container worldlyContainer, Direction face) {
		ITrackedContentsItemHandler fromHandler = storageWrapper.getInventoryForUpgradeProcessing();

		outputFilterLogic.setInventory(EmptyHandler.INSTANCE);
		for (int slot = 0; slot < fromHandler.getSlots(); slot++) {
			ItemStack slotStack = fromHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && outputFilterLogic.matchesFilter(slotStack)) {
				ItemStack extractedStack = fromHandler.extractItem(slot, Math.min(worldlyContainer.getMaxStackSize(), upgradeItem.getMaxTransferStackSize()),
						true);
				if (!extractedStack.isEmpty() && pushStackToContainer(worldlyContainer, face, extractedStack, fromHandler, slot)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean pushStackToContainer(Container container, Direction face, ItemStack extractedStack, ITrackedContentsItemHandler fromHandler,
			int slotToExtractFrom) {
		for (int containerSlot = 0; containerSlot < container.getContainerSize(); containerSlot++) {

			boolean canPlaceItem = container instanceof WorldlyContainer worldlyContainer
					? worldlyContainer.canPlaceItemThroughFace(containerSlot, extractedStack, face)
					: container.canPlaceItem(containerSlot, extractedStack);

			if (canPlaceItem) {
				ItemStack existingStack = container.getItem(containerSlot);
				if (existingStack.isEmpty()) {
					container.setItem(containerSlot, extractedStack);
					fromHandler.extractItem(slotToExtractFrom, extractedStack.getCount(), false);
					return true;
				} else if (ItemHandlerHelper.canItemStacksStack(existingStack, extractedStack)) {
					int maxStackSize = Math.min(container.getMaxStackSize(), existingStack.getMaxStackSize());
					int remainder = maxStackSize - existingStack.getCount();
					if (remainder > 0) {
						int countToExtract = Math.min(extractedStack.getCount(), remainder);
						existingStack.grow(countToExtract);
						container.setItem(containerSlot, existingStack);
						fromHandler.extractItem(slotToExtractFrom, countToExtract, false);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean pullItemsFromContainer(Container container, Direction face) {
		ITrackedContentsItemHandler toHandler = storageWrapper.getInventoryForUpgradeProcessing();
		for (int containerSlot = 0; containerSlot < container.getContainerSize(); containerSlot++) {
			ItemStack stackToInsert = container.getItem(containerSlot).copy();
			if (stackToInsert.getCount() > upgradeItem.getMaxTransferStackSize()) {
				stackToInsert.setCount(upgradeItem.getMaxTransferStackSize());
			}
			if (!stackToInsert.isEmpty() && (!(container instanceof WorldlyContainer worldlyContainer)
					|| worldlyContainer.canTakeItemThroughFace(containerSlot, stackToInsert, face)) && inputFilterLogic.matchesFilter(stackToInsert)) {
				ItemStack remainingStack = InventoryHelper.insertIntoInventory(stackToInsert, toHandler, false);

				if (remainingStack.getCount() < stackToInsert.getCount()) {
					container.removeItem(containerSlot, stackToInsert.getCount() - remainingStack.getCount());
					return true;
				}
			}
		}

		return false;
	}

	private void initDirections(Level level, BlockPos pos) {
		if (upgrade.hasTag() && (upgrade.getItem() != ModItems.HOPPER_UPGRADE.get() || directionsInitialized)) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof StorageBlockBase storageBlock) {
			Direction horizontalDirection = storageBlock.getHorizontalDirection(state);
			VerticalFacing verticalFacing = storageBlock.getVerticalFacing(state);
			pullDirections.clear();
			pushDirections.clear();
			initDirections(BlockSide.BOTTOM.toDirection(horizontalDirection, verticalFacing), BlockSide.TOP.toDirection(horizontalDirection, verticalFacing));
			directionsInitialized = true;
		} else {
			initDirections(Direction.DOWN, Direction.UP);
		}
	}

	private List<WorldlyContainer> getWorldlyContainers(Level level, BlockPos pos, Direction direction) {
		BlockState storageState = level.getBlockState(pos);
		List<BlockPos> offsetPositions = storageState.getBlock() instanceof StorageBlockBase storageBlock
				? storageBlock.getNeighborPos(storageState, pos, direction)
				: List.of(pos.relative(direction));
		List<WorldlyContainer> worldlyContainers = new ArrayList<>();
		offsetPositions.forEach(offsetPos -> {
			BlockState state = level.getBlockState(offsetPos);
			if (state.getBlock() instanceof WorldlyContainerHolder worldlyContainerHolder) {
				worldlyContainers.add(worldlyContainerHolder.getContainer(state, level, offsetPos));
			}
		});
		return worldlyContainers;
	}

	private boolean pullItems(IItemHandler fromHandler) {
		return moveItems(fromHandler, storageWrapper.getInventoryForUpgradeProcessing(), inputFilterLogic);
	}

	private boolean pushItems(IItemHandler toHandler) {
		outputFilterLogic.setInventory(toHandler);
		return moveItems(storageWrapper.getInventoryForUpgradeProcessing(), toHandler, outputFilterLogic);
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
			updateCacheOnSide(level, pos, direction);
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
			if (!handler.isPresent()) {
				return true;
			}
		}

		return false;
	}

	public void updateCacheOnSide(Level level, BlockPos pos, Direction direction) {
		if (!level.isLoaded(pos) || !level.isLoaded(pos.relative(direction))) {
			handlerCache.remove(direction);
			return;
		}

		BlockState storageState = level.getBlockState(pos);
		List<BlockPos> offsetPositions = storageState.getBlock() instanceof StorageBlockBase storageBlock
				? storageBlock.getNeighborPos(storageState, pos, direction)
				: List.of(pos.relative(direction));
		List<ItemHandlerTarget> itemHandlerTargets = new ArrayList<>();
		AtomicBoolean refreshOnEveryNeighborChange = new AtomicBoolean(false);
		offsetPositions.forEach(offsetPos -> WorldHelper.getLoadedBlockEntity(level, offsetPos).ifPresent(blockEntity -> {
			boolean blockExtraction = false;
			if (blockEntity instanceof StorageInputBlockEntity input) {
				refreshOnEveryNeighborChange.set(true);
				Optional<BlockPos> controllerPos = input.getControllerPos();
				if (controllerPos.isPresent()) {
					blockEntity = level.getBlockEntity(controllerPos.get());
					blockExtraction = blockEntity != null;
				}
			}

			if (blockEntity == null) {
				return;
			}

			LazyOptional<IItemHandler> lazyOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
			if (lazyOptional.isPresent()) {
				lazyOptional.addListener(l -> updateCacheOnSide(level, pos, direction));
				itemHandlerTargets.add(new ItemHandlerTarget(lazyOptional, blockExtraction));
			}
		}));
		handlerCache.put(direction, new ItemHandlerHolder(itemHandlerTargets, refreshOnEveryNeighborChange.get()));
	}

	private List<ItemHandlerTarget> getItemHandlers(Level level, BlockPos pos, Direction direction, boolean useCache) {
		if (useCache) {
			if (!handlerCache.containsKey(direction)) {
				updateCacheOnSide(level, pos, direction);
			}
		}
		return handlerCache.containsKey(direction) ? handlerCache.get(direction).handlers() : Collections.emptyList();
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
		NBTHelper.putList(upgrade.getOrCreateTag(), "pullDirections", pullDirections, d -> StringTag.valueOf(d.getSerializedName()));
		save();
	}

	private void serializePushDirections() {
		NBTHelper.putList(upgrade.getOrCreateTag(), "pushDirections", pushDirections, d -> StringTag.valueOf(d.getSerializedName()));
		save();
	}

	public void deserialize() {
		pullDirections.clear();
		pushDirections.clear();
		if (upgrade.hasTag()) {
			pullDirections = NBTHelper.getCollection(upgrade.getOrCreateTag(), "pullDirections", Tag.TAG_STRING,
					t -> Optional.ofNullable(Direction.byName(t.getAsString())), HashSet::new).orElseGet(HashSet::new);
			pushDirections = NBTHelper.getCollection(upgrade.getOrCreateTag(), "pushDirections", Tag.TAG_STRING,
					t -> Optional.ofNullable(Direction.byName(t.getAsString())), HashSet::new).orElseGet(HashSet::new);
		}
	}

	public void initDirections(Direction pushDirection, Direction pullDirection) {
		setPushingTo(pushDirection, true);
		setPullingFrom(pullDirection, true);
	}

	private static class ItemHandlerTarget {
		private final LazyOptional<IItemHandler> handler;
		private final boolean blockExtraction;
		@Nullable
		private IItemHandler wrappedHandler;
		@Nullable
		private LazyOptional<IItemHandler> noExtractHandler;

		private ItemHandlerTarget(LazyOptional<IItemHandler> handler, boolean blockExtraction) {
			this.handler = handler;
			this.blockExtraction = blockExtraction;
		}

		private boolean isPresent() {
			return handler.isPresent();
		}

		private LazyOptional<IItemHandler> getCapability() {
			if (!blockExtraction) {
				return handler;
			}

			Optional<IItemHandler> itemHandler = handler.resolve();
			if (itemHandler.isEmpty()) {
				return LazyOptional.empty();
			}

			if (noExtractHandler == null || wrappedHandler != itemHandler.get()) {
				wrappedHandler = itemHandler.get();
				IItemHandler noExtractHandlerValue = new NoExtractItemHandler(wrappedHandler);
				noExtractHandler = LazyOptional.of(() -> noExtractHandlerValue);
			}
			return noExtractHandler;
		}
	}

	private record NoExtractItemHandler(IItemHandler itemHandler) implements IItemHandler {
		@Override
		public int getSlots() {
			return itemHandler.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandler.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return itemHandler.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return itemHandler.isItemValid(slot, stack);
		}
	}

	private record ItemHandlerHolder(List<ItemHandlerTarget> handlers, boolean refreshOnEveryNeighborChange) {
	}
}
