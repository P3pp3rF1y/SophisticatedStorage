package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class StorageLinkBlockEntity extends BlockEntity implements ILinkable {
	@Nullable
	private BlockPos controllerPos = null;

	private boolean chunkBeingUnloaded = false;

	public StorageLinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_LINK_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveControllerPos(out);
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		loadControllerPos(in);
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
	public boolean canConnectStorages() {
		return true;
	}

	@Override
	public boolean canBeConnected() {
		return false;
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
	public Set<BlockPos> getConnectablePositions() {
		return Collections.singleton(getBlockPos().offset(getBlockState().getValue(BlockStateProperties.FACING).getOpposite().getUnitVec3i()));
	}

	@Override
	public boolean connectLinkedSelf() {
		return false;
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
	public boolean isLinked() {
		return getControllerPos().isPresent();
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
		unlinkFromController();
	}
}
