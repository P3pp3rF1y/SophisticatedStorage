package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
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
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.Optional;

public class ShulkerBoxFromChestRecipe extends ShapedRecipe implements IWrapperRecipe<ShapedRecipe> {
	private final ShapedRecipe compose;

	public ShulkerBoxFromChestRecipe(ShapedRecipe compose) {
		super(compose.getGroup(), compose.category(), compose.pattern, compose.result);
		this.compose = compose;
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		return super.matches(inv, level) && getChest(inv).map(c -> !WoodStorageBlockItem.isPacked(c)).orElse(false);
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
		getChest(inv).ifPresent(chest -> {
			if (chest.hasCustomHoverName()) {
				shulker.setHoverName(chest.getHoverName());
			}
			if (shulker.getItem() instanceof StorageBlockItem storageBlockItem) {
				StorageBlockItem.getMainColorFromStack(chest).ifPresent(mc -> storageBlockItem.setMainColor(shulker, mc));
				StorageBlockItem.getAccentColorFromStack(chest).ifPresent(ac -> storageBlockItem.setAccentColor(shulker, ac));
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
