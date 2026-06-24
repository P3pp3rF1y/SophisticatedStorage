package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.RotatedShapes;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class StorageLinkBlock extends BlockBase implements EntityBlock {
	private static final Map<Direction, VoxelShape> ROTATED_SHAPES = new EnumMap<>(Direction.class);
	private static final RotatedShapes SHAPE = new RotatedShapes(false, box(1, 14, 1, 15, 16, 15));

	public StorageLinkBlock(Properties properties) {
		super(properties.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.5F, 5.0F));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(SimpleMaterialBlockData.OPAQUE, true));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageLinkBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SimpleMaterialBlockData.OPAQUE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getClickedFace();
		BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos().relative(direction.getOpposite()));
		Direction facing = blockstate.is(this) && blockstate.getValue(FACING) == direction ? direction.getOpposite() : direction;
		return defaultBlockState().setValue(FACING, facing).setValue(SimpleMaterialBlockData.OPAQUE, SimpleMaterialBlockData.isOpaque(context.getItemInHand()));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		SimpleMaterialBlockData.copyToBlockEntity(level, pos, stack);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
		SimpleMaterialBlockData.copyFromBlockEntity(level, pos, stack);
		return stack;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction dir = state.getValue(FACING);
		return ROTATED_SHAPES.computeIfAbsent(dir, k -> SHAPE.getRotatedShape(dir));
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(SimpleMaterialBlockData.OPAQUE) ? SHAPE.getRotatedShape(state.getValue(FACING)) : Shapes.empty();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		WorldHelper.getBlockEntity(level, pos, ILinkable.class).ifPresent(ILinkable::unlinkFromController);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}
}
