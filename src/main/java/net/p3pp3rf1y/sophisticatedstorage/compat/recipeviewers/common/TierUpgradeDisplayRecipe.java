package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public record TierUpgradeDisplayRecipe(ResourceLocation id, CraftingRecipe recipe, boolean shapeless, int width, int height,
		NonNullList<Ingredient> ingredients, int storageIngredientIndex, List<TierUpgradeVariantPair> variantPairs) {
	public Optional<TierUpgradeVariantPair> findBySource(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItemSameTags(pair.source(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findBySourceItem(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItem(pair.source(), stack) && matchesStorageKind(pair.source(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findByResult(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItemSameTags(pair.result(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findByResultItem(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItem(pair.result(), stack) && matchesStorageKind(pair.result(), stack)).findFirst();
	}

	private static boolean matchesStorageKind(ItemStack recipeStack, ItemStack focusedStack) {
		return !(recipeStack.getItem() instanceof ChestBlockItem) || ChestBlockItem.isDoubleChest(recipeStack) == ChestBlockItem.isDoubleChest(focusedStack);
	}

	private static TierUpgradeVariantPair withComponentsFromSource(TierUpgradeVariantPair pair, ItemStack sourceStack) {
		return new TierUpgradeVariantPair(sourceStack.copy(), copyWithItem(sourceStack, pair.result().getItem()));
	}

	private static TierUpgradeVariantPair withComponentsFromResult(TierUpgradeVariantPair pair, ItemStack resultStack) {
		return new TierUpgradeVariantPair(copyWithItem(resultStack, pair.source().getItem()), resultStack.copy());
	}

	private static ItemStack copyWithItem(ItemStack stack, Item item) {
		ItemStack copy = new ItemStack(item, stack.getCount());
		copy.setTag(stack.getTag() == null ? null : stack.getTag().copy());
		return copy;
	}

	public CraftingDisplaySpec toSpec() {
		VariantLookup variantLookup = new VariantLookup(variantPairs);
		return new CraftingDisplaySpec(id, shapeless, width, height, ingredients, variantPairs.stream().map(this::toVariant).toList(), getGlobalVariants(),
				Set.of(recipe.getId()),
				new SourceResultFocusBehavior(storageIngredientIndex, (variant, focusedInput) -> focusSource(variantLookup, variant, focusedInput),
						(variant, focusedOutput) -> focusResult(variantLookup, variant, focusedOutput)));
	}

	private List<CraftingDisplayVariant> getGlobalVariants() {
		return variantPairs.stream()
				.filter(pair -> StorageBlockItem.getMainColorFromStack(pair.source()).isEmpty()
						&& StorageBlockItem.getAccentColorFromStack(pair.source()).isEmpty() && StorageBlockItem.getMainColorFromStack(pair.result()).isEmpty()
						&& StorageBlockItem.getAccentColorFromStack(pair.result()).isEmpty())
				.map(this::toVariant).toList();
	}

	private CraftingDisplayVariant toVariant(TierUpgradeVariantPair pair) {
		List<ItemStack> inputs = new ArrayList<>(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++) {
			inputs.add(i == storageIngredientIndex ? pair.source() : ItemStack.EMPTY);
		}
		return new CraftingDisplayVariant(inputs, List.of(pair.result()));
	}

	private Optional<CraftingDisplayVariant> focusSource(VariantLookup variantLookup, CraftingDisplayVariant variant, ItemStack focusedInput) {
		ItemStack source = getSource(variant);
		Optional<TierUpgradeVariantPair> exactPair = variantLookup.findBySource(focusedInput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameTags(source, pair.source())).map(this::toVariant);
		}
		return variantLookup.findBySourceItem(focusedInput).filter(pair -> ItemStack.isSameItemSameTags(source, pair.source()))
				.map(pair -> withComponentsFromSource(pair, focusedInput)).map(this::toVariant);
	}

	private Optional<CraftingDisplayVariant> focusResult(VariantLookup variantLookup, CraftingDisplayVariant variant, ItemStack focusedOutput) {
		Optional<TierUpgradeVariantPair> exactPair = variantLookup.findByResult(focusedOutput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameTags(variant.firstOutput(), pair.result())).map(this::toVariant);
		}
		return variantLookup.findByResultItem(focusedOutput).filter(pair -> ItemStack.isSameItemSameTags(variant.firstOutput(), pair.result()))
				.map(pair -> withComponentsFromResult(pair, focusedOutput)).map(this::toVariant);
	}

	private static ItemStack getSource(CraftingDisplayVariant variant) {
		return variant.inputs().stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
	}

	private static class VariantLookup {
		private final Map<Integer, List<TierUpgradeVariantPair>> sourcePairsByHash = new HashMap<>();
		private final Map<Integer, List<TierUpgradeVariantPair>> resultPairsByHash = new HashMap<>();
		private final Map<Item, TierUpgradeVariantPair> sourcePairsByItem = new HashMap<>();
		private final Map<Item, TierUpgradeVariantPair> doubleChestSourcePairsByItem = new HashMap<>();
		private final Map<Item, TierUpgradeVariantPair> resultPairsByItem = new HashMap<>();
		private final Map<Item, TierUpgradeVariantPair> doubleChestResultPairsByItem = new HashMap<>();

		private VariantLookup(List<TierUpgradeVariantPair> variantPairs) {
			for (TierUpgradeVariantPair pair : variantPairs) {
				addPair(sourcePairsByHash, pair.source(), pair);
				addPair(resultPairsByHash, pair.result(), pair);
				getItemPairs(pair.source(), sourcePairsByItem, doubleChestSourcePairsByItem).putIfAbsent(pair.source().getItem(), pair);
				getItemPairs(pair.result(), resultPairsByItem, doubleChestResultPairsByItem).putIfAbsent(pair.result().getItem(), pair);
			}
		}

		private Optional<TierUpgradeVariantPair> findBySource(ItemStack stack) {
			return findExact(sourcePairsByHash, stack, TierUpgradeVariantPair::source);
		}

		private Optional<TierUpgradeVariantPair> findBySourceItem(ItemStack stack) {
			return Optional.ofNullable(getItemPairs(stack, sourcePairsByItem, doubleChestSourcePairsByItem).get(stack.getItem()));
		}

		private Optional<TierUpgradeVariantPair> findByResult(ItemStack stack) {
			return findExact(resultPairsByHash, stack, TierUpgradeVariantPair::result);
		}

		private Optional<TierUpgradeVariantPair> findByResultItem(ItemStack stack) {
			return Optional.ofNullable(getItemPairs(stack, resultPairsByItem, doubleChestResultPairsByItem).get(stack.getItem()));
		}

		private static void addPair(Map<Integer, List<TierUpgradeVariantPair>> pairsByHash, ItemStack stack, TierUpgradeVariantPair pair) {
			pairsByHash.computeIfAbsent(getBucketHash(stack), key -> new ArrayList<>()).add(pair);
		}

		private static Optional<TierUpgradeVariantPair> findExact(Map<Integer, List<TierUpgradeVariantPair>> pairsByHash, ItemStack stack,
				Function<TierUpgradeVariantPair, ItemStack> stackGetter) {
			List<TierUpgradeVariantPair> pairs = pairsByHash.get(getBucketHash(stack));
			if (pairs == null) {
				return Optional.empty();
			}
			return pairs.stream().filter(pair -> ItemStack.isSameItemSameTags(stackGetter.apply(pair), stack)).findFirst();
		}

		private static Map<Item, TierUpgradeVariantPair> getItemPairs(ItemStack stack, Map<Item, TierUpgradeVariantPair> pairsByItem,
				Map<Item, TierUpgradeVariantPair> doubleChestPairsByItem) {
			return stack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(stack) ? doubleChestPairsByItem : pairsByItem;
		}

		private static int getBucketHash(ItemStack stack) {
			return 31 * stack.getItem().hashCode() + Objects.hashCode(stack.getTag());
		}
	}
}
