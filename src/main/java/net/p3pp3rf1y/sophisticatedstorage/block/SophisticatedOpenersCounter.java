package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
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
	public void incrementOpeners(LivingEntity livingEntity, Level level, BlockPos pos, BlockState state, double containerInteractionRange) {
		int i = openCount++;
		if (i == 0) {
			onOpen(level, pos, state);
			if (isForPhysicalBlock) {
				level.gameEvent(livingEntity, GameEvent.CONTAINER_OPEN, pos);
				scheduleRecheck(level, pos, state);
			}
		}

		openerCountChanged(level, pos, state, i, openCount);
		maxInteractionRange = Math.max(containerInteractionRange, maxInteractionRange);
	}

	@Override
	public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
		List<ContainerUser> list = getEntitiesWithContainerOpen(level, pos);
		maxInteractionRange = 0.0;

		ContainerUser containerUser;
		for (Iterator<ContainerUser> it = list.iterator(); it
				.hasNext(); maxInteractionRange = Math.max(containerUser.getContainerInteractionRange(), maxInteractionRange)) {
			containerUser = it.next();
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
