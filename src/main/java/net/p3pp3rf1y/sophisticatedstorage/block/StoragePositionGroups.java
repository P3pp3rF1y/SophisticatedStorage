package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StoragePositionGroups {
	private static final Comparator<BlockPos> BLOCK_POS_COMPARATOR = Comparator.comparingLong(BlockPos::asLong);

	private StoragePositionGroups() {
	}

	public static StoragePositionGroup getGroup(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, ChestBlockEntity.class)
				.map(StoragePositionGroups::getChestGroup)
				.orElseGet(() -> singleton(pos));
	}

	public static List<List<BlockPos>> getGroupPositions(Level level, Collection<BlockPos> positions) {
		return new ArrayList<>(getGroups(level, positions).values());
	}

	public static Map<BlockPos, List<BlockPos>> getGroups(Level level, Collection<BlockPos> positions) {
		Map<BlockPos, List<BlockPos>> groups = new LinkedHashMap<>();
		positions.forEach(pos -> {
			StoragePositionGroup group = getGroup(level, pos);
			groups.putIfAbsent(group.anchorPos(), group.memberPositions());
		});
		return groups;
	}

	public static Vec3 getCenter(Level level, BlockPos pos) {
		return getCenter(getGroup(level, pos).memberPositions());
	}

	private static StoragePositionGroup getChestGroup(ChestBlockEntity chestBlockEntity) {
		BlockPos mainPos = chestBlockEntity.getMainPos();
		if (chestBlockEntity.getLevel() == null) {
			return singleton(mainPos);
		}

		return WorldHelper.getBlockEntity(chestBlockEntity.getLevel(), mainPos, ChestBlockEntity.class)
				.map(mainChest -> {
					List<BlockPos> positions = new ArrayList<>();
					positions.add(mainPos);
					if (mainChest.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
						positions.add(mainPos.relative(ChestBlock.getConnectedDirection(mainChest.getBlockState())));
					}
					return new StoragePositionGroup(mainPos, positions.stream().sorted(BLOCK_POS_COMPARATOR).toList());
				})
				.orElseGet(() -> singleton(mainPos));
	}

	private static StoragePositionGroup singleton(BlockPos pos) {
		return new StoragePositionGroup(pos, List.of(pos));
	}

	private static Vec3 getCenter(List<BlockPos> positions) {
		BlockPos min = positions.get(0);
		BlockPos max = positions.get(0);
		for (BlockPos pos : positions) {
			min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
			max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
		}
		return new Vec3((min.getX() + max.getX() + 1) / 2D, (min.getY() + max.getY() + 1) / 2D, (min.getZ() + max.getZ() + 1) / 2D);
	}

	public record StoragePositionGroup(BlockPos anchorPos, List<BlockPos> memberPositions) {
	}
}
