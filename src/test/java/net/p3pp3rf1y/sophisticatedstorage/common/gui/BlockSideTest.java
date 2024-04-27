package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.Direction;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

class BlockSideTest {
	@ParameterizedTest
	@MethodSource("fromDirectionConvertsCorrectly")
	void fromDirectionConvertsCorrectly(FromDirectionParams fromDirectionParams) {
		Assertions.assertEquals(fromDirectionParams.expectedSide, BlockSide.fromDirection(fromDirectionParams.direction, fromDirectionParams.baseHorizontalDirection, fromDirectionParams.baseVerticalFacing));
	}

	record FromDirectionParams(Direction direction, Direction baseHorizontalDirection, VerticalFacing baseVerticalFacing, BlockSide expectedSide) {}

	private static List<FromDirectionParams> fromDirectionConvertsCorrectly() {
		return List.of(
				new FromDirectionParams(Direction.UP, Direction.NORTH, VerticalFacing.NO, BlockSide.TOP),
				new FromDirectionParams(Direction.DOWN, Direction.NORTH, VerticalFacing.NO, BlockSide.BOTTOM),
				new FromDirectionParams(Direction.NORTH, Direction.NORTH, VerticalFacing.NO, BlockSide.FRONT),
				new FromDirectionParams(Direction.SOUTH, Direction.NORTH, VerticalFacing.NO, BlockSide.BACK),
				new FromDirectionParams(Direction.WEST, Direction.NORTH, VerticalFacing.NO, BlockSide.RIGHT),
				new FromDirectionParams(Direction.EAST, Direction.NORTH, VerticalFacing.NO, BlockSide.LEFT),

				new FromDirectionParams(Direction.UP, Direction.EAST, VerticalFacing.NO, BlockSide.TOP),
				new FromDirectionParams(Direction.DOWN, Direction.EAST, VerticalFacing.NO, BlockSide.BOTTOM),
				new FromDirectionParams(Direction.NORTH, Direction.EAST, VerticalFacing.NO, BlockSide.RIGHT),
				new FromDirectionParams(Direction.SOUTH, Direction.EAST, VerticalFacing.NO, BlockSide.LEFT),
				new FromDirectionParams(Direction.WEST, Direction.EAST, VerticalFacing.NO, BlockSide.BACK),
				new FromDirectionParams(Direction.EAST, Direction.EAST, VerticalFacing.NO, BlockSide.FRONT),

				new FromDirectionParams(Direction.UP, Direction.NORTH, VerticalFacing.UP, BlockSide.FRONT),
				new FromDirectionParams(Direction.DOWN, Direction.NORTH, VerticalFacing.UP, BlockSide.BACK),
				new FromDirectionParams(Direction.NORTH, Direction.NORTH, VerticalFacing.UP, BlockSide.BOTTOM),
				new FromDirectionParams(Direction.SOUTH, Direction.NORTH, VerticalFacing.UP, BlockSide.TOP),
				new FromDirectionParams(Direction.WEST, Direction.NORTH, VerticalFacing.UP, BlockSide.RIGHT),
				new FromDirectionParams(Direction.EAST, Direction.NORTH, VerticalFacing.UP, BlockSide.LEFT),

				new FromDirectionParams(Direction.UP, Direction.EAST, VerticalFacing.UP, BlockSide.FRONT),
				new FromDirectionParams(Direction.DOWN, Direction.EAST, VerticalFacing.UP, BlockSide.BACK),
				new FromDirectionParams(Direction.NORTH, Direction.EAST, VerticalFacing.UP, BlockSide.RIGHT),
				new FromDirectionParams(Direction.SOUTH, Direction.EAST, VerticalFacing.UP, BlockSide.LEFT),
				new FromDirectionParams(Direction.WEST, Direction.EAST, VerticalFacing.UP, BlockSide.TOP),
				new FromDirectionParams(Direction.EAST, Direction.EAST, VerticalFacing.UP, BlockSide.BOTTOM),

				new FromDirectionParams(Direction.UP, Direction.NORTH, VerticalFacing.DOWN, BlockSide.BACK),
				new FromDirectionParams(Direction.DOWN, Direction.NORTH, VerticalFacing.DOWN, BlockSide.FRONT),
				new FromDirectionParams(Direction.NORTH, Direction.NORTH, VerticalFacing.DOWN, BlockSide.TOP),
				new FromDirectionParams(Direction.SOUTH, Direction.NORTH, VerticalFacing.DOWN, BlockSide.BOTTOM),
				new FromDirectionParams(Direction.WEST, Direction.NORTH, VerticalFacing.DOWN, BlockSide.RIGHT),
				new FromDirectionParams(Direction.EAST, Direction.NORTH, VerticalFacing.DOWN, BlockSide.LEFT),

				new FromDirectionParams(Direction.UP, Direction.EAST, VerticalFacing.DOWN, BlockSide.BACK),
				new FromDirectionParams(Direction.DOWN, Direction.EAST, VerticalFacing.DOWN, BlockSide.FRONT),
				new FromDirectionParams(Direction.NORTH, Direction.EAST, VerticalFacing.DOWN, BlockSide.RIGHT),
				new FromDirectionParams(Direction.SOUTH, Direction.EAST, VerticalFacing.DOWN, BlockSide.LEFT),
				new FromDirectionParams(Direction.WEST, Direction.EAST, VerticalFacing.DOWN, BlockSide.BOTTOM),
				new FromDirectionParams(Direction.EAST, Direction.EAST, VerticalFacing.DOWN, BlockSide.TOP)
		);
	}


