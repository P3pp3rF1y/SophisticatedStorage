package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ChestBlockEntity extends WoodStorageBlockEntity {
	public static final String STORAGE_TYPE = "chest";
	private final ChestLidController chestLidController = new ChestLidController();

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.CHEST_OPEN);
		}

		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.CHEST_CLOSE);
		}

		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenCount, int openCount) {
			chestLidController.shouldBeOpen(openCount > 0);
		}

		protected boolean isOwnContainer(Player player) {
			if (player.containerMenu instanceof StorageContainerMenu storageContainerMenu) {
				return storageContainerMenu.getStorageBlockEntity() == ChestBlockEntity.this;
			} else {
				return false;
			}
		}
	};

	@Override
	protected ContainerOpenersCounter getOpenersCounter() {
		return openersCounter;
	}

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	public ChestBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get());
	}

	public static void lidAnimateTick(ChestBlockEntity chestBlockEntity) {
		chestBlockEntity.chestLidController.tickLid();
	}

	public float getOpenNess(float partialTicks) {
		return chestLidController.getOpenness(partialTicks);
	}
}
