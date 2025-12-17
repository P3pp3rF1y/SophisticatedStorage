package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.crafting.CustomShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ShulkerBoxFromVanillaShapelessRecipe extends CustomShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	private final ShapelessRecipe compose;

	public ShulkerBoxFromVanillaShapelessRecipe(ShapelessRecipe compose) {
		super("", CraftingBookCategory.MISC, compose.result, compose.ingredients);
		this.compose = compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && getVanillaShulkerBox(input).map(storage -> !(storage.getItem() instanceof WoodStorageBlockItem) || !WoodStorageBlockItem.isPacked(storage)).orElse(false);
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack upgradedStorage = super.assemble(input, registries);
		getVanillaShulkerBox(input).ifPresent(vanillaShulkerBox -> {
			@Nullable ResourceHandler<ItemResource> itemCap = vanillaShulkerBox.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(vanillaShulkerBox));
			if (itemCap != null) {
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(registries, upgradedStorage);
				try (Transaction tx = Transaction.openRoot()) {
					InventoryHelper.iterate(itemCap, (slot, resource, amount) -> {
						if (!resource.isEmpty()) {
							wrapper.getInventoryHandler().insert(resource, amount, tx);
						}
					});
					tx.commit();
				}
			}
		});
		return upgradedStorage;
	}

	private Optional<ItemStack> getVanillaShulkerBox(CraftingInput input) {
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack slotStack = input.getItem(slot);
			if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<ShulkerBoxFromVanillaShapelessRecipe> getSerializer() {
		return ModBlocks.SHULKER_BOX_FROM_VANILLA_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapelessRecipe, ShulkerBoxFromVanillaShapelessRecipe> {
		public Serializer() {
			super(ShulkerBoxFromVanillaShapelessRecipe::new, RecipeSerializer.SHAPELESS_RECIPE);
		}
	}
}