	@ParameterizedTest
	@MethodSource("toDirectionConvertsCorrectly")
	void toDirectionConvertsCorrectly(ToDirectionParams toDirectionParams) {
		Assertions.assertEquals(toDirectionParams.expectedDirection, toDirectionParams.side.toDirection(toDirectionParams.baseHorizontalDirection, toDirectionParams.baseVerticalFacing));
	}

	private record ToDirectionParams(BlockSide side, Direction baseHorizontalDirection, VerticalFacing baseVerticalFacing, Direction expectedDirection) {}

	private static List<ToDirectionParams> toDirectionConvertsCorrectly() {
		return List.of(
				new ToDirectionParams(BlockSide.TOP, Direction.NORTH, VerticalFacing.NO, Direction.UP),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.NORTH, VerticalFacing.NO, Direction.DOWN),
				new ToDirectionParams(BlockSide.FRONT, Direction.NORTH, VerticalFacing.NO, Direction.NORTH),
				new ToDirectionParams(BlockSide.BACK, Direction.NORTH, VerticalFacing.NO, Direction.SOUTH),
				new ToDirectionParams(BlockSide.RIGHT, Direction.NORTH, VerticalFacing.NO, Direction.WEST),
				new ToDirectionParams(BlockSide.LEFT, Direction.NORTH, VerticalFacing.NO, Direction.EAST),

				new ToDirectionParams(BlockSide.TOP, Direction.EAST, VerticalFacing.NO, Direction.UP),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.EAST, VerticalFacing.NO, Direction.DOWN),
				new ToDirectionParams(BlockSide.FRONT, Direction.EAST, VerticalFacing.NO, Direction.EAST),
				new ToDirectionParams(BlockSide.BACK, Direction.EAST, VerticalFacing.NO, Direction.WEST),
				new ToDirectionParams(BlockSide.RIGHT, Direction.EAST, VerticalFacing.NO, Direction.NORTH),
				new ToDirectionParams(BlockSide.LEFT, Direction.EAST, VerticalFacing.NO, Direction.SOUTH),

