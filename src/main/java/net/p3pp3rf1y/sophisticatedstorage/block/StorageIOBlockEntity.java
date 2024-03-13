package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.*;

public class StorageIOBlockEntity extends BlockEntity implements IControllerBoundable, ILinkable {
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private Map<Capability<?>, Map<Direction, LazyOptional<?>>> capabilitySideCache = new HashMap<>();
	protected StorageIOBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public StorageIOBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlocks.STORAGE_IO_BLOCK_ENTITY_TYPE.get(), pos, state);
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
	public void setControllerPos(BlockPos controllerPos) {
		this.controllerPos = controllerPos;
		invalidateAllCapabilityCache();
		setChanged();
	}

	@Override
	public Optional<BlockPos> getControllerPos() {
		return Optional.ofNullable(controllerPos);
	}

	@Override
	public boolean isLinked() {
		return isLinkedToController && getControllerPos().isPresent();
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
	public void setNotLinked() {
		ILinkable.super.setNotLinked();
		isLinkedToController = false;
		removeControllerPos();
		setChanged();
	}

	@Override
	public void removeControllerPos() {
		controllerPos = null;
		invalidateAllCapabilityCache();
		capabilitySideCache.clear();
		setChanged();
	}

	@SuppressWarnings("java:S1640") //can't use EnumMap because one of keys is null
	private void invalidateAllCapabilityCache() {
		capabilitySideCache.forEach((cap, map) -> {
			HashMap<Direction, LazyOptional<?>> copy = new HashMap<>(map); //to prevent concurrent modification exception
			copy.forEach((side, lazyOptional) -> lazyOptional.invalidate());
		});
	}

	@Override
	public BlockPos getStorageBlockPos() {
		return getBlockPos();
	}

	@Override
	public Level getStorageBlockLevel() {
		return getLevel();
	}

	@Override
	public boolean canConnectStorages() {
		return false;
	}

	@Override
	public void addToController(Level level, BlockPos pos, BlockPos controllerPos) {
		WorldHelper.getBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(pos));
	}

	@Override
	public boolean canBeConnected() {
		return isLinked() || getControllerPos().isEmpty();
	}

	@Override
	public void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getBlockPos());
	}

	@Override
	public void unregisterController() {
		removeControllerPos();
	}

	public void removeFromController() {
		if (!level.isClientSide()) {
			getControllerPos().flatMap(p -> WorldHelper.getBlockEntity(level, p, ControllerBlockEntityBase.class)).ifPresent(c -> c.removeNonConnectingBlock(getBlockPos()));
			removeControllerPos();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveControllerPos(tag);
		if (isLinkedToController) {
			tag.putBoolean("isLinkedToController", isLinkedToController);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		loadControllerPos(tag);
		isLinkedToController = NBTHelper.getBoolean(tag, "isLinkedToController").orElse(false);
	}

	@Override
	@SuppressWarnings("java:S1640") //can't use EnumMap because one of keys is null
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (getControllerPos().isEmpty()) {
			return super.getCapability(cap, side);
		}

		if (!capabilitySideCache.containsKey(cap) || !capabilitySideCache.get(cap).containsKey(side)) {
			LazyOptional<T> lazyOptional = getControllerPos().flatMap(p -> WorldHelper.getLoadedBlockEntity(getLevel(), p, ControllerBlockEntity.class))
					.map(c -> getControllerCapability(cap, side, c))
					.orElseGet(() -> super.getCapability(cap, side));
			capabilitySideCache.computeIfAbsent(cap, k -> new HashMap<>()).put(side, lazyOptional);
			if (lazyOptional.isPresent()) {
				lazyOptional.addListener(l -> removeCapabilityCacheOnSide(cap, side));
			}
		}

		return capabilitySideCache.get(cap).get(side).cast();
	}

	private <T> void removeCapabilityCacheOnSide(Capability<T> cap, @Nullable Direction side) {
		if (capabilitySideCache.containsKey(cap) && capabilitySideCache.get(cap).containsKey(side)) {
			capabilitySideCache.get(cap).get(side).invalidate();
			capabilitySideCache.get(cap).remove(side);
		}
	}

	protected <T> LazyOptional<T> getControllerCapability(Capability<T> cap, @Nullable Direction side, ControllerBlockEntity c) {
		return c.getCapability(cap, side);
	}
}
