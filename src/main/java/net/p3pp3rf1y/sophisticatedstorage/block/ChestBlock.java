package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
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
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class ChestBlock extends WoodStorageBlockBase implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
	protected static final VoxelShape AABB = box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape NORTH_AABB = box(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape SOUTH_AABB = box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D);
	protected static final VoxelShape WEST_AABB = box(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	protected static final VoxelShape EAST_AABB = box(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D);

	public ChestBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, Properties properties) {
		this(numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier, 2.5F, properties);
	}

	public ChestBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, float explosionResistance,
			Properties properties) {
		super(properties.mapColor(MapColor.WOOD).strength(2.5F, explosionResistance).sound(SoundType.WOOD), numberOfInventorySlotsSupplier,
				numberOfUpgradeSlotsSupplier);
		registerDefaultState(
				stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(TICKING, false).setValue(TYPE, ChestType.SINGLE));
	}

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

	private static boolean isCatSittingOnChest(LevelAccessor level, BlockPos pos) {
		List<Cat> list = level.getEntitiesOfClass(Cat.class,
				new AABB(pos.getX(), (double) pos.getY() + 1, pos.getZ(), (double) pos.getX() + 1, (double) pos.getY() + 2, (double) pos.getZ() + 1));
		if (!list.isEmpty()) {
			for (Cat cat : list) {
				if (cat.isInSittingPose()) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(StorageBlockEntity::isBeingUpgraded).orElse(false)) {
			return state;
		}
		if (Boolean.TRUE.equals(state.getValue(WATERLOGGED))) {
			scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		if (isSameChest(neighborState, level, pos, neighborPos) && direction.getAxis().isHorizontal()) {
			ChestType chesttype = neighborState.getValue(TYPE);
			if (state.getValue(TYPE) == ChestType.SINGLE && chesttype != ChestType.SINGLE && state.getValue(FACING) == neighborState.getValue(FACING)
					&& getConnectedDirection(neighborState) == direction.getOpposite()) {
				level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
					if (state.getBlock() instanceof ChestBlock chestBlock
							&& be.getStorageWrapper().getInventoryHandler().getSlots() <= chestBlock.getNumberOfInventorySlots()) {
						joinWithChest(level, neighborPos, chesttype.getOpposite(), be);
					}
					if (be.isMainChest()) {
						be.getStorageWrapper().getUpgradeHandler().refreshUpgradeWrappers();
					}
				});
				return state.setValue(TYPE, chesttype.getOpposite());
			}
		} else if (getConnectedDirection(state) == direction) {
			level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				if (!level.isClientSide() && !be.isBeingUpgraded() && !be.isPacked()) {
					if (be.isMainChest() && state.getBlock() instanceof ChestBlock chestBlock) {
						be.dropSecondPartContents(chestBlock, neighborPos);
					}
				}
			});
			return state.setValue(TYPE, ChestType.SINGLE);
		}

		return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	private boolean isSameChest(BlockState facingState, LevelReader level, BlockPos currentPos, BlockPos facingPos) {
		if (!facingState.is(this)) {
			return false;
		}

		return level.getBlockEntity(facingPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.flatMap(facingBE -> level.getBlockEntity(currentPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
						.map(currentBE -> currentBE.isPacked() == facingBE.isPacked()
								&& currentBE.getStorageWrapper().getMainColor() == facingBE.getStorageWrapper().getMainColor()
								&& currentBE.getStorageWrapper().getAccentColor() == facingBE.getStorageWrapper().getAccentColor()
								&& ((currentBE.getWoodType().isEmpty() && facingBE.getWoodType().isEmpty()) || (currentBE.getWoodType().isPresent()
										&& facingBE.getWoodType().isPresent() && currentBE.getWoodType().get() == facingBE.getWoodType().get()))))
				.orElse(false);
	}

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
		boolean isDoubleChest = ChestBlockItem.isDoubleChest(chestBeingPlaced);

		if (isDoubleChest) {
			BlockPos otherPartPos = context.getClickedPos().relative(context.getHorizontalDirection().getClockWise());
			Level level = context.getLevel();
			if (!level.getBlockState(otherPartPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(otherPartPos)) {
				return null;
			}
		}

		Direction direction = context.getHorizontalDirection().getOpposite();
		StackStorageWrapper wrapper = StackStorageWrapper.fromStack(context.getLevel().registryAccess(), chestBeingPlaced);
		return getStateForPlacement(context, direction, fluidstate, StorageBlockItem.getMainColorFromComponentHolder(chestBeingPlaced).orElse(-1),
				StorageBlockItem.getAccentColorFromComponentHolder(chestBeingPlaced).orElse(-1),
				WoodStorageBlockItem.getWoodType(chestBeingPlaced).orElse(WoodType.ACACIA),
				!wrapper.hasContents() || InventoryHelper.isEmpty(wrapper.getUpgradeHandler()), isDoubleChest);
	}

	private BlockState getStateForPlacement(BlockPlaceContext context, Direction direction, FluidState fluidstate, int mainColor, int accentColor,
			WoodType woodType, boolean itemHasNoUpgrades, boolean isDoubleChest) {
		ChestType chestType = ChestType.SINGLE;
		Direction clickedFace = context.getClickedFace();
		boolean isHoldingSneak = context.isSecondaryUseActive();
		if (!isDoubleChest && clickedFace.getAxis().isHorizontal() && isHoldingSneak) {
			Direction partnerFacing = candidatePartnerFacing(context, clickedFace.getOpposite(), mainColor, accentColor, woodType, itemHasNoUpgrades);
			if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
				direction = partnerFacing;
				chestType = partnerFacing.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
			}
		}

		if (!isDoubleChest && chestType == ChestType.SINGLE && !isHoldingSneak) {
			if (direction == candidatePartnerFacing(context, direction.getClockWise(), mainColor, accentColor, woodType, itemHasNoUpgrades)) {
				chestType = ChestType.LEFT;
			} else if (direction == candidatePartnerFacing(context, direction.getCounterClockWise(), mainColor, accentColor, woodType, itemHasNoUpgrades)) {
				chestType = ChestType.RIGHT;
			}
		}
		return defaultBlockState().setValue(FACING, direction).setValue(TYPE, chestType).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
	}

	@Nullable
	private Direction candidatePartnerFacing(BlockPlaceContext context, Direction direction, int mainColor, int accentColor, WoodType woodType,
			boolean itemHasNoUpgrades) {
		BlockPos neighborChestPos = context.getClickedPos().relative(direction);
		BlockState blockstate = context.getLevel().getBlockState(neighborChestPos);
		if (!blockstate.is(this) || blockstate.getValue(TYPE) != ChestType.SINGLE) {
			return null;
		}

		if (context.getLevel().getBlockEntity(neighborChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.map(be -> mainColor == be.getStorageWrapper().getMainColor() && accentColor == be.getStorageWrapper().getAccentColor()
						&& woodType == be.getWoodType().orElse(WoodType.ACACIA)
						&& (itemHasNoUpgrades || InventoryHelper.isEmpty(be.getStorageWrapper().getUpgradeHandler())))
				.orElse(false)) {
			return blockstate.getValue(FACING);
		}
		return null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return level.isClientSide
				? createTickerHelper(blockEntityType, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), (l, p, s, be) -> ChestBlockEntity.lidAnimateTick(be))
				: super.getTicker(level, state, blockEntityType);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return Boolean.TRUE.equals(state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		return WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class).map(b -> {
			BlockPos mainChestPos = b.getMainPos();
			b = WorldHelper.getBlockEntity(level, mainChestPos, ChestBlockEntity.class).orElse(b);

			if (b.isPacked()) {
				return InteractionResult.FAIL;
			}
			if (level.isClientSide || hand == InteractionHand.OFF_HAND) {
				return InteractionResult.SUCCESS;
			}

			InteractionResult result = tryItemInteraction(player, hand, b, stack, state.getValue(FACING), hitResult);
			if (result.consumesAction() || result == InteractionResult.FAIL) {
				return result;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}).orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (isChestBlockedAt(level, pos) || (state.getValue(TYPE) != ChestType.SINGLE && isChestBlockedAt(level, pos.relative(getConnectedDirection(state))))) {
			return InteractionResult.PASS;
		}

		return WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class).map(b -> {
			BlockPos mainChestPos = b.getMainPos();
			b = WorldHelper.getBlockEntity(level, mainChestPos, ChestBlockEntity.class).orElse(b);

			if (b.isPacked()) {
				return InteractionResult.PASS;
			}

			if (level.isClientSide) {
				return InteractionResult.SUCCESS;
			}

			player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));

			player.openMenu(new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, mainChestPos), b.getDisplayName()), mainChestPos);

			if (player.level() instanceof ServerLevel serverLevel) {
				PiglinAi.angerNearbyPiglins(serverLevel, player, true);
			}

			return InteractionResult.CONSUME;
		}).orElse(InteractionResult.PASS);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (ChestBlockItem.isDoubleChest(stack) && !level.isClientSide()) {
			BlockState rightState = state.setValue(TYPE, ChestType.RIGHT);
			BlockState leftState = state.setValue(TYPE, ChestType.LEFT);
			BlockPos otherPartPos = pos.relative(rightState.getValue(FACING).getCounterClockWise());
			int blockUpdateFlags = UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE;
			level.setBlock(pos, rightState, blockUpdateFlags);
			level.setBlock(otherPartPos, leftState, blockUpdateFlags);
			level.getBlockEntity(otherPartPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				setRenderBlockRenderProperties(stack, be);
				be.tryToAddToController();
			});
			joinChests(level, pos, otherPartPos, ChestType.RIGHT);
			rightState.updateNeighbourShapes(level, pos, 3);
			leftState.updateNeighbourShapes(level, otherPartPos, 3);
			normalizeDoubleChestControllerRegistration(level, pos, otherPartPos);
			return;
		}

		ChestType chestType = state.getValue(TYPE);
		if (chestType == ChestType.SINGLE || level.isClientSide()) {
			return;
		}

		BlockPos otherPos = pos.relative(getConnectedDirection(state));
		joinChests(level, pos, otherPos, chestType);
		state.updateNeighbourShapes(level, pos, 3);
		normalizeDoubleChestControllerRegistration(level, pos, otherPos);
	}

	private static void normalizeDoubleChestControllerRegistration(Level level, BlockPos pos, BlockPos otherPos) {
		if (level.isClientSide()) {
			return;
		}

		level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(ChestBlock::normalizeDoubleChestPartControllerRegistration);
		level.getBlockEntity(otherPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(ChestBlock::normalizeDoubleChestPartControllerRegistration);
	}

	private static void normalizeDoubleChestPartControllerRegistration(ChestBlockEntity be) {
		if (be.hasStorageData()) {
			return;
		}

		be.removeFromController();
		be.tryToAddToController();
	}

	private static void joinChests(LevelAccessor level, BlockPos pos, BlockPos otherPos, ChestType currentChestType) {
		level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.ifPresent(currentBE -> level.getBlockEntity(otherPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(otherChest -> {
					if (!InventoryHelper.isEmpty(currentBE.getStorageWrapper().getUpgradeHandler())
							&& !InventoryHelper.isEmpty(otherChest.getStorageWrapper().getUpgradeHandler())) {
						return;
					}
					joinWithChest(level, otherPos, currentChestType, currentBE);
				}));
	}

	private static void joinWithChest(LevelReader level, BlockPos otherPos, ChestType currentChestType, ChestBlockEntity currentBE) {
		level.getBlockEntity(otherPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(otherBE -> {
			if (currentChestType == ChestType.LEFT) {
				currentBE.joinWithChest(otherBE);
				currentBE.syncTogglesFrom(otherBE);
			} else {
				otherBE.joinWithChest(currentBE);
				otherBE.syncTogglesFrom(currentBE);
			}
		});
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (state.getValue(TYPE) != ChestType.SINGLE) {
			level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(be -> {
				be.setDestroyedByPlayer();
				if ((be.isPacked() || Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) && !be.isMainChest()) {
					// copy storage wrapper to "not main" chest so that its data can be transferred to stack properly
					BlockPos otherPartPos = pos.relative(getConnectedDirection(state));
					level.getBlockEntity(otherPartPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(mainBe -> {
						be.getStorageWrapper().load(mainBe.getStorageWrapper().save(new CompoundTag()));

						// remove main chest contents
						CompoundTag contentsTag = new CompoundTag();
						contentsTag.put(StorageWrapper.CONTENTS_TAG, new CompoundTag());
						mainBe.getStorageWrapper().load(contentsTag);
					});
				}
			});
		}
		return super.playerWillDestroy(level, pos, state, player);
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
	protected InteractionResult packStorage(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand) {
		InteractionResult result = super.packStorage(player, hand, b, stackInHand);

		if (b.getBlockState().getValue(TYPE) == ChestType.SINGLE) {
			return result;
		}

		player.level().getBlockEntity(b.getBlockPos().relative(getConnectedDirection(b.getBlockState())), ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.ifPresent(be -> super.packStorage(player, hand, be, stackInHand));

		return result;
	}

	@Nullable
	@Override
	public ChestBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ChestBlockEntity(pos, state);
	}

	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
		return mirror == Mirror.NONE ? rotated : rotated.setValue(TYPE, rotated.getValue(TYPE).getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, TICKING, TYPE);
	}

	@Override
	protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
		return false;
	}

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

	@Override
	public List<BlockPos> getNeighborPos(BlockState state, BlockPos origin, Direction facing) {
		if (state.getValue(TYPE) == ChestType.SINGLE) {
			return List.of(origin.relative(facing));
		} else {
			Direction connectedDirection = getConnectedDirection(state);
			if (connectedDirection == facing) {
				return List.of(origin.relative(facing).relative(facing));
			} else if (connectedDirection.getOpposite() == facing) {
				return List.of(origin.relative(facing));
			}
			return List.of(origin.relative(facing), origin.relative(connectedDirection).relative(facing));
		}
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
		if (state.getValue(TYPE) != ChestType.SINGLE && pos.relative(getConnectedDirection(state)).equals(neighbor)) {
			return;
		}
		super.onNeighborChange(state, level, pos, neighbor);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		super.entityInside(state, level, pos, entity);
		if (!level.isClientSide && entity instanceof ItemEntity itemEntity) {
			WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class).ifPresent(be -> tryToPickup(level, itemEntity, be.getMainStorageWrapper()));
		}
	}

	@Override
	protected Vector3f getMiddleFacePoint(BlockState state, BlockPos pos, Direction facing, Vector3f vector) {
		Vector3f point = new Vector3f(vector);
		float xOffset = 0;
		ChestType type = state.getValue(TYPE);
		if (type == ChestType.LEFT) {
			xOffset = -0.5f;
		} else if (type == ChestType.RIGHT) {
			xOffset = 0.5f;
		}
		point.add(xOffset, 0, 0.6f);
		point.rotate(Axis.XP.rotationDegrees(-90.0F));
		point.rotate(facing.getRotation());

		point.add(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
		return point;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public boolean tryFillUpgrades(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack itemInHand) {
		return super.tryFillUpgrades(player, hand, level,
				WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class).map(ChestBlockEntity::getMainPos).orElse(pos), itemInHand);
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class)
				.map(be -> InventoryHelper.getAnalogOutputSignal(be.getMainStorageWrapper().getInventoryForInputOutput())).orElse(0);
	}
}
