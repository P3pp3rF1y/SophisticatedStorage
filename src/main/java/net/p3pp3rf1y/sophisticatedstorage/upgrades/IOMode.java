package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public enum IOMode implements StringRepresentable {
	PUSH("push"),
	PULL("pull"),
	PUSH_PULL("push_pull"),
	OFF("off"),
	DISABLED("disabled");

	private final String name;

	IOMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public IOMode next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, IOMode> NAME_VALUES;
	private static final IOMode[] VALUES;

	static {
		ImmutableMap.Builder<String, IOMode> builder = new ImmutableMap.Builder<>();
		for (IOMode value : IOMode.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static IOMode fromName(String name) {
		return NAME_VALUES.getOrDefault(name, OFF);
	}
}
