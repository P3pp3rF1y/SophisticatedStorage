package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class BarrelDynamicModel extends BarrelDynamicModelBase<BarrelDynamicModel> {

	public BarrelDynamicModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		super(parentLocation, woodOverrides, flatTopModelName, dynamicPartModels, woodPartitionedModelPartDefinitions);
	}

	@Override
	protected BarrelBakedModelBase instantiateBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
		return new BarrelBakedModel(baker, woodModelParts, flatTopModel, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private static class BarrelBakedModel extends BarrelBakedModelBase {
		public BarrelBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
			super(baker, woodModelParts, flatTopModel, woodDynamicBakingData, woodPartitionedModelParts);
		}

		@Override
		protected int getInWorldBlockHash(BlockState state, ModelData data, @Nullable RenderType renderType) {
			int hash = super.getInWorldBlockHash(state, data, renderType);
			hash = hash * 31 + (Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? 1 : 0);
			hash = hash * 31 + state.getValue(BarrelBlock.FACING).get3DDataValue();

			return hash;
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.BASE_OPEN : BarrelModelPart.BASE;
		}

		@Override
		protected List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state) {
			return DIRECTION_ROTATES.get(state.getValue(BarrelBlock.FACING)).process(quads);
		}

		@Override
		protected boolean rendersOpen() {
			return true;
		}

		@Override
		protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
			int hash = super.calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
			hash = 31 * hash + dir.hashCode();
			return hash;
		}
	}

	@SuppressWarnings("java:S6548") //singleton is intended here
	public static final class Loader extends BarrelDynamicModelBase.Loader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected BarrelDynamicModel instantiateModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new BarrelDynamicModel(parentLocation, woodOverrides, flatTopModelName, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
