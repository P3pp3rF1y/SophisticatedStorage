package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum VerticalFacing implements StringRepresentable {
	NO("no", Direction.NORTH, 0),
	UP("up", Direction.UP, 1),
	DOWN("down", Direction.DOWN, 2);

	private final String serializedName;

	private final Direction direction;

	private final int index;

	VerticalFacing(String serializedName, Direction direction, int index) {
		this.serializedName = serializedName;
		this.direction = direction;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	public static VerticalFacing fromDirection(Direction direction) {
		if (direction.getAxis().isHorizontal()) {
			return NO;
		}
		if (direction == Direction.UP) {
			return UP;
		} else {
			return DOWN;
		}
	}
}
