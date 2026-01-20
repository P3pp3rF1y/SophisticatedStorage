package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.network.StorageOpennessPayload;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nullable;
import java.util.*;

public abstract class StorageBlockEntity extends BlockEntity implements IControllableStorage, ILinkable, ILockable, Nameable, ITierDisplay, IUpgradeDisplay {
	public static final String STORAGE_WRAPPER = "storageWrapper";
	public static final String UPDATE_BLOCK_RENDER_TAG = "updateBlockRender";
	private final StorageWrapper storageWrapper;
	@Nullable
	protected Component displayName = null;

	private boolean updateBlockRender = true;
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private boolean isBeingUpgraded = false;

	public abstract SophisticatedOpenersCounter getOpenersCounter();

	private boolean isDroppingContents = false;

	private boolean chunkBeingUnloaded = false;

	private boolean locked = false;
	private boolean showLock = true;
	private boolean showTier = true;
	private boolean showUpgrades = false;
	@Nullable
	private ContentsFilteredItemHandler contentsFilteredItemHandler = null;

	protected StorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends StorageBlockEntity> blockEntityType) {
		super(blockEntityType, pos, state);
		storageWrapper = new StorageWrapper(() -> this::setChanged, () -> {
			if (level != null && !level.isClientSide()) {
				WorldHelper.notifyBlockUpdate(this);
			}
		}, () -> {
			if (level != null && !level.isClientSide()) {
				setChanged();
				WorldHelper.notifyBlockUpdate(this);
			}
		}, this instanceof BarrelBlockEntity ? 4 : 1, this instanceof ICountDisplay || this instanceof IFillLevelDisplay) {

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
				BlockState state = getBlockState();
				return addWrappedStorageStackData(state.getBlock().getCloneItemStack(getLevel(), pos, state, true, null), state);
			}

			@Override
			protected void onUpgradeRefresh() {
				if (canRefreshUpgrades() && getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
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
			protected boolean isAllowedInStorage(ItemResource resource) {
				return StorageBlockEntity.this.isAllowedInStorage(resource);
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
			public String getStorageType() {
				return StorageBlockEntity.this.getStorageType();
			}

			@Override
			public Component getDisplayName() {
				return StorageBlockEntity.this.getDisplayName();
			}

			@Override
			protected boolean emptyInventorySlotsAcceptItems() {
				return !locked || allowsEmptySlotsMatchingItemInsertsWhenLocked();
			}

			@Override
			public ITrackedContentsItemResourceHandler getInventoryForInputOutput() {
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

	protected boolean canRefreshUpgrades() {
		return !isDroppingContents && level != null && !level.isClientSide();
	}

	@SuppressWarnings("java:S1172") //parameter used in override
	protected ItemStack addWrappedStorageStackData(ItemStack cloneItemStack, BlockState state) {
		return cloneItemStack;
	}

	protected abstract String getStorageType();

	protected void onUpgradeCachesInvalidated() {
		invalidateCapabilitiesAndControllerCache();
	}

	private void invalidateCapabilitiesAndControllerCache() {
		invalidateCapabilities();
		if (level != null) {
			onInventoryInputOutputHandlerRefresh();
		}
	}

	public boolean isOpen() {
		return getOpenersCounter().getOpenerCount() > 0;
	}

	@Override
	public Component getCustomName() {
		return displayName;
	}

	@Override
	public void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveStorageWrapper(out);
		saveSynchronizedData(out);
		if (isLinkedToController) {
			out.putBoolean("isLinkedToController", isLinkedToController);
		}
	}

	private void saveStorageWrapper(ValueOutput out) {
		out.putChild(STORAGE_WRAPPER, storageWrapper);
	}

	private void saveStorageWrapperClientData(ValueOutput out) {
		storageWrapper.saveClientData(out.child(STORAGE_WRAPPER));
	}

	protected void saveSynchronizedData(ValueOutput out) {
		if (displayName != null) {
			out.store("displayName", ComponentSerialization.CODEC, displayName);
		}
		if (updateBlockRender) {
			out.putBoolean(UPDATE_BLOCK_RENDER_TAG, true);
		}
		updateBlockRender = false;
		if (locked) {
			out.putBoolean("locked", locked);
		}
		if (!showLock) {
			out.putBoolean("showLock", showLock);
		}
		if (!showTier) {
			out.putBoolean("showTier", showTier);
		}
		if (showUpgrades) {
			out.putBoolean("showUpgrades", showUpgrades);
		}
		saveControllerPos(out);
	}

	public void startOpen(ContainerUser containerUser) {
		if (level == null || level.isClientSide() || remove || containerUser.getLivingEntity().isSpectator()) {
			return;
		}
		getOpenersCounter().incrementOpeners(containerUser.getLivingEntity(), level, getBlockPos(), getBlockState(), containerUser.getContainerInteractionRange());
		sendOpenness();
	}

	public void stopOpen(Player player) {
		if (level == null || level.isClientSide() || remove || player.isSpectator()) {
			return;
		}
		getOpenersCounter().decrementOpeners(player, level, getBlockPos(), getBlockState());
		sendOpenness();
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			int countBeforeCheck = getOpenersCounter().getOpenerCount();
			getOpenersCounter().recheckOpeners(level, getBlockPos(), getBlockState());
			int countAfterCheck = getOpenersCounter().getOpenerCount();
			if (countBeforeCheck != countAfterCheck && (countBeforeCheck == 0 || countAfterCheck == 0)) {
				sendOpenness();
			}
		}
	}

	private void sendOpenness() {
		if (level instanceof ServerLevel serverLevel) {
			ChunkPos chunkPos = level.getChunkAt(getBlockPos()).getPos();
			PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, new StorageOpennessPayload(getBlockPos(), getOpenersCounter().getOpenerCount() > 0));
		}
	}

	void playSound(BlockState state, SoundEvent sound) {
		if (level == null || !(state.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		Vec3i vec3i = storageBlock.getFacing(state).getUnitVec3i();
		double d0 = worldPosition.getX() + 0.5D + vec3i.getX() / 2.0D;
		double d1 = worldPosition.getY() + 0.5D + vec3i.getY() / 2.0D;
		double d2 = worldPosition.getZ() + 0.5D + vec3i.getZ() / 2.0D;
		level.playSound(null, d0, d1, d2, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		loadStorageWrapper(in);
		loadSynchronizedData(in);

		isLinkedToController = in.getBooleanOr("isLinkedToController", false);
	}

	private void loadStorageWrapper(ValueInput in) {
		in.child(STORAGE_WRAPPER).ifPresent(storageWrapper::deserialize);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		storageWrapper.onInit();
		registerWithControllerOnLoad();
	}

	public void loadSynchronizedData(ValueInput in) {
		displayName = in.read("displayName", ComponentSerialization.CODEC).orElse(null);
		locked = in.getBooleanOr("locked", false);
		showLock = in.getBooleanOr("showLock", true);
		showTier = in.getBooleanOr("showTier", true);
		showUpgrades = in.getBooleanOr("showUpgrades", false);
		if (level != null && level.isClientSide()) {
			if (in.getBooleanOr(UPDATE_BLOCK_RENDER_TAG, false)) {
				WorldHelper.notifyBlockUpdate(this);
			}
		}
		loadControllerPos(in);
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
	public void onDataPacket(Connection net, ValueInput in) {
		loadStorageWrapperClient(in);
		loadSynchronizedData(in);
	}

	public void setUpdateBlockRender() {
		updateBlockRender = true;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag(registries).merge(ValueIOHelper.collectOutputToTag(registries, out -> {
			saveStorageWrapperClientData(out);
			saveSynchronizedData(out);
		}));
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		loadStorageWrapperClient(input);
		loadSynchronizedData(input);
	}

	private void loadStorageWrapperClient(ValueInput in) {
		storageWrapper.loadClientData(in.childOrEmpty(STORAGE_WRAPPER));
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

	@SuppressWarnings("unused") //resource param used in override
	protected boolean isAllowedInStorage(ItemResource resource) {
		return true;
	}

	public void changeStorageSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		int currentInventorySlots = getStorageWrapper().getInventoryHandler().size();
		getStorageWrapper().changeSize(additionalInventorySlots, additionalUpgradeSlots);
		changeSlots(currentInventorySlots + additionalInventorySlots);
		invalidateCapabilitiesAndControllerCache();
	}

	public void dropContents() {
		if (level == null || level.isClientSide()) {
			return;
		}
		isDroppingContents = true;
		InventoryHelper.dropResources(storageWrapper.getInventoryHandler(), level, worldPosition);

		InventoryHelper.dropResources(storageWrapper.getUpgradeHandler(), level, worldPosition);
		isDroppingContents = false;
	}

	public void setCustomName(Component customName) {
		displayName = customName;
		setChanged();
	}

	@Nullable
	public ResourceHandler<ItemResource> getExternalItemHandler(@Nullable Direction side) {
		return getStorageWrapper().getInventoryForInputOutput();
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
		if (controllerPos != null) {
			controllerPos = null;
			setChanged();
		}
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

	public void setBeingUpgraded(boolean isBeingUpgraded) {
		this.isBeingUpgraded = isBeingUpgraded;
	}

	public boolean isBeingUpgraded() {
		return isBeingUpgraded;
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
			getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0, getStorageWrapper().getInventoryHandler().size());
		}
		updateEmptySlots();
		if (allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
			contentsFilteredItemHandler = null;
			invalidateCapabilitiesAndControllerCache();
		}
		onInventoryInputOutputHandlerRefresh();
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
			invalidateCapabilitiesAndControllerCache();
		}
		onInventoryInputOutputHandlerRefresh();
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
		Direction direction = getNeighborDirection(neighborPos);
		if (direction == null) {
			return;
		}
		storageWrapper.getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class).forEach(upgrade -> upgrade.onNeighborChange(level, worldPosition, direction));
	}

	@Nullable
	protected Direction getNeighborDirection(BlockPos neighborPos) {
		Direction direction = null;
		int normalX = Integer.signum(neighborPos.getX() - worldPosition.getX());
		int normalY = Integer.signum(neighborPos.getY() - worldPosition.getY());
		int normalZ = Integer.signum(neighborPos.getZ() - worldPosition.getZ());
		for (Direction value : Direction.values()) {
			Vec3i normal = value.getUnitVec3i();
			if (normal.getX() == normalX && normal.getY() == normalY && normal.getZ() == normalZ) {
				direction = value;
				break;
			}
		}
		return direction;
	}

	@SuppressWarnings("unused") //parameter used in override
	public float getSlotFillPercentage(int slot) {
		return 0; //only used in limited barrels
	}

	public void setShouldBeOpen(boolean shouldBeOpen) {
		//noop by default
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		removeFromController();
		if (shouldDropContents()) {
			dropContents();
		}
	}

	@Override
	public void setChanged() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			super.setChanged();
		}
	}
}
