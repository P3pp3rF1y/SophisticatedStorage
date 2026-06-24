package net.p3pp3rf1y.sophisticatedstorage.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class DecorationHelper {
	public static final int BLOCK_TOTAL_PARTS = 24;
	private static final int MAIN_COLOR_PARTS = 18;
	private static final int ACCENT_COLOR_PARTS = 6;
	private static final Map<BarrelMaterial, Integer> DECORATIVE_SLOT_PARTS_NEEDED = Map.of(BarrelMaterial.TOP_INNER_TRIM, 1, BarrelMaterial.TOP_TRIM, 1,
			BarrelMaterial.SIDE_TRIM, 4, BarrelMaterial.BOTTOM_TRIM, 1, BarrelMaterial.TOP, 3, BarrelMaterial.SIDE, 12, BarrelMaterial.BOTTOM, 3);

	private DecorationHelper() {
	}

	public static Optional<ResourceLocation> getMaterialLocation(Item item) {
		if (item instanceof BlockItem blockItem) {
			return Optional.of(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
		}
		return Optional.empty();
	}

	public static boolean consumeDyes(int mainColorBeingSet, int accentColorBeingSet, Map<ResourceLocation, Integer> remainingParts,
			SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, List<ResourceHandler<ItemResource>> dyes, Integer storageMainColor,
			Integer storageAccentColor, TransactionContext tx) {
		Map<TagKey<Item>, Integer> partsNeeded = getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor);
		if (partsNeeded.isEmpty()) {
			return true;
		}

		return consumeDyePartsNeeded(partsNeeded, dyes, remainingParts, remainingPartsJournal, tx).hasEnough();
	}

	public static Map<TagKey<Item>, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor) {
		return getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor, MAIN_COLOR_PARTS, ACCENT_COLOR_PARTS);
	}

	public static Map<TagKey<Item>, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor,
			int mainColorParts, int accentColorParts) {
		Map<TagKey<Item>, Integer> partsNeeded = new HashMap<>();
		if (mainColorBeingSet != -1 && mainColorBeingSet != storageMainColor) {
			int[] rgbPartsNeeded = calculateRGBPartsNeeded(mainColorBeingSet, mainColorParts);
			addPartsNeededIfAny(rgbPartsNeeded, partsNeeded);
		}
		if (accentColorBeingSet != -1 && accentColorBeingSet != storageAccentColor) {
			int[] rgbPartsNeeded = calculateRGBPartsNeeded(accentColorBeingSet, accentColorParts);
			addPartsNeededIfAny(rgbPartsNeeded, partsNeeded);
		}
		return partsNeeded;
	}

	private static void addPartsNeededIfAny(int[] rgbPartsNeeded, Map<TagKey<Item>, Integer> partsNeeded) {
		addPartsNeededIfAny(rgbPartsNeeded[0], partsNeeded, Tags.Items.DYES_RED);
		addPartsNeededIfAny(rgbPartsNeeded[1], partsNeeded, Tags.Items.DYES_GREEN);
		addPartsNeededIfAny(rgbPartsNeeded[2], partsNeeded, Tags.Items.DYES_BLUE);
	}

	private static void addPartsNeededIfAny(int parts, Map<TagKey<Item>, Integer> partsNeeded, TagKey<Item> dyeName) {
		if (parts != 0) {
			partsNeeded.compute(dyeName, (location, partsTotal) -> partsTotal == null ? parts : partsTotal + parts);
		}
	}

	private static int[] calculateRGBPartsNeeded(int color, int totalParts) {
		float[] ratios = new float[3];
		ratios[0] = ARGB.red(color) / 255f;
		ratios[1] = ARGB.green(color) / 255f;
		ratios[2] = ARGB.blue(color) / 255f;

		float totalRaios = ratios[0] + ratios[1] + ratios[2];
		ratios[0] /= totalRaios;
		ratios[1] /= totalRaios;
		ratios[2] /= totalRaios;

		int n = ratios.length;
		int[] result = new int[n];
		double[] remainders = new double[n];

		double[] scaled = new double[n];
		for (int i = 0; i < n; i++) {
			scaled[i] = ratios[i] * totalParts;
			result[i] = (int) scaled[i];
			remainders[i] = scaled[i] - result[i];
		}

		int remaining = totalParts - Arrays.stream(result).sum();

		Integer[] indices = new Integer[n];
		for (int i = 0; i < n; i++)
			indices[i] = i;

		Arrays.sort(indices, Comparator.comparingDouble(i -> -remainders[i]));

		for (int i = 0; i < remaining; i++) {
			result[indices[i % n]]++;
		}

		return result;
	}

	public static boolean consumeMaterials(Map<ResourceLocation, Integer> remainingParts, SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal,
			List<ResourceHandler<ItemResource>> decorativeBlocks, Map<BarrelMaterial, ResourceLocation> originalMaterials,
			Map<BarrelMaterial, ResourceLocation> materials, TransactionContext tx) {
		Map<ResourceLocation, Integer> partsNeeded = getMaterialPartsNeeded(originalMaterials, materials);
		return consumeMaterialPartsNeeded(partsNeeded, remainingParts, remainingPartsJournal, decorativeBlocks, tx).hasEnough();
	}

	public static ConsumptionResult consumeMaterialPartsNeeded(Map<ResourceLocation, Integer> partsNeeded, Map<ResourceLocation, Integer> remainingParts,
			SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, List<ResourceHandler<ItemResource>> decorativeBlocks,
			TransactionContext tx) {
		return consumePartsNeeded(partsNeeded, decorativeBlocks, location -> location,
				(materialLocation, stack) -> getMaterialLocation(stack.getItem()).map(ml -> ml.equals(materialLocation)).orElse(false), remainingParts,
				remainingPartsJournal, tx);
	}

	public static boolean consumeSimpleMaterial(Map<ResourceLocation, Integer> remainingParts,
			SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, List<ResourceHandler<ItemResource>> decorativeBlocks,
			Optional<ResourceLocation> originalMaterial, ResourceLocation material, TransactionContext tx) {
		return consumeMaterialPartsNeeded(getSimpleMaterialPartsNeeded(originalMaterial, material), remainingParts, remainingPartsJournal, decorativeBlocks, tx)
				.hasEnough();
	}

	public static Map<ResourceLocation, Integer> getSimpleMaterialPartsNeeded(Optional<ResourceLocation> originalMaterial, ResourceLocation material) {
		if (originalMaterial.filter(material::equals).isPresent()) {
			return Collections.emptyMap();
		}
		return Map.of(material, BLOCK_TOTAL_PARTS);
	}

	public static Map<ResourceLocation, Integer> getMaterialPartsNeeded(Map<BarrelMaterial, ResourceLocation> originalMaterials,
			Map<BarrelMaterial, ResourceLocation> materialsToApply) {
		Map<ResourceLocation, Integer> partsNeeded = new HashMap<>();
		BarrelBlockItem.uncompactMaterials(materialsToApply);

		ResourceLocation topInnerTrimMaterialLocation = addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.TOP_INNER_TRIM, null, partsNeeded,
				originalMaterials);
		ResourceLocation topTrimMaterialLocation = addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.TOP_TRIM, topInnerTrimMaterialLocation,
				partsNeeded, originalMaterials);
		ResourceLocation sideTrimMaterialLocation = addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.SIDE_TRIM, topTrimMaterialLocation,
				partsNeeded, originalMaterials);
		addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.BOTTOM_TRIM, sideTrimMaterialLocation, partsNeeded, originalMaterials);
		ResourceLocation topMaterialLocation = addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.TOP, topTrimMaterialLocation, partsNeeded,
				originalMaterials);
		ResourceLocation sideMaterialLocation = addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.SIDE, topMaterialLocation, partsNeeded,
				originalMaterials);
		addMaterialCostForSlotAndGetMaterial(materialsToApply, BarrelMaterial.BOTTOM, sideMaterialLocation, partsNeeded, originalMaterials);
		return partsNeeded;
	}

	@Nullable
	private static ResourceLocation addMaterialCostForSlotAndGetMaterial(Map<BarrelMaterial, ResourceLocation> materials, BarrelMaterial barrelMaterial,
			@Nullable ResourceLocation defaultMaterialLocation, Map<ResourceLocation, Integer> partsNeeded,
			Map<BarrelMaterial, ResourceLocation> originalMaterials) {
		boolean materialIsTheSame = Objects.deepEquals(originalMaterials.get(barrelMaterial), materials.get(barrelMaterial));
		boolean newHasNoMaterial = !materials.containsKey(barrelMaterial);
		boolean hasNoCost = (barrelMaterial == BarrelMaterial.TOP_TRIM && defaultMaterialLocation != null) || materialIsTheSame || newHasNoMaterial;

		ResourceLocation materialLocation = materials.getOrDefault(barrelMaterial, defaultMaterialLocation);
		if (hasNoCost) {
			return materialLocation;
		}

		if (materialLocation != null) {
			int parts = DECORATIVE_SLOT_PARTS_NEEDED.get(barrelMaterial);
			partsNeeded.compute(materialLocation, (key, value) -> value == null ? parts : value + parts);
		}
		return materialLocation;
	}

	public static ConsumptionResult consumeDyePartsNeeded(Map<TagKey<Item>, Integer> partsNeeded, List<ResourceHandler<ItemResource>> resourceHandlers,
			Map<ResourceLocation, Integer> remainingParts, SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, TransactionContext tx) {
		return consumePartsNeeded(partsNeeded, resourceHandlers, TagKey::location, (dyeName, resource) -> resource.is(dyeName), remainingParts,
				remainingPartsJournal, tx);
	}

	private static <T> ConsumptionResult consumePartsNeeded(Map<T, Integer> partsNeeded, List<ResourceHandler<ItemResource>> resourceHandlers,
			Function<T, ResourceLocation> locationGetter, BiPredicate<T, ItemResource> resourceMatcher, Map<ResourceLocation, Integer> remainingParts,
			SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, TransactionContext tx) {
		Map<ResourceLocation, Integer> missingParts = new HashMap<>();
		for (Map.Entry<T, Integer> entry : partsNeeded.entrySet()) {
			T material = entry.getKey();
			Integer parts = entry.getValue();
			ResourceLocation materialLocation = locationGetter.apply(material);
			int remainingPartCount = remainingParts.getOrDefault(materialLocation, 0);
			if (remainingPartCount > parts) {
				remainingPartsJournal.updateSnapshots(tx);
				remainingParts.put(materialLocation, remainingPartCount - parts);
				continue;
			} else {
				remainingPartsJournal.updateSnapshots(tx);
				remainingParts.remove(materialLocation);
				if (remainingPartCount == parts) {
					continue;
				}
			}

			parts -= remainingPartCount;

			SingleItemConsumptionResult singleItemConsumptionResult = consumeFromHandlers(resourceHandlers, resourceMatcher, remainingParts,
					remainingPartsJournal, tx, material, parts, materialLocation);
			if (!singleItemConsumptionResult.hasEnough()) {
				missingParts.put(materialLocation, singleItemConsumptionResult.countMissing());
			}
		}
		return new ConsumptionResult(missingParts.isEmpty(), missingParts);
	}

	private static <T> SingleItemConsumptionResult consumeFromHandlers(List<ResourceHandler<ItemResource>> resourceHandlers,
			BiPredicate<T, ItemResource> resourceMatcher, Map<ResourceLocation, Integer> remainingParts,
			SnapshotJournal<Map<ResourceLocation, Integer>> remainingPartsJournal, TransactionContext tx, T material, Integer parts,
			ResourceLocation materialLocation) {
		for (ResourceHandler<ItemResource> resources : resourceHandlers) {
			for (int slot = 0; slot < resources.size(); slot++) {
				ItemResource resource = resources.getResource(slot);
				if (!resourceMatcher.test(material, resource)) {
					continue;
				}

				int toRemove = Math.ceilDiv(parts, BLOCK_TOTAL_PARTS);
				int removed = resources.extract(slot, resource, toRemove, tx);
				int partsRemoved = removed * BLOCK_TOTAL_PARTS;

				if (partsRemoved >= parts) {
					if (partsRemoved > parts) {
						remainingPartsJournal.updateSnapshots(tx);
						remainingParts.put(materialLocation, partsRemoved - parts);
					}
					return new SingleItemConsumptionResult(true, 0);
				}
				parts -= partsRemoved;
			}
		}
		return new SingleItemConsumptionResult(false, parts);
	}

	private record SingleItemConsumptionResult(boolean hasEnough, int countMissing) {
	}

	public record ConsumptionResult(boolean hasEnough, Map<ResourceLocation, Integer> missingParts) {
	}
}
