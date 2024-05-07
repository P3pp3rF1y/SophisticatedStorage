package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseTierWoodenStorageIngredient extends Ingredient {
	public static final BaseTierWoodenStorageIngredient INSTANCE = new BaseTierWoodenStorageIngredient();
	public static final Codec<BaseTierWoodenStorageIngredient> CODEC = MapCodec.unit(INSTANCE).stable().codec();

	public BaseTierWoodenStorageIngredient() {
		super(getChestsAndBarrels(), ModBlocks.BASE_TIER_WOODEN_STORAGE_INGREDIENT_TYPE);
	}

	private static Stream<? extends Value> getChestsAndBarrels() {
		Stream<? extends Value> chestsStream = Stream.empty();
		if (ModBlocks.CHEST_ITEM.get() instanceof BlockItemBase itemBase) {
			List<Value> chestIngredientValues = new ArrayList<>();
			itemBase.addCreativeTabItems(i -> chestIngredientValues.add(new Ingredient.ItemValue(i)));
			chestsStream = chestIngredientValues.stream();
		}
		Stream<? extends Value> barrelsStream = Stream.empty();
		if (ModBlocks.BARREL_ITEM.get() instanceof BlockItemBase itemBase) {
			List<Value> barrelIngredientValues = new ArrayList<>();
			itemBase.addCreativeTabItems(i -> barrelIngredientValues.add(new Ingredient.ItemValue(i)));
			barrelsStream = barrelIngredientValues.stream();
		}

		return Stream.concat(chestsStream, barrelsStream);
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		return stack != null && stack.is(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG);
	}
}
