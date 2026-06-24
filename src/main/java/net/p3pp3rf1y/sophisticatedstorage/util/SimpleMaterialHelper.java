package net.p3pp3rf1y.sophisticatedstorage.util;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SimpleMaterialHelper {
	private static final List<BarrelMaterial> SINGLE_MATERIAL_PRIORITY = List.of(BarrelMaterial.TOP_INNER_TRIM, BarrelMaterial.TOP_TRIM,
			BarrelMaterial.SIDE_TRIM, BarrelMaterial.BOTTOM_TRIM, BarrelMaterial.TOP, BarrelMaterial.SIDE, BarrelMaterial.BOTTOM, BarrelMaterial.ALL);

	private SimpleMaterialHelper() {
	}

	public static Optional<ResourceLocation> getSingleMaterial(Map<BarrelMaterial, ResourceLocation> materials) {
		Map<BarrelMaterial, ResourceLocation> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		uncompactedMaterials.putAll(materials);
		BarrelBlockItem.uncompactMaterials(uncompactedMaterials);

		return SINGLE_MATERIAL_PRIORITY.stream().map(uncompactedMaterials::get).filter(material -> material != null).findFirst();
	}
}
