package net.p3pp3rf1y.sophisticatedstorage.block;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;

public enum BarrelMaterial implements StringRepresentable {
	SIDE("side", MaterialModelPart.CORE),
	SIDE_TRIM("side_trim", MaterialModelPart.TRIM),
	BOTTOM("bottom", MaterialModelPart.CORE),
	BOTTOM_TRIM("bottom_trim", MaterialModelPart.TRIM),
	TOP("top", MaterialModelPart.CORE),
	TOP_TRIM("top_trim", MaterialModelPart.TRIM),
	TOP_INNER_TRIM("top_inner_trim", MaterialModelPart.TRIM),
	ALL("all", MaterialModelPart.BOTH, SIDE, SIDE_TRIM, BOTTOM, BOTTOM_TRIM, TOP, TOP_TRIM, TOP_INNER_TRIM),
	ALL_TRIM("all_trim", MaterialModelPart.TRIM, SIDE_TRIM, BOTTOM_TRIM, TOP_TRIM, TOP_INNER_TRIM),
	ALL_BUT_TRIM("all_but_trim", MaterialModelPart.CORE, SIDE, BOTTOM, TOP),
	TOP_ALL("top_all", MaterialModelPart.BOTH, TOP, TOP_TRIM, TOP_INNER_TRIM),
	SIDE_ALL("side_all", MaterialModelPart.BOTH, SIDE, SIDE_TRIM),
	BOTTOM_ALL("bottom_all", MaterialModelPart.BOTH, BOTTOM, BOTTOM_TRIM);

	private final String name;

	private MaterialModelPart materialModelPart;

	private final BarrelMaterial[] children;
	BarrelMaterial(String name, MaterialModelPart materialModelPart, BarrelMaterial... children) {
		this.name = name;
		this.materialModelPart = materialModelPart;
		this.children = children;
	}

	public MaterialModelPart getMaterialModelPart() {
		return materialModelPart;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public BarrelMaterial[] getChildren() {
		return children.length > 0 ? children : new BarrelMaterial[] {this};
	}

	public boolean isLeaf() {
		return children.length == 0;
	}

	private static final Map<String, BarrelMaterial> NAME_VALUES;

	static {
		ImmutableMap.Builder<String, BarrelMaterial> builder = new ImmutableMap.Builder<>();
		for (BarrelMaterial value : BarrelMaterial.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
	}

	public static BarrelMaterial fromName(String name) {
		return NAME_VALUES.getOrDefault(name, SIDE);
	}

	public static List<BarrelMaterial> getFillFromDefaults(BarrelMaterial material) {
		return switch (material) {
			case SIDE -> List.of(BOTTOM, TOP, TOP_INNER_TRIM);
			case SIDE_TRIM -> List.of(BOTTOM_TRIM, TOP_TRIM);
			case BOTTOM -> List.of(SIDE, TOP, TOP_INNER_TRIM);
			case BOTTOM_TRIM -> List.of(SIDE_TRIM, TOP_TRIM);
			case TOP -> List.of(TOP_INNER_TRIM, SIDE, BOTTOM);
			case TOP_TRIM -> List.of(SIDE_TRIM, BOTTOM_TRIM);
			case TOP_INNER_TRIM -> List.of(TOP_TRIM, SIDE_TRIM, BOTTOM_TRIM);
			default -> List.of();
		};
	}

	public enum MaterialModelPart {
		BOTH,
		TRIM,
		CORE
	}
}
