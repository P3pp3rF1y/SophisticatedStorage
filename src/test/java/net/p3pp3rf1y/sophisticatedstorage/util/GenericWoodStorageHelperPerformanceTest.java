package net.p3pp3rf1y.sophisticatedstorage.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
class GenericWoodStorageHelperPerformanceTest {
	private static final int ITEM_COUNT = 100_000;
	private static final int WOOD_TYPE_COUNT = 50;
	private static final int WOOD_TYPES_WITH_BLOCKS_COUNT = 40;
	private static final int WARMUP_RUNS = 3;
	private static final int MEASUREMENT_RUNS = 7;
	private static BenchmarkInput benchmarkInput;
	private static Map<TagKey<Item>, List<Holder<Item>>> originalItemTags;

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		originalItemTags = BuiltInRegistries.ITEM.getTags().collect(Collectors.toMap(Pair::getFirst, pair -> pair.getSecond().stream().toList()));
		Map<TagKey<Item>, List<Holder<Item>>> benchmarkItemTags = new HashMap<>(originalItemTags);
		benchmarkItemTags.put(ItemTags.PLANKS, List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.OAK_PLANKS)));
		benchmarkItemTags.put(ItemTags.WOODEN_SLABS, List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.OAK_SLAB)));
		BuiltInRegistries.ITEM.bindTags(benchmarkItemTags);
		benchmarkInput = createBenchmarkInput();
	}

	@AfterAll
	static void restoreTags() {
		Map<TagKey<Item>, List<Holder<Item>>> restoredItemTags = new HashMap<>(originalItemTags);
		restoredItemTags.putIfAbsent(ItemTags.PLANKS, List.of());
		restoredItemTags.putIfAbsent(ItemTags.WOODEN_SLABS, List.of());
		BuiltInRegistries.ITEM.bindTags(restoredItemTags);
	}

	@Test
	void scansLargeModpackRegistryOnceInsteadOfOncePerWoodTypeAndSuffix() {
		BenchmarkResult indexed = measureIndexedDiscovery();
		BenchmarkResult legacy = measureLegacyDiscovery();
		int expectedCandidates = WOOD_TYPES_WITH_BLOCKS_COUNT * 2;

		assertEquals(benchmarkInput.entries().size(), indexed.registryVisits());
		assertEquals((long) benchmarkInput.entries().size() * WOOD_TYPE_COUNT * 2, legacy.registryVisits());
		assertEquals(expectedCandidates, indexed.matchingCandidates());
		assertEquals(expectedCandidates, legacy.matchingCandidates());

		for (int i = 0; i < WARMUP_RUNS; i++) {
			measureIndexedDiscovery();
			measureLegacyDiscovery();
		}

		long indexedNanos = medianNanos(GenericWoodStorageHelperPerformanceTest::measureIndexedDiscovery);
		long legacyNanos = medianNanos(GenericWoodStorageHelperPerformanceTest::measureLegacyDiscovery);
		double speedup = (double) legacyNanos / indexedNanos;
		System.out.printf(Locale.ROOT,
				"Generic wood cache benchmark: %,d items (%d plank/slab candidates), %,d wood types, indexed %.2f ms, legacy %.2f ms, %.1fx faster%n",
				benchmarkInput.entries().size(), expectedCandidates, WOOD_TYPE_COUNT, indexedNanos / 1_000_000.0, legacyNanos / 1_000_000.0, speedup);
		assertTrue(speedup >= 10.0,
				() -> String.format(Locale.ROOT, "Expected indexed discovery to be at least 10x faster, but it was %.1fx (indexed %.2f ms, legacy %.2f ms)",
						speedup, indexedNanos / 1_000_000.0, legacyNanos / 1_000_000.0));
	}

	private static long medianNanos(Supplier<BenchmarkResult> measurement) {
		long[] durations = new long[MEASUREMENT_RUNS];
		for (int i = 0; i < MEASUREMENT_RUNS; i++) {
			durations[i] = measurement.get().durationNanos();
		}
		Arrays.sort(durations);
		return durations[durations.length / 2];
	}

	private static BenchmarkResult measureIndexedDiscovery() {
		CountingIterable entries = new CountingIterable(benchmarkInput.entries());
		long start = System.nanoTime();
		GenericWoodStorageHelper.WoodItemCandidates candidates = GenericWoodStorageHelper.getBlockItemCandidates(entries,
				GenericWoodStorageHelperPerformanceTest::isInTag);
		int matchingCandidates = countMatchingCandidates(candidates);
		return new BenchmarkResult(System.nanoTime() - start, entries.visits(), matchingCandidates);
	}

	private static BenchmarkResult measureLegacyDiscovery() {
		long start = System.nanoTime();
		long registryVisits = 0;
		int matchingCandidates = 0;
		for (String woodTypeName : benchmarkInput.woodTypeNames()) {
			for (Map.Entry<ResourceKey<Item>, Item> entry : benchmarkInput.entries()) {
				registryVisits++;
				if (isMatchingCandidate(entry, "_planks", woodTypeName, ItemTags.PLANKS)) {
					matchingCandidates++;
				}
			}
			for (Map.Entry<ResourceKey<Item>, Item> entry : benchmarkInput.entries()) {
				registryVisits++;
				if (isMatchingCandidate(entry, "_slab", woodTypeName, ItemTags.WOODEN_SLABS)) {
					matchingCandidates++;
				}
			}
		}
		return new BenchmarkResult(System.nanoTime() - start, registryVisits, matchingCandidates);
	}

	private static int countMatchingCandidates(GenericWoodStorageHelper.WoodItemCandidates candidates) {
		int matchingCandidates = 0;
		for (String woodTypeName : benchmarkInput.woodTypeNames()) {
			matchingCandidates += candidates.planks().stream().filter(candidate -> candidate.matches(woodTypeName)).count();
			matchingCandidates += candidates.slabs().stream().filter(candidate -> candidate.matches(woodTypeName)).count();
		}
		return matchingCandidates;
	}

	private static boolean isMatchingCandidate(Map.Entry<ResourceKey<Item>, Item> entry, String suffix, String woodTypeName, TagKey<Item> tag) {
		Item item = entry.getValue();
		ResourceLocation location = entry.getKey().location();
		String path = location.getPath();
		if (!path.endsWith(suffix) || !isInTag(item, tag) || !(item instanceof BlockItem)) {
			return false;
		}

		String basePath = path.substring(0, path.length() - suffix.length());
		String namespace = location.getNamespace();
		return woodTypeName.equals(basePath) || woodTypeName.equals(namespace + ":" + basePath) || woodTypeName.equals(namespace + "_" + basePath);
	}

	private static BenchmarkInput createBenchmarkInput() {
		List<Map.Entry<ResourceKey<Item>, Item>> entries = new ArrayList<>(ITEM_COUNT);
		List<String> woodTypeNames = new ArrayList<>(WOOD_TYPE_COUNT);
		for (int i = 0; i < ITEM_COUNT - WOOD_TYPES_WITH_BLOCKS_COUNT * 2; i++) {
			entries.add(itemEntry("modpack_item_" + i, Items.STICK));
		}
		for (int i = 0; i < WOOD_TYPE_COUNT; i++) {
			String woodTypeName = "modpack_wood_" + i;
			woodTypeNames.add(woodTypeName);
			if (i < WOOD_TYPES_WITH_BLOCKS_COUNT) {
				entries.add(itemEntry(woodTypeName + "_planks", Items.OAK_PLANKS));
				entries.add(itemEntry(woodTypeName + "_slab", Items.OAK_SLAB));
			}
		}
		return new BenchmarkInput(List.copyOf(entries), List.copyOf(woodTypeNames));
	}

	private static boolean isInTag(Item item, TagKey<Item> tag) {
		return new ItemStack(item).is(tag);
	}

	private static Map.Entry<ResourceKey<Item>, Item> itemEntry(String path, Item item) {
		ResourceLocation location = new ResourceLocation("benchmark", path);
		return Map.entry(ResourceKey.create(Registries.ITEM, location), item);
	}

	private record BenchmarkInput(List<Map.Entry<ResourceKey<Item>, Item>> entries, List<String> woodTypeNames) {
	}

	private record BenchmarkResult(long durationNanos, long registryVisits, int matchingCandidates) {
	}

	private static class CountingIterable implements Iterable<Map.Entry<ResourceKey<Item>, Item>> {
		private final List<Map.Entry<ResourceKey<Item>, Item>> entries;
		private long visits = 0;

		private CountingIterable(List<Map.Entry<ResourceKey<Item>, Item>> entries) {
			this.entries = entries;
		}

		@Override
		public Iterator<Map.Entry<ResourceKey<Item>, Item>> iterator() {
			Iterator<Map.Entry<ResourceKey<Item>, Item>> delegate = entries.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return delegate.hasNext();
				}

				@Override
				public Map.Entry<ResourceKey<Item>, Item> next() {
					visits++;
					return delegate.next();
				}
			};
		}

		private long visits() {
			return visits;
		}
	}
}
