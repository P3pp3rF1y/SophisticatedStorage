package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedstorage.Config;

import java.util.function.Supplier;

public enum TankTier {
	WOOD(() -> Config.SERVER.woodTank), COPPER(() -> Config.SERVER.copperTank), IRON(() -> Config.SERVER.ironTank), GOLD(() -> Config.SERVER.goldTank), DIAMOND(
			() -> Config.SERVER.diamondTank), NETHERITE(() -> Config.SERVER.netheriteTank);

	public static final int MAX_HEIGHT = 5;
	private final Supplier<Config.Server.TankConfig> config;

	TankTier(Supplier<Config.Server.TankConfig> config) {
		this.config = config;
	}

	public int capacity(int height, double multiplier) {
		validateHeight(height);
		if (!Double.isFinite(multiplier) || multiplier < 1) {
			throw new IllegalArgumentException("Tank stack multiplier must be finite and at least one");
		}
		return (int) Math.min(Integer.MAX_VALUE, getValue(config.get().capacity) * (double) height * multiplier);
	}

	private static int getValue(ModConfigSpec.IntValue value) {
		return Config.SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
	}

	public int upgradeSlots() {
		return getValue(config.get().upgradeSlotCount);
	}

	public static void validateHeight(int height) {
		if (height < 1 || height > MAX_HEIGHT) {
			throw new IllegalArgumentException("Tank height must be between one and five");
		}
	}
}
