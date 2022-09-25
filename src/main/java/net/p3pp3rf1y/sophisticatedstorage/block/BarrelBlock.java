package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IBlockRenderProperties;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BarrelBlock extends WoodStorageBlockBase {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	private static final VoxelShape ITEM_ENTITY_COLLISION_SHAPE = box(0.01, 0.01, 0.01, 15.99, 15.99, 15.99);
	private final StorageTier storageTier;

	public BarrelBlock(StorageTier storageTier, Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, Properties properties) {
		super(properties.noOcclusion(), numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(TICKING, false));
		this.storageTier = storageTier;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		level.sendParticles(new CustomTintTerrainParticleData(state1, pos), entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void initializeClient(Consumer<IBlockRenderProperties> consumer) {
		consumer.accept(new BarrelBlockRenderProperties(this));
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).map(b -> {
			ItemStack stackInHand = player.getItemInHand(hand);
			if (b.isPacked()) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide) {
				return InteractionResult.SUCCESS;
			}

			if (tryPackBlock(player, hand, b, stackInHand)) {
				return InteractionResult.SUCCESS;
			}

			player.awardStat(Stats.OPEN_BARREL);
			NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
					WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(TextComponent.EMPTY)), pos);
			PiglinAi.angerNearbyPiglins(player, true);
			return InteractionResult.CONSUME;
		}).orElse(InteractionResult.PASS);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
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
		builder.add(FACING, OPEN, TICKING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		return defaultBlockState().setValue(FACING, blockPlaceContext.getNearestLookingDirection().getOpposite());
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof ItemEntity || isCalledByCollisionCacheLogic(level, pos) ? ITEM_ENTITY_COLLISION_SHAPE : super.getCollisionShape(state, level, pos, context);
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

	public StorageTier getStorageTier() {
		return storageTier;
	}
}
