package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class LimitedBarrelBlock extends BarrelBlock {
	public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<VerticalFacing> VERTICAL_FACING = EnumProperty.create("vertical_facing", VerticalFacing.class);
	private final Supplier<Integer> getBaseStackSizeMultiplier;

	public LimitedBarrelBlock(int numberOfInventorySlots, Supplier<Integer> getBaseStackSizeMultiplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, Properties properties) {
		super(() -> numberOfInventorySlots, numberOfUpgradeSlotsSupplier, properties,
				stateDef -> stateDef.any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(VERTICAL_FACING, VerticalFacing.NO).setValue(TICKING, false).setValue(FLAT_TOP, false)
		);
		this.getBaseStackSizeMultiplier = getBaseStackSizeMultiplier;
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

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flat) {
		int numberOfInventorySlots = getNumberOfInventorySlots();
		String translationKey = numberOfInventorySlots == 1 ? "limited_barrel_singular" : "limited_barrel_plural";
		tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translBlockTooltipKey(translationKey), String.valueOf(numberOfInventorySlots), String.valueOf(getBaseStackSizeMultiplier())).withStyle(ChatFormatting.DARK_GRAY));
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
	protected boolean tryItemInteraction(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand, Direction facing, BlockHitResult hitResult) {
		if (super.tryItemInteraction(player, hand, b, stackInHand, facing, hitResult)) {
			return true;
		}
		if (hitResult.getDirection() != facing || player.isShiftKeyDown()) {
			return false;
		}
		int slot = getInteractionSlot(b.getBlockPos(), b.getBlockState(), hitResult);
		if (b instanceof LimitedBarrelBlockEntity limitedBarrelBlockEntity) {
			if (b.isPacked()) {
				return false;
			} else if (limitedBarrelBlockEntity.depositItem(player, hand, stackInHand, slot)) {
				return true;
			} else if (stackInHand.getItem() instanceof DyeItem dyeItem && limitedBarrelBlockEntity.applyDye(slot, stackInHand, dyeItem.getDyeColor(), player.isShiftKeyDown())) {
				return true;
			}
		}
		return true;
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

		Vector3f blockCoords = new Vector3f(hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
		blockCoords.add(-0.5f, -0.5f, -0.5f); // move to corner
		VerticalFacing verticalFacing = getVerticalFacing(state);
		if (verticalFacing != VerticalFacing.NO) {
			blockCoords.transform(getNorthBasedRotation(state.getValue(HORIZONTAL_FACING)));
			blockCoords.transform(getNorthBasedRotation(verticalFacing.getDirection().getOpposite()));
		} else {
			blockCoords.transform(getNorthBasedRotation(state.getValue(HORIZONTAL_FACING).getOpposite()));
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

	public static Quaternion getNorthBasedRotation(Direction dir) {
		return switch (dir) {
			case DOWN -> {
				Quaternion quaternion = Vector3f.XP.rotationDegrees(90);
				quaternion.mul(Vector3f.YP.rotationDegrees(180));
				yield quaternion;
			}
			case UP -> {
				Quaternion quaternion = Vector3f.XP.rotationDegrees(-90);
				quaternion.mul(Vector3f.YP.rotationDegrees(180));
				yield quaternion;
			}
			case NORTH -> Quaternion.ONE.copy();
			case SOUTH -> Vector3f.YP.rotationDegrees(180.0F);
			case WEST -> Vector3f.YP.rotationDegrees(-90.0F);
			case EAST -> Vector3f.YP.rotationDegrees(90.0F);
		};
	}

	private Optional<BlockHitResult> getHitResult(Player player) {
		HitResult hitResult = player.pick(player.getReachDistance(), 0, false);
		return hitResult instanceof BlockHitResult blockHitResult ? Optional.of(blockHitResult) : Optional.empty();
	}

	@SuppressWarnings({"deprecation"})
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
			StorageWrapper storageWrapper = be.getStorageWrapper();
			SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
			settingsHandler.getTypeCategory(ItemDisplaySettingsCategory.class).selectSlots(0, storageWrapper.getNumberOfInventorySlots());
			settingsHandler.getTypeCategory(NoSortSettingsCategory.class).selectSlots(0, storageWrapper.getNumberOfInventorySlots());
			settingsHandler.getTypeCategory(MemorySettingsCategory.class).setIgnoreNbt(false);
		});
	}

	@Override
	public int getDisplayItemsCount(List<RenderInfo.DisplayItem> displayItems) {
		return getNumberOfInventorySlots();
	}

	@Override
	public boolean hasFixedIndexDisplayItems() {
		return true;
	}
}
