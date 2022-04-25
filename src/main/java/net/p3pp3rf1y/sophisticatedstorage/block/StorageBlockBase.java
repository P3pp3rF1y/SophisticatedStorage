package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeRenderRegistry;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeRenderDataType;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class StorageBlockBase extends Block implements IStorageBlock, EntityBlock {
	public static final BooleanProperty TICKING = BooleanProperty.create("ticking");
	protected final int numberOfInventorySlots;
	protected final int numberOfUpgradeSlots;

	protected StorageBlockBase(Properties properties, int numberOfInventorySlots, int numberOfUpgradeSlots) {
		super(properties);
		this.numberOfInventorySlots = numberOfInventorySlots;
		this.numberOfUpgradeSlots = numberOfUpgradeSlots;
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
		return numberOfInventorySlots;
	}

	@Override
	public int getNumberOfUpgradeSlots() {
		return numberOfUpgradeSlots;
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
				if (shouldDropContents()) {
					b.dropContents();
				}
				level.updateNeighbourForOutputSignal(pos, this);
			});
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}

	protected boolean shouldDropContents() {
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, Random rand) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			RenderInfo renderInfo = be.getStorageWrapper().getRenderInfo();
			renderUpgrades(level, rand, pos, getFacing(state), renderInfo);
		});

	}

	protected abstract Direction getFacing(BlockState state);
}
