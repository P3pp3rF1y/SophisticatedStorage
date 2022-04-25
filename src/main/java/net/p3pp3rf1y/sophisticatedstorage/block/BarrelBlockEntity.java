package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class BarrelBlockEntity extends StorageBlockEntity{
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_OPEN);
			updateBlockState(state, true);
		}

		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_CLOSE);
			updateBlockState(state, false);
		}

		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenerCount, int newOpenerCount) {
			//noop
		}

		protected boolean isOwnContainer(Player player) {
			if (player.containerMenu instanceof StorageContainerMenu storageContainerMenu) {
				return storageContainerMenu.getStorageBlockEntity() == BarrelBlockEntity.this;
			} else {
				return false;
			}
		}
	};

	@Override
	protected ContainerOpenersCounter getOpenersCounter() {
		return openersCounter;
	}

	public BarrelBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get());
	}

	void updateBlockState(BlockState state, boolean open) {
		if (level == null) {
			return;
		}
		level.setBlock(getBlockPos(), state.setValue(BarrelBlock.OPEN, open), 3);
	}
}
