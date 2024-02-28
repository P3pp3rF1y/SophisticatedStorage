package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IDisplaySideStorage;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class ChestBlock extends WoodStorageBlockBase implements SimpleWaterloggedBlock, IDisplaySideStorage {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
	protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape NORTH_AABB = Block.box(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape SOUTH_AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D);
	protected static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape EAST_AABB = Block.box(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D);

	public ChestBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier) {
		this(numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier, 2.5F);
	}

	public ChestBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, float explosionResistance) {
		super(Properties.of().mapColor(MapColor.WOOD).strength(2.5F, explosionResistance).sound(SoundType.WOOD), numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(TICKING, false).setValue(TYPE, ChestType.SINGLE));
	}

	public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState state, Level pLevel, BlockPos pos, boolean override) {
		BiPredicate<LevelAccessor, BlockPos> isChestBlocked = override ? ((l, p) -> false) : ChestBlock::isChestBlockedAt;
		return DoubleBlockCombiner.combineWithNeigbour(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), this::getBlockType, ChestBlock::getConnectedDirection, FACING, state, pLevel, pos, isChestBlocked);
	}

	//TODO add check for this during chest open
	public static boolean isChestBlockedAt(LevelAccessor level, BlockPos pos) {
		return isBlockedChestByBlock(level, pos) || isCatSittingOnChest(level, pos);
	}

	private static boolean isBlockedChestByBlock(BlockGetter level, BlockPos pos) {
		BlockPos blockpos = pos.above();
		return level.getBlockState(blockpos).isRedstoneConductor(level, blockpos);
	}

	public static Direction getConnectedDirection(BlockState blockState) {
		Direction direction = blockState.getValue(FACING);
		return blockState.getValue(TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
	}

	private static boolean isCatSittingOnChest(LevelAccessor pLevel, BlockPos pPos) {
		List<Cat> list = pLevel.getEntitiesOfClass(Cat.class, new AABB(pPos.getX(), pPos.getY() + 1, pPos.getZ(), pPos.getX() + 1, pPos.getY() + 2, pPos.getZ() + 1));
		if (!list.isEmpty()) {
			for (Cat cat : list) {
				if (cat.isInSittingPose()) {
					return true;
				}
			}
		}

		return false;
	}

	private DoubleBlockCombiner.BlockType getBlockType(BlockState blockState) {
		ChestType chesttype = blockState.getValue(TYPE);
		if (chesttype == ChestType.SINGLE) {
			return DoubleBlockCombiner.BlockType.SINGLE;
		} else {
			return chesttype == ChestType.RIGHT ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		if (facingState.is(this) && facing.getAxis().isHorizontal()) {
			ChestType chesttype = facingState.getValue(TYPE);
			if (state.getValue(TYPE) == ChestType.SINGLE && chesttype != ChestType.SINGLE && state.getValue(FACING) == facingState.getValue(FACING) && getConnectedDirection(facingState) == facing.getOpposite()) {
				return state.setValue(TYPE, chesttype.getOpposite());
			}
		} else if (getConnectedDirection(state) == facing) {
			return state.setValue(TYPE, ChestType.SINGLE);
		}

		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(TYPE) == ChestType.SINGLE) {
			return AABB;
		} else {
			return switch (getConnectedDirection(state)) {
				case SOUTH -> SOUTH_AABB;
				case WEST -> WEST_AABB;
				case EAST -> EAST_AABB;
				default -> NORTH_AABB;
			};
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		ItemStack chestBeingPlaced = context.getItemInHand();

		if (ChestBlockItem.isDoubleChest(chestBeingPlaced)) {
			BlockPos otherPartPos = context.getClickedPos().relative(context.getHorizontalDirection().getClockWise());
			Level level = context.getLevel();
			if (!level.getBlockState(otherPartPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(otherPartPos)) {
				return null;
			}
		}

		Direction direction = context.getHorizontalDirection().getOpposite();
		return chestBeingPlaced.getCapability(CapabilityStorageWrapper.getCapabilityInstance())
				.map(wrapper ->
						getStateForPlacement(context, direction, fluidstate,
								wrapper.getMainColor(), wrapper.getAccentColor(), InventoryHelper.isEmpty(wrapper.getUpgradeHandler()))
				)
				.orElse(
						getStateForPlacement(context, direction, fluidstate,
								StorageBlockItem.getMainColorFromStack(chestBeingPlaced).orElse(-1),
								StorageBlockItem.getAccentColorFromStack(chestBeingPlaced).orElse(-1), true)
				);
	}

	private BlockState getStateForPlacement(BlockPlaceContext context, Direction direction, FluidState fluidstate, int mainColor, int accentColor, boolean itemHasNoUpgrades) {
		ChestType chestType = ChestType.SINGLE;
		Direction clickedFace = context.getClickedFace();
		boolean isHoldingSneak = context.isSecondaryUseActive();
		if (clickedFace.getAxis().isHorizontal() && isHoldingSneak) {
			Direction partnerFacing = this.candidatePartnerFacing(context, clickedFace.getOpposite(), mainColor, accentColor, itemHasNoUpgrades);
			if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
				direction = partnerFacing;
				chestType = partnerFacing.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
			}
		}

		if (chestType == ChestType.SINGLE && !isHoldingSneak) {
			if (direction == this.candidatePartnerFacing(context, direction.getClockWise(), mainColor, accentColor, itemHasNoUpgrades)) {
				chestType = ChestType.LEFT;
			} else if (direction == this.candidatePartnerFacing(context, direction.getCounterClockWise(), mainColor, accentColor, itemHasNoUpgrades)) {
				chestType = ChestType.RIGHT;
			}
		}
		return this.defaultBlockState().setValue(FACING, direction).setValue(TYPE, chestType).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
	}

	@Nullable
	private Direction candidatePartnerFacing(BlockPlaceContext context, Direction direction, int mainColor, int accentColor, boolean itemHasNoUpgrades) {
		BlockPos neighborChestPos = context.getClickedPos().relative(direction);
		BlockState blockstate = context.getLevel().getBlockState(neighborChestPos);
		if (!blockstate.is(this) || blockstate.getValue(TYPE) != ChestType.SINGLE) {
			return null;
		}

		if (context.getLevel().getBlockEntity(neighborChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.map(be -> mainColor == be.getStorageWrapper().getMainColor() && accentColor == be.getStorageWrapper().getAccentColor()
						&& (itemHasNoUpgrades || InventoryHelper.isEmpty(be.getStorageWrapper().getUpgradeHandler()))).orElse(false)) {
			return blockstate.getValue(FACING);
		}
		return null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return level.isClientSide ? createTickerHelper(blockEntityType, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), (l, p, s, be) -> ChestBlockEntity.lidAnimateTick(be)) : super.getTicker(level, state, blockEntityType);
	}

	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(BlockState state) {
		return Boolean.TRUE.equals(state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class).map(b -> {
			ItemStack stackInHand = player.getItemInHand(hand);

			if (!b.isMainChest()) {
				b = WorldHelper.getBlockEntity(level, pos.relative(getConnectedDirection(state)), ChestBlockEntity.class).orElse(b);
			}

			if (b.isPacked()) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide || hand == InteractionHand.OFF_HAND) {
				return InteractionResult.SUCCESS;
			}

			if (tryItemInteraction(player, hand, b, stackInHand, state.getValue(FACING), hitResult)) {
				return InteractionResult.SUCCESS;
			}

			player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
			NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
					WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty())), pos);
			PiglinAi.angerNearbyPiglins(player, true);

			return InteractionResult.CONSUME;
		}).orElse(InteractionResult.PASS);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (ChestBlockItem.isDoubleChest(stack) && !level.isClientSide()) {
			BlockPos otherPartPos = pos.relative(state.getValue(FACING).getCounterClockWise());
			level.setBlock(otherPartPos, state.setValue(TYPE, ChestType.LEFT), 3);
			level.getBlockEntity(otherPartPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				setRenderBlockRenderProperties(stack, be);
				be.setMainPos(pos);
				be.tryToAddToController();
			});
		}

		ChestType chestType = state.getValue(TYPE);
		if (chestType == ChestType.SINGLE || level.isClientSide()) {
			return;
		}

		level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be ->
				level.getBlockEntity(pos.relative(getConnectedDirection(state)), ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
						.ifPresent(otherBE -> {
							if (InventoryHelper.isEmpty(be.getStorageWrapper().getUpgradeHandler())
									&& (chestType == ChestType.LEFT || !InventoryHelper.isEmpty(otherBE.getStorageWrapper().getUpgradeHandler()))) {
								be.joinWithChest(otherBE);
							} else {
								otherBE.joinWithChest(be);
							}
							be.syncTogglesFrom(otherBE);
						})
		);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
		if (state.getValue(TYPE) != ChestType.SINGLE) {
			level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				if (be.isPacked() && !be.isMainChest()) {
					//copy storage wrapper to "not main" chest so that its data can be transferred to stack properly
					BlockPos otherPartPos = pos.relative(getConnectedDirection(state));
					level.getBlockEntity(otherPartPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
							.ifPresent(mainBe -> be.getStorageWrapper().load(mainBe.getStorageWrapper().save(new CompoundTag())));
				}
			});
		}

		return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getValue(TYPE) != ChestType.SINGLE) {
			level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				if (be.isPacked()) {
					level.removeBlock(pos.relative(getConnectedDirection(state)), false);
				}
			});
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}


	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		if (be instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.isPacked() && be.getBlockState().getValue(TYPE) != ChestType.SINGLE) {
			super.addDropData(stack, be);
			ChestBlockItem.setDoubleChest(stack, true);
		} else {
			super.addDropData(stack, be);
		}
	}

	@Override
	protected void packStorage(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand) {
		super.packStorage(player, hand, b, stackInHand);

		if (b.getBlockState().getValue(TYPE) == ChestType.SINGLE) {
			return;
		}

		player.level().getBlockEntity(b.getBlockPos().relative(getConnectedDirection(b.getBlockState())), ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.ifPresent(be -> super.packStorage(player, hand, be, stackInHand));
	}

	@Nullable
	@Override
	public ChestBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ChestBlockEntity(pos, state);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, TICKING, TYPE);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(StorageBlockEntity::recheckOpen);
	}

	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get();
	}

	@Override
	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	//TODO add break override that properly handles copying stacks and settings over to the other part and handling overflowed stacks by dropping them on the ground

	@Override
	public boolean canChangeDisplaySide(BlockState state) {
		return state.getValue(TYPE) != ChestType.SINGLE;
	}
}
