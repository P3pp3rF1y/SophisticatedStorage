package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.Config;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class CompressionChainHelper {
	private CompressionChainHelper() {
	}

	static Set<Integer> getCompressionChainErrorSlots(SlotRange slotRange, Map<Integer, ItemStack> stacks) {
		Set<Integer> errorSlots = new LinkedHashSet<>();
		Optional<Integer> firstFilledSlot = getFirstFilledSlot(slotRange, stacks);
		if (firstFilledSlot.isEmpty()) {
			return errorSlots;
		}

		ItemStack expectedStack = stacks.get(firstFilledSlot.get());
		boolean hasExpectedStack = true;
		for (int slot = firstFilledSlot.get(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			ItemStack stack = stacks.get(slot);
			if (stack != null && (!hasExpectedStack || !ItemStack.isSameItemSameTags(stack, expectedStack))) {
				errorSlots.add(slot);
			}

			if (hasExpectedStack) {
				Optional<DecompressionDefinition> decompressionDefinition = getDecompressionDefinition(expectedStack);
				if (decompressionDefinition.isPresent()) {
					expectedStack = decompressionDefinition.get().result();
				} else {
					hasExpectedStack = false;
				}
			}
		}

		return errorSlots;
	}

	private static Optional<Integer> getFirstFilledSlot(SlotRange slotRange, Map<Integer, ItemStack> stacks) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			if (stacks.containsKey(slot)) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	static Optional<CompressionDefinition> getCompressionDefinition(ItemStack stack,
			Function<Item, Optional<CompressionUpgradeConfig.DecompressionResult>> decompressionResultProvider,
			Function<ItemStack, Optional<CompressionUpgradeConfig.CompressionResult>> compressionResultProvider) {
		Set<RecipeHelper.CompactingShape> compactingShapes = RecipeHelper.getItemCompactingShapes(stack);

		if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)) {
			return getCompressionDefinition(stack, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE);
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE)) {
			return getCompressionDefinition(stack, RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE);
		}

		Optional<CompressionDefinition> compressionDefinition = Optional.empty();
		if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE)) {
			RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.THREE_BY_THREE);
			compressionDefinition = decompressionResultProvider.apply(compactingResult.getResult().getItem())
					.filter(decompressionResult -> decompressionResult.matches(stack, RecipeHelper.CompactingShape.THREE_BY_THREE.getNumberOfIngredients()))
					.map(decompressionResult -> new CompressionDefinition(compactingResult.getResult(), decompressionResult.count()));
		}
		if (compressionDefinition.isEmpty() && compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO)) {
			RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.TWO_BY_TWO);
			compressionDefinition = decompressionResultProvider.apply(compactingResult.getResult().getItem())
					.filter(decompressionResult -> decompressionResult.matches(stack, RecipeHelper.CompactingShape.TWO_BY_TWO.getNumberOfIngredients()))
					.map(decompressionResult -> new CompressionDefinition(compactingResult.getResult(), decompressionResult.count()));
		}
		if (compressionDefinition.isPresent()) {
			return compressionDefinition;
		}
		return compressionResultProvider.apply(stack)
				.map(compressionResult -> new CompressionDefinition(compressionResult.result(), compressionResult.count()));
	}

	private static Optional<CompressionDefinition> getCompressionDefinition(ItemStack stack, RecipeHelper.CompactingShape shape) {
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, shape);
		return compactingResult.getResult().isEmpty()
				? Optional.empty()
				: Optional.of(new CompressionDefinition(compactingResult.getResult(), shape.getNumberOfIngredients()));
	}

	static Optional<DecompressionDefinition> getDecompressionDefinition(ItemStack stack,
			Function<Item, Optional<CompressionUpgradeConfig.DecompressionResult>> decompressionResultProvider) {
		RecipeHelper.UncompactingResult uncompactingResult = RecipeHelper.getUncompactingResult(stack);
		if (uncompactingResult.getCompactUsingShape() == RecipeHelper.CompactingShape.NONE) {
			return decompressionResultProvider.apply(stack.getItem())
					.map(decompressionResult -> new DecompressionDefinition(decompressionResult.result(), decompressionResult.count()));
		}
		return Optional.of(new DecompressionDefinition(uncompactingResult.getResult(), uncompactingResult.getCompactUsingShape().getNumberOfIngredients()));
	}

	private static Optional<DecompressionDefinition> getDecompressionDefinition(ItemStack stack) {
		return getDecompressionDefinition(stack, CompressionChainHelper::getDecompressionResultFromConfig);
	}

	private static Optional<CompressionUpgradeConfig.DecompressionResult> getDecompressionResultFromConfig(Item item) {
		return Config.SERVER_SPEC.isLoaded() ? Config.SERVER.compressionUpgrade.getDecompressionResult(item) : Optional.empty();
	}

	record CompressionDefinition(ItemStack result, int count) {
		CompressionDefinition {
			result = result.copyWithCount(1);
		}
	}

	record DecompressionDefinition(ItemStack result, int count) {
		DecompressionDefinition {
			result = result.copyWithCount(1);
		}
	}
}
