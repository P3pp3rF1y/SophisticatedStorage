package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class NonSophisticatedChestIngredient implements ICustomIngredient {
	public static final NonSophisticatedChestIngredient INSTANCE = new NonSophisticatedChestIngredient();
	public static final MapCodec<NonSophisticatedChestIngredient> CODEC = MapCodec.unit(INSTANCE).stable();

	private NonSophisticatedChestIngredient() {
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		return stack != null && stack.is(Tags.Items.CHESTS) && !isSophisticatedChest(stack.getItem());
	}

	@Override
	public Stream<ItemStack> getItems() {
		return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(Tags.Items.CHESTS).spliterator(), false)
				.filter(item -> !isSophisticatedChest(item.value())).map(Holder::value).map(ItemStack::new);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return ModBlocks.NON_SOPHISTICATED_CHEST_INGREDIENT_TYPE.get();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NonSophisticatedChestIngredient;
	}

	@Override
	public int hashCode() {
		return NonSophisticatedChestIngredient.class.hashCode();
	}

	private static boolean isSophisticatedChest(Item item) {
		return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ChestBlock;
	}
}
