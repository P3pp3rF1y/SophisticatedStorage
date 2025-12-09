package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class StorageIOBlockEntity extends BlockEntity implements IControllerBoundable, ILinkable {
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private boolean chunkBeingUnloaded = false;

	@Nullable
	private BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> controllerItemHandlerCache;

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
		controllerItemHandlerCache = null;
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
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
		controllerItemHandlerCache = null;
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
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
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveControllerPos(out);
		if (isLinkedToController) {
			out.putBoolean("isLinkedToController", isLinkedToController);
		}
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		loadControllerPos(in);
		isLinkedToController = in.getBooleanOr("isLinkedToController", false);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag(registries).merge(ValueIOHelper.collectOutputToTag(registries, this::saveAdditional));
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	protected void invalidateItemHandlerCache() {
		controllerItemHandlerCache = null;
		invalidateCapabilities();
	}

	@Nullable
	@SuppressWarnings("java:S1640") //can't use EnumMap because one of keys is null
	public ResourceHandler<ItemResource> getExternalItemResourceHandler(@Nullable Direction side) {
		if (getControllerPos().isEmpty()) {
			return null;
		}

		if (controllerItemHandlerCache == null && level instanceof ServerLevel serverLevel) {
			controllerItemHandlerCache = BlockCapabilityCache.create(
					Capabilities.Item.BLOCK,
					serverLevel,
					getControllerPos().get(),
					side,
					() -> !isRemoved(),
					this::invalidateItemHandlerCache
			);
		}

		if (controllerItemHandlerCache != null) {
			return controllerItemHandlerCache.getCapability();
		} else {
			return WorldHelper.getBlockEntity(getLevel(), getControllerPos().get(), ControllerBlockEntity.class).map(c -> c.getExternalItemResourceHandler()).orElse(null);
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		chunkBeingUnloaded = true;
	}

	@Override
	public void setRemoved() {
		if (!chunkBeingUnloaded && level != null) {
			unlinkFromController();
		}

		super.setRemoved();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		removeFromController();
	}
}
