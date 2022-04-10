package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IStorageBlock {
	int getNumberOfInventorySlots();

	int getNumberOfUpgradeSlots();

	void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking);
}
