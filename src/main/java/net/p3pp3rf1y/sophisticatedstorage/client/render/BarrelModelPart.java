package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelType;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageTier;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public enum BarrelModelPart {
	BASE(Constants.CUBE_BOTTOM_TOP,
			(barrelType, storageTier) -> switch (barrelType) {
				case REGULAR -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.BASE};
				case LIMITED_1 ->
						new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_1};
				case LIMITED_2 ->
						new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_2};
				case LIMITED_3 ->
						new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_3};
				case LIMITED_4 ->
						new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_4};
			}),
	BASE_OPEN(Constants.CUBE_BOTTOM_TOP, false, StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_OPEN),
	METAL_BANDS(SophisticatedStorage.getRL("block/barrel_metal_bands"), StorageTextureManager.BarrelMaterial.METAL_BANDS),
	ACCENT(SophisticatedStorage.getRL(Constants.BLOCK_BARREL_TINTABLE_ACCENT),
			(barrelType, storageTier) -> switch (barrelType) {
				case REGULAR -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT};
				case LIMITED_1 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT,
						StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT_1};
				case LIMITED_2 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT,
						StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT_2};
				case LIMITED_3 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT,
						StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT_3};
				case LIMITED_4 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT,
						StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT_4};
			}),
	MAIN(SophisticatedStorage.getRL(Constants.BLOCK_BARREL_TINTABLE_MAIN), false, StorageTextureManager.BarrelMaterial.TINTABLE_MAIN),
	LIMITED_MAIN(SophisticatedStorage.getRL(Constants.BLOCK_LIMITED_BARREL_TINTABLE_MAIN), false, true,
			(barrelType, storageTier) -> switch (barrelType) {
				case REGULAR -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_MAIN};
				case LIMITED_1 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_MAIN,
						StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_1};
				case LIMITED_2 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_MAIN,
						StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_2};
				case LIMITED_3 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_MAIN,
						StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_3};
				case LIMITED_4 -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.TINTABLE_MAIN,
						StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_4};
			}),
	MAIN_OPEN(SophisticatedStorage.getRL("block/barrel_tintable_main_open"), false, StorageTextureManager.BarrelMaterial.TINTABLE_MAIN, StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_OPEN),
	TIER(Constants.CUBE_BOTTOM_TOP, (barrelType, storageTier) ->
			switch (storageTier) {
				case WOOD -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.WOOD_TIER};
				case IRON -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.IRON_TIER};
				case GOLD -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.GOLD_TIER};
				case DIAMOND -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.DIAMOND_TIER};
				case NETHERITE -> new StorageTextureManager.BarrelMaterial[] {StorageTextureManager.BarrelMaterial.NETHERITE_TIER};
			}
	),
	PACKED(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.PACKED);

	public final ResourceLocation modelName;
	private final boolean regularBarrelPart;
	private final boolean limitedBarrelPart;
	private final BiFunction<BarrelType, StorageTier, StorageTextureManager.BarrelMaterial[]> getBarrelMaterials;

	private static final BarrelModelPart[] REGULAR_BARREL_PARTS;
	private static final BarrelModelPart[] LIMITED_BARREL_PARTS;

	BarrelModelPart(ResourceLocation modelName, StorageTextureManager.BarrelMaterial... barrelMaterials) {
		this(modelName, true, barrelMaterials);
	}

	BarrelModelPart(ResourceLocation modelName, boolean limitedBarrelPart, StorageTextureManager.BarrelMaterial... barrelMaterials) {
		this(modelName, true, limitedBarrelPart, (bt, st) -> barrelMaterials);
	}

	BarrelModelPart(ResourceLocation modelName, BiFunction<BarrelType, StorageTier, StorageTextureManager.BarrelMaterial[]> getBarrelMaterials) {
		this(modelName, true, true, getBarrelMaterials);
	}

	BarrelModelPart(ResourceLocation modelName, boolean regularBarrelPart, boolean limitedBarrelPart, BiFunction<BarrelType, StorageTier, StorageTextureManager.BarrelMaterial[]> getBarrelMaterials) {
		this.modelName = modelName;
		this.regularBarrelPart = regularBarrelPart;
		this.limitedBarrelPart = limitedBarrelPart;
		this.getBarrelMaterials = getBarrelMaterials;
	}

	public StorageTextureManager.BarrelMaterial[] getBarrelMaterials(BarrelType barrelType, StorageTier tier) {
		return getBarrelMaterials.apply(barrelType, tier);
	}

	public static BarrelModelPart[] getRegularBarrelParts() {
		return REGULAR_BARREL_PARTS;
	}

	public static BarrelModelPart[] getLimitedBarrelParts() {
		return LIMITED_BARREL_PARTS;
	}

	static {
		List<BarrelModelPart> regularBarrelParts = new ArrayList<>();
		List<BarrelModelPart> limitedBarrelParts = new ArrayList<>();
		for (BarrelModelPart part : BarrelModelPart.values()) {
			if (part.limitedBarrelPart) {
				limitedBarrelParts.add(part);
			}
			if (part.regularBarrelPart) {
				regularBarrelParts.add(part);
			}
		}
		REGULAR_BARREL_PARTS = regularBarrelParts.toArray(new BarrelModelPart[0]);
		LIMITED_BARREL_PARTS = limitedBarrelParts.toArray(new BarrelModelPart[0]);
	}

	private static class Constants {
		private static final ResourceLocation CUBE_BOTTOM_TOP = new ResourceLocation("minecraft:block/cube_bottom_top");
		private static final String BLOCK_BARREL_TINTABLE_ACCENT = "block/barrel_tintable_accent";
		private static final String BLOCK_BARREL_TINTABLE_MAIN = "block/barrel_tintable_main";
		private static final String BLOCK_LIMITED_BARREL_TINTABLE_MAIN = "block/limited_barrel_tintable_main";
	}
}
