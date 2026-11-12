package net.p3pp3rf1y.sophisticatedstorage.block;

public record TankFluidPartition(int lower, int dropped, int upper) {
	public static TankFluidPartition split(int height, int brokenIndex, int capacity, int amount, boolean lighterThanAir) {
		TankTier.validateHeight(height);
		if (brokenIndex < 0 || brokenIndex >= height || capacity <= 0 || amount < 0 || amount > capacity) {
			throw new IllegalArgumentException("Invalid tank partition");
		}
		long bottom = (long) capacity * brokenIndex / height;
		long top = (long) capacity * (brokenIndex + 1) / height;
		long fluidBottom = lighterThanAir ? capacity - amount : 0;
		long fluidTop = lighterThanAir ? capacity : amount;
		return new TankFluidPartition(overlap(0, bottom, fluidBottom, fluidTop), overlap(bottom, top, fluidBottom, fluidTop),
				overlap(top, capacity, fluidBottom, fluidTop));
	}

	private static int overlap(long bottom, long top, long fluidBottom, long fluidTop) {
		return (int) Math.max(0, Math.min(top, fluidTop) - Math.max(bottom, fluidBottom));
	}
}
