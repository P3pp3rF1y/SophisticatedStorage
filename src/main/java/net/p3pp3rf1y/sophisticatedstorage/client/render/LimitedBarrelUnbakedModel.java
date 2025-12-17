package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.getNorthBasedRotation;

public class LimitedBarrelUnbakedModel extends BarrelUnbakedModelBase {
	public LimitedBarrelUnbakedModel(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
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
		protected List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state) {
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				quads = transformQuads(quads, DIRECTION_ROTATES.get(verticalFacing.getDirection()));
			}
			quads = transformQuads(quads, DIRECTION_ROTATES.get(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
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
	public static final class Loader extends BarrelUnbakedModelBase.Loader<LimitedBarrelUnbakedModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		protected LimitedBarrelUnbakedModel instantiateModel(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
			return new LimitedBarrelUnbakedModel(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}
	}
}
