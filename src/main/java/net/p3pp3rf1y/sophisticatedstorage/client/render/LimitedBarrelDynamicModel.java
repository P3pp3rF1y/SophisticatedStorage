package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelType;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageTier;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LimitedBarrelDynamicModel extends BarrelDynamicModelBase<LimitedBarrelDynamicModel> {
	public LimitedBarrelDynamicModel(Map<String, Map<BarrelModelPart, UnbakedModel>> woodModels) {
		super(woodModels);
	}

	@Override
	protected BarrelBakedModelBase instantiateBakedModel(ImmutableMap<String, Map<BarrelModelPart, BakedModel>> woodModelParts) {
		return new LimitedBarrelBakedModel(woodModelParts);
	}

	private static class LimitedBarrelBakedModel extends BarrelBakedModelBase {
		public LimitedBarrelBakedModel(Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts) {
			super(woodModelParts);
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return BarrelModelPart.BASE;
		}

		@Override
		protected BarrelModelPart getMainPart(@Nullable BlockState state) {
			return BarrelModelPart.LIMITED_MAIN;
		}

		@Override
		protected BarrelModelPart getMainPart() {
			return BarrelModelPart.LIMITED_MAIN;
		}
	}

	public static final class Loader implements IGeometryLoader<LimitedBarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public LimitedBarrelDynamicModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			ImmutableMap.Builder<String, Map<BarrelModelPart, UnbakedModel>> woodModelsBuilder = ImmutableMap.builder();

			StorageTier tier = StorageTier.valueOf(modelContents.getAsJsonPrimitive("tier").getAsString().toUpperCase(Locale.ROOT));
			BarrelType barrelType = BarrelType.valueOf(modelContents.getAsJsonPrimitive("barrelType").getAsString().toUpperCase(Locale.ROOT));

			WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> {
				ImmutableMap.Builder<BarrelModelPart, UnbakedModel> modelsBuilder = ImmutableMap.builder();
				for (BarrelModelPart barrelPart : BarrelModelPart.getLimitedBarrelParts()) {
					Map<String, Either<Material, String>> materials = new HashMap<>();

					for (StorageTextureManager.BarrelMaterial barrelMaterial : barrelPart.getBarrelMaterials(barrelType, tier)) {
						putMaterial(materials, StorageTextureManager.INSTANCE::getLimitedBarrelMaterial, woodType, StorageTextureManager.BarrelFace.TOP, barrelMaterial);
						putMaterial(materials, StorageTextureManager.INSTANCE::getLimitedBarrelMaterial, woodType, StorageTextureManager.BarrelFace.BOTTOM, barrelMaterial);
						putMaterial(materials, StorageTextureManager.INSTANCE::getLimitedBarrelMaterial, woodType, StorageTextureManager.BarrelFace.SIDE, barrelMaterial);
					}
					modelsBuilder.put(barrelPart, new BlockModel(barrelPart.modelName, Collections.emptyList(), materials, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));
				}
				woodModelsBuilder.put(woodType.name(), modelsBuilder.build());
			});

			return new LimitedBarrelDynamicModel(woodModelsBuilder.build());
		}
	}

}
