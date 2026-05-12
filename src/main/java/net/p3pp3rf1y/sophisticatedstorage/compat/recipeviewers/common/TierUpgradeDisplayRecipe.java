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
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
		return new CraftingDisplaySpec(id, shapeless, width, height, ingredients, variantPairs.stream().map(this::toVariant).toList(), getGlobalVariants(), Set.of(recipe.getId()),
				new SourceResultFocusBehavior(storageIngredientIndex, this::focusSource, this::focusResult));
	}

	private List<CraftingDisplayVariant> getGlobalVariants() {
		return variantPairs.stream()
				.filter(pair -> StorageBlockItem.getMainColorFromStack(pair.source()).isEmpty() && StorageBlockItem.getAccentColorFromStack(pair.source()).isEmpty()
						&& StorageBlockItem.getMainColorFromStack(pair.result()).isEmpty() && StorageBlockItem.getAccentColorFromStack(pair.result()).isEmpty())
				.map(this::toVariant)
				.toList();
	}

	private CraftingDisplayVariant toVariant(TierUpgradeVariantPair pair) {
		List<ItemStack> inputs = new ArrayList<>(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++) {
			inputs.add(i == storageIngredientIndex ? pair.source() : ItemStack.EMPTY);
		}
		return new CraftingDisplayVariant(inputs, List.of(pair.result()));
	}

	private Optional<CraftingDisplayVariant> focusSource(CraftingDisplayVariant variant, ItemStack focusedInput) {
		ItemStack source = getSource(variant);
		Optional<TierUpgradeVariantPair> exactPair = findBySource(focusedInput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameTags(source, pair.source())).map(this::toVariant);
		}
		return findBySourceItem(focusedInput)
				.filter(pair -> ItemStack.isSameItemSameTags(source, pair.source()))
				.map(pair -> withComponentsFromSource(pair, focusedInput))
				.map(this::toVariant);
	}

	private Optional<CraftingDisplayVariant> focusResult(CraftingDisplayVariant variant, ItemStack focusedOutput) {
		Optional<TierUpgradeVariantPair> exactPair = findByResult(focusedOutput);
		if (exactPair.isPresent()) {
			return exactPair.filter(pair -> ItemStack.isSameItemSameTags(variant.firstOutput(), pair.result())).map(this::toVariant);
		}
		return findByResultItem(focusedOutput)
				.filter(pair -> ItemStack.isSameItemSameTags(variant.firstOutput(), pair.result()))
				.map(pair -> withComponentsFromResult(pair, focusedOutput))
				.map(this::toVariant);
	}

	private static ItemStack getSource(CraftingDisplayVariant variant) {
		return variant.inputs().stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
	}
}
