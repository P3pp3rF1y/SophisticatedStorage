package net.p3pp3rf1y.sophisticatedstorage.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GenericWoodStorageHelper {
	private static final Object CACHE_LOCK = new Object();
	@Nullable
	private static volatile CacheState cacheState = null;

	private GenericWoodStorageHelper() {
	}

	public static boolean isCustomTexturedWood(WoodType woodType) {
		return WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.containsKey(woodType);
	}

	public static boolean isGenericWood(WoodType woodType) {
		return getGenericWoodInfo(woodType).isPresent();
	}

	public static Optional<GenericWoodInfo> getGenericWoodInfo(WoodType woodType) {
		return Optional.ofNullable(getCacheState().genericWoods().get(woodType));
	}

	public static Collection<WoodType> getGenericWoodTypes() {
		return getCacheState().genericWoods().keySet();
	}

	public static void invalidateCache() {
		synchronized (CACHE_LOCK) {
			cacheState = null;
		}
	}

	public static Optional<WoodType> getWoodTypeForPlanks(ItemStack stack) {
		return Optional.ofNullable(getCacheState().plankItems().get(stack.getItem()));
	}

	public static Optional<WoodType> getWoodTypeForSlab(ItemStack stack) {
		return Optional.ofNullable(getCacheState().slabItems().get(stack.getItem()));
	}

	public static Component getWoodDisplayName(WoodType woodType) {
		WoodName woodName = getDisplayWoodName(woodType);
		return Component.translatableWithFallback("wood_name.sophisticatedstorage." + woodName.translationKeyName(),
				getFallbackWoodDisplayName(woodName.path()));
	}

	private static WoodName getDisplayWoodName(WoodType woodType) {
		String woodTypeName = woodType.name().toLowerCase(Locale.ROOT);
		return getGenericWoodInfo(woodType).map(info -> {
			Identifier planksLocation = BuiltInRegistries.BLOCK.getKey(info.planks());
			String planksBasePath = removeSuffix(planksLocation.getPath(), "_planks");
			return woodTypeName.equals(planksLocation.getNamespace() + "_" + planksBasePath)
					? new WoodName(planksLocation.getNamespace(), planksBasePath, planksLocation.getNamespace() + "." + planksBasePath)
					: getWoodName(woodType);
		}).orElseGet(() -> getWoodName(woodType));
	}

	private static String getFallbackWoodDisplayName(String woodName) {
		String[] parts = woodName.split("_");
		StringBuilder displayName = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (!displayName.isEmpty()) {
				displayName.append(' ');
			}
			displayName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return displayName.toString();
	}

	private static CacheState getCacheState() {
		int woodTypeCount = (int) WoodType.values().count();
		boolean itemTagsAvailable = areItemTagsAvailable();
		CacheState currentCacheState = cacheState;
		if (currentCacheState != null && currentCacheState.matches(woodTypeCount, itemTagsAvailable)) {
			return currentCacheState;
		}

		synchronized (CACHE_LOCK) {
			woodTypeCount = (int) WoodType.values().count();
			itemTagsAvailable = areItemTagsAvailable();
			currentCacheState = cacheState;
			if (currentCacheState != null && currentCacheState.matches(woodTypeCount, itemTagsAvailable)) {
				return currentCacheState;
			}

			Map<WoodType, GenericWoodInfo> discoveredWoods = new LinkedHashMap<>();
			Map<Item, WoodType> discoveredPlankItems = new HashMap<>();
			Map<Item, WoodType> discoveredSlabItems = new HashMap<>();

			WoodType.values().filter(woodType -> !isCustomTexturedWood(woodType)).forEach(woodType -> findGenericWoodInfo(woodType).ifPresent(info -> {
				discoveredWoods.put(woodType, info);
				discoveredPlankItems.put(info.planks().asItem(), woodType);
				discoveredSlabItems.put(info.slab().asItem(), woodType);
			}));

			currentCacheState = new CacheState(Map.copyOf(discoveredWoods), Map.copyOf(discoveredPlankItems), Map.copyOf(discoveredSlabItems), woodTypeCount,
					itemTagsAvailable);
			cacheState = currentCacheState;
			return currentCacheState;
		}
	}

	private static boolean areItemTagsAvailable() {
		return Items.OAK_PLANKS.getDefaultInstance().is(ItemTags.PLANKS);
	}

	private static Optional<GenericWoodInfo> findGenericWoodInfo(WoodType woodType) {
		String woodTypeName = woodType.name().toLowerCase(Locale.ROOT);
		List<WoodItemCandidate> plankCandidates = getBlockItemCandidates("_planks", woodTypeName, ItemTags.PLANKS);
		if (plankCandidates.isEmpty()) {
			return Optional.empty();
		}

		List<WoodItemCandidate> slabCandidates = getBlockItemCandidates("_slab", woodTypeName, ItemTags.WOODEN_SLABS);
		if (slabCandidates.isEmpty()) {
			return Optional.empty();
		}

		Optional<WoodItemCandidate> planks = getUnambiguousCandidate(plankCandidates, slabCandidates);
		if (planks.isEmpty()) {
			return Optional.empty();
		}

		WoodItemCandidate plankItem = planks.get();
		Optional<Block> slab = slabCandidates.stream().filter(slabCandidate -> slabCandidate.matches(plankItem.namespace(), plankItem.basePath())).findFirst()
				.map(WoodItemCandidate::blockItem).map(BlockItem::getBlock);
		return slab.map(block -> new GenericWoodInfo(woodType, plankItem.getBlock(), block));
	}

	private static Optional<WoodItemCandidate> getUnambiguousCandidate(List<WoodItemCandidate> plankCandidates, List<WoodItemCandidate> slabCandidates) {
		if (plankCandidates.size() == 1) {
			return Optional.of(plankCandidates.get(0));
		}

		List<WoodItemCandidate> matchingNamespacePlanks = plankCandidates.stream()
				.filter(planks -> slabCandidates.stream().anyMatch(slab -> slab.matches(planks.namespace(), planks.basePath()))).toList();
		return matchingNamespacePlanks.size() == 1 ? Optional.of(matchingNamespacePlanks.get(0)) : Optional.empty();
	}

	private static WoodName getWoodName(WoodType woodType) {
		String name = woodType.name().toLowerCase(Locale.ROOT);
		Identifier location = Identifier.tryParse(name);
		if (name.contains(":") && location != null) {
			return new WoodName(location.getNamespace(), location.getPath(), location.getNamespace() + "." + location.getPath());
		}

		return new WoodName(null, name, name);
	}

	private static List<WoodItemCandidate> getBlockItemCandidates(String suffix, String woodTypeName, net.minecraft.tags.TagKey<Item> tag) {
		return BuiltInRegistries.ITEM.entrySet().stream().filter(entry -> entry.getKey().identifier().getPath().endsWith(suffix))
				.filter(entry -> new ItemStack(entry.getValue()).is(tag))
				.map(entry -> new WoodItemCandidate(entry.getValue(), entry.getKey().identifier(), suffix)).filter(WoodItemCandidate::isBlockItem)
				.filter(candidate -> candidate.matches(woodTypeName)).toList();
	}

	private static String removeSuffix(String value, String suffix) {
		return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
	}

	private record WoodName(@Nullable String namespace, String path, String translationKeyName) {
	}

	private record WoodItemCandidate(Item item, Identifier location, String suffix) {
		private boolean isBlockItem() {
			return item instanceof BlockItem;
		}

		private BlockItem blockItem() {
			return (BlockItem) item;
		}

		private Block getBlock() {
			return blockItem().getBlock();
		}

		private String namespace() {
			return location.getNamespace();
		}

		private String basePath() {
			return removeSuffix(location.getPath(), suffix);
		}

		private boolean matches(String woodTypeName) {
			String basePath = basePath();
			String namespace = namespace();
			return woodTypeName.equals(basePath) || woodTypeName.equals(namespace + ":" + basePath) || woodTypeName.equals(namespace + "_" + basePath);
		}

		private boolean matches(String namespace, String basePath) {
			return namespace().equals(namespace) && basePath().equals(basePath);
		}
	}

	private record CacheState(Map<WoodType, GenericWoodInfo> genericWoods, Map<Item, WoodType> plankItems, Map<Item, WoodType> slabItems, int woodTypeCount,
			boolean itemTagsAvailable) {
		private boolean matches(int currentWoodTypeCount, boolean currentItemTagsAvailable) {
			return woodTypeCount == currentWoodTypeCount && itemTagsAvailable == currentItemTagsAvailable;
		}
	}

	public record GenericWoodInfo(WoodType woodType, Block planks, Block slab) {
		public Identifier planksLocation() {
			return BuiltInRegistries.BLOCK.getKey(planks);
		}
	}
}
