package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;

public class StorageIOBlock extends BlockBase implements EntityBlock {
	public StorageIOBlock(Properties properties) {
		super(properties.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3F, 6.0F));
		registerDefaultState(stateDefinition.any().setValue(SimpleMaterialBlockData.OPAQUE, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SimpleMaterialBlockData.OPAQUE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(SimpleMaterialBlockData.OPAQUE, SimpleMaterialBlockData.isOpaque(context.getItemInHand()));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageIOBlockEntity(pos, state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		WorldHelper.getBlockEntity(level, pos, StorageIOBlockEntity.class).ifPresent(StorageIOBlockEntity::removeFromController);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(SimpleMaterialBlockData.OPAQUE) ? Shapes.block() : Shapes.empty();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		SimpleMaterialBlockData.copyToBlockEntity(level, pos, stack);
		WorldHelper.getBlockEntity(level, pos, StorageIOBlockEntity.class).ifPresent(IControllerBoundable::addToAdjacentController);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
		SimpleMaterialBlockData.copyFromBlockEntity(level, pos, stack);
		return stack;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockState ret = super.playerWillDestroy(level, pos, state, player);
		WorldHelper.getBlockEntity(level, pos, StorageIOBlockEntity.class).ifPresent(StorageIOBlockEntity::removeFromController);
		return ret;
	}
}
