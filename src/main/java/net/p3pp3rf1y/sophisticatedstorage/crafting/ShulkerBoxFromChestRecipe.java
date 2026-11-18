package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ShulkerBoxFromChestRecipe extends ShapedRecipe implements IWrapperRecipe<ShapedRecipe> {
	public static final Set<ResourceLocation> REGISTERED_RECIPES = new LinkedHashSet<>();

	private final ShapedRecipe compose;

	public ShulkerBoxFromChestRecipe(ShapedRecipe compose) {
		super(compose.getId(), compose.getGroup(), compose.category(), compose.getRecipeWidth(), compose.getRecipeHeight(), compose.getIngredients(),
				compose.getResultItem(null));
		this.compose = compose;
		REGISTERED_RECIPES.add(compose.getId());
	}

	@Override
	public boolean matches(CraftingContainer pInv, Level pLevel) {
		if (!super.matches(pInv, pLevel)) {
			return false;
		}
		ItemStack result = super.assemble(pInv, null);
		return getChest(pInv).map(chest -> canConvert(chest, result)).orElse(true);
	}

	private boolean canConvert(ItemStack chest, ItemStack shulker) {
		return !ChestBlockItem.isDoubleChest(chest) && tiersMatch(chest, shulker);
	}

	private boolean tiersMatch(ItemStack chest, ItemStack shulker) {
		return chest.is(ModBlocks.CHEST_ITEM.get()) && shulker.is(ModBlocks.SHULKER_BOX_ITEM.get())
				|| chest.is(ModBlocks.COPPER_CHEST_ITEM.get()) && shulker.is(ModBlocks.COPPER_SHULKER_BOX_ITEM.get())
				|| chest.is(ModBlocks.IRON_CHEST_ITEM.get()) && shulker.is(ModBlocks.IRON_SHULKER_BOX_ITEM.get())
				|| chest.is(ModBlocks.GOLD_CHEST_ITEM.get()) && shulker.is(ModBlocks.GOLD_SHULKER_BOX_ITEM.get())
				|| chest.is(ModBlocks.DIAMOND_CHEST_ITEM.get()) && shulker.is(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get())
				|| chest.is(ModBlocks.NETHERITE_CHEST_ITEM.get()) && shulker.is(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get());
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getChest(CraftingContainer inv) {
		for (int slot = 0; slot < inv.getContainerSize(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ChestBlock) {
				return Optional.of(slotStack);
			}
		}
		return Optional.empty();
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
		ItemStack shulker = super.assemble(inv, registryAccess);
		Optional<ItemStack> chest = getChest(inv);
		if (chest.isPresent() && !canConvert(chest.get(), shulker)) {
			return ItemStack.EMPTY;
		}
		chest.ifPresent(chestStack -> {
			if (chestStack.hasTag()) {
				CompoundTag tag = chestStack.getTag().copy();
				tag.remove(WoodStorageBlockItem.PACKED_TAG);
				tag.remove(WoodStorageBlockItem.WOOD_TYPE_TAG);
				tag.remove(ChestBlockItem.DOUBLE_CHEST_TAG);
				shulker.setTag(tag);
			}
		});
		return shulker;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.SHULKER_BOX_FROM_CHEST_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapedRecipe, ShulkerBoxFromChestRecipe> {
		public Serializer() {
			super(ShulkerBoxFromChestRecipe::new, RecipeSerializer.SHAPED_RECIPE);
		}
	}
}
