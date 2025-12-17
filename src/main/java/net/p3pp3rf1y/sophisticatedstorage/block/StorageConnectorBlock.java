package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jspecify.annotations.Nullable;

public class StorageConnectorBlock extends BlockBase implements EntityBlock {
	public StorageConnectorBlock(Properties properties) {
		super(properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new StorageConnectorBlockEntity(blockPos, blockState);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		WorldHelper.getBlockEntity(level, pos, StorageConnectorBlockEntity.class).ifPresent(IControllerBoundable::addToAdjacentController);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockState ret = super.playerWillDestroy(level, pos, state, player);
		WorldHelper.getBlockEntity(level, pos, StorageConnectorBlockEntity.class).ifPresent(StorageConnectorBlockEntity::removeFromController);
		return ret;
	}
}
