package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ShulkerBoxFromVanillaShapelessRecipe extends ShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
	public static final Set<ResourceLocation> REGISTERED_RECIPES = new LinkedHashSet<>();
	private final ShapelessRecipe compose;

	public ShulkerBoxFromVanillaShapelessRecipe(ShapelessRecipe compose) {
		super(compose.getId(), compose.getGroup(), compose.category(), compose.getResultItem(null), compose.getIngredients());
		this.compose = compose;
		REGISTERED_RECIPES.add(compose.getId());
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level level) {
		return super.matches(inventory, level) && getVanillaShulkerBox(inventory).map(storage -> !(storage.getItem() instanceof WoodStorageBlockItem) || !WoodStorageBlockItem.isPacked(storage)).orElse(false);
	}

	@Override
	public ShapelessRecipe getCompose() {
		return compose;
	}

	@Override
	public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
		ItemStack upgradedStorage = super.assemble(input, registries);
		getVanillaShulkerBox(input).ifPresent(vanillaShulkerBox -> {

			vanillaShulkerBox.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(itemCap -> {
				upgradedStorage.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(wrapper -> {
					wrapper.ensureContentsUuid();
					InventoryHelper.iterate(itemCap, (slot, stack) -> {
						if (!stack.isEmpty()) {
							wrapper.getInventoryHandler().insertItem(stack.copy(), false);
						}
					});
				});
			});
		});
		return upgradedStorage;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getVanillaShulkerBox(CraftingContainer input) {
		for (int slot = 0; slot < input.getContainerSize(); slot++) {
			ItemStack slotStack = input.getItem(slot);
			if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
				return Optional.of(slotStack);
			}
		}

		return Optional.empty();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.SHULKER_BOX_FROM_VANILLA_SHAPELESS_RECIPE_SERIALIZER.get();
	}

	public static class Serializer extends RecipeWrapperSerializer<ShapelessRecipe, ShulkerBoxFromVanillaShapelessRecipe> {
		public Serializer() {
			super(ShulkerBoxFromVanillaShapelessRecipe::new, RecipeSerializer.SHAPELESS_RECIPE);
		}
	}
}
