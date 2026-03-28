package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class BarrelUnbakedModel extends BarrelUnbakedModelBase {

	public BarrelUnbakedModel(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		super(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
	}

	@Override
	protected BarrelBlockStateModelBase instantiateBlockStateModel(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts, Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts) {
		return new BarrelBlockStateModel(baker, woodModelParts, particleIcons, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private static class BarrelBlockStateModel extends BarrelBlockStateModelBase {
		public BarrelBlockStateModel(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts, Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts) {
			super(baker, woodModelParts, particleIcons, woodDynamicBakingData, woodPartitionedModelParts);
		}

		@Override
		protected int createHash(@Nullable BlockState state) {
			int hash = super.createHash(state);
			if (state != null) {
				hash = hash * 31 + (state.getValue(BarrelBlock.OPEN) ? 1 : 0);
				hash = hash * 31 + state.getValue(BarrelBlock.FACING).get3DDataValue();
			}

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

	@SuppressWarnings("java:S6548") //singleton is intended here
	public static final class Loader extends BarrelUnbakedModelBase.Loader<BarrelUnbakedModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected BarrelUnbakedModel instantiateModel(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new BarrelUnbakedModel(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
