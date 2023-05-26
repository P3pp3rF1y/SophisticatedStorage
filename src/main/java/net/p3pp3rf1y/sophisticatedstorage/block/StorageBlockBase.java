package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeRenderRegistry;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeRenderDataType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public abstract class StorageBlockBase extends Block implements IStorageBlock, ISneakItemInteractionBlock, EntityBlock {
	public static final BooleanProperty TICKING = BooleanProperty.create("ticking");
	protected final Supplier<Integer> numberOfInventorySlotsSupplier;
	protected final Supplier<Integer> numberOfUpgradeSlotsSupplier;

	protected StorageBlockBase(Properties properties, Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier) {
		super(properties);
		this.numberOfInventorySlotsSupplier = numberOfInventorySlotsSupplier;
		this.numberOfUpgradeSlotsSupplier = numberOfUpgradeSlotsSupplier;
	}

	@Override
	public abstract StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state);

	@Nullable
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typePassedIn, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> blockEntityTicker) {
		//noinspection unchecked
		return typeExpected == typePassedIn ? (BlockEntityTicker<A>) blockEntityTicker : null;
	}

	protected static void renderUpgrades(Level level, Random rand, BlockPos pos, Direction facing, RenderInfo renderInfo) {
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		renderInfo.getUpgradeRenderData().forEach((type, data) -> UpgradeRenderRegistry.getUpgradeRenderer(type).ifPresent(renderer -> StorageBlockBase.renderUpgrade(renderer, level, rand, pos, facing, type, data)));
	}

	private static Vector3f getMiddleFacePoint(BlockPos pos, Direction facing, Vector3f vector) {
		Vector3f point = vector.copy();
		point.add(0, 0, 0.6f);
		point.transform(Vector3f.XP.rotationDegrees(-90.0F));
		point.transform(facing.getRotation());
		point.add(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
		return point;
	}

	private static <T extends IUpgradeRenderData> void renderUpgrade(IUpgradeRenderer<T> renderer, Level level, Random rand, BlockPos pos, Direction facing, UpgradeRenderDataType<?> type, IUpgradeRenderData data) {
		//noinspection unchecked
		type.cast(data).ifPresent(renderData -> renderer.render(level, rand, vector -> StorageBlockBase.getMiddleFacePoint(pos, facing, vector), (T) renderData));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
		super.entityInside(state, world, pos, entity);
		if (!world.isClientSide && entity instanceof ItemEntity itemEntity) {
			WorldHelper.getBlockEntity(world, pos, StorageBlockEntity.class).ifPresent(te -> tryToPickup(world, itemEntity, te.getStorageWrapper()));
		}
	}

	private void tryToPickup(Level world, ItemEntity itemEntity, IStorageWrapper w) {
		ItemStack remainingStack = itemEntity.getItem().copy();
		remainingStack = InventoryHelper.runPickupOnPickupResponseUpgrades(world, w.getUpgradeHandler(), remainingStack, false);
		if (remainingStack.getCount() < itemEntity.getItem().getCount()) {
			itemEntity.setItem(remainingStack);
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return !level.isClientSide && Boolean.TRUE.equals(state.getValue(StorageBlockBase.TICKING)) ? StorageBlockBase.createTickerHelper(blockEntityType, getBlockEntityType(), (l, blockPos, blockState, storageBlockEntity) -> StorageBlockEntity.serverTick(l, blockPos, storageBlockEntity)) : null;
	}

	protected abstract BlockEntityType<? extends StorageBlockEntity> getBlockEntityType();

	@Override
	public int getNumberOfInventorySlots() {
		return numberOfInventorySlotsSupplier.get();
	}

	@Override
	public int getNumberOfUpgradeSlots() {
		return numberOfUpgradeSlotsSupplier.get();
	}

	@Override
	public void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking) {
		level.setBlockAndUpdate(pos, currentState.setValue(StorageBlockBase.TICKING, ticking));
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> InventoryHelper.getAnalogOutputSignal(be.getStorageWrapper().getInventoryForInputOutput())).orElse(0);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(b -> {
				b.removeFromController();
				if (b.shouldDropContents()) {
					b.dropContents();
				}
				level.updateNeighbourForOutputSignal(pos, this);
			});
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
		super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
		WorldHelper.getBlockEntity(pLevel, pPos, StorageBlockEntity.class).ifPresent(IControllableStorage::removeFromController);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, Random rand) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			RenderInfo renderInfo = be.getStorageWrapper().getRenderInfo();
			renderUpgrades(level, rand, pos, getFacing(state), renderInfo);
		});

	}

	@SuppressWarnings("java:S1172") // Used in overrides
	public VerticalFacing getVerticalFacing(BlockState state) {
		Direction facing = getFacing(state);
		if (facing == Direction.UP) {
			return VerticalFacing.UP;
		} else if (facing == Direction.DOWN) {
			return VerticalFacing.DOWN;
		}

		return VerticalFacing.NO;
	}

	public Direction getHorizontalDirection(BlockState state) {
		Direction facing = getFacing(state);

		if (facing == Direction.UP) {
			return Direction.NORTH;
		} else if (facing == Direction.DOWN) {
			return Direction.SOUTH;
		}

		return facing;
	}

	public abstract Direction getFacing(BlockState state);

	public int getDisplayItemsCount(List<RenderInfo.DisplayItem> displayItems) {
		return displayItems.size();
	}

	public boolean hasFixedIndexDisplayItems() {
		return false;
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> be.onNeighborChange(neighbor));
	}

	protected boolean tryAddUpgrade(Player player, InteractionHand hand, StorageBlockEntity b, ItemStack itemInHand, Direction facing, BlockHitResult hitResult) {
		if (player.getLevel().isClientSide) {
			return true;
		}

		if (hitResult.getDirection() != facing) {
			return false;
		}

		return tryAddSingleUpgrade(player, hand, b, itemInHand);
	}

	private static boolean isStorageUpgrade(ItemStack itemInHand) {
		return itemInHand.getItem() instanceof UpgradeItemBase<?> upgradeItem && RegistryHelper.getRegistryName(upgradeItem).map(r -> r.getNamespace().equals(SophisticatedStorage.MOD_ID)).orElse(false);
	}

	public boolean tryAddSingleUpgrade(Player player, InteractionHand hand, StorageBlockEntity b, ItemStack itemInHand) {
		if (isStorageUpgrade(itemInHand)) {
			UpgradeHandler upgradeHandler = b.getStorageWrapper().getUpgradeHandler();
			if (InventoryHelper.insertIntoInventory(itemInHand, upgradeHandler, true).getCount() != itemInHand.getCount()) {
				InventoryHelper.insertIntoInventory(ItemHandlerHelper.copyStackWithSize(itemInHand, 1), upgradeHandler, false);
				itemInHand.shrink(1);
				if (itemInHand.isEmpty()) {
					player.setItemInHand(hand, ItemStack.EMPTY);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean trySneakItemInteraction(Player player, InteractionHand hand, BlockState state, Level level, BlockPos pos, BlockHitResult hitVec, ItemStack itemInHand) {


		if (hitVec.getDirection() != getFacing(state)) {
			return false;
		}

		return tryFillUpgrades(player, hand, level, pos, itemInHand);
	}

	public boolean tryFillUpgrades(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack itemInHand) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(b -> {
			boolean result = false;
			while (!itemInHand.isEmpty()) {
				if (tryAddSingleUpgrade(player, hand, b, itemInHand)) {
					result = true;
				} else {
					break;
				}
			}
			return result;
		}).orElse(false);
	}
}
