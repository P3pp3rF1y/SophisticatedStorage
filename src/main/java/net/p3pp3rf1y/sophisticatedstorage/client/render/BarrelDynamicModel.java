package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelType;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageTier;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarrelDynamicModel extends BarrelDynamicModelBase<BarrelDynamicModel> {

	public BarrelDynamicModel(Map<String, Map<BarrelModelPart, UnbakedModel>> woodModels) {
		super(woodModels);
	}

	@Override
	protected BarrelBakedModelBase instantiateBakedModel(ImmutableMap<String, Map<BarrelModelPart, BakedModel>> woodModelParts) {
		return new BarrelBakedModel(woodModelParts);
	}

	private static class BarrelBakedModel extends BarrelBakedModelBase {
		public BarrelBakedModel(Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts) {
			super(woodModelParts);
		}

		@Override
		protected int getInWorldBlockHash(BlockState state, ModelData data) {
			int hash = super.getInWorldBlockHash(state, data);
			hash = hash * 31 + (Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? 1 : 0);
			hash = hash * 31 + state.getValue(BarrelBlock.FACING).get3DDataValue();

			return hash;
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.BASE_OPEN : BarrelModelPart.BASE;
		}

		@Override
		protected BarrelModelPart getMainPart(@Nullable BlockState state) {
			return state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.MAIN_OPEN : BarrelModelPart.MAIN;
		}

		@Override
		protected BarrelModelPart getMainPart() {
			return BarrelModelPart.MAIN;
		}

		@Override
		protected List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state) {
			return DIRECTION_ROTATES.get(state.getValue(BarrelBlock.FACING)).process(quads);
		}

		@Override
		protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
			int hash = super.calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
			hash = 31 * hash + dir.hashCode();
			return hash;
		}
	}

	public static final class Loader implements IGeometryLoader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public BarrelDynamicModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) throws JsonParseException {
			ImmutableMap.Builder<String, Map<BarrelModelPart, UnbakedModel>> woodModelsBuilder = ImmutableMap.builder();

			StorageTier tier = StorageTier.valueOf(modelContents.getAsJsonPrimitive("tier").getAsString().toUpperCase(Locale.ROOT));
			WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> {
				ImmutableMap.Builder<BarrelModelPart, UnbakedModel> modelsBuilder = ImmutableMap.builder();
				for (BarrelModelPart barrelPart : BarrelModelPart.getRegularBarrelParts()) {
					Map<String, Either<Material, String>> materials = new HashMap<>();

					for (StorageTextureManager.BarrelMaterial barrelMaterial : barrelPart.getBarrelMaterials(BarrelType.REGULAR, tier)) {
						putMaterial(materials, StorageTextureManager.INSTANCE::getBarrelMaterial, woodType, StorageTextureManager.BarrelFace.TOP, barrelMaterial);
						putMaterial(materials, StorageTextureManager.INSTANCE::getBarrelMaterial, woodType, StorageTextureManager.BarrelFace.BOTTOM, barrelMaterial);
						putMaterial(materials, StorageTextureManager.INSTANCE::getBarrelMaterial, woodType, StorageTextureManager.BarrelFace.SIDE, barrelMaterial);

						if (barrelPart == BarrelModelPart.MAIN || barrelPart == BarrelModelPart.MAIN_OPEN) {
							putMaterial(materials, StorageTextureManager.INSTANCE::getBarrelMaterial, woodType, StorageTextureManager.BarrelFace.TOP, StorageTextureManager.BarrelMaterial.HANDLE, "handle");
						}
					}
					modelsBuilder.put(barrelPart, new BlockModel(barrelPart.modelName, Collections.emptyList(), materials, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));
				}
				woodModelsBuilder.put(woodType.name(), modelsBuilder.build());
			});

			return new BarrelDynamicModel(woodModelsBuilder.build());
		}
	}
}
