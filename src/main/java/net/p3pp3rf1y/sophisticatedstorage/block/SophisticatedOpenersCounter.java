package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Iterator;
import java.util.List;

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
		maxInteractionRange = Math.max(player.blockInteractionRange(), maxInteractionRange);
	}

	@Override
	public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
		List<Player> list = getPlayersWithContainerOpen(level, pos);
		maxInteractionRange = 0.0;

		Player player;
		for(Iterator<Player> it = list.iterator(); it.hasNext(); maxInteractionRange = Math.max(player.blockInteractionRange(), maxInteractionRange)) {
			player = it.next();
		}

		int i = list.size();
		int j = openCount;
		if (j != i) {
			boolean flag = i != 0;
			boolean flag1 = j != 0;
			if (flag && !flag1) {
				onOpen(level, pos, state);
				if (isForPhysicalBlock) {
					level.gameEvent(null, GameEvent.CONTAINER_OPEN, pos);
				}
			} else if (!flag) {
				onClose(level, pos, state);
				if (isForPhysicalBlock) {
					level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
				}
			}

			openCount = i;
		}

		openerCountChanged(level, pos, state, j, i);
		if (isForPhysicalBlock && i > 0) {
			scheduleRecheck(level, pos, state);
		}

	}
}
