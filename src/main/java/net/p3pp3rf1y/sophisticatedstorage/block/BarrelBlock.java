package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IBlockRenderProperties;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticle;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class BarrelBlock extends Block implements EntityBlock, IStorageBlock, IAdditionalDropDataBlock, ITintableBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty TICKING = BooleanProperty.create("ticking");
	private static final VoxelShape SLIGHTLY_SMALLER_SHAPE = box(0.01, 0.01, 0.01, 15.99, 15.99, 15.99);
	private static final VoxelShape ITEM_ENTITY_COLLISION_SHAPE = box(0.1, 0.1, 0.1, 15.9, 15.9, 15.9);

	private final int numberOfInventorySlots;
	private final int numberOfUpgradeSlots;

	public static final Set<WoodType> CUSTOM_TEXTURE_WOOD_TYPES = Set.of(WoodType.ACACIA, WoodType.BIRCH, WoodType.CRIMSON, WoodType.DARK_OAK, WoodType.JUNGLE, WoodType.OAK, WoodType.SPRUCE, WoodType.WARPED);

	public BarrelBlock(int numberOfInventorySlots, int numberOfUpgradeSlots, Properties properties) {
		super(properties);
		this.numberOfInventorySlots = numberOfInventorySlots;
		this.numberOfUpgradeSlots = numberOfUpgradeSlots;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(TICKING, false));
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
		ItemStack stack = new ItemStack(this);
		addWoodAndTintData(stack, world, pos);
		return stack;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
		super.entityInside(state, world, pos, entity);
		if (!world.isClientSide && entity instanceof ItemEntity itemEntity) {
			WorldHelper.getBlockEntity(world, pos, StorageBlockEntity.class).ifPresent(te -> tryToPickup(world, itemEntity, te));
		}
	}

	private void tryToPickup(Level world, ItemEntity itemEntity, IStorageWrapper w) {
		ItemStack remainingStack = itemEntity.getItem().copy();
		remainingStack = InventoryHelper.runPickupOnPickupResponseUpgrades(world, w.getUpgradeHandler(), remainingStack, false);
		if (remainingStack.getCount() < itemEntity.getItem().getCount()) {
			itemEntity.setItem(remainingStack);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		return SLIGHTLY_SMALLER_SHAPE;
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> items.add(BarrelBlockItem.setWoodType(new ItemStack(this), woodType)));

		for (DyeColor color : DyeColor.values()) {
			ItemStack barrelStack = new ItemStack(this);
			setMainColor(barrelStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
			setAccentColor(barrelStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
			items.add(barrelStack);
		}
		ItemStack barrelStack = new ItemStack(this);
		setMainColor(barrelStack, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
		setAccentColor(barrelStack, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
		items.add(barrelStack);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		level.sendParticles(new CustomTintTerrainParticleData(state1), entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void initializeClient(Consumer<IBlockRenderProperties> consumer) {
		consumer.accept(new IBlockRenderProperties() {
			@Override
			public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
				if (state.getBlock() != BarrelBlock.this || !(level instanceof ClientLevel clientLevel)) {
					return false;
				}

				VoxelShape voxelshape = state.getShape(level, pos);
				voxelshape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
					double d1 = Math.min(1.0D, maxX - minX);
					double d2 = Math.min(1.0D, maxY - minY);
					double d3 = Math.min(1.0D, maxZ - minZ);
					int i = Math.max(2, Mth.ceil(d1 / 0.25D));
					int j = Math.max(2, Mth.ceil(d2 / 0.25D));
					int k = Math.max(2, Mth.ceil(d3 / 0.25D));

					for (int l = 0; l < i; ++l) {
						for (int i1 = 0; i1 < j; ++i1) {
							for (int j1 = 0; j1 < k; ++j1) {
								double d4 = (l + 0.5D) / i;
								double d5 = (i1 + 0.5D) / j;
								double d6 = (j1 + 0.5D) / k;
								double d7 = d4 * d1 + minX;
								double d8 = d5 * d2 + minY;
								double d9 = d6 * d3 + minZ;
								manager.add(new CustomTintTerrainParticle(clientLevel, pos.getX() + d7, pos.getY() + d8, pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, state, pos).updateSprite(state, pos));
							}
						}
					}
				});
				return true;
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(b -> {
			player.awardStat(Stats.OPEN_BARREL);
			NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
					WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(TextComponent.EMPTY)), pos);
		});

		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			BarrelBlockItem.getWoodType(stack).ifPresent(be::setWoodType);
			BarrelBlockItem.getMaincolorFromStack(stack).ifPresent(be::setMainColor);
			BarrelBlockItem.getAccentColorFromStack(stack).ifPresent(be::setAccentColor);
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(b -> {
				b.dropContents();
				level.updateNeighbourForOutputSignal(pos, this);
			});

			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void tick(BlockState pState, ServerLevel level, BlockPos pos, Random pRandom) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(StorageBlockEntity::recheckOpen);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return !level.isClientSide && Boolean.TRUE.equals(state.getValue(TICKING)) ? createTickerHelper(blockEntityType, ModBlocks.BARREL_TILE_TYPE.get(), (l, blockPos, blockState, storageBlockEntity) -> StorageBlockEntity.serverTick(l, blockPos, storageBlockEntity)) : null;
	}

	@Nullable
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typePassedIn, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> blockEntityTicker) {
		//noinspection unchecked
		return typeExpected == typePassedIn ? (BlockEntityTicker<A>) blockEntityTicker : null;
	}

	@Nullable
	@Override
	public StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageBlockEntity(pos, state);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean hasAnalogOutputSignal(BlockState pState) {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> InventoryHelper.getAnalogOutputSignal(be.getInventoryForInputOutput())).orElse(0);
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
		return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
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

	@Override
	public int getNumberOfInventorySlots() {
		return numberOfInventorySlots;
	}

	@Override
	public int getNumberOfUpgradeSlots() {
		return numberOfUpgradeSlots;
	}

	@Override
	public boolean isTicking(BlockState state) {
		return state.getValue(TICKING);
	}

	@Override
	public void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking) {
		level.setBlockAndUpdate(pos, currentState.setValue(TICKING, ticking));
	}

	private void removeWoodType(ItemStack barrelStack) {
		barrelStack.getOrCreateTag().remove(BarrelBlockItem.WOOD_TYPE_TAG);
	}

	@Override
	public void setMainColor(ItemStack barrelStack, int mainColor) {
		if (BarrelBlockItem.getAccentColorFromStack(barrelStack).isPresent()) {
			removeWoodType(barrelStack);
		}
		barrelStack.getOrCreateTag().putInt("mainColor", mainColor);
	}

	@Override
	public Optional<Integer> getMainColor(ItemStack barrelStack) {
		return BarrelBlockItem.getMaincolorFromStack(barrelStack);
	}

	@Override
	public void setAccentColor(ItemStack barrelStack, int accentColor) {
		if (BarrelBlockItem.getMaincolorFromStack(barrelStack).isPresent()) {
			removeWoodType(barrelStack);
		}
		barrelStack.getOrCreateTag().putInt("accentColor", accentColor);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		return pContext instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof ItemEntity ? ITEM_ENTITY_COLLISION_SHAPE : super.getCollisionShape(pState, pLevel, pPos, pContext);
	}

	@Override
	public Optional<Integer> getAccentColor(ItemStack stack) {
		return BarrelBlockItem.getAccentColorFromStack(stack);
	}

	public void addWoodAndTintData(ItemStack stack, BlockGetter level, BlockPos pos) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> addDropData(stack, be));
	}

	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		int mainColor = be.getMainColor();
		if (mainColor > -1) {
			setMainColor(stack, mainColor);
		}
		int accentColor = be.getAccentColor();
		if (accentColor > -1) {
			setAccentColor(stack, accentColor);
		}
		be.getWoodType().ifPresent(n -> BarrelBlockItem.setWoodType(stack, n));
		be.getCustomName().ifPresent(stack::setHoverName);
	}
}
