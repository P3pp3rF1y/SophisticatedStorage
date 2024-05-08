package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseTierWoodenStorageIngredient extends Ingredient {
	public static final BaseTierWoodenStorageIngredient INSTANCE = new BaseTierWoodenStorageIngredient();
	private BaseTierWoodenStorageIngredient() {
		super(getChestsAndBarrels());
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

	public static class Serializer implements IIngredientSerializer<BaseTierWoodenStorageIngredient>
	{
		public static final Serializer INSTANCE = new Serializer();

		@Override
		public BaseTierWoodenStorageIngredient parse(FriendlyByteBuf buffer) {
			return new BaseTierWoodenStorageIngredient();
		}

		@Override
		public BaseTierWoodenStorageIngredient parse(JsonObject json) {
			return new BaseTierWoodenStorageIngredient();
		}

		@Override
		public void write(FriendlyByteBuf buffer, BaseTierWoodenStorageIngredient ingredient) {
			//noop
		}
	}
}
