package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface INeighborChangeListenerUpgrade {
	void onNeighborChange(Level level, BlockPos pos, Direction direction);
}
