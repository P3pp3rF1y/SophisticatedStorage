package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
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
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerStorageKey;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.network.StorageOpennessPayload;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nullable;

import java.util.*;

public abstract class StorageBlockEntity extends BlockEntity
		implements
			IControllableStorage,
			ILinkable,
			ILockable,
			Nameable,
			ITierDisplay,
			IUpgradeDisplay,
			Clearable,
			ILinkedStorageBlockEndpoint {
	public static final String STORAGE_WRAPPER = "storageWrapper";
	public static final String UPDATE_BLOCK_RENDER_TAG = "updateBlockRender";
	private static final String LINKED_STORAGE_ENDPOINT = "linkedStorageEndpoint";
	private static final String LINKED_STORAGE_PRIMARY = "linkedStoragePrimary";
	private static final StorageLinkedStorageEndpointAdapter LINKED_STORAGE_ENDPOINT_ADAPTER = new StorageLinkedStorageEndpointAdapter();
	private final StorageWrapper storageWrapper;
	@Nullable
	protected Component displayName = null;

	private boolean updateBlockRender = true;
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private boolean isBeingUpgraded = false;
	@Nullable
	private LinkedStorageEndpointData linkedStorageEndpoint;
	@Nullable
	private StorageLinkedStorageHostWrapper linkedStorageHost;
	private boolean refreshingLinkedStorageState = false;
	private Runnable linkedStorageSubscription = () -> {
	};
	private Runnable linkedStorageRootContentsReplacementSubscription = () -> {
	};

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
		storageWrapper = new StorageWrapper(() -> this::setChanged, () -> this::markStorageContentsDirty, () -> {
			if (level != null && !level.isClientSide()) {
				WorldHelper.notifyBlockUpdate(this);
			}
		}, this::markStorageContentsDirty, this instanceof BarrelBlockEntity ? 4 : 1, this instanceof ICountDisplay || this instanceof IFillLevelDisplay) {

			@Override
			public Optional<UUID> getContentsUuid() {
				if (linkedStorageEndpoint != null) {
					return Optional.of(linkedStorageEndpoint.groupId());
				}
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
					storageBlock.setTicking(level, getBlockPos(), getBlockState(),
							!storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
				}
			}

			@Override
			protected boolean canDeselectDisplayItems() {
				return !(StorageBlockEntity.this instanceof LimitedBarrelBlockEntity);
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
				return linkedStorageHost == null ? StorageBlockEntity.this.isAllowedInStorage(resource) : linkedStorageHost.isAllowedInStorage(resource);
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
				return getBlockState().getBlock() instanceof IStorageBlock storageBlock
						? storageBlock.getBaseStackSizeMultiplier()
						: super.getBaseStackSizeMultiplier();
			}

			@Override
			public String getStorageType() {
				return StorageBlockEntity.this.getStorageType();
			}

			@Override
			public Component getDisplayName() {
				return StorageBlockEntity.this.getMenuDisplayName();
			}

			@Override
			protected boolean emptyInventorySlotsAcceptItems() {
				return !locked || allowsEmptySlotsMatchingItemInsertsWhenLocked();
			}

			@Override
			public ITrackedContentsItemResourceHandler getInventoryForInputOutput() {
				if (locked) {
					if (contentsFilteredItemHandler == null) {
						contentsFilteredItemHandler = new ContentsFilteredItemHandler(super::getInventoryForInputOutput,
								() -> getStorageWrapper().getInventoryHandler().getSlotTracker(),
								() -> getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
								!allowsEmptySlotsMatchingItemInsertsWhenLocked());
					}
					return contentsFilteredItemHandler;
				}

				return super.getInventoryForInputOutput();
			}
		};
		storageWrapper.getRenderDataHandler().setRenderUpdateChangeListener(renderDataHandler -> {
			setUpdateBlockRender();
			if (level != null && !level.isClientSide()) {
				WorldHelper.notifyBlockUpdate(this);
			}
		});
		storageWrapper.setUpgradeCachesInvalidatedHandler(this::onUpgradeCachesInvalidated);
	}

	protected boolean canRefreshUpgrades() {
		return !isDroppingContents && level != null && !level.isClientSide();
	}

	private void markStorageContentsDirty() {
		setChanged();
		if (!refreshingLinkedStorageState && linkedStorageEndpoint != null && level instanceof ServerLevel serverLevel) {
			linkedStorageHost.onLinkedStorageContentsChanged();
			LinkedStorageGroupsSavedData.get(serverLevel).manager().resolveContents(linkedStorageEndpoint.groupId())
					.ifPresent(ILinkedStorageContents::markDirty);
		}
		if (level != null && !level.isClientSide()) {
			WorldHelper.notifyBlockUpdate(this);
		}
	}

	@SuppressWarnings("java:S1172") // parameter used in override
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
		if (linkedStorageEndpoint == null) {
			out.putChild(STORAGE_WRAPPER, storageWrapper);
		} else {
			storageWrapper.saveEndpointData(out.child(STORAGE_WRAPPER));
		}
	}

	private void saveStorageWrapperClientData(ValueOutput out) {
		if (linkedStorageEndpoint == null) {
			storageWrapper.saveClientData(out.child(STORAGE_WRAPPER));
		} else {
			storageWrapper.saveEndpointClientData(out.child(STORAGE_WRAPPER),
					linkedStorageHost != null ? linkedStorageHost.getRenderDataHandler().getData() : storageWrapper.getRenderDataHandler().getData());
		}
	}

	protected void saveSynchronizedData(ValueOutput out) {
		if (linkedStorageEndpoint != null) {
			out.store(LINKED_STORAGE_ENDPOINT, LinkedStorageEndpointData.CODEC, linkedStorageEndpoint);
			storageWrapper.getLinkedStorageEndpointRole().ifPresent(role -> out.putBoolean(LINKED_STORAGE_PRIMARY, role == LinkedStorageEndpointRole.PRIMARY));
		}
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
		getOpenersCounter().incrementOpeners(containerUser.getLivingEntity(), level, getBlockPos(), getBlockState(),
				containerUser.getContainerInteractionRange());
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
			PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos,
					new StorageOpennessPayload(getBlockPos(), getOpenersCounter().getOpenerCount() > 0));
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
		loadSynchronizedData(in);
		loadStorageWrapper(in);

		isLinkedToController = in.getBooleanOr("isLinkedToController", false);
	}

	private void loadStorageWrapper(ValueInput in) {
		in.child(STORAGE_WRAPPER).ifPresent(wrapperData -> {
			if (linkedStorageEndpoint == null) {
				storageWrapper.deserialize(wrapperData);
			} else {
				storageWrapper.loadEndpointData(wrapperData);
			}
		});
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level instanceof ServerLevel serverLevel && linkedStorageEndpoint != null) {
			if (!bindLinkedStorage(serverLevel, linkedStorageEndpoint)) {
				clearLinkedStorage();
			}
		}
		storageWrapper.onInit(level);
		registerWithControllerOnLoad();
	}

	public void loadSynchronizedData(ValueInput in) {
		linkedStorageEndpoint = in.read(LINKED_STORAGE_ENDPOINT, LinkedStorageEndpointData.CODEC).orElse(null);
		storageWrapper.setLinkedStorageEndpoint(linkedStorageEndpoint,
				linkedStorageEndpoint == null
						? null
						: in.getBooleanOr(LINKED_STORAGE_PRIMARY, false) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY);
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
		if (level instanceof ServerLevel serverLevel) {
			StorageLinkedStorageJukeboxPlaybackAnchors.removeBlockAnchor(serverLevel, this);
		}
		closeLinkedStorageSubscription();
	}

	@Override
	public void setRemoved() {
		if (!isBeingUpgraded && !chunkBeingUnloaded && level != null) {
			removeFromController();
		}
		if (level instanceof ServerLevel serverLevel) {
			StorageLinkedStorageJukeboxPlaybackAnchors.removeBlockAnchor(serverLevel, this);
		}
		closeLinkedStorageSubscription();

		super.setRemoved();
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ValueInput in) {
		loadSynchronizedData(in);
		loadStorageWrapperClient(in);
	}

	public void setUpdateBlockRender() {
		updateBlockRender = true;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag(registries).merge(ValueIOHelper.collectOutputToTag(registries, out -> {
			updateBlockRender = true;
			saveStorageWrapperClientData(out);
			saveSynchronizedData(out);
		}));
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		loadSynchronizedData(input);
		loadStorageWrapperClient(input);
	}

	private void loadStorageWrapperClient(ValueInput in) {
		if (linkedStorageEndpoint == null) {
			storageWrapper.loadClientData(in.childOrEmpty(STORAGE_WRAPPER));
		} else {
			storageWrapper.loadEndpointClientData(in.childOrEmpty(STORAGE_WRAPPER));
		}
	}

	public static void serverTick(Level level, BlockPos blockPos, StorageBlockEntity storageBlockEntity) {
		if (storageBlockEntity.isSecondaryLinkedStorageEndpoint(level)) {
			return;
		}
		if (level instanceof ServerLevel serverLevel) {
			StorageLinkedStorageJukeboxPlaybackAnchors.refreshBlockAnchor(serverLevel, storageBlockEntity);
		}
		storageBlockEntity.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class)
				.forEach(upgrade -> upgrade.tick(null, level, blockPos));
	}

	@Override
	public StorageWrapper getStorageWrapper() {
		if (linkedStorageHost == null && linkedStorageEndpoint != null && level instanceof ServerLevel serverLevel) {
			bindLinkedStorage(serverLevel, linkedStorageEndpoint);
		}
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

	public Component getMenuDisplayName() {
		if (level instanceof ServerLevel serverLevel && linkedStorageEndpoint != null) {
			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
			if (manager.isEndpointMember(linkedStorageEndpoint.groupId(), linkedStorageEndpoint.endpointId())) {
				return manager.resolveVirtualHost(linkedStorageEndpoint.groupId()).flatMap(ILinkedStorageVirtualHost::getLinkedStorageDisplayName)
						.filter(name -> !name.getString().isEmpty()).orElseGet(this::getDisplayName);
			}
		}
		return getDisplayName();
	}

	@SuppressWarnings("unused") // resource param used in override
	protected boolean isAllowedInStorage(ItemResource resource) {
		return true;
	}

	public void changeStorageSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		int currentInventorySlots = getStorageWrapper().getInventoryHandler().size();
		getStorageWrapper().changeSize(additionalInventorySlots, additionalUpgradeSlots);
		changeSlots(currentInventorySlots + additionalInventorySlots);
		invalidateCapabilitiesAndControllerCache();
	}

	public boolean canUpgradeStorageTier(ServerLevel serverLevel) {
		return linkedStorageEndpoint == null || LinkedStorageGroupsSavedData.get(serverLevel).manager().isPrimaryEndpoint(linkedStorageEndpoint.groupId(),
				linkedStorageEndpoint.endpointId());
	}

	public boolean completePrimaryLinkedStorageTierUpgrade(ServerLevel serverLevel, int inventorySlots, int upgradeSlots) {
		if (linkedStorageEndpoint == null || !canUpgradeStorageTier(serverLevel)) {
			return false;
		}
		if (!bindLinkedStorage(serverLevel, linkedStorageEndpoint) || linkedStorageHost == null) {
			return false;
		}

		linkedStorageHost.changeSize(inventorySlots - linkedStorageHost.getInventoryHandler().size(),
				upgradeSlots - linkedStorageHost.getUpgradeHandler().size());
		storageWrapper.changeSize(inventorySlots - storageWrapper.getInventoryHandler().size(), upgradeSlots - storageWrapper.getUpgradeHandler().size());
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
		manager.resolveContents(linkedStorageEndpoint.groupId()).orElseThrow().setContents(linkedStorageEndpoint.groupId(),
				linkedStorageHost.copyContentsForLinkedStorage());
		manager.updatePrimaryHostDescriptor(linkedStorageEndpoint.groupId(), linkedStorageEndpoint.endpointId(),
				new LinkedStorageHostDescriptor(StorageLinkedStorageHostWrapper.FACTORY_ID, StorageLinkedStorageHostWrapper.createVirtualCarrier(this)));
		changeSlots(inventorySlots);
		invalidateCapabilitiesAndControllerCache();
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
		return true;
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

	@Override
	public void clearContent() {
		for (int slot = 0; slot < storageWrapper.getInventoryHandler().size(); slot++) {
			storageWrapper.getInventoryHandler().setStackInSlot(slot, ItemStack.EMPTY);
		}
		for (int slot = 0; slot < storageWrapper.getUpgradeHandler().size(); slot++) {
			storageWrapper.getUpgradeHandler().setStackInSlot(slot, ItemStack.EMPTY);
		}
		setChanged();
		invalidateCapabilitiesAndControllerCache();
	}

	public void setCustomName(@Nullable Component customName) {
		displayName = customName;
		if (level instanceof ServerLevel serverLevel && linkedStorageEndpoint != null) {
			StorageLinkedStorageEndpointAdapter.synchronizePrimaryCarrier(serverLevel, linkedStorageEndpoint, customName);
		}
		setChanged();
	}

	@Nullable
	public ResourceHandler<ItemResource> getExternalItemHandler(@Nullable Direction side) {
		return getStorageWrapper().getInventoryForInputOutput();
	}

	public boolean shouldDropContents() {
		return linkedStorageEndpoint == null;
	}

	@Nullable
	@Override
	public LinkedStorageEndpointData getLinkedStorageEndpointData() {
		return linkedStorageEndpoint;
	}

	@Override
	public ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> getLinkedStorageBlockEndpointAdapter() {
		return LINKED_STORAGE_ENDPOINT_ADAPTER;
	}

	@Override
	public boolean isLinkedStorageLinkCandidate() {
		return linkedStorageEndpoint != null || isLinkedStorageCandidate();
	}

	public boolean isLinkedStorageCandidate() {
		if (isLinked() || this instanceof WoodStorageBlockEntity woodStorage && woodStorage.isPacked()) {
			return false;
		}
		return this instanceof BarrelBlockEntity || this instanceof LimitedBarrelBlockEntity || this instanceof ShulkerBoxBlockEntity
				|| this instanceof ChestBlockEntity chest && chest.isMainChest();
	}

	public boolean isLinkedStorage() {
		return linkedStorageEndpoint != null;
	}

	public void writeLinkedStorageMenuData(FriendlyByteBuf buffer) {
		if (!(level instanceof ServerLevel serverLevel) || linkedStorageEndpoint == null || linkedStorageHost == null) {
			buffer.writeBoolean(false);
			return;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
		Optional<ILinkedStorageContents> contents = manager.resolveContents(linkedStorageEndpoint.groupId());
		Optional<CompoundTag> virtualCarrier = linkedStorageHost.getVirtualCarrierSnapshot();
		if (contents.isEmpty() || virtualCarrier.isEmpty()) {
			buffer.writeBoolean(false);
			return;
		}

		buffer.writeBoolean(true);
		buffer.writeUUID(linkedStorageEndpoint.groupId());
		buffer.writeUUID(linkedStorageEndpoint.endpointId());
		buffer.writeBoolean(storageWrapper.getLinkedStorageEndpointRole().orElse(LinkedStorageEndpointRole.SECONDARY) == LinkedStorageEndpointRole.PRIMARY);
		buffer.writeVarLong(manager.getRevision(linkedStorageEndpoint.groupId()));
		ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, linkedStorageHost.getDisplayName());
		FriendlyByteBuf.writeNbt(buffer, (CompoundTag) ContainerContents.CODEC.encodeStart(NbtOps.INSTANCE, contents.get().contents()).getOrThrow());
		buffer.writeVarInt(linkedStorageHost.getInventoryHandler().size());
		buffer.writeVarInt(linkedStorageHost.getUpgradeHandler().size());
		buffer.writeVarInt(linkedStorageHost.getColumnsTaken());
		FriendlyByteBuf.writeNbt(buffer, virtualCarrier.get());
	}

	public void bindClientLinkedStorage(LinkedStorageEndpointData endpoint, LinkedStorageEndpointRole role, ILinkedStorageContents contents,
			CompoundTag virtualCarrier) {
		closeLinkedStorageSubscription();
		linkedStorageEndpoint = endpoint;
		StorageLinkedStorageHostWrapper.applyClientSnapshotProfile(virtualCarrier, ClientLinkedStorageContents.getGroupName(endpoint.groupId()).orElseThrow(),
				ClientLinkedStorageContents.getInventorySlots(endpoint.groupId()).orElseThrow(),
				ClientLinkedStorageContents.getUpgradeSlots(endpoint.groupId()).orElseThrow());
		linkedStorageHost = StorageLinkedStorageHostWrapper.create(contents, virtualCarrier);
		storageWrapper.setLinkedStorageEndpoint(endpoint, role);
		refreshLinkedStorageState();
	}

	public void updateClientLinkedStorageContents(UUID groupId) {
		if (linkedStorageEndpoint != null && linkedStorageEndpoint.groupId().equals(groupId) && linkedStorageHost != null) {
			linkedStorageHost.onLinkedStorageContentsChanged();
		}
	}

	public void syncLinkedStorageContentsToPlayer(ServerPlayer player) {
		if (linkedStorageEndpoint != null) {
			PacketDistributor.sendToPlayer(player, LinkedStorageContentsPayload.createSnapshot(player.level(), linkedStorageEndpoint.groupId()));
		}
	}

	public static Optional<LinkedStorageEndpointData> getLinkedStorageEndpointData(ItemStack stack) {
		return Optional.ofNullable(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
	}

	public static boolean hasLinkedStorageEndpoint(ItemStack stack) {
		return getLinkedStorageEndpointData(stack).isPresent();
	}

	public static Optional<LinkedStorageEndpointRole> getLinkedStorageEndpointRole(ItemStack stack) {
		return LinkedStorageStackLifecycle.getEndpointRole(stack);
	}

	public void copyLinkedStorageEndpointTo(ItemStack stack) {
		if (linkedStorageEndpoint == null) {
			return;
		}
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, linkedStorageEndpoint);
		if (storageWrapper.getLinkedStorageEndpointRole().orElse(null) == LinkedStorageEndpointRole.PRIMARY) {
			stack.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, true);
		} else {
			stack.remove(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT);
		}
		stack.remove(ModCoreDataComponents.STORAGE_UUID);
	}

	public boolean restoreLinkedStorageEndpoint(ServerLevel serverLevel, ItemStack stack) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null || !LinkedStorageGroupsSavedData.get(serverLevel).manager().isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
			clearLinkedStorage();
			return false;
		}
		bindLinkedStorage(serverLevel, endpoint);
		if (endpoint.equals(linkedStorageEndpoint)) {
			reregisterWithController();
			return true;
		}
		return false;
	}

	public boolean restoreCreativeLinkedStorageEndpoint(ServerLevel serverLevel, ItemStack stack) {
		LinkedStorageEndpointData sourceEndpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (sourceEndpoint == null
				|| !LinkedStorageGroupsSavedData.get(serverLevel).manager().isEndpointMember(sourceEndpoint.groupId(), sourceEndpoint.endpointId())) {
			return false;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
		LinkedStorageEndpointData endpoint = manager.createSecondaryEndpoint(sourceEndpoint).orElseThrow();
		if (bindLinkedStorage(serverLevel, endpoint)) {
			reregisterWithController();
			return true;
		} else {
			manager.unregisterEndpoint(endpoint.groupId(), endpoint.endpointId());
			return false;
		}
	}

	boolean transferLinkedStorageEndpointTo(StorageBlockEntity target) {
		if (!(level instanceof ServerLevel serverLevel) || target.getLevel() != serverLevel || linkedStorageEndpoint == null) {
			return false;
		}

		LinkedStorageEndpointData endpoint = linkedStorageEndpoint;
		@Nullable
		Component customName = getCustomName();
		closeMenusForThisBlock();
		clearLinkedStorage();
		if (!target.bindLinkedStorage(serverLevel, endpoint)) {
			return false;
		}

		target.setCustomName(customName);
		return true;
	}

	boolean bindLinkedStorage(ServerLevel serverLevel, LinkedStorageEndpointData endpoint) {
		if (!isLinkedStorageCandidate()) {
			return false;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
		if (!manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
			return false;
		}
		Optional<StorageLinkedStorageHostWrapper> host = manager.resolveVirtualHost(endpoint, false).filter(StorageLinkedStorageHostWrapper.class::isInstance)
				.map(StorageLinkedStorageHostWrapper.class::cast);
		if (host.isEmpty()) {
			return false;
		}
		closeLinkedStorageSubscription();
		closeMenusForThisBlock();
		linkedStorageEndpoint = endpoint;
		linkedStorageHost = host.get();
		storageWrapper.setLinkedStorageEndpoint(endpoint,
				manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY);
		linkedStorageSubscription = manager.subscribeToGroupChanges(endpoint.groupId(), this::refreshLinkedStorageState);
		linkedStorageRootContentsReplacementSubscription = manager.subscribeToRootContentsReplacements(endpoint.groupId(), this::closeMenusForThisBlock);
		refreshLinkedStorageState();
		return true;
	}

	void onLinkedStorageEndpointLinked() {
		setChanged();
		reregisterWithController();
		refreshLinkedStorageState();
	}

	private void clearLinkedStorage() {
		closeLinkedStorageSubscription();
		linkedStorageEndpoint = null;
		linkedStorageHost = null;
		storageWrapper.setLinkedStorageEndpoint(null, null);
		storageWrapper.clearCanonicalStorageData();
	}

	private void refreshLinkedStorageState() {
		refreshingLinkedStorageState = true;
		try {
			contentsFilteredItemHandler = null;
			if (linkedStorageHost != null) {
				storageWrapper.replaceLinkedStorageContents(linkedStorageHost.getContents(), linkedStorageHost.getInventoryHandler().size(),
						linkedStorageHost.getUpgradeHandler().size(), linkedStorageHost.getColumnsTaken());
				storageWrapper.getRenderDataHandler().reloadFrom(linkedStorageHost.getRenderDataHandler().getData());
			}
			setUpdateBlockRender();
			if (level != null && !level.isClientSide() && getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
				storageBlock.setTicking(level, getBlockPos(), getBlockState(), !isSecondaryLinkedStorageEndpoint(level)
						&& !storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
			}
			setChanged();
			if (level != null && !level.isClientSide()) {
				WorldHelper.notifyBlockUpdate(this);
			}
		} finally {
			refreshingLinkedStorageState = false;
		}
	}

	boolean isSecondaryLinkedStorageEndpoint(Level level) {
		return storageWrapper.getLinkedStorageEndpointRole().map(role -> role == LinkedStorageEndpointRole.SECONDARY)
				.orElseGet(() -> linkedStorageEndpoint != null && level instanceof ServerLevel serverLevel && !LinkedStorageGroupsSavedData.get(serverLevel)
						.manager().isPrimaryEndpoint(linkedStorageEndpoint.groupId(), linkedStorageEndpoint.endpointId()));
	}

	private void closeLinkedStorageSubscription() {
		linkedStorageSubscription.run();
		linkedStorageSubscription = () -> {
		};
		linkedStorageRootContentsReplacementSubscription.run();
		linkedStorageRootContentsReplacementSubscription = () -> {
		};
	}

	public void closeMenusForThisBlock() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		serverLevel.players().forEach(player -> {
			if ((player.containerMenu instanceof StorageContainerMenu storageMenu && storageMenu.getStorageBlockEntity() == this)
					|| (player.containerMenu instanceof StorageSettingsContainerMenu settingsMenu && settingsMenu.getBlockPosition().equals(worldPosition))) {
				player.closeContainer();
			}
		});
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
	public ControllerStorageKey getControllerStorageKey() {
		return linkedStorageEndpoint == null
				? IControllableStorage.super.getControllerStorageKey()
				: new ControllerStorageKey(getControlledStorageBlockPos(), linkedStorageEndpoint.groupId());
	}

	@Override
	public void onInventoryInputOutputHandlerRefresh() {
		if (linkedStorageEndpoint == null || level == null || level.isClientSide()) {
			IControllableStorage.super.onInventoryInputOutputHandlerRefresh();
			return;
		}

		getControllerPos().flatMap(controllerPos -> WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class))
				.ifPresent(controller -> controller.rebindStorage(getStorageBlockPos()));
	}

	@Override
	public Level getStorageBlockLevel() {
		return Objects.requireNonNull(getLevel());
	}

	@Override
	public void linkToController(BlockPos controllerPos) {
		if (isLinkedStorage()) {
			return;
		}
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
			getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0,
					getStorageWrapper().getInventoryHandler().size());
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
		if (storageWrapper.isUpgradeHandlerInitializing()) {
			return;
		}
		storageWrapper.getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class)
				.forEach(upgrade -> upgrade.onNeighborChange(level, worldPosition, direction));
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

	@SuppressWarnings("unused") // parameter used in override
	public float getSlotFillPercentage(int slot) {
		return 0; // only used in limited barrels
	}

	public void setShouldBeOpen(boolean shouldBeOpen) {
		// noop by default
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
