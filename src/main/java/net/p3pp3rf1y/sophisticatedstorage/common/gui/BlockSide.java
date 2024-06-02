package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;

import java.util.Map;

public enum BlockSide implements StringRepresentable {
	TOP("top"),
	BOTTOM("bottom"),
	FRONT("front"),
	BACK("back"),
	LEFT("left"),
	RIGHT("right");

	private final String name;

	BlockSide(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	private static final Map<String, BlockSide> NAME_VALUES;

	static {
		ImmutableMap.Builder<String, BlockSide> builder = new ImmutableMap.Builder<>();
		for (BlockSide value : BlockSide.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
	}

	public static BlockSide fromName(String name) {
		return NAME_VALUES.getOrDefault(name, FRONT);
	}

	public static BlockSide fromDirection(Direction direction, Direction baseHorizontalDirection, VerticalFacing baseVerticalFacing) {
		if (direction == baseHorizontalDirection.getClockWise()) {
			return LEFT;
		} else if (direction == baseHorizontalDirection.getCounterClockWise()) {
			return RIGHT;
		}

		if (baseVerticalFacing == VerticalFacing.NO) {
			if (direction == Direction.UP) {
				return TOP;
			} else if (direction == Direction.DOWN) {
				return BOTTOM;
			} else if (direction == baseHorizontalDirection) {
				return FRONT;
			} else if (direction == baseHorizontalDirection.getOpposite()) {
				return BACK;
			}
		}

		if (direction == baseVerticalFacing.getDirection()) {
			return FRONT;
		} else if (direction == baseVerticalFacing.getDirection().getOpposite()) {
			return BACK;
		}

		if (baseVerticalFacing == VerticalFacing.UP) {
			if (direction == baseHorizontalDirection) {
				return BOTTOM;
			} else if (direction == baseHorizontalDirection.getOpposite()) {
				return TOP;
			}
		}

		if (direction == baseHorizontalDirection) {
			return TOP;
		}
		return BOTTOM;
	}

	public Direction toDirection(Direction baseHorizontalDirection, VerticalFacing baseVerticalFacing) {
		return switch (this) {
			case TOP -> {
				if (baseVerticalFacing == VerticalFacing.NO) {
					if (baseHorizontalDirection == Direction.UP) {
						yield Direction.SOUTH;
					} else if (baseHorizontalDirection == Direction.DOWN) {
						yield Direction.NORTH;
					}
					yield Direction.UP;
				}
				yield baseVerticalFacing == VerticalFacing.UP ? baseHorizontalDirection.getOpposite() : baseHorizontalDirection;
			}
			case BOTTOM -> {
				if (baseVerticalFacing == VerticalFacing.NO) {
					if (baseHorizontalDirection == Direction.UP) {
						yield Direction.NORTH;
					} else if (baseHorizontalDirection == Direction.DOWN) {
						yield Direction.SOUTH;
					}
					yield Direction.DOWN;
				}
				yield baseVerticalFacing == VerticalFacing.UP ? baseHorizontalDirection : baseHorizontalDirection.getOpposite();
			}
			case FRONT -> baseVerticalFacing == VerticalFacing.NO ? baseHorizontalDirection : baseVerticalFacing.getDirection();
			case BACK -> baseVerticalFacing == VerticalFacing.NO ? baseHorizontalDirection.getOpposite() : baseVerticalFacing.getDirection().getOpposite();
			case LEFT -> baseVerticalFacing == VerticalFacing.NO && baseHorizontalDirection.getAxis() == Direction.Axis.Y ? Direction.EAST : baseHorizontalDirection.getClockWise();
			case RIGHT -> baseVerticalFacing == VerticalFacing.NO && baseHorizontalDirection.getAxis() == Direction.Axis.Y ? Direction.WEST : baseHorizontalDirection.getCounterClockWise();
		};
	}
}
