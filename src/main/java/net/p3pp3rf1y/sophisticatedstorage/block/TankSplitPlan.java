package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record TankSplitPlan(TankFluidPartition fluid, List<ItemStack> lowerUpgrades, List<ItemStack> droppedUpgrades, List<ItemStack> upperUpgrades,
		List<ItemStack> ejectedUpgrades) {
	public static Optional<TankSplitPlan> create(TankTier tier, int height, int brokenIndex, int capacity, int amount, boolean gas, List<ItemStack> upgrades) {
		TankFluidPartition fluid = TankFluidPartition.split(height, brokenIndex, capacity, amount, gas);
		int[] amounts = {fluid.lower(), fluid.dropped(), fluid.upper()};
		int[] heights = {brokenIndex, 1, height - brokenIndex - 1};
		int requiredTarget = -1;
		double requiredMultiplier = 1;
		for (int target = 0; target < heights.length; target++) {
			if (heights[target] > 0 && amounts[target] > tier.capacity(heights[target], 1)) {
				if (requiredTarget != -1) {
					return Optional.empty();
				}
				requiredTarget = target;
				requiredMultiplier = (double) amounts[target] / tier.capacity(heights[target], 1);
			}
		}
		int retainedMask = minimumStackConfiguration(upgrades, requiredMultiplier);
		if (retainedMask == -1) {
			return Optional.empty();
		}
		List<ItemStack> lower = new ArrayList<>();
		List<ItemStack> dropped = new ArrayList<>();
		List<ItemStack> upper = new ArrayList<>();
		List<ItemStack> ejected = new ArrayList<>();
		List<List<ItemStack>> destinations = List.of(lower, dropped, upper);
		for (int slot = 0; slot < upgrades.size(); slot++) {
			ItemStack upgrade = upgrades.get(slot);
			if (upgrade.isEmpty()) {
				continue;
			}
			if (requiredTarget >= 0 && (retainedMask & 1 << slot) != 0) {
				destinations.get(requiredTarget).add(upgrade.copy());
			} else if (brokenIndex > 0) {
				lower.add(upgrade.copy());
			} else {
				ejected.add(upgrade.copy());
			}
		}
		return Optional.of(new TankSplitPlan(fluid, List.copyOf(lower), List.copyOf(dropped), List.copyOf(upper), List.copyOf(ejected)));
	}

	private static int minimumStackConfiguration(List<ItemStack> upgrades, double requiredMultiplier) {
		int bestMask = -1;
		double bestMultiplier = Double.MAX_VALUE;
		for (int mask = 0; mask < 1 << upgrades.size(); mask++) {
			double multiplier = 1;
			boolean valid = true;
			for (int slot = 0; slot < upgrades.size(); slot++) {
				if ((mask & 1 << slot) == 0) {
					continue;
				}
				ItemStack upgrade = upgrades.get(slot);
				if (!(upgrade.getItem() instanceof StackUpgradeItem stack) || !upgrade.getOrDefault(ModCoreDataComponents.ENABLED, true)) {
					valid = false;
					break;
				}
				multiplier *= stack.getStackSizeMultiplier();
			}
			if (valid && multiplier >= requiredMultiplier && (bestMask == -1 || Integer.bitCount(mask) < Integer.bitCount(bestMask)
					|| Integer.bitCount(mask) == Integer.bitCount(bestMask) && multiplier < bestMultiplier)) {
				bestMask = mask;
				bestMultiplier = multiplier;
			}
		}
		return bestMask;
	}
}
