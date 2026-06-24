package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.p3pp3rf1y.sophisticatedcore.crafting.StorageDyeRecipeBase;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.List;

public class StorageDyeRecipe extends StorageDyeRecipeBase {
	public static final StorageDyeRecipe INSTANCE = new StorageDyeRecipe(CraftingBookCategory.MISC);
	public static final MapCodec<StorageDyeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, StorageDyeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<StorageDyeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public StorageDyeRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public RecipeSerializer<StorageDyeRecipe> getSerializer() {
		return ModBlocks.STORAGE_DYE_RECIPE_SERIALIZER.get();
	}

	@Override
	protected boolean isDyeableStorageItem(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem instanceof ITintableBlockItem tintableBlockItem
				&& tintableBlockItem.isTintable(stack);
	}

	@Override
	protected void applyColors(ItemStack coloredStorage, List<DyeColor> mainDyes, List<DyeColor> trimDyes) {
		if (coloredStorage.getItem() instanceof BlockItem blockItem && blockItem instanceof ITintableBlockItem tintableBlockItem) {
			if (!mainDyes.isEmpty()) {
				tintableBlockItem.setMainColor(coloredStorage,
						ColorHelper.calculateColor(tintableBlockItem.getMainColor(coloredStorage).orElse(0), 0, mainDyes));
			}
			if (!trimDyes.isEmpty()) {
				tintableBlockItem.setAccentColor(coloredStorage,
						ColorHelper.calculateColor(tintableBlockItem.getAccentColor(coloredStorage).orElse(0), 0, trimDyes));
			}
		}
	}
}
