package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;

import javax.annotation.Nullable;
import java.util.Map;

public class LimitedBarrelUnbakedModel extends BarrelUnbakedModelBase {
	public LimitedBarrelUnbakedModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		super(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
	}

	@Override
	protected BarrelBlockStateModelBase instantiateBlockStateModel(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts, Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts) {
		return new LimitedBarrelBlockStateModel(baker, woodModelParts, particleIcons, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private static class LimitedBarrelBlockStateModel extends BarrelBlockStateModelBase {
		public LimitedBarrelBlockStateModel(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts, Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts) {
			super(baker, woodModelParts, particleIcons, woodDynamicBakingData, woodPartitionedModelParts);
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return BarrelModelPart.BASE;
		}

		@Override
		protected int createHash(@Nullable BlockState state) {
			int hash = super.createHash(state);
			if (state != null) {
				hash = hash * 31 + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
				hash = hash * 31 + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			}
			return hash;
		}

		@Override
		protected boolean rendersOpen() {
			return false;
		}
	}

	@SuppressWarnings("java:S6548") //singleton is intended here
	public static final class Loader extends BarrelUnbakedModelBase.Loader<LimitedBarrelUnbakedModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected LimitedBarrelUnbakedModel instantiateModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new LimitedBarrelUnbakedModel(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
