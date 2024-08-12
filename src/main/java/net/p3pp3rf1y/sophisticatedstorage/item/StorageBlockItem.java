package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Optional;

public class StorageBlockItem extends BlockItemBase implements ITintableBlockItem {

    public StorageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static Optional<Integer> getMainColorFromStack(ItemStack storageStack) {
        return Optional.ofNullable(storageStack.get(ModCoreDataComponents.MAIN_COLOR));
    }

    public static Optional<Integer> getAccentColorFromStack(ItemStack storageStack) {
        return Optional.ofNullable(storageStack.get(ModCoreDataComponents.ACCENT_COLOR));
    }

    @Override
    public void setMainColor(ItemStack storageStack, int mainColor) {
        storageStack.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
    }

    @Override
    public Optional<Integer> getMainColor(ItemStack storageStack) {
        return StorageBlockItem.getMainColorFromStack(storageStack);
    }

    @Override
    public void setAccentColor(ItemStack storageStack, int accentColor) {
        storageStack.set(ModCoreDataComponents.ACCENT_COLOR, accentColor);
    }

    @Override
    public void removeMainColor(ItemStack stack) {
        stack.remove(ModCoreDataComponents.MAIN_COLOR);
    }

    @Override
    public void removeAccentColor(ItemStack stack) {
        stack.remove(ModCoreDataComponents.ACCENT_COLOR);
    }

    @Override
    public Optional<Integer> getAccentColor(ItemStack stack) {
        return StorageBlockItem.getAccentColorFromStack(stack);
    }

    public static boolean showsTier(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SHOWS_TIER, true);
    }

    public static void setShowsTier(ItemStack stack, boolean showsTier) {
        if (showsTier) {
            stack.remove(ModDataComponents.SHOWS_TIER);
        } else {
            stack.set(ModDataComponents.SHOWS_TIER, false);
        }
    }
}
