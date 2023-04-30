package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum BarrelModelPart {
	BASE,
	BASE_OPEN,
	METAL_BANDS,
	TINTABLE_ACCENT,
	TINTABLE_MAIN,
	TINTABLE_MAIN_OPEN,
	TIER,
	PACKED,
	LOCKED;

	private static final Map<String, BarrelModelPart> NAME_TO_PART;

	static {
		ImmutableMap.Builder<String, BarrelModelPart> builder = ImmutableMap.builder();
		for (BarrelModelPart part : values()) {
			builder.put(part.name().toLowerCase(Locale.ROOT), part);
		}
		NAME_TO_PART = builder.build();
	}

	public static Optional<BarrelModelPart> getByNameOptional(String name) {
		return Optional.ofNullable(NAME_TO_PART.get(name.toLowerCase(Locale.ROOT)));
	}
}
