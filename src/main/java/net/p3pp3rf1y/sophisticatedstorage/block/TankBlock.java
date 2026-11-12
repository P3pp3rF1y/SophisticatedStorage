package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.PackingTapeItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

import javax.annotation.Nullable;

import java.util.List;

public class TankBlock extends StorageBlockBase {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<TankForm> FORM = EnumProperty.create("form", TankForm.class);
	public static final BooleanProperty SHOW_TIER = BooleanProperty.create("show_tier");
	public static final BooleanProperty PACKED = BooleanProperty.create("packed");
	public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
	private static final VoxelShape MIDDLE_SHAPE = box(1, 0, 1, 15, 16, 15);
	private static final VoxelShape BOTTOM_SHAPE = Shapes.or(MIDDLE_SHAPE, box(0, 0, 0, 16, 2, 16));
	private static final VoxelShape TOP_SHAPE = Shapes.or(MIDDLE_SHAPE, box(0, 14, 0, 16, 16, 16));
	private static final VoxelShape SINGLE_SHAPE = Shapes.or(BOTTOM_SHAPE, TOP_SHAPE);
	private final TankTier tier;
	public TankBlock(TankTier tier) {
		super(Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.STONE).noOcclusion().lightLevel(state -> state.getValue(LIGHT_LEVEL)),
				() -> 0, tier::upgradeSlots);
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORM, TankForm.SINGLE).setValue(SHOW_TIER, true)
				.setValue(PACKED, false).setValue(TICKING, false).setValue(LIGHT_LEVEL, 0));
	}
	public TankTier getTier() {
		return tier;
	}
	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(LIGHT_LEVEL);
	}
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FORM)) {
			case SINGLE -> SINGLE_SHAPE;
			case BOTTOM -> BOTTOM_SHAPE;
			case MIDDLE -> MIDDLE_SHAPE;
			case TOP -> TOP_SHAPE;
		};
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FORM, SHOW_TIER, PACKED, TICKING, LIGHT_LEVEL);
	}
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
	@Override
	public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
		return rotate(state, rotation);
	}
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
	@Override
	public TankBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TankBlockEntity(pos, state);
	}
	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.TANK_BLOCK_ENTITY_TYPE.get();
	}
	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide
				? null
				: createTickerHelper(type, ModBlocks.TANK_BLOCK_ENTITY_TYPE.get(), (l, pos, blockState, tank) -> TankBlockEntity.tick(l, pos, tank));
	}
	@Override
	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof TankBlockItem) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		return WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).flatMap(TankBlockEntity::getPrimary).map(primary -> {
			if (stack.getItem() instanceof PackingTapeItem) {
				primary.pack();
				if (!player.isCreative()) {
					stack.setDamageValue(stack.getDamageValue() + 1);
					if (stack.getDamageValue() >= stack.getMaxDamage()) {
						stack.shrink(1);
					}
				}
				return InteractionResult.SUCCESS;
			}
			if (!primary.isPacked() && (FluidUtil.interactWithFluidHandler(player, hand, primary.getStorageWrapper().getTankFluidHandler())
					|| tryAddSingleUpgrade(player, hand, primary, stack))) {
				return InteractionResult.SUCCESS;
			}
			if (!primary.isPacked()) {
				NetworkHooks.openScreen((ServerPlayer) player,
						new SimpleMenuProvider((id, inventory, opener) -> new TankContainerMenu(id, opener, primary.getBlockPos()),
								primary.getMenuDisplayName()),
						buffer -> StorageContainerMenu.writeMenuData(buffer, player, primary.getBlockPos()));
			}
			return InteractionResult.SUCCESS;
		}).orElse(InteractionResult.FAIL);
	}
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		return List.of();
	}
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock())) {
			WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).ifPresent(TankBlockEntity::onSegmentRemoved);
		}
		super.onRemove(state, level, pos, newState, moving);
	}
	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		spawnDestroyParticles(level, player, pos, state);
		level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
	}
	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
		TankBlockEntity tank = WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).orElse(null);
		if (tank == null) {
			return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
		}
		tank.setCreativePlayerRemoval(player.isCreative());
		try {
			return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
		} finally {
			tank.setCreativePlayerRemoval(false);
		}
	}
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
	}
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).filter(TankBlockEntity::isPrimary).filter(tank -> !tank.isPacked())
				.ifPresent(tank -> renderUpgrades(level, random, pos, getFacing(state), tank.getStorageWrapper().getRenderInfo(), state));
	}
	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter level, BlockPos pos, Player player) {
		return new ItemStack(this);
	}
	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).flatMap(TankBlockEntity::getPrimary).map(tank -> {
			TankFluidHandler fluid = tank.getStorageWrapper().getTankFluidHandler();
			int amount = fluid.getFluidInTank(0).getAmount();
			return amount == 0 ? 0 : 1 + (int) ((long) amount * 14 / fluid.getTankCapacity(0));
		}).orElse(0);
	}
}
