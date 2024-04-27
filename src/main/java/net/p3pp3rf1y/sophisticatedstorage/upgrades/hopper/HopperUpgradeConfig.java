package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.neoforged.neoforge.common.ModConfigSpec;

public class HopperUpgradeConfig {
	public final ModConfigSpec.IntValue inputFilterSlots;
	public final ModConfigSpec.IntValue inputFilterSlotsInRow;
	public final ModConfigSpec.IntValue outputFilterSlots;
	public final ModConfigSpec.IntValue outputFilterSlotsInRow;
	public final ModConfigSpec.IntValue transferSpeedTicks;
	public final ModConfigSpec.IntValue maxTransferStackSize;

	public HopperUpgradeConfig(ModConfigSpec.Builder builder, String upgradeName, String path, int defaultInputFilterSlots, int defaultInputFilterSlotsInRow, int defaultOutputFilterSlots, int defaultOutputFilterSlotsInRow, int defaultTransferSpeedTicks, int defaultMaxTransferStackSize) {
		builder.comment(upgradeName + " Settings").push(path);
		inputFilterSlots = builder.comment("Number of input filter slots").defineInRange("inputFilterSlots", defaultInputFilterSlots, 1, 8);
		inputFilterSlotsInRow = builder.comment("Number of input filter slots displayed in a row").defineInRange("inputFilterSlotsInRow", defaultInputFilterSlotsInRow, 1, 4);
		outputFilterSlots = builder.comment("Number of fuel filter slots").defineInRange("outputFilterSlots", defaultOutputFilterSlots, 1, 8);
		outputFilterSlotsInRow = builder.comment("Number of fuel filter slots displayed in a row").defineInRange("outputFilterSlotsInRow", defaultOutputFilterSlotsInRow, 1, 4);
		transferSpeedTicks = builder.comment("Number of ticks between each transfer").defineInRange("transferSpeedTicks", defaultTransferSpeedTicks, 1, 100);
		maxTransferStackSize = builder.comment("Maximum stack size that can be transferred in one transfer").defineInRange("maxTransferStackSize", defaultMaxTransferStackSize, 1, 64);
		builder.pop();

	}
}
