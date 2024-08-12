package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseTierWoodenStorageIngredient implements ICustomIngredient {
	public static final BaseTierWoodenStorageIngredient INSTANCE = new BaseTierWoodenStorageIngredient();
	public static final MapCodec<BaseTierWoodenStorageIngredient> CODEC = MapCodec.unit(INSTANCE).stable();

	@Override
	public boolean test(@Nullable ItemStack stack) {
		return stack != null && stack.is(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG);
	}

	@Override
	public Stream<ItemStack> getItems() {
		Stream<ItemStack> chestsStream = Stream.empty();
		if (ModBlocks.CHEST_ITEM.get() instanceof BlockItemBase itemBase) {
			List<ItemStack> chestIngredientValues = new ArrayList<>();
			itemBase.addCreativeTabItems(chestIngredientValues::add);
			chestsStream = chestIngredientValues.stream();
		}
		Stream<ItemStack> barrelsStream = Stream.empty();
		if (ModBlocks.BARREL_ITEM.get() instanceof BlockItemBase itemBase) {
			List<ItemStack> barrelIngredientValues = new ArrayList<>();
			itemBase.addCreativeTabItems(barrelIngredientValues::add);
			barrelsStream = barrelIngredientValues.stream();
		}
		return Stream.concat(chestsStream, barrelsStream);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return ModBlocks.BASE_TIER_WOODEN_STORAGE_INGREDIENT_TYPE.get();
	}
}
