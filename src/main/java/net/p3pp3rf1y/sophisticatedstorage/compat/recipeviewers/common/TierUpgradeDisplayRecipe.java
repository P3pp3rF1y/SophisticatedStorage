package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record TierUpgradeDisplayRecipe(ResourceLocation id, RecipeHolder<CraftingRecipe> recipeHolder, boolean shapeless, int width, int height,
		NonNullList<Ingredient> ingredients, int storageIngredientIndex, List<TierUpgradeVariantPair> variantPairs) {
	public Optional<TierUpgradeVariantPair> findBySource(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItemSameComponents(pair.source(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findBySourceItem(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItem(pair.source(), stack) && matchesStorageKind(pair.source(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findByResult(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItemSameComponents(pair.result(), stack)).findFirst();
	}

	public Optional<TierUpgradeVariantPair> findByResultItem(ItemStack stack) {
		return variantPairs.stream().filter(pair -> ItemStack.isSameItem(pair.result(), stack) && matchesStorageKind(pair.result(), stack)).findFirst();
	}

	private static boolean matchesStorageKind(ItemStack recipeStack, ItemStack focusedStack) {
		return !(recipeStack.getItem() instanceof ChestBlockItem) || ChestBlockItem.isDoubleChest(recipeStack) == ChestBlockItem.isDoubleChest(focusedStack);
	}

	private static TierUpgradeVariantPair withComponentsFromSource(TierUpgradeVariantPair pair, ItemStack sourceStack) {
		return new TierUpgradeVariantPair(sourceStack.copy(), sourceStack.transmuteCopy(pair.result().getItem(), 1));
	}

	private static TierUpgradeVariantPair withComponentsFromResult(TierUpgradeVariantPair pair, ItemStack resultStack) {
		return new TierUpgradeVariantPair(resultStack.transmuteCopy(pair.source().getItem(), 1), resultStack.copy());
	}

	public CraftingDisplaySpec toSpec() {
		VariantLookup lookup = new VariantLookup(variantPairs);
		return new CraftingDisplaySpec(id, shapeless, width, height, ingredients, variantPairs.stream().map(this::toVariant).toList(), getGlobalVariants(),
				Set.of(recipeHolder.id()),
				new SourceResultFocusBehavior(storageIngredientIndex, (variant, focusedInput) -> focusSource(variant, focusedInput, lookup),
						(variant, focusedOutput) -> focusResult(variant, focusedOutput, lookup)));
	}

	private List<CraftingDisplayVariant> getGlobalVariants() {
		return variantPairs.stream()
				.filter(pair -> StorageBlockItem.getMainColorFromComponentHolder(pair.source()).isEmpty()
						&& StorageBlockItem.getAccentColorFromComponentHolder(pair.source()).isEmpty()
						&& StorageBlockItem.getMainColorFromComponentHolder(pair.result()).isEmpty()
						&& StorageBlockItem.getAccentColorFromComponentHolder(pair.result()).isEmpty())
				.map(this::toVariant).toList();
	}

	private CraftingDisplayVariant toVariant(TierUpgradeVariantPair pair) {
		List<ItemStack> inputs = new ArrayList<>(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++) {
			inputs.add(i == storageIngredientIndex ? pair.source() : ItemStack.EMPTY);
		}
		return new CraftingDisplayVariant(inputs, List.of(pair.result()));
	}

	private Optional<CraftingDisplayVariant> focusSource(CraftingDisplayVariant variant, ItemStack focusedInput, VariantLookup lookup) {
		ItemStack source = getSource(variant);
		Optional<TierUpgradeVariantPair> exactPair = lookup.findBySource(focusedInput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameComponents(source, pair.source())).map(this::toVariant);
		}
		return lookup.findBySourceItem(focusedInput).filter(pair -> ItemStack.isSameItemSameComponents(source, pair.source()))
				.map(pair -> withComponentsFromSource(pair, focusedInput)).map(this::toVariant);
	}

	private Optional<CraftingDisplayVariant> focusResult(CraftingDisplayVariant variant, ItemStack focusedOutput, VariantLookup lookup) {
		Optional<TierUpgradeVariantPair> exactPair = lookup.findByResult(focusedOutput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameComponents(variant.firstOutput(), pair.result())).map(this::toVariant);
		}
		return lookup.findByResultItem(focusedOutput).filter(pair -> ItemStack.isSameItemSameComponents(variant.firstOutput(), pair.result()))
				.map(pair -> withComponentsFromResult(pair, focusedOutput)).map(this::toVariant);
	}

	private static ItemStack getSource(CraftingDisplayVariant variant) {
		return variant.inputs().stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
	}

	private static final class VariantLookup {
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
				addFirstPair(sourcePairsByItem, doubleChestSourcePairsByItem, pair.source(), pair);
				addFirstPair(resultPairsByItem, doubleChestResultPairsByItem, pair.result(), pair);
			}
		}

		private Optional<TierUpgradeVariantPair> findBySource(ItemStack stack) {
			List<TierUpgradeVariantPair> pairs = sourcePairsByHash.get(ItemStack.hashItemAndComponents(stack));
			if (pairs != null) {
				for (TierUpgradeVariantPair pair : pairs) {
					if (ItemStack.isSameItemSameComponents(pair.source(), stack)) {
						return Optional.of(pair);
					}
				}
			}
			return Optional.empty();
		}

		private Optional<TierUpgradeVariantPair> findBySourceItem(ItemStack stack) {
			return Optional.ofNullable(getFirstPair(sourcePairsByItem, doubleChestSourcePairsByItem, stack));
		}

		private Optional<TierUpgradeVariantPair> findByResult(ItemStack stack) {
			List<TierUpgradeVariantPair> pairs = resultPairsByHash.get(ItemStack.hashItemAndComponents(stack));
			if (pairs != null) {
				for (TierUpgradeVariantPair pair : pairs) {
					if (ItemStack.isSameItemSameComponents(pair.result(), stack)) {
						return Optional.of(pair);
					}
				}
			}
			return Optional.empty();
		}

		private Optional<TierUpgradeVariantPair> findByResultItem(ItemStack stack) {
			return Optional.ofNullable(getFirstPair(resultPairsByItem, doubleChestResultPairsByItem, stack));
		}

		private static void addPair(Map<Integer, List<TierUpgradeVariantPair>> pairsByHash, ItemStack stack, TierUpgradeVariantPair pair) {
			pairsByHash.computeIfAbsent(ItemStack.hashItemAndComponents(stack), ignored -> new ArrayList<>()).add(pair);
		}

		private static void addFirstPair(Map<Item, TierUpgradeVariantPair> pairsByItem, Map<Item, TierUpgradeVariantPair> doubleChestPairsByItem,
				ItemStack stack, TierUpgradeVariantPair pair) {
			Map<Item, TierUpgradeVariantPair> pairs = stack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(stack)
					? doubleChestPairsByItem
					: pairsByItem;
			pairs.putIfAbsent(stack.getItem(), pair);
		}

		private static TierUpgradeVariantPair getFirstPair(Map<Item, TierUpgradeVariantPair> pairsByItem,
				Map<Item, TierUpgradeVariantPair> doubleChestPairsByItem, ItemStack stack) {
			return (stack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(stack) ? doubleChestPairsByItem : pairsByItem)
					.get(stack.getItem());
		}
	}
}
