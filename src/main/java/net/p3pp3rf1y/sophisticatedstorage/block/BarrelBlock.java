package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BarrelBlock extends WoodStorageBlockBase {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty FLAT_TOP = BooleanProperty.create("flat_top");
	public static final BooleanProperty OPAQUE = BooleanProperty.create("opaque");
	private static final VoxelShape ITEM_ENTITY_COLLISION_SHAPE = box(0.05, 0.05, 0.05, 15.95, 15.95, 15.95);

	public BarrelBlock(Config.Server.StorageConfig config, float explosionResistance, Properties properties) {
		this(config::numberOfInventorySlots, config::numberOfUpgradeSlots, explosionResistance, stateDef -> stateDef.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(TICKING, false).setValue(FLAT_TOP, false).setValue(OPAQUE, true), properties);
	}

	public BarrelBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, float explosionResistance, Function<StateDefinition<Block, BlockState>, BlockState> getDefaultState, Properties properties) {
		super(properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).isRedstoneConductor((state, level, pos) -> isFlatTop(state)).explosionResistance(explosionResistance), numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		registerDefaultState(getDefaultState.apply(stateDefinition));
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		super.addCreativeTabItems(itemConsumer);
		if (this != ModBlocks.BARREL.get()) {
			return;
		}

		ItemStack flatBarrel = WoodStorageBlockItem.setWoodType(new ItemStack(this), WoodType.ACACIA);
		BarrelBlockItem.toggleFlatTop(flatBarrel);
		itemConsumer.accept(flatBarrel);
	}

	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		level.sendParticles(new CustomTintTerrainParticleData(state1, pos), entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
		return true;
	}

	@Override
	public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
		Vec3 vec3 = entity.getDeltaMovement();
		level.addParticle(new CustomTintTerrainParticleData(state, pos),
				entity.getX() + (level.getRandom().nextDouble() - 0.5D) * entity.getBbWidth(), entity.getY() + 0.1D, entity.getZ() + (level.getRandom().nextDouble() - 0.5D) * entity.getBbWidth(),
				vec3.x * -4.0D, 1.5D, vec3.z * -4.0D);
		return true;
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).map(b -> {
			if (b.isPacked()) {
				return InteractionResult.FAIL;
			}
			if (level.isClientSide() || hand == InteractionHand.OFF_HAND) {
				return InteractionResult.SUCCESS;
			}

			InteractionResult result = tryItemInteraction(player, hand, b, stack, getFacing(state), hitResult);
			if (result.consumesAction()) {
				return result;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}).orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		player.awardStat(Stats.OPEN_BARREL);
		player.openMenu(
				new SimpleMenuProvider(
						(w, p, pl) -> instantiateContainerMenu(w, pl, pos),
						WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty())
				), pos
		);
		if (player.level() instanceof ServerLevel serverLevel) {
			PiglinAi.angerNearbyPiglins(serverLevel, player, true);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		WorldHelper.getBlockEntity(level, pos, BarrelBlockEntity.class).ifPresent(barrel -> {
			Map<BarrelMaterial, Identifier> materials = BarrelBlockItem.getMaterials(stack);
			if (!materials.isEmpty()) {
				barrel.setMaterials(new EnumMap<>(materials));
			}
		});
	}

	protected StorageContainerMenu instantiateContainerMenu(int w, Player pl, BlockPos pos) {
		return new StorageContainerMenu(w, pl, pos);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(StorageBlockEntity::recheckOpen);
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
		return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, TICKING, FLAT_TOP, OPAQUE);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack cloneItemStack = super.getCloneItemStack(level, pos, state, includeData);
		BarrelBlockItem.setFlatTop(cloneItemStack, state.getValue(FLAT_TOP));
		WorldHelper.getBlockEntity(level, pos, BarrelBlockEntity.class).ifPresent(barrelBlockEntity -> {
			Map<BarrelMaterial, Identifier> materials = barrelBlockEntity.getMaterials();
			if (!materials.isEmpty()) {
				BarrelBlockItem.setMaterials(cloneItemStack, materials);
			}
		});
		return cloneItemStack;
	}

	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		BlockState state = be.getBlockState();
		BarrelBlockItem.setFlatTop(stack, state.getValue(FLAT_TOP));
		if (be instanceof BarrelBlockEntity barrelBlockEntity) {
			Map<BarrelMaterial, Identifier> materials = barrelBlockEntity.getMaterials();
			if (!materials.isEmpty()) {
				BarrelBlockItem.setMaterials(stack, materials);
			}
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		ItemStack stack = blockPlaceContext.getItemInHand();
		return defaultBlockState()
				.setValue(FACING, blockPlaceContext.getNearestLookingDirection().getOpposite())
				.setValue(FLAT_TOP, BarrelBlockItem.isFlatTop(stack))
				.setValue(OPAQUE, areMaterialsOpaque(BarrelBlockItem.getMaterials(stack)));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof ItemEntity || isCalledByCollisionCacheLogic(level, pos) ? ITEM_ENTITY_COLLISION_SHAPE : super.getCollisionShape(state, level, pos, context);
	}

	@Override
	public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
		return Shapes.block();
	}

	private boolean isCalledByCollisionCacheLogic(BlockGetter level, BlockPos pos) {
		return level instanceof EmptyBlockGetter && pos == BlockPos.ZERO;
	}

	@Nullable
	@Override
	public StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BarrelBlockEntity(pos, state);
	}

	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get();
	}

	@Override
	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(OPAQUE) ? Shapes.block() : Shapes.empty();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	private static boolean isFlatTop(BlockState state) {
		return state.getValue(FLAT_TOP);
	}

	public static boolean areMaterialsOpaque(Map<BarrelMaterial, Identifier> materials) {
		if (materials.isEmpty()) {
			return true;
		}

		Map<BarrelMaterial, Identifier> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		uncompactedMaterials.putAll(materials);
		BarrelBlockItem.uncompactMaterials(uncompactedMaterials);
		return uncompactedMaterials.values().stream().allMatch(BarrelBlock::isMaterialOpaque);
	}

	private static boolean isMaterialOpaque(Identifier materialLocation) {
		return BuiltInRegistries.BLOCK.getOptional(materialLocation)
				.map(block -> block.defaultBlockState().canOcclude())
				.orElse(true);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}
}