				new ToDirectionParams(BlockSide.TOP, Direction.UP, VerticalFacing.NO, Direction.SOUTH),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.UP, VerticalFacing.NO, Direction.NORTH),
				new ToDirectionParams(BlockSide.FRONT, Direction.UP, VerticalFacing.NO, Direction.UP),
				new ToDirectionParams(BlockSide.BACK, Direction.UP, VerticalFacing.NO, Direction.DOWN),
				new ToDirectionParams(BlockSide.RIGHT, Direction.UP, VerticalFacing.NO, Direction.WEST),
				new ToDirectionParams(BlockSide.LEFT, Direction.UP, VerticalFacing.NO, Direction.EAST),

				new ToDirectionParams(BlockSide.TOP, Direction.DOWN, VerticalFacing.NO, Direction.NORTH),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.DOWN, VerticalFacing.NO, Direction.SOUTH),
				new ToDirectionParams(BlockSide.FRONT, Direction.DOWN, VerticalFacing.NO, Direction.DOWN),
				new ToDirectionParams(BlockSide.BACK, Direction.DOWN, VerticalFacing.NO, Direction.UP),
				new ToDirectionParams(BlockSide.RIGHT, Direction.DOWN, VerticalFacing.NO, Direction.WEST),
				new ToDirectionParams(BlockSide.LEFT, Direction.DOWN, VerticalFacing.NO, Direction.EAST),

				new ToDirectionParams(BlockSide.TOP, Direction.NORTH, VerticalFacing.UP, Direction.SOUTH),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.NORTH, VerticalFacing.UP, Direction.NORTH),
				new ToDirectionParams(BlockSide.FRONT, Direction.NORTH, VerticalFacing.UP, Direction.UP),
				new ToDirectionParams(BlockSide.BACK, Direction.NORTH, VerticalFacing.UP, Direction.DOWN),
				new ToDirectionParams(BlockSide.RIGHT, Direction.NORTH, VerticalFacing.UP, Direction.WEST),
				new ToDirectionParams(BlockSide.LEFT, Direction.NORTH, VerticalFacing.UP, Direction.EAST),

				new ToDirectionParams(BlockSide.TOP, Direction.EAST, VerticalFacing.UP, Direction.WEST),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.EAST, VerticalFacing.UP, Direction.EAST),
				new ToDirectionParams(BlockSide.FRONT, Direction.EAST, VerticalFacing.UP, Direction.UP),
				new ToDirectionParams(BlockSide.BACK, Direction.EAST, VerticalFacing.UP, Direction.DOWN),
				new ToDirectionParams(BlockSide.RIGHT, Direction.EAST, VerticalFacing.UP, Direction.NORTH),
				new ToDirectionParams(BlockSide.LEFT, Direction.EAST, VerticalFacing.UP, Direction.SOUTH),

				new ToDirectionParams(BlockSide.TOP, Direction.NORTH, VerticalFacing.DOWN, Direction.NORTH),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.NORTH, VerticalFacing.DOWN, Direction.SOUTH),
				new ToDirectionParams(BlockSide.FRONT, Direction.NORTH, VerticalFacing.DOWN, Direction.DOWN),
				new ToDirectionParams(BlockSide.BACK, Direction.NORTH, VerticalFacing.DOWN, Direction.UP),
				new ToDirectionParams(BlockSide.RIGHT, Direction.NORTH, VerticalFacing.DOWN, Direction.WEST),
				new ToDirectionParams(BlockSide.LEFT, Direction.NORTH, VerticalFacing.DOWN, Direction.EAST),

				new ToDirectionParams(BlockSide.TOP, Direction.EAST, VerticalFacing.DOWN, Direction.EAST),
				new ToDirectionParams(BlockSide.BOTTOM, Direction.EAST, VerticalFacing.DOWN, Direction.WEST),
				new ToDirectionParams(BlockSide.FRONT, Direction.EAST, VerticalFacing.DOWN, Direction.DOWN),
				new ToDirectionParams(BlockSide.BACK, Direction.EAST, VerticalFacing.DOWN, Direction.UP),
				new ToDirectionParams(BlockSide.RIGHT, Direction.EAST, VerticalFacing.DOWN, Direction.NORTH),
				new ToDirectionParams(BlockSide.LEFT, Direction.EAST, VerticalFacing.DOWN, Direction.SOUTH)
		);
	}
}