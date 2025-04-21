package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public abstract class SophisticatedOpenersCounter extends ContainerOpenersCounter {
	private boolean isForPhysicalBlock = true;

	public void setForPhysicalBlock(boolean isForPhysicalBlock) {
		this.isForPhysicalBlock = isForPhysicalBlock;
	}

	@Override
	public void incrementOpeners(Player player, Level level, BlockPos pos, BlockState state) {
		int i = openCount++;
		if (i == 0) {
			onOpen(level, pos, state);
			if (isForPhysicalBlock) {
				level.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
				scheduleRecheck(level, pos, state);
			}
		}

		openerCountChanged(level, pos, state, i, openCount);
	}

	@Override
	public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
		int i = this.getOpenCount(level, pos);
		int j = this.openCount;
		if (j != i) {
			boolean flag = i != 0;
			boolean flag1 = j != 0;
			if (flag && !flag1) {
				this.onOpen(level, pos, state);
				if (isForPhysicalBlock) {
					level.gameEvent((Entity) null, GameEvent.CONTAINER_OPEN, pos);
				}
			} else if (!flag) {
				this.onClose(level, pos, state);
				if (isForPhysicalBlock) {
					level.gameEvent((Entity) null, GameEvent.CONTAINER_CLOSE, pos);
				}
			}

			this.openCount = i;
		}

		this.openerCountChanged(level, pos, state, j, i);
		if (isForPhysicalBlock && i > 0) {
			scheduleRecheck(level, pos, state);
		}
	}
}
