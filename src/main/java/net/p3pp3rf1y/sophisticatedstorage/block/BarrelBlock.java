package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class BarrelBlock extends Block implements EntityBlock, IStorageBlock, IAdditionalDropDataBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty TICKING = BooleanProperty.create("ticking");
	private static final String WOOD_NAME_TAG = "woodName";

	private final int numberOfInventorySlots;
	private final int numberOfUpgradeSlots;

	public static final Set<String> CUSTOM_TEXTURE_WOOD_TYPES = Set.of("acacia", "birch", "crimson", "dark_oak", "jungle", "oak", "spruce", "warped");

	public BarrelBlock(int numberOfInventorySlots, int numberOfUpgradeSlots, Properties properties) {
		super(properties);
		this.numberOfInventorySlots = numberOfInventorySlots;
		this.numberOfUpgradeSlots = numberOfUpgradeSlots;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(TICKING, false));
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodName -> items.add(setWoodName(new ItemStack(this), woodName)));

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
			getWoodName(stack).ifPresent(be::setWoodName);
			getMaincolor(stack).ifPresent(be::setMainColor);
			getAccentColor(stack).ifPresent(be::setAccentColor);
		});
	}

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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageBlockEntity(pos, state);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState pState) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> InventoryHelper.getAnalogOutputSignal(be.getInventoryForInputOutput())).orElse(0);
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
		return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
	}

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

	public static Optional<String> getWoodName(ItemStack barrelStack) {
		return NBTHelper.getString(barrelStack, WOOD_NAME_TAG);
	}

	private static ItemStack setWoodName(ItemStack barrelStack, String woodName) {
		barrelStack.getOrCreateTag().putString(WOOD_NAME_TAG, woodName);
		return barrelStack;
	}

	private static void setMainColor(ItemStack barrelStack, int mainColor) {
		barrelStack.getOrCreateTag().putInt("mainColor", mainColor);
	}

	public static Optional<Integer> getMaincolor(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "mainColor");
	}

	private static void setAccentColor(ItemStack barrelStack, int accentColor) {
		barrelStack.getOrCreateTag().putInt("accentColor", accentColor);
	}

	public static Optional<Integer> getAccentColor(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "accentColor");
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
		be.getWoodName().ifPresent(n -> setWoodName(stack, n));
	}
}
