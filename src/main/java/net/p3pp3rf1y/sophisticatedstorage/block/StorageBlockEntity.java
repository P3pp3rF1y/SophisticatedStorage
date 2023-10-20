package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageBlockEntity extends BlockEntity implements IControllableStorage, ILinkable, ILockable, Nameable, ITierDisplay, IUpgradeDisplay {
	public static final String STORAGE_WRAPPER_TAG = "storageWrapper";
	private final StorageWrapper storageWrapper;
	@Nullable
	protected Component displayName = null;

	private boolean updateBlockRender = false;
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private boolean isBeingUpgraded = false;

	protected abstract ContainerOpenersCounter getOpenersCounter();

	private boolean isDroppingContents = false;

	private boolean chunkBeingUnloaded = false;

	@Nullable
	private LazyOptional<IItemHandler> itemHandlerCap;
	private boolean locked = false;
	private boolean showLock = true;
	private boolean showTier = true;
	private boolean showUpgrades = false;
	@Nullable
	private ContentsFilteredItemHandler contentsFilteredItemHandler = null;

	protected StorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends StorageBlockEntity> blockEntityType) {
		super(blockEntityType, pos, state);
		storageWrapper = new StorageWrapper(() -> this::setChanged, () -> WorldHelper.notifyBlockUpdate(this), () -> {
			setChanged();
			WorldHelper.notifyBlockUpdate(this);
		}, this instanceof BarrelBlockEntity ? 4 : 1) {

			@Override
			public Optional<UUID> getContentsUuid() {
				if (contentsUuid == null) {
					contentsUuid = UUID.randomUUID();
					save();
				}
				return Optional.of(contentsUuid);
			}

			@Override
			public ItemStack getWrappedStorageStack() {
				BlockPos pos = getBlockPos();
				return getBlockState().getBlock().getCloneItemStack(getBlockState(), new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.DOWN, pos, true), getLevel(), pos, null);
			}

			@Override
			protected void onUpgradeRefresh() {
				if (!isDroppingContents && level != null && !level.isClientSide && getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					storageBlock.setTicking(level, getBlockPos(), getBlockState(), !storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
				}
			}

			@Override
			public int getDefaultNumberOfInventorySlots() {
				if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					return storageBlock.getNumberOfInventorySlots();
				}
				return 0;
			}

			@Override
			protected boolean isAllowedInStorage(ItemStack stack) {
				return StorageBlockEntity.this.isAllowedInStorage(stack);
			}

			@Override
			public int getDefaultNumberOfUpgradeSlots() {
				if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					return storageBlock.getNumberOfUpgradeSlots();
				}
				return 0;
			}

			@Override
			public int getBaseStackSizeMultiplier() {
				return getBlockState().getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getBaseStackSizeMultiplier() : super.getBaseStackSizeMultiplier();
			}

			@Override
			protected boolean emptyInventorySlotsAcceptItems() {
				return !locked || allowsEmptySlotsMatchingItemInsertsWhenLocked();
			}

			@Override
			public ITrackedContentsItemHandler getInventoryForInputOutput() {
				if (locked && allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
					if (contentsFilteredItemHandler == null) {
						contentsFilteredItemHandler = new ContentsFilteredItemHandler(super::getInventoryForInputOutput, () -> getStorageWrapper().getInventoryHandler().getSlotTracker(), () -> getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
					}
					return contentsFilteredItemHandler;
				}

				return super.getInventoryForInputOutput();
			}
		};
		storageWrapper.setUpgradeCachesInvalidatedHandler(this::onUpgradeCachesInvalidated);
	}

	protected void onUpgradeCachesInvalidated() {
		invalidateStorageCap();
	}

	public boolean isOpen() {
		return getOpenersCounter().getOpenerCount() > 0;
	}

	@Override
	public Component getCustomName() {
		return displayName;
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveStorageWrapper(tag);
		saveSynchronizedData(tag);
		saveControllerPos(tag);
		if (isLinkedToController) {
			tag.putBoolean("isLinkedToController", isLinkedToController);
		}
	}

	private void saveStorageWrapper(CompoundTag tag) {
		tag.put(STORAGE_WRAPPER_TAG, storageWrapper.save(new CompoundTag()));
	}

	private void saveStorageWrapperClientData(CompoundTag tag) {
		tag.put(STORAGE_WRAPPER_TAG, storageWrapper.saveData(new CompoundTag()));
	}

	protected void saveSynchronizedData(CompoundTag tag) {
		if (displayName != null) {
			tag.putString("displayName", Component.Serializer.toJson(displayName));
		}
		if (updateBlockRender) {
			tag.putBoolean("updateBlockRender", true);
		}
		if (locked) {
			tag.putBoolean("locked", locked);
		}
		if (!showLock) {
			tag.putBoolean("showLock", showLock);
		}
		if (!showTier) {
			tag.putBoolean("showTier", showTier);
		}
		if (showUpgrades) {
			tag.putBoolean("showUpgrades", showUpgrades);
		}
	}

	public void startOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			getOpenersCounter().incrementOpeners(player, level, getBlockPos(), getBlockState());
		}

	}

	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			getOpenersCounter().decrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			getOpenersCounter().recheckOpeners(level, getBlockPos(), getBlockState());
		}
	}

	void playSound(BlockState state, SoundEvent sound) {
		if (level == null || !(state.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		Vec3i vec3i = storageBlock.getFacing(state).getNormal();
		double d0 = worldPosition.getX() + 0.5D + vec3i.getX() / 2.0D;
		double d1 = worldPosition.getY() + 0.5D + vec3i.getY() / 2.0D;
		double d2 = worldPosition.getZ() + 0.5D + vec3i.getZ() / 2.0D;
		level.playSound(null, d0, d1, d2, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		loadStorageWrapper(tag);
		loadSynchronizedData(tag);
		loadControllerPos(tag);

		isLinkedToController = NBTHelper.getBoolean(tag, "isLinkedToController").orElse(false);
	}

	private void loadStorageWrapper(CompoundTag tag) {
		NBTHelper.getCompound(tag, STORAGE_WRAPPER_TAG).ifPresent(storageWrapper::load);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		storageWrapper.onInit();
		registerWithControllerOnLoad();
	}

	public void loadSynchronizedData(CompoundTag tag) {
		displayName = NBTHelper.getComponent(tag, "displayName").orElse(null);
		locked = NBTHelper.getBoolean(tag, "locked").orElse(false);
		showLock = NBTHelper.getBoolean(tag, "showLock").orElse(true);
		showTier = NBTHelper.getBoolean(tag, "showTier").orElse(true);
		showUpgrades = NBTHelper.getBoolean(tag, "showUpgrades").orElse(false);
		if (level != null && level.isClientSide) {
			if (tag.getBoolean("updateBlockRender")) {
				WorldHelper.notifyBlockUpdate(this);
			}
		} else {
			updateBlockRender = true;
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		chunkBeingUnloaded = true;
	}

	@Override
	public void setRemoved() {
		if (!isBeingUpgraded && !chunkBeingUnloaded && level != null) {
			removeFromController();
		}

		super.setRemoved();
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag tag = pkt.getTag();
		if (tag == null) {
			return;
		}

		loadStorageWrapper(tag);
		loadSynchronizedData(tag);
	}

	public void setUpdateBlockRender() {
		updateBlockRender = true;
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		saveStorageWrapperClientData(tag);
		saveSynchronizedData(tag);
		return tag;
	}

	public static void serverTick(Level level, BlockPos blockPos, StorageBlockEntity storageBlockEntity) {
		storageBlockEntity.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).forEach(upgrade -> upgrade.tick(null, level, blockPos));
	}

	@Override
	public StorageWrapper getStorageWrapper() {
		return storageWrapper;
	}

	@Override
	public Component getName() {
		return getDisplayName();
	}

	@Override
	public Component getDisplayName() {
		if (displayName != null) {
			return displayName;
		}
		return getBlockState().getBlock().getName();
	}

	@SuppressWarnings("unused") //stack param used in override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return true;
	}

	public void increaseStorageSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		int currentInventorySlots = getStorageWrapper().getInventoryHandler().getSlots();
		getStorageWrapper().increaseSize(additionalInventorySlots, additionalUpgradeSlots);
		if (additionalInventorySlots > 0) {
			changeSlots(currentInventorySlots + additionalInventorySlots);
		}
	}

	public void dropContents() {
		if (level == null || level.isClientSide) {
			return;
		}
		isDroppingContents = true;
		InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, worldPosition);

		InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, worldPosition);
		isDroppingContents = false;
	}

	public void setCustomName(Component customName) {
		displayName = customName;
		setChanged();
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (itemHandlerCap == null) {
				itemHandlerCap = LazyOptional.of(() -> new CachedFailedInsertInventoryHandler(getStorageWrapper().getInventoryForInputOutput(), () -> level != null ? level.getGameTime() : 0));
			}
			return itemHandlerCap.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		invalidateStorageCap();
	}

	private void invalidateStorageCap() {
		if (itemHandlerCap != null) {
			LazyOptional<IItemHandler> tempItemHandlerCap = itemHandlerCap;
			itemHandlerCap = null;
			tempItemHandlerCap.invalidate();
		}
	}

	public boolean shouldDropContents() {
		return true;
	}

	@Override
	public void setControllerPos(BlockPos controllerPos) {
		this.controllerPos = controllerPos;
		setChanged();
	}

	@Override
	public Optional<BlockPos> getControllerPos() {
		return Optional.ofNullable(controllerPos);
	}

	@Override
	public void removeControllerPos() {
		controllerPos = null;
		setChanged();
	}

	@Override
	public BlockPos getStorageBlockPos() {
		return getBlockPos();
	}

	@Override
	public Level getStorageBlockLevel() {
		return Objects.requireNonNull(getLevel());
	}

	@Override
	public void linkToController(BlockPos controllerPos) {
		if (getControllerPos().isPresent()) {
			return;
		}

		isLinkedToController = true;
		ILinkable.super.linkToController(controllerPos);
		setChanged();
	}

	@Override
	public boolean isLinked() {
		return isLinkedToController && getControllerPos().isPresent();
	}

	@Override
	public void setNotLinked() {
		ILinkable.super.setNotLinked();
		isLinkedToController = false;
		setChanged();
	}

	@Override
	public boolean canConnectStorages() {
		return !isLinkedToController;
	}

	@Override
	public Set<BlockPos> getConnectablePositions() {
		return Collections.emptySet();
	}

	@Override
	public boolean connectLinkedSelf() {
		return true;
	}

	@Override
	public boolean canBeConnected() {
		return isLinked() || IControllableStorage.super.canBeConnected();
	}

	public void setBeingUpgraded() {
		isBeingUpgraded = true;
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	@Override
	public void toggleLock() {
		if (locked) {
			unlock();
		} else {
			lock();
		}
	}

	public boolean memorizesItemsWhenLocked() {
		return false;
	}

	public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
		return true;
	}

	private void lock() {
		locked = true;
		if (memorizesItemsWhenLocked()) {
			getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0, getStorageWrapper().getInventoryHandler().getSlots());
		}
		updateEmptySlots();
		if (allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
			contentsFilteredItemHandler = null;
			invalidateStorageCap();
		}
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	private void unlock() {
		locked = false;
		if (memorizesItemsWhenLocked()) {
			getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).unselectAllSlots();
			ItemDisplaySettingsCategory itemDisplaySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
			InventoryHelper.iterate(getStorageWrapper().getInventoryHandler(), (slot, stack) -> {
				if (stack.isEmpty()) {
					itemDisplaySettings.itemChanged(slot);
				}
			});
		}
		updateEmptySlots();
		if (allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
			contentsFilteredItemHandler = null;
			invalidateStorageCap();
		}
		setChanged();
		setUpdateBlockRender();
		WorldHelper.notifyBlockUpdate(this);
	}

	@Override
	public boolean shouldShowLock() {
		return showLock;
	}

	@Override
	public void toggleLockVisibility() {
		showLock = !showLock;
		setChanged();
		setUpdateBlockRender();
		WorldHelper.notifyBlockUpdate(this);
	}

	@Override
	public boolean shouldShowTier() {
		return showTier;
	}

	@Override
	public void toggleTierVisiblity() {
		showTier = !showTier;
		setChanged();
		setUpdateBlockRender();
		WorldHelper.notifyBlockUpdate(this);
	}

	@Override
	public boolean shouldShowUpgrades() {
		return showUpgrades;
	}

	@Override
	public void toggleUpgradesVisiblity() {
		showUpgrades = !showUpgrades;
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	public void onNeighborChange(BlockPos neighborPos) {
		Direction direction = Direction.fromNormal(Integer.signum(neighborPos.getX() - worldPosition.getX()), Integer.signum(neighborPos.getY() - worldPosition.getY()), Integer.signum(neighborPos.getZ() - worldPosition.getZ()));
		if (direction == null) {
			return;
		}
		storageWrapper.getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class).forEach(upgrade -> upgrade.onNeighborChange(level, worldPosition, direction));
	}

	private static class ContentsFilteredItemHandler implements ITrackedContentsItemHandler {

		private final Supplier<ITrackedContentsItemHandler> itemHandlerGetter;
		private final Supplier<ISlotTracker> slotTrackerGetter;
		private final Supplier<MemorySettingsCategory> memorySettingsGetter;

		private ContentsFilteredItemHandler(Supplier<ITrackedContentsItemHandler> itemHandlerGetter, Supplier<ISlotTracker> slotTrackerGetter, Supplier<MemorySettingsCategory> memorySettingsGetter) {
			this.itemHandlerGetter = itemHandlerGetter;
			this.slotTrackerGetter = slotTrackerGetter;
			this.memorySettingsGetter = memorySettingsGetter;
		}

		@Override
		public int getSlots() {
			return itemHandlerGetter.get().getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandlerGetter.get().getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			if (matchesContents(stack)) {
				return itemHandlerGetter.get().insertItem(slot, stack, simulate);
			}
			return stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return itemHandlerGetter.get().extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandlerGetter.get().getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return matchesContents(stack) && itemHandlerGetter.get().isItemValid(slot, stack);
		}

		private boolean matchesContents(ItemStack stack) {
			return slotTrackerGetter.get().getItems().contains(stack.getItem()) || memorySettingsGetter.get().matchesFilter(stack);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (matchesContents(stack)) {
				return itemHandlerGetter.get().insertItem(stack, simulate);
			}
			return stack;
		}

		@Override
		public Set<ItemStackKey> getTrackedStacks() {
			return itemHandlerGetter.get().getTrackedStacks();
		}

		@Override
		public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			itemHandlerGetter.get().registerTrackingListeners(onAddStackKey, onRemoveStackKey, onAddFirstEmptySlot, onRemoveLastEmptySlot);
		}

		@Override
		public void unregisterStackKeyListeners() {
			itemHandlerGetter.get().unregisterStackKeyListeners();
		}

		@Override
		public boolean hasEmptySlots() {
			return itemHandlerGetter.get().hasEmptySlots();
		}

		@Override
		public int getInternalSlotLimit(int slot) {
			return itemHandlerGetter.get().getInternalSlotLimit(slot);
		}

		@Override
		public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
			itemHandlerGetter.get().setStackInSlot(slot, stack);
		}
	}
}
