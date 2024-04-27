package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

public class LimitedBarrelDynamicModel extends BarrelDynamicModelBase<LimitedBarrelDynamicModel> {
	public LimitedBarrelDynamicModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		super(parentLocation, woodOverrides, flatTopModelName, dynamicPartModels, woodPartitionedModelPartDefinitions);
	}

	@Override
	protected BarrelBakedModelBase instantiateBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
		return new LimitedBarrelBakedModel(baker, woodModelParts, flatTopModel, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private static class LimitedBarrelBakedModel extends BarrelBakedModelBase {
		public LimitedBarrelBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
			super(baker, woodModelParts, flatTopModel, woodDynamicBakingData, woodPartitionedModelParts);
		}

		@Override
		protected BarrelModelPart getBasePart(@Nullable BlockState state) {
			return BarrelModelPart.BASE;
		}

		@Override
		protected int getInWorldBlockHash(BlockState state, ModelData data, @Nullable RenderType renderType) {
			int hash = super.getInWorldBlockHash(state, data, renderType);
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
			hash = hash * 31 + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			return hash;
		}

		@Override
		protected List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state) {
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				quads = DIRECTION_ROTATES.get(verticalFacing.getDirection()).process(quads);
			}
			quads = DIRECTION_ROTATES.get(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)).process(quads);
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
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				getNorthBasedRotation(verticalFacing.getDirection()).transform(frontOffset);
			}
			getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)).transform(frontOffset);
		}

		@Override
		protected int calculateDirectionMoveHash(BlockState state, ItemStack displayItem, int displayItemIndex, int displayItemCount, boolean isFlatTop) {
			int hash = super.calculateDirectionMoveHash(state, displayItem, displayItemIndex, displayItemCount, isFlatTop);
			hash = 31 * hash + state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING).get2DDataValue();
			hash = 31 * hash + state.getValue(LimitedBarrelBlock.VERTICAL_FACING).getIndex();
			return hash;
		}

		@Override
		protected boolean rendersOpen() {
			return false;
		}
	}

	@SuppressWarnings("java:S6548") //singleton is intended here
	public static final class Loader extends BarrelDynamicModelBase.Loader<LimitedBarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected LimitedBarrelDynamicModel instantiateModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new LimitedBarrelDynamicModel(parentLocation, woodOverrides, flatTopModelName, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
