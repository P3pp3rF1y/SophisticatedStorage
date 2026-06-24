package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;

import javax.annotation.Nullable;

import java.util.Map;

public class BarrelDynamicModel extends BarrelDynamicModelBase {

	public BarrelDynamicModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
			Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels,
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		super(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
	}

	@Override
	protected BarrelBakedModelBase instantiateBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts,
			Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData,
			Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
		return new BarrelBakedModel(baker, woodModelParts, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private static class BarrelBakedModel extends BarrelBakedModelBase {
		public BarrelBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts,
				Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData,
				Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
			super(baker, woodModelParts, woodDynamicBakingData, woodPartitionedModelParts);
		}

		@Override
		protected int getInWorldBlockHash(BlockState state, ModelData data, @Nullable RenderType renderType) {
			int hash = super.getInWorldBlockHash(state, data, renderType);
			hash = hash * 31 + (state.getValue(BarrelBlock.OPEN) ? 1 : 0);
			hash = hash * 31 + state.getValue(BarrelBlock.FACING).get3DDataValue();

			return hash;
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.BASE_OPEN : BarrelModelPart.BASE;
		}

		@Override
		protected boolean rendersOpen() {
			return true;
		}
	}

	@SuppressWarnings("java:S6548") // singleton is intended here
	public static final class Loader extends BarrelDynamicModelBase.Loader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected BarrelDynamicModel instantiateModel(@Nullable ResourceLocation parentLocation,
				Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
				Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels,
				Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new BarrelDynamicModel(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
