package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TankSplitPlanTest {
	@ParameterizedTest
	@MethodSource("splitScenarios")
	void createOnlyAcceptsSplitsNeedingUpgradesInAtMostOneDestination(SplitParams params) {
		List<ItemStack> upgrades = params.upgrades();
		boolean expectedToFit = requiredUpgradeTargets(params) <= 1;
		Optional<TankSplitPlan> result = createPlan(params, upgrades);
		assertEquals(expectedToFit, result.isPresent());
	}
	@ParameterizedTest
	@MethodSource("acceptedSplitScenarios")
	void createConservesFluidAcrossAllDestinations(SplitParams params) {
		List<ItemStack> upgrades = params.upgrades();
		TankSplitPlan plan = createPlan(params, upgrades).orElseThrow();
		assertEquals(params.amount(), plan.fluid().lower() + plan.fluid().dropped() + plan.fluid().upper());
	}
	@ParameterizedTest
	@MethodSource("acceptedSplitScenarios")
	void createConservesEveryUpgradeIncludingEjectedItems(SplitParams params) {
		List<ItemStack> upgrades = params.upgrades();
		TankSplitPlan plan = createPlan(params, upgrades).orElseThrow();
		List<ItemStack> all = Stream.of(plan.lowerUpgrades(), plan.droppedUpgrades(), plan.upperUpgrades(), plan.ejectedUpgrades()).flatMap(List::stream)
				.toList();
		for (ItemStack upgrade : upgrades) {
			assertEquals(upgrades.stream().filter(candidate -> ItemStack.isSameItemSameTags(upgrade, candidate)).count(),
					all.stream().filter(candidate -> ItemStack.isSameItemSameTags(upgrade, candidate)).count());
		}
	}
	@ParameterizedTest
	@MethodSource("acceptedSplitScenarios")
	void createKeepsEachDestinationWithinItsRetainedCapacity(SplitParams params) {
		List<ItemStack> upgrades = params.upgrades();
		TankSplitPlan plan = createPlan(params, upgrades).orElseThrow();
		assertFits(params.tier(), params.brokenIndex(), plan.fluid().lower(), plan.lowerUpgrades());
		assertFits(params.tier(), 1, plan.fluid().dropped(), plan.droppedUpgrades());
		assertFits(params.tier(), params.height() - params.brokenIndex() - 1, plan.fluid().upper(), plan.upperUpgrades());
	}
	private static Stream<SplitParams> splitScenarios() {
		List<SplitParams> scenarios = new ArrayList<>();
		for (TankTier tier : TankTier.values()) {
			for (int height = 1; height <= TankTier.MAX_HEIGHT; height++) {
				for (int stackCount = 0; stackCount <= Math.min(2, tier.upgradeSlots()); stackCount++) {
					addFillAndBreakScenarios(scenarios, tier, height, stackCount);
				}
			}
		}
		return scenarios.stream();
	}
	private static void addFillAndBreakScenarios(List<SplitParams> scenarios, TankTier tier, int height, int stackCount) {
		int capacity = tier.capacity(height, Math.pow(2, stackCount));
		for (int amount : new int[]{0, 1, capacity / 3, capacity / 2, capacity - 1, capacity}) {
			for (int index = 0; index < height; index++) {
				for (boolean gas : new boolean[]{false, true}) {
					scenarios.add(new SplitParams(tier, height, stackCount, amount, index, gas));
				}
			}
		}
	}
	private static Stream<SplitParams> acceptedSplitScenarios() {
		return splitScenarios().filter(params -> requiredUpgradeTargets(params) <= 1);
	}
	private static int requiredUpgradeTargets(SplitParams params) {
		TankFluidPartition partition = TankFluidPartition.split(params.height(), params.brokenIndex(), params.capacity(), params.amount(), params.gas());
		int lowerHeight = params.brokenIndex();
		int upperHeight = params.height() - params.brokenIndex() - 1;
		return (lowerHeight > 0 && partition.lower() > params.tier().capacity(lowerHeight, 1) ? 1 : 0)
				+ (partition.dropped() > params.tier().capacity(1, 1) ? 1 : 0)
				+ (upperHeight > 0 && partition.upper() > params.tier().capacity(upperHeight, 1) ? 1 : 0);
	}
	private static Optional<TankSplitPlan> createPlan(SplitParams params, List<ItemStack> upgrades) {
		return TankSplitPlan.create(params.tier(), params.height(), params.brokenIndex(), params.capacity(), params.amount(), params.gas(), upgrades);
	}
	private record SplitParams(TankTier tier, int height, int stackCount, int amount, int brokenIndex, boolean gas) {
		int capacity() {
			return tier.capacity(height, Math.pow(2, stackCount));
		}
		List<ItemStack> upgrades() {
			List<ItemStack> upgrades = new ArrayList<>();
			for (int slot = 0; slot < stackCount; slot++) {
				upgrades.add(new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			}
			if (upgrades.size() < tier.upgradeSlots()) {
				upgrades.add(new ItemStack(ModItems.PUMP_UPGRADE.get()));
			}
			return upgrades;
		}
	}
	private static void assertFits(TankTier tier, int height, int amount, List<ItemStack> upgrades) {
		double multiplier = upgrades.stream().filter(stack -> stack.getItem() instanceof StackUpgradeItem)
				.mapToDouble(stack -> ((StackUpgradeItem) stack.getItem()).getStackSizeMultiplier()).reduce(1, (a, b) -> a * b);
		assertTrue(height == 0 ? amount == 0 : amount <= tier.capacity(height, multiplier));
	}
	@Test
	void createRejectsSplitsRequiringTheSameStackUpgradeTwice() {
		List<ItemStack> upgrades = List.of(new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		Optional<TankSplitPlan> plan = TankSplitPlan.create(TankTier.WOOD, 3, 1, 48000, 48000, false, upgrades);
		assertTrue(plan.isEmpty());
	}
	@Test
	void createTransfersOnlyRequiredStackAndEjectsOtherUpgradesOnPrimaryBreak() {
		ItemStack stack = new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get());
		ItemStack pump = new ItemStack(ModItems.PUMP_UPGRADE.get());
		TankSplitPlan plan = TankSplitPlan.create(TankTier.IRON, 3, 0, 96000, 30000, false, List.of(stack, pump)).orElseThrow();
		assertEquals(30000, plan.fluid().dropped());
		assertEquals(1, plan.droppedUpgrades().size());
		assertTrue(ItemStack.isSameItemSameTags(stack, plan.droppedUpgrades().get(0)));
		assertEquals(1, plan.ejectedUpgrades().size());
		assertTrue(ItemStack.isSameItemSameTags(pump, plan.ejectedUpgrades().get(0)));
		assertTrue(plan.upperUpgrades().isEmpty());
	}
	@Test
	void createKeepsNonRequiredUpgradesOnOriginalLowerPrimary() {
		ItemStack pump = new ItemStack(ModItems.PUMP_UPGRADE.get());
		ItemStack stack = new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get());
		TankSplitPlan plan = TankSplitPlan.create(TankTier.IRON, 3, 1, 96000, 1000, false, List.of(stack, pump)).orElseThrow();
		assertEquals(1000, plan.fluid().lower());
		assertEquals(2, plan.lowerUpgrades().size());
		assertTrue(plan.droppedUpgrades().isEmpty());
		assertTrue(plan.upperUpgrades().isEmpty());
		assertTrue(plan.ejectedUpgrades().isEmpty());
	}
}
