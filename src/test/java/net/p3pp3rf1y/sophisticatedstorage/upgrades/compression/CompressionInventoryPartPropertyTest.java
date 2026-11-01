package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CompressionInventoryPartPropertyTest extends CompressionInventoryPartTestSupport {
	private static final int SEED = 593;
	private static final int SEQUENCE_LENGTH = 300;

	@ParameterizedTest
	@MethodSource("insertAndExtractMixedSequenceParams")
	void insertAndExtractMixedSequenceConservesAndCompacts(int limit) {
		InventoryHandler inventory = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), limit);
		CompressionInventoryPart part = initCompressionInventoryPart(inventory, new SlotRange(0, 3), () -> getMemorySettings(inventory, Map.of()));
		Random random = new Random(SEED);
		Item[] items = {Items.IRON_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET};
		int[] weights = {81, 9, 1};
		long total = 81;

		for (int operation = 0; operation < SEQUENCE_LENGTH; operation++) {
			int slot = random.nextInt(3);
			int amount = random.nextInt(100) + 1;
			boolean insert = random.nextBoolean();
			try {
				if (insert) {
					ItemStack offered = new ItemStack(items[slot], amount);
					int simulated = part.insertItem(slot, offered, true, (s, stack, sim) -> stack).getCount();
					assertIronContents(inventory, part, 0, limit, total);
					ItemStack remainder = part.insertItem(slot, offered, false, (s, stack, sim) -> stack);
					assertEquals(simulated, remainder.getCount());
					total += (long) (amount - remainder.getCount()) * weights[slot];
				} else {
					int simulated = part.extractItem(slot, amount, true).getCount();
					assertIronContents(inventory, part, 0, limit, total);
					ItemStack extracted = part.extractItem(slot, amount, false);
					assertEquals(simulated, extracted.getCount());
					total -= (long) extracted.getCount() * weights[slot];
				}
				assertIronContents(inventory, part, 0, limit, total);
			} catch (AssertionError failure) {
				throw new AssertionError("Seed " + SEED + ", limit " + limit + ", operation " + operation + ", insert " + insert + ", slot " + slot
						+ ", amount " + amount + ", total " + total, failure);
			}
		}
	}

	private static List<Integer> insertAndExtractMixedSequenceParams() {
		return List.of(8, 64, 4096);
	}

	@ParameterizedTest(name = "starting total {0}")
	@MethodSource("extractItemSmallInventoryTotals")
	void extractItemConservesEverySmallInventoryState(int total) {
		InventoryHandler inventory = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, total / 81), 1,
				new ItemStack(Items.IRON_INGOT, total / 9 % 9), 2, new ItemStack(Items.IRON_NUGGET, total % 9)), 8);
		CompressionInventoryPart part = initCompressionInventoryPart(inventory, new SlotRange(0, 3), () -> getMemorySettings(inventory, Map.of()));

		for (int slot = 0; slot < 3; slot++) {
			long before = inventory.getSlotStack(0).getCount() * 81L + inventory.getSlotStack(1).getCount() * 9L + inventory.getSlotStack(2).getCount();
			ItemStack extracted = part.extractItem(slot, 1, false);
			try {
				assertIronContents(inventory, part, 0, 8, before - extracted.getCount() * (slot == 0 ? 81L : slot == 1 ? 9L : 1L));
			} catch (AssertionError failure) {
				throw new AssertionError("Starting total " + total + ", extraction slot " + slot + ", previous total " + before, failure);
			}
		}
	}

	private static IntStream extractItemSmallInventoryTotals() {
		return IntStream.rangeClosed(1, 728);
	}

	@Test
	void insertAndExtractNineSlotMixedRatioChainConservesContents() {
		Item[] items = {Items.COAL, Items.CHARCOAL, Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.LAPIS_LAZULI, Items.AMETHYST_SHARD, Items.FLINT,
				Items.CLAY_BALL};
		int[] ratios = {1, 4, 8, 9, 4, 8, 9, 4, 8};
		long[] weights = new long[9];
		weights[8] = 1;
		for (int slot = 7; slot >= 0; slot--) {
			weights[slot] = weights[slot + 1] * ratios[slot + 1];
		}
		Map<Integer, ItemStack> initial = new HashMap<>();
		for (int slot = 0; slot < 9; slot++) {
			initial.put(slot, new ItemStack(items[slot], slot == 0 ? 1 : 0));
		}
		InventoryHandler inventory = getFilledInventoryHandler(initial, 64);
		CompressionInventoryPart part = spy(new CompressionInventoryPart(inventory, new SlotRange(0, 9), () -> getMemorySettings(inventory, Map.of())));
		doReturn(Optional.empty()).when(part).getCompressionResultFromConfig(any(ItemStack.class));
		doReturn(Optional.empty()).when(part).getDecompressionResultFromConfig(any(Item.class));
		for (int slot = 1; slot < 9; slot++) {
			Item item = items[slot];
			doReturn(Optional.of(new CompressionUpgradeConfig.CompressionResult(new ItemStack(items[slot - 1]), ratios[slot]))).when(part)
					.getCompressionResultFromConfig(argThat(stack -> stack.is(item)));
			doReturn(Optional.of(new CompressionUpgradeConfig.DecompressionResult(new ItemStack(item), ratios[slot]))).when(part)
					.getDecompressionResultFromConfig(items[slot - 1]);
		}
		part.onInit();
		long total = weights[0];
		Random random = new Random(SEED);

		for (int operation = 0; operation < SEQUENCE_LENGTH; operation++) {
			int slot = random.nextInt(9);
			int amount = random.nextInt(64) + 1;
			boolean insert = operation % 2 == 0;
			try {
				if (insert) {
					ItemStack remainder = part.insertItem(slot, new ItemStack(items[slot], amount), false, (s, stack, simulate) -> stack);
					total += (amount - remainder.getCount()) * weights[slot];
				} else {
					total -= part.extractItem(slot, amount, false).getCount() * weights[slot];
				}
				long remaining = total;
				long calculated = 0;
				for (int index = 0; index < 9; index++) {
					int expected = (int) Math.min(64, remaining / weights[index]);
					remaining -= expected * weights[index];
					assertEquals(expected, inventory.getSlotStack(index).getCount(), "Internal slot " + index);
					calculated = Math.min(Integer.MAX_VALUE, calculated * ratios[index] + expected);
					int expectedCalculated = calculated >= Integer.MAX_VALUE - 64 ? Integer.MAX_VALUE - 64 : (int) calculated;
					assertEquals(expectedCalculated, part.getStackInSlot(index, inventory::getSlotStack).getCount(), "Calculated slot " + index);
				}
				assertEquals(0, remaining);
			} catch (AssertionError failure) {
				throw new AssertionError(
						"Seed " + SEED + ", operation " + operation + ", insert " + insert + ", slot " + slot + ", amount " + amount + ", total " + total,
						failure);
			}
		}
	}
}
