package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Optional;

public class StorageConnectorBlockEntity extends BlockEntity implements IControllerBoundable {
	@Nullable
	private BlockPos controllerPos = null;
	private boolean chunkBeingUnloaded = false;

	public StorageConnectorBlockEntity(BlockPos pos, BlockState blockState) {
		super(ModBlocks.STORAGE_CONNECTOR_BLOCK_ENTITY_TYPE.get(), pos, blockState);
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
		return getLevel();
	}

	@Override
	public void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getBlockPos());
	}

	@Override
	public void unregisterController() {
		removeControllerPos();
	}

	@Override
	public boolean canConnectStorages() {
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveControllerPos(tag);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		loadControllerPos(tag);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		chunkBeingUnloaded = true;
	}

	@Override
	public void setRemoved() {
		if (!chunkBeingUnloaded && level != null) {
			removeFromController();
		}

		super.setRemoved();
	}

	@Override
	public void addToController(Level level, BlockPos pos, BlockPos controllerPos) {
		WorldHelper.getBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(pos));
	}

	public void removeFromController() {
		if (controllerPos != null && !level.isClientSide()) {
			WorldHelper.getBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(controller -> controller.removeBoundable(worldPosition));
			removeControllerPos();
		}
	}
}
