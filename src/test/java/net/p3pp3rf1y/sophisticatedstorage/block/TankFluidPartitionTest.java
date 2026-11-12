package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraftforge.fluids.FluidType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TankFluidPartitionTest {
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void splitConservesVisibleSharesAtEveryHeightAndBreakIndex(TankTier tier) {
		for (int height = 1; height <= TankTier.MAX_HEIGHT; height++) {
			for (double multiplier : new double[]{1, 2, 4, 8, 16, 1_048_576}) {
				int capacity = tier.capacity(height, multiplier);
				for (int amount : new int[]{0, 1, capacity / 3, capacity / 2, capacity - 1, capacity}) {
					for (int index = 0; index < height; index++) {
						for (boolean gas : new boolean[]{false, true}) {
							TankFluidPartition result = TankFluidPartition.split(height, index, capacity, amount, gas);

							assertEquals(amount, (long) result.lower() + result.dropped() + result.upper());
							assertTrue(result.lower() >= 0 && result.dropped() >= 0 && result.upper() >= 0);
							if (index == 0) {
								assertEquals(0, result.lower());
							}
							if (index == height - 1) {
								assertEquals(0, result.upper());
							}
						}
					}
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void splitAllocatesFluidFromItsDensityDependentEnd(boolean gas) {
		TankFluidPartition expected = gas ? new TankFluidPartition(0, 2000, 8000) : new TankFluidPartition(8000, 2000, 0);

		TankFluidPartition result = TankFluidPartition.split(3, 1, 24000, 10000, gas);

		assertEquals(expected, result);
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void capacityUsesTheTierBucketVolume(TankTier tier) {
		int[] buckets = {8, 12, 16, 32, 64, 128};

		int capacity = tier.capacity(1, 1);

		assertEquals(buckets[tier.ordinal()] * FluidType.BUCKET_VOLUME, capacity);
	}
}
