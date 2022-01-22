package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class BarrelBlockEntity extends BlockEntity {
	public BarrelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.BARREL_TILE_TYPE.get(), pos, state);
	}

	public static void serverTick(Level l, BlockPos blockPos, BarrelBlockEntity barrelBlockEntity) {
		//TODO add calling tickable upgrades
	}
}
