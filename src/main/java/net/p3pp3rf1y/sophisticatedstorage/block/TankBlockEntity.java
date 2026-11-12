package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TankBlockEntity extends StorageBlockEntity implements INativeControllerFluidStorage {
	private static final String PRIMARY_POS_TAG = "tankPrimary";
	private static final String HEIGHT_TAG = "tankHeight";
	private static final String PACKED_TAG = "tankPacked";
	private static final String CONTENTS_TAG = "tankContents";
	private static final String SHARED_CONTENTS_TAG = "tankShared";
	private static final String CARRIER_TAG = "tankCarrier";
	private static final String REVISION_TAG = "tankRevision";
	private static final String PRIMARY_ENDPOINT_TAG = "tankPrimaryEndpoint";
	private static final TankLinkedStorageEndpointAdapter ENDPOINT_ADAPTER = new TankLinkedStorageEndpointAdapter();
	@Nullable
	private BlockPos primaryPos;
	private int height = 1;
	private boolean packed;
	private boolean suppressRemoval;
	private boolean creativePlayerRemoval;
	private boolean unloaded;
	private final SophisticatedOpenersCounter openers = new SophisticatedOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
		}
		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
		}
		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int after) {
		}
		@Override
		protected boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof TankContainerMenu menu && menu.getStorageBlockEntity() == TankBlockEntity.this;
		}
	};

	public TankBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.TANK_BLOCK_ENTITY_TYPE.get());
	}

	@Override
	protected StorageWrapper createStorageWrapper() {
		return new TankStorageWrapper(this::setChanged, this::synchronize, this::getTier, () -> height, this::isLocked) {
			@Override
			protected void onUpgradeRefresh() {
				if (canRefreshUpgrades() && isPrimary()) {
					synchronize();
				}
			}
			@Override
			public ItemStack getWrappedStorageStack() {
				return new ItemStack(getBlockState().getBlock());
			}
			@Override
			public String getStorageType() {
				return STORAGE_TYPE;
			}
			@Override
			public Component getDisplayName() {
				return TankBlockEntity.this.getDisplayName();
			}
		};
	}

	public TankTier getTier() {
		return ((TankBlock) getBlockState().getBlock()).getTier();
	}
	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		if (isPrimary()) {
			updateLinkedCarrier();
		}
	}
	@Override
	public boolean isLinkedStorageCandidate() {
		return isPrimary() && !packed;
	}
	@Override
	public ILinkedStorageBlockEndpoint getLinkedStorageInteractionTarget() {
		return getPrimary().orElse(this);
	}
	@Override
	public ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> getLinkedStorageBlockEndpointAdapter() {
		return ENDPOINT_ADAPTER;
	}
	public CompoundTag createTankCarrier() {
		CompoundTag carrier = TankLinkedStorageHostWrapper.carrier(getTier(), height, getDisplayName(), isLocked());
		carrier.put(StorageWrapper.RENDER_INFO_TAG, getStorageWrapper().getRenderInfo().getNbt().copy());
		return carrier;
	}
	private LinkedStorageHostDescriptor createLinkedStorageHostDescriptor() {
		return new LinkedStorageHostDescriptor(TankLinkedStorageHostWrapper.FACTORY_ID, createTankCarrier());
	}
	private void updateLinkedCarrier() {
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		if (level instanceof ServerLevel serverLevel && endpoint != null) {
			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
			if (manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(), createLinkedStorageHostDescriptor())) {
				manager.resolveVirtualHost(endpoint.groupId()).filter(TankLinkedStorageHostWrapper.class::isInstance)
						.map(TankLinkedStorageHostWrapper.class::cast)
						.ifPresent(host -> host.changeSize(0, getTier().upgradeSlots() - host.getUpgradeHandler().getSlots()));
			}
		}
	}
	@Override
	public boolean hasStorageData() {
		return isPrimary() && !packed;
	}
	@Override
	public BlockPos getControlledStorageBlockPos() {
		return primaryPos == null ? worldPosition : primaryPos;
	}
	@Override
	public Optional<IFluidHandler> getControllerFluidHandler() {
		return isPrimary() ? Optional.ofNullable(getExternalFluidHandler(null)) : Optional.empty();
	}
	public boolean isPrimary() {
		return primaryPos == null || primaryPos.equals(worldPosition);
	}
	public Optional<TankBlockEntity> getPrimary() {
		if (isPrimary()) {
			return Optional.of(this);
		}
		if (level == null || !level.hasChunkAt(primaryPos)) {
			return Optional.empty();
		}
		return WorldHelper.getBlockEntity(level, primaryPos, TankBlockEntity.class)
				.filter(primary -> primary.isPrimary() && primary.getTier() == getTier() && worldPosition.getX() == primary.worldPosition.getX()
						&& worldPosition.getZ() == primary.worldPosition.getZ() && worldPosition.getY() > primary.worldPosition.getY()
						&& worldPosition.getY() < primary.worldPosition.getY() + primary.height);
	}
	@Override
	public TankStorageWrapper getStorageWrapper() {
		return getPrimary().filter(primary -> primary != this).map(TankBlockEntity::getStorageWrapper).orElseGet(this::getLocalWrapper);
	}
	private TankStorageWrapper getLocalWrapper() {
		return (TankStorageWrapper) super.getStorageWrapper();
	}
	public int getHeight() {
		return getPrimary().map(primary -> primary.height).orElse(1);
	}
	public boolean isPacked() {
		return getPrimary().map(primary -> primary.packed).orElse(true);
	}
	public boolean canExtendWith(ItemStack stack) {
		return getMergedContents(stack).isPresent();
	}
	public void extendWith(ItemStack stack) {
		CompoundTag merged = getMergedContents(stack).orElseThrow();
		configureGroup(height + TankItemContents.height(stack));
		if (getLinkedStorageEndpointData() == null) {
			getLocalWrapper().applyPartition(merged);
		}
	}
	public void joinAbove() {
		if (level == null || !isPrimary()) {
			return;
		}
		WorldHelper.getBlockEntity(level, worldPosition.above(height), TankBlockEntity.class).filter(TankBlockEntity::isPrimary)
				.filter(upper -> !upper.isPacked() && upper.getBlockState().is(getBlockState().getBlock())).ifPresent(upper -> {
					ItemStack contents = upper.createPackedStack();
					if (canExtendWith(contents)) {
						extendWith(contents);
					}
				});
	}
	private Optional<CompoundTag> getMergedContents(ItemStack stack) {
		if (!isPrimary() || packed || height + TankItemContents.height(stack) > TankTier.MAX_HEIGHT || !stack.is(getBlockState().getBlock().asItem())
				|| hasLinkedStorageEndpoint(stack)) {
			return Optional.empty();
		}
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(stack.copyWithCount(1));
		FluidStack incomingFluid = incoming.getTankFluidHandler().getFluidInTank(0);
		List<ItemStack> incomingUpgrades = getUpgradeStacks(incoming);
		boolean incomingHasUpgrades = incomingUpgrades.stream().anyMatch(upgrade -> !upgrade.isEmpty());
		if (getLinkedStorageEndpointData() != null) {
			return incomingFluid.isEmpty() && !incomingHasUpgrades && !incoming.hasContainerItems() ? Optional.of(new CompoundTag()) : Optional.empty();
		}
		TankStorageWrapper storage = getLocalWrapper();
		if (storage.hasContainerItems() && incoming.hasContainerItems()) {
			return Optional.empty();
		}
		List<ItemStack> existingUpgrades = getUpgradeStacks(storage);
		if (incomingHasUpgrades && existingUpgrades.stream().anyMatch(upgrade -> !upgrade.isEmpty())) {
			return Optional.empty();
		}
		FluidStack existingFluid = storage.getTankFluidHandler().getFluidInTank(0);
		if (!existingFluid.isEmpty() && !incomingFluid.isEmpty() && !existingFluid.isFluidEqual(incomingFluid)) {
			return Optional.empty();
		}
		int combinedHeight = height + TankItemContents.height(stack);
		double multiplier = incomingHasUpgrades ? incoming.getStackMultiplier() : storage.getStackMultiplier();
		long amount = (long) existingFluid.getAmount() + incomingFluid.getAmount();
		if (amount > getTier().capacity(combinedHeight, multiplier)) {
			return Optional.empty();
		}
		FluidStack mergedFluid = new FluidStack(existingFluid.isEmpty() ? incomingFluid : existingFluid, (int) amount);
		CompoundTag merged = storage.partitionContents(mergedFluid, incomingHasUpgrades ? incomingUpgrades : existingUpgrades, true);
		if (incoming.hasContainerItems()) {
			merged.getCompound(StorageWrapper.CONTENTS_TAG).put(TankStorageWrapper.CONTAINERS_TAG, incoming.getContainerInventory().serializeNBT());
		}
		return Optional.of(merged);
	}
	private static List<ItemStack> getUpgradeStacks(TankStorageWrapper storage) {
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = 0; slot < storage.getUpgradeHandler().getSlots(); slot++) {
			upgrades.add(storage.getUpgradeHandler().getStackInSlot(slot).copy());
		}
		return upgrades;
	}
	@Override
	public boolean isLocked() {
		return isPrimary() ? super.isLocked() : getPrimary().map(TankBlockEntity::isLocked).orElse(true);
	}
	@Override
	public boolean shouldShowLock() {
		return isPrimary() ? super.shouldShowLock() : getPrimary().map(TankBlockEntity::shouldShowLock).orElse(false);
	}
	@Override
	public boolean shouldShowTier() {
		return isPrimary() ? super.shouldShowTier() : getPrimary().map(TankBlockEntity::shouldShowTier).orElse(false);
	}
	@Override
	public boolean shouldShowUpgrades() {
		return isPrimary() ? super.shouldShowUpgrades() : getPrimary().map(TankBlockEntity::shouldShowUpgrades).orElse(false);
	}
	@Override
	public void toggleLockVisibility() {
		if (isPrimary()) {
			super.toggleLockVisibility();
		} else {
			getPrimary().ifPresent(TankBlockEntity::toggleLockVisibility);
		}
	}
	@Override
	public void toggleTierVisiblity() {
		if (isPrimary()) {
			super.toggleTierVisiblity();
			updateGroupModelStates();
		} else {
			getPrimary().ifPresent(TankBlockEntity::toggleTierVisiblity);
		}
	}
	private void updateGroupModelStates() {
		if (level != null && !level.isClientSide) {
			for (int index = 0; index < height; index++) {
				WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class)
						.ifPresent(segment -> segment.setModelState(shouldShowTier(), packed));
			}
		}
	}
	private void setModelState(boolean showTier, boolean packed) {
		BlockState state = getBlockState().setValue(TankBlock.SHOW_TIER, showTier).setValue(TankBlock.PACKED, packed);
		if (level != null && !level.isClientSide && state != getBlockState()) {
			level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS);
		}
	}
	@Override
	public void toggleUpgradesVisiblity() {
		if (isPrimary()) {
			super.toggleUpgradesVisiblity();
		} else {
			getPrimary().ifPresent(TankBlockEntity::toggleUpgradesVisiblity);
		}
	}
	@Override
	public void toggleLock() {
		if (!isPrimary()) {
			getPrimary().ifPresent(TankBlockEntity::toggleLock);
			return;
		}
		super.toggleLock();
		updateLinkedCarrier();
	}
	public void pack() {
		getPrimary().ifPresent(primary -> {
			primary.packed = true;
			primary.updateGroupModelStates();
			primary.removeFromController();
			primary.invalidateGroup();
		});
	}
	public void configureGroup(int newHeight) {
		TankTier.validateHeight(newHeight);
		if (level == null) {
			throw new IllegalStateException("Cannot configure a tank before it has a level");
		}
		for (int index = 0; index < newHeight; index++) {
			TankBlockEntity segment = WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class).orElseThrow();
			segment.removeFromController();
			segment.primaryPos = index == 0 ? null : worldPosition;
			segment.height = index == 0 ? newHeight : 1;
			if (index > 0) {
				segment.getLocalWrapper().clearCanonicalStorageData();
			}
			level.setBlock(segment.worldPosition, segment.getBlockState().setValue(TankBlock.FACING, getBlockState().getValue(TankBlock.FACING))
					.setValue(TankBlock.FORM, TankForm.at(index, newHeight)).setValue(TankBlock.SHOW_TIER, shouldShowTier()).setValue(TankBlock.PACKED, packed),
					Block.UPDATE_CLIENTS);
			segment.invalidateStorageCap();
			segment.setChanged();
			segment.synchronize();
		}
		updateLinkedCarrier();
		reregisterWithController();
	}
	public boolean canChangeTier(TankTier target) {
		if (!isPrimary() || packed || level == null || level.isClientSide || isSecondaryLinkedStorageEndpoint(level)) {
			return false;
		}
		TankStorageWrapper wrapper = getStorageWrapper();
		if (wrapper.getTankFluidHandler().getFluidInTank(0).getAmount() > target.capacity(height, wrapper.getStackMultiplier())) {
			return false;
		}
		for (int slot = target.upgradeSlots(); slot < wrapper.getUpgradeHandler().getSlots(); slot++) {
			if (!wrapper.getUpgradeHandler().getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}
	public boolean changeTier(TankBlock target) {
		if (!canChangeTier(target.getTier())) {
			return false;
		}
		List<TankBlockEntity> segments = new ArrayList<>();
		List<CompoundTag> saved = new ArrayList<>();
		for (int index = 0; index < height; index++) {
			TankBlockEntity segment = WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class).orElse(null);
			if (segment == null || segment.getTier() != getTier()) {
				return false;
			}
			CompoundTag data = new CompoundTag();
			segment.saveAdditional(data);
			segments.add(segment);
			saved.add(data);
		}
		closeMenusForThisBlock();
		int physicalHeight = height;
		for (TankBlockEntity segment : segments) {
			segment.suppressTransactionalRemoval();
		}
		for (int index = 0; index < physicalHeight; index++) {
			BlockPos pos = worldPosition.above(index);
			BlockState state = target.defaultBlockState().setValue(TankBlock.FACING, segments.get(index).getBlockState().getValue(TankBlock.FACING))
					.setValue(TankBlock.FORM, TankForm.at(index, physicalHeight));
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).orElseThrow().load(saved.get(index));
		}
		finishTierChange(target, physicalHeight);
		return true;
	}
	private void finishTierChange(TankBlock target, int physicalHeight) {
		TankBlockEntity primary = WorldHelper.getBlockEntity(level, worldPosition, TankBlockEntity.class).orElseThrow();
		if (primary.getLinkedStorageEndpointData() != null && level instanceof ServerLevel serverLevel) {
			primary.restoreLinkedStorageEndpoint(serverLevel, createPackedStack());
		} else {
			primary.getLocalWrapper().changeSize(0, target.getTier().upgradeSlots() - primary.getLocalWrapper().getUpgradeHandler().getSlots());
		}
		primary.configureGroup(physicalHeight);
		for (int index = 0; index < physicalHeight; index++) {
			level.updateNeighborsAt(worldPosition.above(index), target);
		}
	}
	private void invalidateGroup() {
		if (level == null) {
			return;
		}
		for (int index = 0; index < height; index++) {
			WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class).ifPresent(segment -> {
				segment.invalidateStorageCap();
				segment.setChanged();
				segment.synchronize();
			});
		}
	}
	private void synchronize() {
		if (level != null && !level.isClientSide) {
			WorldHelper.notifyBlockUpdate(this);
		}
	}
	@Override
	@Nullable
	public IFluidHandler getExternalFluidHandler(@Nullable Direction side) {
		return getLiveFluidHandlers().isEmpty() ? null : new ControllerFluidHandler(this::getLiveFluidHandlers);
	}
	private List<IFluidHandler> getLiveFluidHandlers() {
		if (level == null || isRemoved() || unloaded || isPacked()) {
			return List.of();
		}
		return getPrimary().filter(primary -> !primary.isRemoved() && !primary.unloaded)
				.map(primary -> List.<IFluidHandler>of(new ContentsFilteredFluidHandler(primary.getStorageWrapper().getTankFluidHandler(), primary::isLocked)))
				.orElseGet(List::of);
	}
	@Override
	public void onLoad() {
		unloaded = false;
		super.onLoad();
		getPrimary().ifPresent(primary -> setModelState(primary.shouldShowTier(), primary.packed));
		updateFluidLighting();
	}
	@Override
	public void clearContent() {
		getStorageWrapper().getTankFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE, true);
		getStorageWrapper().removeContainerItems();
		super.clearContent();
	}
	private void updateFluidLighting() {
		if (level == null || !isPrimary() || isRemoved() || unloaded) {
			return;
		}
		for (int index = 0; index < height; index++) {
			BlockPos pos = worldPosition.above(index);
			if (level instanceof ServerLevel serverLevel) {
				WorldHelper.getBlockEntity(serverLevel, pos, TankBlockEntity.class).ifPresent(segment -> {
					BlockState state = serverLevel.getBlockState(pos);
					int emission = segment.getFluidLightEmission();
					if (state.getBlock() instanceof TankBlock && state.getValue(TankBlock.LIGHT_LEVEL) != emission) {
						serverLevel.setBlock(pos, state.setValue(TankBlock.LIGHT_LEVEL, emission), 3);
					}
				});
				serverLevel.getLightEngine().checkBlock(pos);
			}
		}
	}
	public int getFluidLightEmission() {
		TankBlockEntity primary = getPrimary().orElse(null);
		if (primary == null || primary.packed) {
			return 0;
		}
		TankStorageWrapper storage = primary.getStorageWrapper();
		FluidStack fluid = storage.getTankFluidHandler().getFluidInTank(0);
		if (fluid.isEmpty()) {
			return 0;
		}
		int segmentIndex = worldPosition.getY() - primary.worldPosition.getY();
		boolean occupied = TankFluidPartition.split(primary.height, segmentIndex, storage.getCapacity(), Math.min(storage.getCapacity(), fluid.getAmount()),
				fluid.getFluid().getFluidType().isLighterThanAir()).dropped() > 0;
		return occupied ? fluid.getFluid().getFluidType().getLightLevel(fluid) : 0;
	}
	public void suppressTransactionalRemoval() {
		suppressRemoval = true;
		setBeingUpgraded(true);
	}
	@Override
	public void onChunkUnloaded() {
		unloaded = true;
		super.onChunkUnloaded();
	}
	private Optional<TankSplitPlan> splitPlan(BlockPos brokenPos) {
		TankStorageWrapper wrapper = getLocalWrapper();
		FluidStack fluid = wrapper.getTankFluidHandler().getFluidInTank(0);
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = 0; slot < wrapper.getUpgradeHandler().getSlots(); slot++) {
			upgrades.add(wrapper.getUpgradeHandler().getStackInSlot(slot));
		}
		return TankSplitPlan.create(getTier(), height, brokenPos.getY() - worldPosition.getY(), wrapper.getCapacity(), fluid.getAmount(),
				!fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir(), upgrades);
	}
	public void onSegmentRemoved() {
		if (suppressRemoval || level == null || level.isClientSide || level.restoringBlockSnapshots) {
			return;
		}
		getPrimary().ifPresent(primary -> primary.removeSegment(worldPosition, creativePlayerRemoval));
	}
	void setCreativePlayerRemoval(boolean creativePlayerRemoval) {
		this.creativePlayerRemoval = creativePlayerRemoval;
	}
	private void removeSegment(BlockPos brokenPos, boolean creative) {
		closeMenusForThisBlock();
		if (packed || getLinkedStorageEndpointData() != null && (brokenPos.equals(worldPosition) || !isSecondaryLinkedStorageEndpoint(level))) {
			removeGroupWithDrop(brokenPos, creative);
			return;
		}
		if (getLinkedStorageEndpointData() != null) {
			removeLinkedSegment(brokenPos, creative);
			return;
		}
		Optional<TankSplitPlan> plan = splitPlan(brokenPos);
		if (plan.isEmpty()) {
			removeGroupWithDrop(brokenPos, creative);
			return;
		}
		splitUnlinkedGroup(brokenPos, creative, plan.get());
	}
	private void removeGroupWithDrop(BlockPos brokenPos, boolean creative) {
		if (creative && getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty()) {
			removeEmptyCreativeGroup(brokenPos);
			return;
		}
		ItemStack drop = createPackedStack();
		removeWholeGroup();
		Block.popResource(level, brokenPos, drop);
	}
	private void removeLinkedSegment(BlockPos brokenPos, boolean creative) {
		int index = brokenPos.getY() - worldPosition.getY();
		int upperHeight = height - index - 1;
		configureGroup(index);
		if (upperHeight > 0) {
			WorldHelper.getBlockEntity(level, brokenPos.above(), TankBlockEntity.class).orElseThrow().configureGroup(upperHeight);
		}
		if (!creative) {
			Block.popResource(level, brokenPos, new ItemStack(getBlockState().getBlock()));
		}
	}
	private void splitUnlinkedGroup(BlockPos brokenPos, boolean creative, TankSplitPlan plan) {
		int index = brokenPos.getY() - worldPosition.getY();
		int upperHeight = height - index - 1;
		TankStorageWrapper wrapper = getLocalWrapper();
		List<ItemStack> ejectedContainers = index == 0 ? wrapper.removeContainerItems() : List.of();
		FluidStack fluid = wrapper.getTankFluidHandler().getFluidInTank(0);
		CompoundTag lower = wrapper.partitionContents(new FluidStack(fluid, plan.fluid().lower()), plan.lowerUpgrades(), true);
		CompoundTag upper = wrapper.partitionContents(new FluidStack(fluid, plan.fluid().upper()), plan.upperUpgrades(), false);
		ItemStack drop = new ItemStack(getBlockState().getBlock());
		TankItemContents.write(drop, wrapper.partitionContents(new FluidStack(fluid, plan.fluid().dropped()), plan.droppedUpgrades(), false));
		if (index > 0) {
			configureGroup(index);
			wrapper.applyPartition(lower);
		}
		if (upperHeight > 0) {
			TankBlockEntity upperPrimary = WorldHelper.getBlockEntity(level, brokenPos.above(), TankBlockEntity.class).orElseThrow();
			upperPrimary.configureGroup(upperHeight);
			upperPrimary.getLocalWrapper().applyPartition(upper);
		}
		if (!creative || plan.fluid().dropped() > 0) {
			Block.popResource(level, brokenPos, drop);
		}
		plan.ejectedUpgrades().forEach(upgrade -> Block.popResource(level, brokenPos, upgrade));
		ejectedContainers.forEach(container -> Block.popResource(level, brokenPos, container));
	}
	private void removeEmptyCreativeGroup(BlockPos brokenPos) {
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		List<ItemStack> upgrades = new ArrayList<>();
		if (endpoint == null || !isSecondaryLinkedStorageEndpoint(level)) {
			TankStorageWrapper wrapper = getStorageWrapper();
			upgrades.addAll(wrapper.removeContainerItems());
			for (int slot = 0; slot < wrapper.getUpgradeHandler().getSlots(); slot++) {
				ItemStack upgrade = wrapper.getUpgradeHandler().getStackInSlot(slot);
				if (!upgrade.isEmpty()) {
					upgrades.add(upgrade.copy());
				}
			}
			if (endpoint != null) {
				wrapper.applyPartition(wrapper.partitionContents(FluidStack.EMPTY, List.of(), true));
			}
		}
		removeWholeGroup();
		if (endpoint != null && level instanceof ServerLevel serverLevel) {
			LinkedStorageGroupsSavedData.get(serverLevel).manager().discardEndpoint(endpoint.groupId(), endpoint.endpointId());
		}
		upgrades.forEach(upgrade -> Block.popResource(level, brokenPos, upgrade));
	}
	private void removeWholeGroup() {
		List<TankBlockEntity> segments = new ArrayList<>();
		for (int index = 0; index < height; index++) {
			WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class).ifPresent(segment -> {
				segment.suppressRemoval = true;
				segments.add(segment);
			});
		}
		try {
			for (TankBlockEntity segment : segments) {
				if (level.getBlockState(segment.worldPosition).getBlock() instanceof TankBlock) {
					level.removeBlock(segment.worldPosition, false);
				}
			}
		} finally {
			segments.forEach(segment -> segment.suppressRemoval = false);
		}
	}
	public ItemStack createPackedStack() {
		ItemStack stack = new ItemStack(getBlockState().getBlock());
		TankItemContents.height(stack, height);
		if (packed) {
			TankItemContents.setFlag(stack, "packed", true);
		}
		if (getLinkedStorageEndpointData() == null) {
			TankItemContents.write(stack, getLocalWrapper().saveCanonicalData(new CompoundTag()));
		} else {
			copyLinkedStorageEndpointTo(stack);
		}
		getLocalWrapper().getContentsUuid().ifPresent(uuid -> stack.getOrCreateTag().putUUID(StorageWrapper.UUID_TAG, uuid));
		if (getCustomName() != null) {
			stack.setHoverName(getDisplayName());
		}
		TankItemContents.setFlag(stack, "locked", isLocked());
		TankItemContents.setFlag(stack, "lockVisible", shouldShowLock());
		TankItemContents.setFlag(stack, "showsTier", shouldShowTier());
		TankItemContents.setFlag(stack, "upgradesVisible", shouldShowUpgrades());
		return stack;
	}
	public void restoreStack(ItemStack stack) {
		getLocalWrapper().applyPartition(TankItemContents.read(stack));
		NBTHelper.getUniqueId(stack, StorageWrapper.UUID_TAG).ifPresent(uuid -> getLocalWrapper().contentsUuid = uuid);
		if (stack.hasCustomHoverName()) {
			setCustomName(stack.getHoverName());
		}
		if (TankItemContents.flag(stack, "locked", false) != isLocked()) {
			toggleLock();
		}
		if (TankItemContents.flag(stack, "lockVisible", true) != shouldShowLock()) {
			toggleLockVisibility();
		}
		if (TankItemContents.flag(stack, "showsTier", true) != shouldShowTier()) {
			toggleTierVisiblity();
		}
		if (TankItemContents.flag(stack, "upgradesVisible", false) != shouldShowUpgrades()) {
			toggleUpgradesVisiblity();
		}
		packed = false;
		updateGroupModelStates();
		if (level instanceof ServerLevel serverLevel && hasLinkedStorageEndpoint(stack)) {
			restoreLinkedStorageEndpoint(serverLevel, stack);
			updateLinkedCarrier();
		}
	}
	public static void tick(Level level, BlockPos pos, TankBlockEntity tank) {
		if (tank.isPrimary() && !tank.packed && !tank.isSecondaryLinkedStorageEndpoint(level)) {
			tank.getStorageWrapper().tickContainers(level.getGameTime());
			for (ITickableUpgrade upgrade : tank.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class)) {
				upgrade.tick(null, level, pos);
			}
		}
		tank.updateFluidLighting();
	}
	@Override
	public boolean shouldDropContents() {
		return false;
	}
	@Override
	protected String getStorageType() {
		return TankStorageWrapper.STORAGE_TYPE;
	}
	@Override
	public SophisticatedOpenersCounter getOpenersCounter() {
		return openers;
	}
	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		writeStructure(tag);
		if (!isPrimary()) {
			tag.remove(STORAGE_WRAPPER_TAG);
		}
	}
	private void writeStructure(CompoundTag tag) {
		if (primaryPos != null) {
			tag.putLong(PRIMARY_POS_TAG, primaryPos.asLong());
		}
		tag.putInt(HEIGHT_TAG, height);
		tag.putBoolean(PACKED_TAG, packed);
	}
	@Override
	public void load(CompoundTag tag) {
		readStructure(tag);
		super.load(tag);
		if (tag.contains(CONTENTS_TAG)) {
			getLocalWrapper().loadCanonicalData(tag.getCompound(CONTENTS_TAG));
		}
		readLinkedSnapshot(tag);
	}
	private void readStructure(CompoundTag tag) {
		primaryPos = tag.contains(PRIMARY_POS_TAG) ? BlockPos.of(tag.getLong(PRIMARY_POS_TAG)) : null;
		height = tag.contains(HEIGHT_TAG) ? tag.getInt(HEIGHT_TAG) : 1;
		TankTier.validateHeight(height);
		packed = tag.getBoolean(PACKED_TAG);
	}
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		writeStructure(tag);
		if (isPrimary()) {
			LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
			if (endpoint != null && level instanceof ServerLevel serverLevel) {
				LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
				manager.resolveVirtualHost(endpoint, false).filter(TankLinkedStorageHostWrapper.class::isInstance).map(TankLinkedStorageHostWrapper.class::cast)
						.ifPresent(host -> {
							tag.put(SHARED_CONTENTS_TAG, host.saveCanonicalData(new CompoundTag()));
							tag.put(CARRIER_TAG, host.getVirtualCarrierSnapshot().orElseThrow());
							tag.putLong(REVISION_TAG, manager.getRevision(endpoint.groupId()));
							tag.putBoolean(PRIMARY_ENDPOINT_TAG, manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
						});
			} else {
				tag.put(CONTENTS_TAG, getLocalWrapper().saveCanonicalData(new CompoundTag()));
			}
		}
		return tag;
	}
	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
		super.onDataPacket(connection, packet);
		if (packet.getTag() == null) {
			return;
		}
		readStructure(packet.getTag());
		if (packet.getTag().contains(CONTENTS_TAG)) {
			getLocalWrapper().loadCanonicalData(packet.getTag().getCompound(CONTENTS_TAG));
		}
		readLinkedSnapshot(packet.getTag());
		updateFluidLighting();
	}
	private void readLinkedSnapshot(CompoundTag tag) {
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		if (level != null && level.isClientSide && endpoint != null && tag.contains(SHARED_CONTENTS_TAG)) {
			CompoundTag data = tag.getCompound(SHARED_CONTENTS_TAG);
			CompoundTag carrier = tag.getCompound(CARRIER_TAG);
			ClientLinkedStorageContents.updateContents(endpoint.groupId(), tag.getLong(REVISION_TAG), data,
					Component.literal(carrier.getString(TankLinkedStorageHostWrapper.DISPLAY_NAME_TAG)), 0,
					data.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG), 0);
			CompoundTag descriptor = new CompoundTag();
			descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, TankLinkedStorageHostWrapper.FACTORY_ID.toString());
			descriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG, carrier);
			ClientLinkedStorageContents.updateHostDescriptor(endpoint.groupId(), descriptor);
			bindClientLinkedStorage(endpoint, tag.getBoolean(PRIMARY_ENDPOINT_TAG) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY,
					ClientLinkedStorageContents.getContents(endpoint.groupId()).orElseThrow(), TankLinkedStorageHostWrapper.FACTORY_ID, carrier);
		}
	}
}
