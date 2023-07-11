package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.List;

public class ShulkerBoxBlockEntity extends StorageBlockEntity {
	private AnimationStatus animationStatus = AnimationStatus.CLOSED;
	private float progress;
	private float progressOld;
	public final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			animationStatus = AnimationStatus.OPENING;
			playSound(state, SoundEvents.SHULKER_BOX_OPEN);
			//noinspection ConstantConditions
			doNeighborUpdates(getLevel(), worldPosition, getBlockState());
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			animationStatus = AnimationStatus.CLOSING;
			playSound(state, SoundEvents.SHULKER_BOX_CLOSE);
			//noinspection ConstantConditions
			doNeighborUpdates(getLevel(), worldPosition, getBlockState());
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenCount, int openCount) {
			//noop
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return false;
		}
	};

	public ShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ShulkerBoxBlockEntity blockEntity) {
		blockEntity.updateAnimation(level, pos, state);
	}

	@SuppressWarnings("java:S1121")
	private void updateAnimation(Level level, BlockPos pos, BlockState state) {
		progressOld = progress;
		switch (animationStatus) {
			case CLOSED -> progress = 0.0F;
			case OPENING -> {
				progress += 0.1F;
				if (progress >= 1.0F) {
					animationStatus = AnimationStatus.OPENED;
					progress = 1.0F;
					doNeighborUpdates(level, pos, state);
				}
				moveCollidedEntities(level, pos, state);
			}
			case CLOSING -> {
				progress -= 0.1F;
				if (progress <= 0.0F) {
					animationStatus = AnimationStatus.CLOSED;
					progress = 0.0F;
					doNeighborUpdates(level, pos, state);
				}
			}
			case OPENED -> progress = 1.0F;
		}
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) && !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(stack.getItem());
	}

	private static void doNeighborUpdates(Level level, BlockPos pos, BlockState state) {
		state.updateNeighbourShapes(level, pos, 3);
	}

	public ShulkerBoxBlockEntity.AnimationStatus getAnimationStatus() {
		return animationStatus;
	}

	public AABB getBoundingBox(BlockState state) {
		return Shulker.getProgressAabb(state.getValue(ShulkerBoxBlock.FACING), 0.5F * getProgress(1.0F));
	}

	private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof ShulkerBoxBlock) {
			Direction direction = state.getValue(ShulkerBoxBlock.FACING);
			AABB aabb = Shulker.getProgressDeltaAabb(direction, progressOld, progress).move(pos);
			List<Entity> list = level.getEntities(null, aabb);
			if (!list.isEmpty()) {
				for (Entity entity : list) {
					if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
						entity.move(MoverType.SHULKER_BOX, new Vec3((aabb.getXsize() + 0.01D) * direction.getStepX(), (aabb.getYsize() + 0.01D) * direction.getStepY(), (aabb.getZsize() + 0.01D) * direction.getStepZ()));
					}
				}
			}
		}
	}

	@Override
	protected ContainerOpenersCounter getOpenersCounter() {
		return openersCounter;
	}

	public float getProgress(float partialTicks) {
		return Mth.lerp(partialTicks, progressOld, progress);
	}

	public boolean isClosed() {
		return animationStatus == AnimationStatus.CLOSED;
	}

	@Override
	public boolean shouldDropContents() {
		return false;
	}

	public enum AnimationStatus {
		CLOSED,
		OPENING,
		OPENED,
		CLOSING
	}
}
