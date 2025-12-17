package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class LimitedBarrelBlock extends BarrelBlock {
	public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<VerticalFacing> VERTICAL_FACING = EnumProperty.create("vertical_facing", VerticalFacing.class);
	private final Supplier<Integer> getBaseStackSizeMultiplier;

	public LimitedBarrelBlock(int numberOfInventorySlots, Config.Server.LimitedBarrelConfig config, float explosionResistance, Properties properties) {
		super(() -> numberOfInventorySlots, config::upgradeSlotCount, explosionResistance,
				stateDef -> stateDef.any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(VERTICAL_FACING, VerticalFacing.NO).setValue(TICKING, false).setValue(FLAT_TOP, false), properties
		);
		this.getBaseStackSizeMultiplier = config::baseSlotLimitMultiplier;
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
		if (getVerticalFacing(state) != VerticalFacing.NO) {
			return state;
		}
		return state.setValue(HORIZONTAL_FACING, direction.rotate(state.getValue(HORIZONTAL_FACING)));
	}

	@Override
	public VerticalFacing getVerticalFacing(BlockState state) {
		return state.getValue(VERTICAL_FACING);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, VERTICAL_FACING, TICKING, FLAT_TOP);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		Direction direction = blockPlaceContext.getNearestLookingDirection().getOpposite();
		Direction horizontalDirection = blockPlaceContext.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(HORIZONTAL_FACING, horizontalDirection).setValue(VERTICAL_FACING, VerticalFacing.fromDirection(direction)).setValue(FLAT_TOP, BarrelBlockItem.isFlatTop(blockPlaceContext.getItemInHand()));
	}

	@Override
	public Direction getFacing(BlockState state) {
		VerticalFacing verticalFacing = getVerticalFacing(state);
		return verticalFacing == VerticalFacing.NO ? state.getValue(HORIZONTAL_FACING) : verticalFacing.getDirection();
	}

	@Override
	public Direction getHorizontalDirection(BlockState state) {
		return state.getValue(HORIZONTAL_FACING);
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return getBaseStackSizeMultiplier.get();
	}

	@Nullable
	@Override
	public StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LimitedBarrelBlockEntity(pos, state);
	}

	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get();
	}

	@Override
	protected InteractionResult tryItemInteraction(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand, Direction facing, BlockHitResult hitResult) {
		InteractionResult result = super.tryItemInteraction(player, hand, b, stackInHand, facing, hitResult);
		if (result.consumesAction()) {
			return result;
		}
		if (hitResult.getDirection() != facing || player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		int slot = getInteractionSlot(b.getBlockPos(), b.getBlockState(), hitResult);
		if (b instanceof LimitedBarrelBlockEntity limitedBarrelBlockEntity) {
			if (b.isPacked()) {
				return InteractionResult.PASS;
			} else if (limitedBarrelBlockEntity.depositItem(player, hand, stackInHand, slot)) {
				return InteractionResult.SUCCESS;
			} else if (Config.SERVER.limitedBarrelCountDyeingEnabled.getAsBoolean() && stackInHand.getItem() instanceof DyeItem dyeItem && limitedBarrelBlockEntity.applyDye(slot, stackInHand, dyeItem.getDyeColor(), player.isShiftKeyDown())) {
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean trySneakItemInteraction(Player player, InteractionHand hand, BlockState state, Level level, BlockPos pos, BlockHitResult hitVec, ItemStack itemInHand) {
		if (super.trySneakItemInteraction(player, hand, state, level, pos, hitVec, itemInHand)) {
			return true;
		}

		return tryToDyeAll(state, level, pos, hitVec, itemInHand);
	}

	public boolean tryToDyeAll(BlockState state, Level level, BlockPos pos, BlockHitResult hitVec, ItemStack itemStack) {
		if (hitVec.getDirection() != getFacing(state) || !(itemStack.getItem() instanceof DyeItem)) {
			return false;
		}
		return WorldHelper.getBlockEntity(level, pos, LimitedBarrelBlockEntity.class).map(barrel ->
				barrel.applyDye(0, itemStack, ((DyeItem) itemStack.getItem()).getDyeColor(), true)
		).orElse(false);
	}

	private int getInteractionSlot(BlockPos pos, BlockState state, BlockHitResult hitResult) {
		int invSlots = getNumberOfInventorySlots();
		if (invSlots == 1) {
			return 0;
		}

		Vector3f blockCoords = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()).toVector3f();
		blockCoords.add(-0.5f, -0.5f, -0.5f); // move to corner
		VerticalFacing verticalFacing = getVerticalFacing(state);
		if (verticalFacing != VerticalFacing.NO) {
			blockCoords.rotate(getNorthBasedRotation(state.getValue(HORIZONTAL_FACING)));
			blockCoords.rotate(getNorthBasedRotation(verticalFacing.getDirection().getOpposite()));
		} else {
			blockCoords.rotate(getNorthBasedRotation(state.getValue(HORIZONTAL_FACING).getOpposite()));
		}
		blockCoords.add(0.5f, 0.5f, 0.5f);
		boolean top = blockCoords.y() > 0.5f;
		boolean right = blockCoords.x() > 0.5f;

		if (invSlots == 2) {
			return top ? 0 : 1;
		} else if (invSlots == 3) {
			if (top) {
				return 0;
			}
			return right ? 2 : 1;
		}

		if (top) {
			return right ? 1 : 0;
		}
		return right ? 3 : 2;
	}

	@Override
	protected StorageContainerMenu instantiateContainerMenu(int w, Player pl, BlockPos pos) {
		return new LimitedBarrelContainerMenu(w, pl, pos);
	}

	public static Quaternionf getNorthBasedRotation(Direction dir) {
		return switch (dir) {
			case DOWN -> {
				Quaternionf quaternion = Axis.XP.rotationDegrees(90);
				quaternion.mul(Axis.YP.rotationDegrees(180));
				yield quaternion;
			}
			case UP -> {
				Quaternionf quaternion = Axis.XP.rotationDegrees(-90);
				quaternion.mul(Axis.YP.rotationDegrees(180));
				yield quaternion;
			}
			case NORTH -> new Quaternionf();
			case SOUTH -> Axis.YP.rotationDegrees(180.0F);
			case WEST -> Axis.YP.rotationDegrees(-90.0F);
			case EAST -> Axis.YP.rotationDegrees(90.0F);
		};
	}

	private Optional<BlockHitResult> getHitResult(Player player) {
		HitResult hitResult = player.pick(player.blockInteractionRange(), 0, false);
		return hitResult instanceof BlockHitResult blockHitResult ? Optional.of(blockHitResult) : Optional.empty();
	}

	@Override
	public void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return;
		}

		tryToTakeItem(state, level, pos, player);
	}

	public boolean tryToTakeItem(BlockState state, Level level, BlockPos pos, Player player) {
		return WorldHelper.getBlockEntity(level, pos, LimitedBarrelBlockEntity.class).map(be -> tryToTakeItem(state, level, pos, player, be)).orElse(false);
	}

	private boolean tryToTakeItem(BlockState state, Level level, BlockPos pos, Player player, LimitedBarrelBlockEntity be) {
		return getHitResult(player).map(blockHitResult -> {
			if (!blockHitResult.getBlockPos().equals(pos) || level.getBlockState(pos) != state || blockHitResult.getDirection() != getFacing(state)) {
				return false;
			}

			return !be.isPacked() && be.tryToTakeItem(player, getInteractionSlot(pos, state, blockHitResult));
		}).orElse(false);
	}

	public boolean isLookingAtFront(Player player, BlockPos pos, BlockState state) {
		return getHitResult(player).map(blockHitResult -> blockHitResult.getBlockPos().equals(pos) && blockHitResult.getDirection() == getFacing(state)).orElse(false);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		WorldHelper.getBlockEntity(level, pos, LimitedBarrelBlockEntity.class).ifPresent(be -> {
			setupDefaultSettings(be.getStorageWrapper());
			LimitedBarrelBlockEntity.setFixedSettings(be.getStorageWrapper(), be.getStorageWrapper().getNumberOfInventorySlots());
		});
	}

	public static void setupDefaultSettings(IStorageWrapper storageWrapper) {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		settingsHandler.getTypeCategory(MemorySettingsCategory.class).setIgnoreNbt(false);
	}

	@Override
	public int getDisplayItemsCount(List<RenderData.DisplayItemData> displayItems) {
		return getNumberOfInventorySlots();
	}

	@Override
	public boolean hasFixedIndexDisplayItems() {
		return true;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
	}
}
