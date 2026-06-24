package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableMenu;
import org.jspecify.annotations.Nullable;

public class DecorationTableBlock extends BlockBase implements EntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	protected static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(1, 8, 1, 15, 12, 15), box(1, 0, 1, 4, 8, 4), box(12, 0, 1, 15, 8, 4),
			box(1, 0, 12, 4, 8, 15), box(12, 0, 12, 15, 8, 15));

	public DecorationTableBlock(Properties properties) {
		super(properties.mapColor(MapColor.WOOD).strength(2.5F, 2.5F).sound(SoundType.WOOD));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		player.openMenu(new SimpleMenuProvider((w, p, pl) -> new DecorationTableMenu(w, pl, pos), getName()), pos);

		return InteractionResult.CONSUME;
	}

	@Nullable
	@Override
	public DecorationTableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationTableBlockEntity(pos, state);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
		return false;
	}
}
