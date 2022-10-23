package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IModelData;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelType;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageTier;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

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

		@Override
		protected int getInWorldBlockHash(BlockState state, IModelData data) {
			int hash = super.getInWorldBlockHash(state, data);
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			return hash;
		}

		@Override
		protected List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state) {
			LimitedBarrelBlock.VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != LimitedBarrelBlock.VerticalFacing.NO) {
				quads = DIRECTION_ROTATES.get(verticalFacing.getDirection()).processMany(quads);
			}
			quads = DIRECTION_ROTATES.get(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)).processMany(quads);
			return quads;
		}

		@Override
		protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
			int hash = super.calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			return hash;
		}

		@Override
		protected void rotateDisplayItemFrontOffset(BlockState state, Direction dir, Vector3f frontOffset) {
			LimitedBarrelBlock.VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != LimitedBarrelBlock.VerticalFacing.NO) {
				frontOffset.transform(getNorthBasedRotation(verticalFacing.getDirection()));
			}
			frontOffset.transform(getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
		}

		@Override
		protected int calculateDirectionMoveHash(BlockState state, ItemStack displayItem, int displayItemIndex, int displayItemCount) {
			int hash = super.calculateDirectionMoveHash(state, displayItem, displayItemIndex, displayItemCount);
			hash = 31 * hash + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
			hash = 31 * hash + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			return hash;
		}
	}

	public static final class Loader implements IModelLoader<LimitedBarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public LimitedBarrelDynamicModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
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

		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			//noop
		}
	}

}
