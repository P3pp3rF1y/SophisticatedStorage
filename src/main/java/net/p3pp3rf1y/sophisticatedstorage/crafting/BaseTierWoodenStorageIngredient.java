package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

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
	public Stream<Holder<Item>> items() {
		return Stream.of(ModBlocks.CHEST_ITEM, ModBlocks.BARREL_ITEM);
	}

	@Override
	public SlotDisplay display() {
		List<ItemStack> items = new ArrayList<>();
		if (ModBlocks.CHEST_ITEM.get() instanceof BlockItemBase itemBase) {
			itemBase.addCreativeTabItems(items::add);
		}
		if (ModBlocks.BARREL_ITEM.get() instanceof BlockItemBase itemBase) {
			itemBase.addCreativeTabItems(items::add);
		}

		return new SlotDisplay.Composite(items.stream().map(SlotDisplay.ItemStackSlotDisplay::new).map(SlotDisplay.class::cast).toList());
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
