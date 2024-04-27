package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.RotatedShapes;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class StorageLinkBlock extends BlockBase implements EntityBlock {
	private static final Map<Direction, VoxelShape> ROTATED_SHAPES = new EnumMap<>(Direction.class);
	private static final RotatedShapes SHAPE = new RotatedShapes(false, Block.box(1, 14, 1, 15, 16, 15));

	public StorageLinkBlock() {
		super(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.5F, 5.0F));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltipComponents, TooltipFlag flag) {
		tooltipComponents.addAll(StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageLinkBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getClickedFace();
		BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos().relative(direction.getOpposite()));
		return blockstate.is(this) && blockstate.getValue(FACING) == direction ? defaultBlockState().setValue(FACING, direction.getOpposite()) : defaultBlockState().setValue(FACING, direction);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction dir = state.getValue(FACING);
		return ROTATED_SHAPES.computeIfAbsent(dir, k -> SHAPE.getRotatedShape(dir));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			WorldHelper.getBlockEntity(level, pos, ILinkable.class).ifPresent(ILinkable::unlinkFromController);
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}
}
