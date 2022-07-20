package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jetbrains.annotations.Nullable;

public class ControllerBlock extends Block implements EntityBlock {
	public ControllerBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ControllerBlockEntity(pos, state);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		WorldHelper.getBlockEntity(level, pos, ControllerBlockEntityBase.class).ifPresent(ControllerBlockEntityBase::detachFromStorages);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		WorldHelper.getBlockEntity(pLevel, pPos, ControllerBlockEntity.class).ifPresent(ControllerBlockEntityBase::searchAndAddStorages);
	}
}
