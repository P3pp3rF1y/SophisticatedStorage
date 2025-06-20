package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ShapelessRecipeBase implements CraftingRecipe {
	protected final ItemStack result;
	private final List<Ingredient> ingredients;
	@Nullable
	private PlacementInfo placementInfo;
	private final boolean isSimple;

	protected ShapelessRecipeBase(ItemStack result, List<Ingredient> ingredients) {
		this.result = result;
		this.ingredients = ingredients;
		this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
	}

	@Override
	public boolean matches(CraftingInput craftingInput, Level level) {
		if (craftingInput.ingredientCount() != ingredients.size()) {
			return false;
		} else if (!isSimple) {
			ArrayList<ItemStack> nonEmptyItems = new ArrayList<>(craftingInput.ingredientCount());

			for (ItemStack item : craftingInput.items()) {
				if (!item.isEmpty()) {
					nonEmptyItems.add(item);
				}
			}

			return RecipeMatcher.findMatches(nonEmptyItems, ingredients) != null;
		} else {
			return craftingInput.size() == 1 && ingredients.size() == 1 ? ingredients.getFirst().test(craftingInput.getItem(0)) : craftingInput.stackedContents().canCraft(this, null);
		}
	}

	@Override
	public PlacementInfo placementInfo() {
		if (placementInfo == null) {
			placementInfo = PlacementInfo.create(ingredients);
		}

		return placementInfo;
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new ShapelessCraftingRecipeDisplay(ingredients.stream().map(Ingredient::display).toList(), new SlotDisplay.ItemStackSlotDisplay(result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	public ItemStack result() {
		return result;
	}

	public List<Ingredient> ingredients() {
		return ingredients;
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

	public interface IRecipeConstructor<T extends ShapelessRecipeBase> {
		T create(ItemStack result, List<Ingredient> ingredients);
	}

	protected static abstract class Serializer<T extends ShapelessRecipeBase> implements RecipeSerializer<T> {
		@Nullable
		private MapCodec<T> codec = null;
		@Nullable
		private StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = null;
		private final IRecipeConstructor<T> constructor;

		public Serializer(IRecipeConstructor<T> constructor) {
			this.constructor = constructor;
		}

		@Override
		public MapCodec<T> codec() {
			if (codec == null) {
				codec = RecordCodecBuilder.mapCodec(
						builder -> builder.group(
								ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ShapelessRecipeBase::result),
								Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, 9)).fieldOf("ingredients").forGetter(ShapelessRecipeBase::ingredients)
						).apply(builder, constructor::create));
			}
			return codec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
			if (streamCodec == null) {
				streamCodec = StreamCodec.composite(
						ItemStack.STREAM_CODEC,
						ShapelessRecipeBase::result,
						Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
						ShapelessRecipeBase::ingredients,
						constructor::create);
			}
			return streamCodec;
		}
	}
}
