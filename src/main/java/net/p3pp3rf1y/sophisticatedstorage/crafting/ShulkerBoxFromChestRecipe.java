package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.List;
import java.util.Optional;

public class ShulkerBoxFromChestRecipe implements CraftingRecipe, IWrapperRecipe<ShapedRecipe> {
	public static final RecipeSerializer<ShulkerBoxFromChestRecipe> SERIALIZER = RecipeWrapperSerializer.create(ShulkerBoxFromChestRecipe::new,
			ShapedRecipe.SERIALIZER);
	private final ShapedRecipe compose;

	public ShulkerBoxFromChestRecipe(ShapedRecipe compose) {
		this.compose = compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return compose.matches(input, level) && getChest(input).map(c -> !WoodStorageBlockItem.isPacked(c)).orElse(false);
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getChest(CraftingInput inv) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ChestBlock) {
				return Optional.of(slotStack);
			}
		}
		return Optional.empty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack shulker = compose.assemble(input);
		getChest(input).ifPresent(chest -> {
			if (chest.has(DataComponents.CUSTOM_NAME)) {
				shulker.set(DataComponents.CUSTOM_NAME, chest.getHoverName());
			}
			if (shulker.getItem() instanceof StorageBlockItem storageBlockItem) {
				StorageBlockItem.getMainColorFromComponentHolder(chest).ifPresent(mc -> storageBlockItem.setMainColor(shulker, mc));
				StorageBlockItem.getAccentColorFromComponentHolder(chest).ifPresent(ac -> storageBlockItem.setAccentColor(shulker, ac));
			}
		});
		return shulker;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public RecipeSerializer<ShulkerBoxFromChestRecipe> getSerializer() {
		return ModBlocks.SHULKER_BOX_FROM_CHEST_RECIPE_SERIALIZER.get();
	}

	@Override
	public boolean showNotification() {
		return compose.showNotification();
	}

	@Override
	public String group() {
		return compose.group();
	}

	@Override
	public CraftingBookCategory category() {
		return compose.category();
	}

	@Override
	public PlacementInfo placementInfo() {
		return compose.placementInfo();
	}

	@Override
	public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
		return compose.display();
	}

}
