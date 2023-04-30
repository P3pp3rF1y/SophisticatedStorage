package net.p3pp3rf1y.sophisticatedstorage.client.render;

import java.util.Locale;
import java.util.Optional;

public enum BarrelFace {
	TOP,
	BOTTOM,
	SIDE;

	public static Optional<BarrelFace> fromString(String faceName) {
		for (BarrelFace value : values()) {
			if (value.name().toLowerCase(Locale.ROOT).equals(faceName)) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}
}
