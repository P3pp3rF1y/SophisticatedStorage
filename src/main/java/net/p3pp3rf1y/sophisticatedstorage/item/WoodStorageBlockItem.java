package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WoodStorageBlockItem extends StorageBlockItem {

    public static final StreamCodec<FriendlyByteBuf, WoodType> WOOD_TYPE_STREAM_CODEC =
            StreamCodec.of((buf, wt) -> buf.writeUtf(wt.name()), buf -> {
                WoodType woodType = WoodType.TYPES.get(buf.readUtf());
                return woodType == null ? WoodType.OAK : woodType;
            });

    public WoodStorageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static void setPacked(ItemStack storageStack, boolean packed) {
        storageStack.set(ModDataComponents.PACKED, packed);
    }

    public static boolean isPacked(ItemStack storageStack) {
        return storageStack.getOrDefault(ModDataComponents.PACKED, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        if (isPacked(stack)) {
            if (flagIn == TooltipFlag.ADVANCED) {
                HolderLookup.Provider registries = context.registries();
                if (registries != null) {
                    StackStorageWrapper.fromStack(registries, stack).getContentsUuid().ifPresent(uuid -> tooltip.add(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY)));
                }
            }
            if (!Screen.hasShiftDown()) {
                tooltip.add(Component.translatable(
                        TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
                        Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA)
                ).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!isPacked(stack)) {
            return Optional.empty();
        }

        if (FMLEnvironment.dist.isClient()) {
            return Optional.ofNullable(StorageItemClient.getTooltipImage(stack));
        }
        return Optional.empty();
    }

    @Override
    public void setMainColor(ItemStack storageStack, int mainColor) {
        if (StorageBlockItem.getAccentColorFromStack(storageStack).isPresent()) {
            removeWoodType(storageStack);
        }
        super.setMainColor(storageStack, mainColor);
    }

    @Override
    public void setAccentColor(ItemStack storageStack, int accentColor) {
        if (StorageBlockItem.getMainColorFromStack(storageStack).isPresent()) {
            removeWoodType(storageStack);
        }
        super.setAccentColor(storageStack, accentColor);
    }

    private void removeWoodType(ItemStack storageStack) {
        storageStack.remove(ModDataComponents.WOOD_TYPE);
    }

    public static Optional<WoodType> getWoodType(ItemStack storageStack) {
        return Optional.ofNullable(storageStack.get(ModDataComponents.WOOD_TYPE));
    }

    public static ItemStack setWoodType(ItemStack storageStack, WoodType woodType) {
        storageStack.set(ModDataComponents.WOOD_TYPE, woodType);
        return storageStack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return getDisplayName(getDescriptionId(), getWoodType(stack).orElse(null));
    }

    public static Component getDisplayName(String descriptionId, @Nullable WoodType woodType) {
        if (woodType == null) {
            return Component.translatable(descriptionId, "", "");
        }
        return Component.translatable(descriptionId, Component.translatable("wood_name.sophisticatedstorage." + woodType.name().toLowerCase(Locale.ROOT)), " ");
    }
}
