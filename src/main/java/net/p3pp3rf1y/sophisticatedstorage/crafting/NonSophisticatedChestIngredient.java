package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;

import javax.annotation.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class NonSophisticatedChestIngredient extends Ingredient {
	public static final NonSophisticatedChestIngredient INSTANCE = new NonSophisticatedChestIngredient();

	private NonSophisticatedChestIngredient() {
		super(Stream.empty());
	}

	@Override
	public ItemStack[] getItems() {
		return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(Tags.Items.CHESTS).spliterator(), false)
				.filter(item -> !isSophisticatedChest(item.value())).map(Holder::value).map(ItemStack::new).toArray(ItemStack[]::new);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		return stack != null && stack.is(Tags.Items.CHESTS) && !isSophisticatedChest(stack.getItem());
	}

	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", CraftingHelper.getID(Serializer.INSTANCE).toString());
		return json;
	}

	@Override
	public IIngredientSerializer<? extends Ingredient> getSerializer() {
		return Serializer.INSTANCE;
	}

	private static boolean isSophisticatedChest(Item item) {
		return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ChestBlock;
	}

	public static class Serializer implements IIngredientSerializer<NonSophisticatedChestIngredient> {
		public static final Serializer INSTANCE = new Serializer();

		@Override
		public NonSophisticatedChestIngredient parse(FriendlyByteBuf buffer) {
			return NonSophisticatedChestIngredient.INSTANCE;
		}

		@Override
		public NonSophisticatedChestIngredient parse(JsonObject json) {
			return NonSophisticatedChestIngredient.INSTANCE;
		}

		@Override
		public void write(FriendlyByteBuf buffer, NonSophisticatedChestIngredient ingredient) {
			// no data beyond the registered ingredient type
		}
	}
}
