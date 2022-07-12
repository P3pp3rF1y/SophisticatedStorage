package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ControllerBlockEntity extends ControllerBlockEntityBase {
	public ControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), pos, state);
	}
}
