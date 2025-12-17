package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeClientTickHandler;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeClientRegistry;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageBlockBase extends BlockBase implements IStorageBlock, ISneakItemInteractionBlock, EntityBlock {
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

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		itemConsumer.accept(new ItemStack(this));
	}

	@Nullable
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typePassedIn, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> blockEntityTicker) {
		//noinspection unchecked
		return typeExpected == typePassedIn ? (BlockEntityTicker<A>) blockEntityTicker : null;
	}

	protected void renderUpgrades(Level level, RandomSource rand, BlockPos pos, Direction facing, RenderDataHandler renderDataHandler, BlockState storageBlockState) {
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		renderDataHandler.getUpgradeClientData().forEach((type, data) -> UpgradeClientRegistry.getUpgradeClientTickHandler(type).ifPresent(renderer -> {
			if (storageBlockState.getBlock() instanceof StorageBlockBase storageBlock) {
				storageBlock.clientTickUpgrade(renderer, level, rand, pos, facing, type, data, storageBlockState, storageBlock);
			}

		}));
	}

	protected Vector3f getMiddleFacePoint(BlockState state, BlockPos pos, Direction facing, Vector3f vector) {
		Vector3f point = new Vector3f(vector);
		point.add(0, 0, 0.6f);
		point.rotate(Axis.XP.rotationDegrees(-90.0F));
		point.rotate(facing.getRotation());
		point.add(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
		return point;
	}

	private <T extends IUpgradeClientData> void clientTickUpgrade(IUpgradeClientTickHandler<T> renderer, Level level, RandomSource rand, BlockPos pos, Direction facing, UpgradeClientDataType<?> type, IUpgradeClientData data, BlockState state, StorageBlockBase storageBlock) {
		//noinspection unchecked
		type.cast(data).ifPresent(clientData -> renderer.onClientTick(level, rand, vector -> storageBlock.getMiddleFacePoint(state, pos, facing, vector), (T) clientData));
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean flag) {
		super.entityInside(state, level, pos, entity, effectApplier, flag);
		if (!level.isClientSide() && entity instanceof ItemEntity itemEntity) {
			WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> tryToPickup(level, itemEntity, be.getStorageWrapper()));
		}
	}

	protected void tryToPickup(Level level, ItemEntity itemEntity, IStorageWrapper w) {
		ItemStack stack = itemEntity.getItem();
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource resource = ItemResource.of(stack);
			int pickedUp = InventoryHelper.runPickupOnPickupResponseUpgrades(level, w.getUpgradeHandler(), resource, stack.getCount(), tx);
			if (pickedUp > 0) {
				tx.commit();
				itemEntity.setItem(resource.toStack(stack.getCount() - pickedUp));
			}
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return !level.isClientSide() && state.getValue(StorageBlockBase.TICKING) ? StorageBlockBase.createTickerHelper(blockEntityType, getBlockEntityType(), (l, blockPos, blockState, storageBlockEntity) -> StorageBlockEntity.serverTick(l, blockPos, storageBlockEntity)) : null;
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

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos, Direction direction) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> InventoryHelper.getAnalogOutputSignal(be.getStorageWrapper().getInventoryHandler())).orElse(0);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		level.updateNeighbourForOutputSignal(pos, this);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		super.playerWillDestroy(level, pos, state, player);
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(IControllableStorage::removeFromController);
		return state;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			RenderDataHandler renderDataHandler = be.getStorageWrapper().getRenderDataHandler();
			renderUpgrades(level, rand, pos, getFacing(state), renderDataHandler, state);
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

	public int getDisplayItemsCount(List<RenderData.DisplayItemData> displayItems) {
		return displayItems.size();
	}

	public boolean hasFixedIndexDisplayItems() {
		return false;
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> be.onNeighborChange(neighbor));
	}

	protected InteractionResult tryAddUpgrade(Player player, StorageBlockEntity b, ItemStack itemInHand, Direction facing, BlockHitResult hitResult) {
		if (hitResult.getDirection() != facing) {
			return InteractionResult.PASS;
		}

		return tryAddSingleUpgrade(player, b, itemInHand);
	}

	public InteractionResult tryAddSingleUpgrade(Player player, StorageBlockEntity b, ItemStack itemInHand) {
		return tryAddSingleUpgrade(player, itemInHand, b.getStorageWrapper());
	}

	public static InteractionResult tryAddSingleUpgrade(Player player, ItemStack itemInHand, IStorageWrapper storageWrapper) {
		if (itemInHand.getItem() instanceof UpgradeItemBase<?> upgradeItem && itemInHand.is(ModItems.STORAGE_UPGRADE_TAG)) {
			if (player.level().isClientSide()) {
				return InteractionResult.PASS;
			}

			UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
			if (upgradeItem.canAddUpgradeTo(storageWrapper, itemInHand, true, player.level().isClientSide()).successful()) {
				try (Transaction tx = Transaction.openRoot()) {
					if (InventoryHelper.insertIntoInventory(List.of(itemInHand.copyWithCount(1)), upgradeHandler, tx).isEmpty()) {
						itemInHand.shrink(1);
						tx.commit();
						return InteractionResult.SUCCESS.heldItemTransformedTo(itemInHand.isEmpty() ? ItemStack.EMPTY : itemInHand);
					}
				}
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public boolean trySneakItemInteraction(Player player, InteractionHand hand, BlockState state, Level level, BlockPos pos, BlockHitResult hitVec, ItemStack itemInHand) {
		if (level.isClientSide() || hitVec.getDirection() != getFacing(state)) {
			return false;
		}

		return tryFillUpgrades(player, hand, level, pos, itemInHand);
	}

	public boolean tryFillUpgrades(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack itemInHand) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(b -> {
			boolean result = false;
			while (!itemInHand.isEmpty()) {
				if (tryAddSingleUpgrade(player, b, itemInHand).consumesAction()) {
					result = true;
				} else {
					break;
				}
			}
			return result;
		}).orElse(false);
	}

	@SuppressWarnings("java:S1172") // Parameter used in overrides
	public List<BlockPos> getNeighborPos(BlockState state, BlockPos origin, Direction facing) {
		return List.of(origin.relative(facing));
	}
}
