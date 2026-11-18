package net.p3pp3rf1y.sophisticatedstorage.crafting;

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
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;

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
		if (!compose.matches(input, level)) {
			return false;
		}
		ItemStack result = compose.assemble(input);
		return getChest(input).map(chest -> canConvert(chest, result)).orElse(true);
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
		Optional<ItemStack> chest = getChest(input);
		if (chest.isPresent() && !canConvert(chest.get(), shulker)) {
			return ItemStack.EMPTY;
		}
		chest.ifPresent(chestStack -> {
			shulker.applyComponents(chestStack.getComponentsPatch());
			shulker.remove(ModDataComponents.PACKED);
			shulker.remove(ModDataComponents.WOOD_TYPE);
			shulker.remove(ModDataComponents.DOUBLE_CHEST);
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
