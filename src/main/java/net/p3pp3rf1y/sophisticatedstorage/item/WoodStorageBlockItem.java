package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WoodStorageBlockItem extends StorageBlockItem {

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
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltip, tooltipFlag);
		if (isPacked(stack)) {
			if (tooltipFlag.isAdvanced()) {
				HolderLookup.Provider registries = context.registries();
				if (registries != null) {
					StackStorageWrapper.fromStack(registries, stack).getContentsUuid()
							.ifPresent(uuid -> tooltip.add(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY)));
				}
			}
			if (!Screen.hasShiftDown()) {
				tooltip.add(Component
						.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
								Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA))
						.withStyle(ChatFormatting.GRAY));
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
		super.setMainColor(storageStack, mainColor);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		super.setAccentColor(storageStack, accentColor);
	}

	public static Optional<WoodType> getWoodType(IDataComponentHolderExtension componentHolder) {
		return Optional.ofNullable(componentHolder.get(ModDataComponents.WOOD_TYPE));
	}

	public static ItemStack setWoodType(ItemStack storageStack, WoodType woodType) {
		storageStack.set(ModDataComponents.WOOD_TYPE, woodType);
		return storageStack;
	}

	@Override
	public Component getName(ItemStack stack) {
		return getDisplayName(getDescriptionId(), isFullyTinted(stack) ? null : getWoodType(stack).orElse(null));
	}

	private static boolean isFullyTinted(ItemStack stack) {
		return getMainColorFromComponentHolder(stack).isPresent() && getAccentColorFromComponentHolder(stack).isPresent();
	}

	public static Component getDisplayName(String descriptionId, @Nullable WoodType woodType) {
		if (woodType == null) {
			return Component.translatable(descriptionId, "", "");
		}
		return Component.translatable(descriptionId, Component.translatable("wood_name.sophisticatedstorage." + woodType.name().toLowerCase(Locale.ROOT)), " ");
	}
}
