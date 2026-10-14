package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BarrelBakedModelCacheKeyTest {
	@Test
	void normalBarrelNormalizationIgnoresOnlyNonRenderingProperties() {
		BlockState state = ModBlocks.BARREL.get().defaultBlockState();
		BlockState tickingState = state.setValue(StorageBlockBase.TICKING, true);
		BlockState nonOpaqueState = state.setValue(BarrelBlock.OPAQUE, false);
		BlockState eastFacingState = state.setValue(BarrelBlock.FACING, Direction.EAST);
		BlockState openState = state.setValue(BarrelBlock.OPEN, true);
		BlockState flatTopState = state.setValue(BarrelBlock.FLAT_TOP, true);

		BlockState normalized = BarrelBakedModelCacheKey.normalizeState(state);
		BlockState normalizedTicking = BarrelBakedModelCacheKey.normalizeState(tickingState);
		BlockState normalizedNonOpaque = BarrelBakedModelCacheKey.normalizeState(nonOpaqueState);
		BlockState normalizedEastFacing = BarrelBakedModelCacheKey.normalizeState(eastFacingState);
		BlockState normalizedOpen = BarrelBakedModelCacheKey.normalizeState(openState);
		BlockState normalizedFlatTop = BarrelBakedModelCacheKey.normalizeState(flatTopState);

		assertEquals(normalized, normalizedTicking);
		assertEquals(normalized, normalizedNonOpaque);
		assertNotEquals(normalized, normalizedEastFacing);
		assertNotEquals(normalized, normalizedOpen);
		assertNotEquals(normalized, normalizedFlatTop);
	}

	@Test
	void limitedBarrelNormalizationIgnoresOnlyNonRenderingProperties() {
		BlockState state = ModBlocks.LIMITED_BARREL_1.get().defaultBlockState();
		BlockState tickingState = state.setValue(StorageBlockBase.TICKING, true);
		BlockState nonOpaqueState = state.setValue(BarrelBlock.OPAQUE, false);
		BlockState eastFacingState = state.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.EAST);
		BlockState upFacingState = state.setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.UP);
		BlockState flatTopState = state.setValue(BarrelBlock.FLAT_TOP, true);

		BlockState normalized = BarrelBakedModelCacheKey.normalizeState(state);
		BlockState normalizedTicking = BarrelBakedModelCacheKey.normalizeState(tickingState);
		BlockState normalizedNonOpaque = BarrelBakedModelCacheKey.normalizeState(nonOpaqueState);
		BlockState normalizedEastFacing = BarrelBakedModelCacheKey.normalizeState(eastFacingState);
		BlockState normalizedUpFacing = BarrelBakedModelCacheKey.normalizeState(upFacingState);
		BlockState normalizedFlatTop = BarrelBakedModelCacheKey.normalizeState(flatTopState);

		assertEquals(normalized, normalizedTicking);
		assertEquals(normalized, normalizedNonOpaque);
		assertNotEquals(normalized, normalizedEastFacing);
		assertNotEquals(normalized, normalizedUpFacing);
		assertNotEquals(normalized, normalizedFlatTop);
	}
}
