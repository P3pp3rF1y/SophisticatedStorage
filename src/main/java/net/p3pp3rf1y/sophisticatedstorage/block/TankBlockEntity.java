package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TankBlockEntity extends StorageBlockEntity implements INativeControllerFluidStorage {
	private static final String PRIMARY_POS_TAG = "tankPrimary";
	private static final String HEIGHT_TAG = "tankHeight";
	private static final String PACKED_TAG = "tankPacked";
	private static final String CONTENTS_TAG = "tankContents";
	private static final String SHARED_CONTENTS_TAG = "tankSharedContents";
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
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof TankContainerMenu menu && menu.getStorageBlockEntity() == TankBlockEntity.this;
		}
	};

	public TankBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.TANK_BLOCK_ENTITY_TYPE.get());
	}

	TankStorageWrapper createTankStorageWrapper() {
		return new TankStorageWrapper(this::setChanged, this::synchronize, this::getTier, () -> height, () -> getStorageBlockLevel().registryAccess(),
				this::isLocked) {
			@Override
			protected void onUpgradeRefresh() {
				if (canRefreshUpgrades() && isPrimary()) {
					synchronize();
				}
			}
			@Override
			public String getStorageType() {
				return STORAGE_TYPE;
			}
			@Override
			public Component getDisplayName() {
				return TankBlockEntity.this.getDisplayName();
			}
			@Override
			public ItemStack getWrappedStorageStack() {
				return new ItemStack(getBlockState().getBlock());
			}
		};
	}

	public TankTier getTier() {
		return ((TankBlock) getBlockState().getBlock()).getTier();
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
		carrier.put("renderInfo", RenderData.CODEC.encodeStart(NbtOps.INSTANCE, getStorageWrapper().getRenderDataHandler().getData()).getOrThrow());
		return carrier;
	}

	private void updateLinkedCarrier() {
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		if (level instanceof ServerLevel server && endpoint != null) {
			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(server).manager();
			if (manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(),
					new LinkedStorageHostDescriptor(TankLinkedStorageHostWrapper.FACTORY_ID, createTankCarrier()))) {
				manager.resolveVirtualHost(endpoint.groupId()).filter(TankLinkedStorageHostWrapper.class::isInstance)
						.map(TankLinkedStorageHostWrapper.class::cast)
						.ifPresent(host -> host.changeSize(0, getTier().upgradeSlots() - host.getUpgradeHandler().size()));
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
	public Optional<IStorageFluidHandler> getControllerFluidHandler() {
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
		TankItemContents.Contents merged = getMergedContents(stack).orElseThrow();
		configureGroup(height + stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1));
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
	private Optional<TankItemContents.Contents> getMergedContents(ItemStack stack) {
		if (!isPrimary() || packed || height + stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1) > TankTier.MAX_HEIGHT
				|| !stack.is(getBlockState().getBlock().asItem()) || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) {
			return Optional.empty();
		}
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(stack.copyWithCount(1));
		FluidStack incomingFluid = incoming.getTankFluidHandler().getFluidInTank(0);
		List<ItemStack> incomingUpgrades = getUpgradeStacks(incoming);
		boolean incomingHasUpgrades = incomingUpgrades.stream().anyMatch(upgrade -> !upgrade.isEmpty());
		if (getLinkedStorageEndpointData() != null) {
			return incomingFluid.isEmpty() && !incomingHasUpgrades && !incoming.hasContainerItems()
					? Optional.of(getLocalWrapper().partitionContents(FluidStack.EMPTY, List.of(), true))
					: Optional.empty();
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
		if (!existingFluid.isEmpty() && !incomingFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(existingFluid, incomingFluid)) {
			return Optional.empty();
		}
		int combinedHeight = height + stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
		double multiplier = incomingHasUpgrades ? incoming.getStackMultiplier() : storage.getStackMultiplier();
		long amount = (long) existingFluid.getAmount() + incomingFluid.getAmount();
		if (amount > getTier().capacity(combinedHeight, multiplier)) {
			return Optional.empty();
		}
		FluidStack merged = (existingFluid.isEmpty() ? incomingFluid : existingFluid).copyWithAmount((int) amount);
		TankItemContents.Contents partition = storage.partitionContents(merged, incomingHasUpgrades ? incomingUpgrades : existingUpgrades, true);
		return Optional.of(incoming.hasContainerItems()
				? new TankItemContents.Contents(partition.fluid(), partition.upgrades(), TankItemContents.read(stack).containers(), partition.upgradeSlots(),
						partition.sortBy())
				: partition);
	}
	private static List<ItemStack> getUpgradeStacks(TankStorageWrapper storage) {
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = 0; slot < storage.getUpgradeHandler().size(); slot++) {
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
	private void updateGroupModelStates() {
		if (level != null && !level.isClientSide()) {
			for (int index = 0; index < height; index++) {
				WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class)
						.ifPresent(segment -> segment.setModelState(shouldShowTier(), packed));
			}
		}
	}
	private void setModelState(boolean showTier, boolean packed) {
		BlockState state = getBlockState().setValue(TankBlock.SHOW_TIER, showTier).setValue(TankBlock.PACKED, packed);
		if (level != null && !level.isClientSide() && state != getBlockState()) {
			level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS);
		}
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
			segment.invalidateCapabilities();
			segment.setChanged();
			segment.synchronize();
		}
		updateLinkedCarrier();
		reregisterWithController();
	}
	public boolean canChangeTier(TankTier target) {
		if (!isPrimary() || packed || level == null || level.isClientSide() || isSecondaryLinkedStorageEndpoint(level)) {
			return false;
		}
		TankStorageWrapper wrapper = getStorageWrapper();
		if (wrapper.getTankFluidHandler().getFluidInTank(0).getAmount() > target.capacity(height, wrapper.getStackMultiplier())) {
			return false;
		}
		for (int slot = target.upgradeSlots(); slot < wrapper.getUpgradeHandler().size(); slot++) {
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
			saved.add(segment.saveWithFullMetadata(level.registryAccess()));
			segments.add(segment);
		}
		ItemStack endpointStack = createPackedStack();
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
			WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).orElseThrow()
					.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.get(index)));
		}
		TankBlockEntity primary = WorldHelper.getBlockEntity(level, worldPosition, TankBlockEntity.class).orElseThrow();
		if (primary.getLinkedStorageEndpointData() != null && level instanceof ServerLevel server) {
			primary.restoreLinkedStorageEndpoint(server, endpointStack);
		} else {
			primary.getLocalWrapper().changeSize(0, target.getTier().upgradeSlots() - primary.getLocalWrapper().getUpgradeHandler().size());
		}
		primary.configureGroup(physicalHeight);
		for (int index = 0; index < physicalHeight; index++) {
			level.updateNeighborsAt(worldPosition.above(index), target);
		}
		return true;
	}
	private void invalidateGroup() {
		if (level == null) {
			return;
		}
		for (int index = 0; index < height; index++) {
			WorldHelper.getBlockEntity(level, worldPosition.above(index), TankBlockEntity.class).ifPresent(segment -> {
				segment.invalidateCapabilities();
				segment.setChanged();
				segment.synchronize();
			});
		}
	}
	private void synchronize() {
		if (level != null && !level.isClientSide()) {
			WorldHelper.notifyBlockUpdate(this);
		}
	}
	@Override
	public @Nullable IStorageFluidHandler getExternalFluidHandler(@Nullable Direction side) {
		return getLiveFluidHandlers().isEmpty() ? null : new ControllerFluidHandler(this::getLiveFluidHandlers);
	}
	private List<IStorageFluidHandler> getLiveFluidHandlers() {
		if (level == null || isRemoved() || unloaded || isPacked()) {
			return List.of();
		}
		return getPrimary().filter(primary -> !primary.isRemoved() && !primary.unloaded)
				.map(primary -> List
						.<IStorageFluidHandler>of(new ContentsFilteredFluidHandler(primary.getStorageWrapper().getTankFluidHandler(), primary::isLocked)))
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
		TankStorageWrapper wrapper = getStorageWrapper();
		try (Transaction tx = Transaction.openRoot()) {
			TankFluidHandler fluid = wrapper.getTankFluidHandler();
			fluid.extract(0, fluid.getResource(0), Integer.MAX_VALUE, tx, true);
			tx.commit();
		}
		wrapper.removeContainerItems();
		super.clearContent();
	}
	private void updateFluidLighting() {
		if (level == null || !isPrimary() || isRemoved() || unloaded) {
			return;
		}
		TankStorageWrapper storage = getStorageWrapper();
		FluidStack fluid = storage.getTankFluidHandler().getFluidInTank(0);
		int emission = packed || fluid.isEmpty() ? 0 : fluid.getFluidType().getLightLevel(fluid);
		int capacity = storage.getCapacity();
		boolean changed = false;
		for (int index = 0; index < height; index++) {
			BlockPos pos = worldPosition.above(index);
			AuxiliaryLightManager lights = level.getAuxLightManager(pos);
			if (lights != null) {
				boolean occupied = emission > 0 && TankFluidPartition
						.split(height, index, capacity, Math.min(capacity, fluid.getAmount()), fluid.getFluidType().isLighterThanAir()).dropped() > 0;
				int value = occupied ? emission : 0;
				if (lights.getLightAt(pos) != value) {
					lights.setLightAt(pos, value);
					changed = true;
				}
			}
		}
		if (changed) {
			synchronize();
		}
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
		return TankSplitPlan.create(getTier(), height, brokenPos.getY() - worldPosition.getY(), wrapper.getCapacity(), fluid.getAmount(),
				!fluid.isEmpty() && fluid.getFluidType().isLighterThanAir(), getUpgradeStacks(wrapper));
	}
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		onSegmentRemoved();
		super.preRemoveSideEffects(pos, state);
	}
	public void onSegmentRemoved() {
		if (suppressRemoval || level == null || level.isClientSide()) {
			return;
		}
		getPrimary().ifPresent(primary -> primary.removeSegment(worldPosition, creativePlayerRemoval));
	}
	void setCreativePlayerRemoval(boolean creative) {
		creativePlayerRemoval = creative;
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
		TankItemContents.Contents lower = wrapper.partitionContents(fluid.copyWithAmount(plan.fluid().lower()), plan.lowerUpgrades(), true);
		TankItemContents.Contents upper = wrapper.partitionContents(fluid.copyWithAmount(plan.fluid().upper()), plan.upperUpgrades(), false);
		ItemStack drop = new ItemStack(getBlockState().getBlock());
		TankItemContents.write(drop, wrapper.partitionContents(fluid.copyWithAmount(plan.fluid().dropped()), plan.droppedUpgrades(), false));
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
			for (ItemStack upgrade : getUpgradeStacks(wrapper)) {
				if (!upgrade.isEmpty()) {
					upgrades.add(upgrade);
				}
			}
			if (endpoint != null) {
				wrapper.applyPartition(wrapper.partitionContents(FluidStack.EMPTY, List.of(), true));
			}
		}
		removeWholeGroup();
		if (endpoint != null && level instanceof ServerLevel server) {
			LinkedStorageGroupsSavedData.get(server).manager().discardEndpoint(endpoint.groupId(), endpoint.endpointId());
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
		stack.set(ModDataComponents.TANK_HEIGHT, height);
		if (packed) {
			stack.set(ModDataComponents.PACKED, true);
		}
		if (getLinkedStorageEndpointData() == null) {
			TankItemContents.write(stack,
					getLocalWrapper().partitionContents(getLocalWrapper().getTankFluidHandler().getFluidInTank(0), getUpgradeStacks(getLocalWrapper()), true));
			getLocalWrapper().getContentsUuid().ifPresent(uuid -> stack.set(ModCoreDataComponents.STORAGE_UUID, uuid));
		} else {
			copyLinkedStorageEndpointTo(stack);
		}
		if (getCustomName() != null) {
			stack.set(DataComponents.CUSTOM_NAME, getDisplayName());
		}
		stack.set(ModDataComponents.LOCKED, isLocked());
		stack.set(ModDataComponents.LOCK_VISIBLE, shouldShowLock());
		stack.set(ModDataComponents.TIER_VISIBLE, shouldShowTier());
		stack.set(ModDataComponents.UPGRADES_VISIBLE, shouldShowUpgrades());
		return stack;
	}
	public void restoreStack(ItemStack stack) {
		if (!hasLinkedStorageEndpoint(stack)) {
			getLocalWrapper().applyPartition(TankItemContents.read(stack));
			if (stack.has(ModCoreDataComponents.STORAGE_UUID)) {
				getLocalWrapper().setContentsUuid(stack.get(ModCoreDataComponents.STORAGE_UUID));
			}
		}
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			setCustomName(stack.get(DataComponents.CUSTOM_NAME));
		}
		if (stack.getOrDefault(ModDataComponents.LOCKED, false) != isLocked()) {
			toggleLock();
		}
		if (stack.has(ModDataComponents.LOCK_VISIBLE) && stack.get(ModDataComponents.LOCK_VISIBLE) != shouldShowLock()) {
			toggleLockVisibility();
		}
		if (stack.has(ModDataComponents.TIER_VISIBLE) && stack.get(ModDataComponents.TIER_VISIBLE) != shouldShowTier()) {
			toggleTierVisiblity();
		}
		if (stack.has(ModDataComponents.UPGRADES_VISIBLE) && stack.get(ModDataComponents.UPGRADES_VISIBLE) != shouldShowUpgrades()) {
			toggleUpgradesVisiblity();
		}
		if (level instanceof ServerLevel server && hasLinkedStorageEndpoint(stack)) {
			restoreLinkedStorageEndpoint(server, stack);
			updateLinkedCarrier();
		}
		if (stack.getOrDefault(ModDataComponents.PACKED, false)) {
			pack();
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
	public void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		writeStructure(out);
	}
	private void writeStructure(ValueOutput out) {
		if (primaryPos != null) {
			out.putLong(PRIMARY_POS_TAG, primaryPos.asLong());
		}
		out.putInt(HEIGHT_TAG, height);
		out.putBoolean(PACKED_TAG, packed);
	}
	@Override
	public void loadAdditional(ValueInput in) {
		readStructure(in);
		super.loadAdditional(in);
	}
	private void readStructure(ValueInput in) {
		primaryPos = in.getLong(PRIMARY_POS_TAG).isPresent() ? BlockPos.of(in.getLongOr(PRIMARY_POS_TAG, 0)) : null;
		height = in.getIntOr(HEIGHT_TAG, 1);
		TankTier.validateHeight(height);
		packed = in.getBooleanOr(PACKED_TAG, false);
	}
	@Override
	protected void saveSynchronizedData(ValueOutput out) {
		super.saveSynchronizedData(out);
		writeStructure(out);
	}
	@Override
	public void loadSynchronizedData(ValueInput in) {
		readStructure(in);
		super.loadSynchronizedData(in);
		updateFluidLighting();
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		if (!isPrimary()) {
			return tag;
		}
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		if (endpoint != null && level instanceof ServerLevel server) {
			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(server).manager();
			manager.resolveVirtualHost(endpoint.groupId()).filter(TankLinkedStorageHostWrapper.class::isInstance).map(TankLinkedStorageHostWrapper.class::cast)
					.ifPresent(host -> {
						tag.merge(ValueIOHelper.collectOutputToTag(registries,
								out -> out.store(SHARED_CONTENTS_TAG, ContainerContents.CODEC, host.getContents().copy())));
						tag.put(CARRIER_TAG, host.getVirtualCarrierSnapshot().orElseThrow());
						tag.putLong(REVISION_TAG, manager.getRevision(endpoint.groupId()));
						tag.putBoolean(PRIMARY_ENDPOINT_TAG, manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
					});
		} else {
			tag.merge(ValueIOHelper.collectOutputToTag(registries,
					out -> out.store(CONTENTS_TAG, ContainerContents.CODEC, getLocalWrapper().getContents().copy())));
		}
		return tag;
	}

	@Override
	public void onDataPacket(Connection connection, ValueInput in) {
		super.onDataPacket(connection, in);
		readClientSnapshot(in);
	}

	@Override
	public void handleUpdateTag(ValueInput in) {
		super.handleUpdateTag(in);
		readClientSnapshot(in);
	}

	private void readClientSnapshot(ValueInput in) {
		if (level == null || !level.isClientSide() || !isPrimary()) {
			return;
		}
		in.read(CONTENTS_TAG, ContainerContents.CODEC).ifPresent(getLocalWrapper()::replaceContents);
		LinkedStorageEndpointData endpoint = getLinkedStorageEndpointData();
		if (endpoint == null) {
			return;
		}
		in.read(SHARED_CONTENTS_TAG, ContainerContents.CODEC).ifPresent(contents -> {
			CompoundTag carrier = in.read(CARRIER_TAG, CompoundTag.CODEC).orElseGet(CompoundTag::new);
			Component name = Component.literal(carrier.getStringOr(TankLinkedStorageHostWrapper.DISPLAY_NAME_TAG, ""));
			int upgradeSlots = contents.upgrades().stacks().size();
			ClientLinkedStorageContents.updateContents(endpoint.groupId(), in.getLongOr(REVISION_TAG, 0), contents, name, 0, upgradeSlots, 0);
			CompoundTag descriptor = new CompoundTag();
			descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, TankLinkedStorageHostWrapper.FACTORY_ID.toString());
			descriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG, carrier);
			ClientLinkedStorageContents.updateHostDescriptor(endpoint.groupId(), descriptor);
			ClientLinkedStorageContents.getContents(endpoint.groupId())
					.ifPresent(shared -> bindClientLinkedStorage(endpoint,
							in.getBooleanOr(PRIMARY_ENDPOINT_TAG, false) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY, shared,
							TankLinkedStorageHostWrapper.FACTORY_ID, carrier, name, 0, upgradeSlots));
		});
		updateFluidLighting();
	}
}
