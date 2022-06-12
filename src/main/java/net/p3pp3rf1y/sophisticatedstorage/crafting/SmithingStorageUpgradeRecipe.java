package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.RecipeWrapperSerializer;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.Objects;
import java.util.Optional;

public class SmithingStorageUpgradeRecipe extends UpgradeRecipe implements IWrapperRecipe<UpgradeRecipe> {
	public static final Serializer SERIALIZER = new Serializer();
	private final UpgradeRecipe compose;

	public SmithingStorageUpgradeRecipe(UpgradeRecipe compose) {
		super(compose.getId(), compose.base,
				compose.addition, compose.getResultItem());
		this.compose = compose;
	}

	@Override
	public boolean matches(Container pInv, Level pLevel) {
		return super.matches(pInv, pLevel) && getStorage(pInv).map(storage -> !(storage.getItem() instanceof WoodStorageBlockItem) || !WoodStorageBlockItem.isPacked(storage)).orElse(false);
	}

	@Override
	public ItemStack assemble(Container inv) {
		ItemStack upgradedStorage = getCraftingResult().copy();
		getStorage(inv).ifPresent(storage -> upgradedStorage.setTag(storage.getTag()));
		return upgradedStorage;
	}

	private ItemStack getCraftingResult() {
		return Objects.requireNonNull(ObfuscationReflectionHelper.getPrivateValue(UpgradeRecipe.class, this, "f_44520_"));
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	private Optional<ItemStack> getStorage(Container inv) {
		ItemStack slotStack = inv.getItem(0);
		if (slotStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof IStorageBlock) {
			return Optional.of(slotStack);
		}
		return Optional.empty();
	}

	@Override
	public Serializer getSerializer() {
		return SERIALIZER;
	}

	@Override
	public UpgradeRecipe getCompose() {
		return compose;
	}

	public static class Serializer extends RecipeWrapperSerializer<UpgradeRecipe, SmithingStorageUpgradeRecipe> {
		public Serializer() {
			super(SmithingStorageUpgradeRecipe::new, RecipeSerializer.SMITHING);
		}
	}
}
