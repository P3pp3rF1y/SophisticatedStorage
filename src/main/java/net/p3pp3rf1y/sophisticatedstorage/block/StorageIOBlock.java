package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jetbrains.annotations.Nullable;

public class StorageIOBlock extends BlockBase implements EntityBlock {
	public StorageIOBlock() {
		super(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3F, 6.0F));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageIOBlockEntity(pos, state);
	}

	@SuppressWarnings({"java:S1874", "deprecation"})
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		WorldHelper.getBlockEntity(level, pos, StorageIOBlockEntity.class).ifPresent(StorageIOBlockEntity::removeFromController);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		WorldHelper.getBlockEntity(level, pos, StorageIOBlockEntity.class).ifPresent(IControllerBoundable::addToAdjacentController);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockState ret = super.playerWillDestroy(level, pos, state, player);
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(IControllableStorage::removeFromController);
		return ret;
	}
}
