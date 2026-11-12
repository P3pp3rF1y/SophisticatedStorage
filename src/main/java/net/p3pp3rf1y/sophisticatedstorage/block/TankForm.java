package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TankForm implements StringRepresentable {
	SINGLE, BOTTOM, MIDDLE, TOP;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static TankForm at(int index, int height) {
		return height == 1 ? SINGLE : index == 0 ? BOTTOM : index == height - 1 ? TOP : MIDDLE;
	}
}
