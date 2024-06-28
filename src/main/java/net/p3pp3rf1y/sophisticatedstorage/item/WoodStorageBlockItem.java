package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WoodStorageBlockItem extends StorageBlockItem {
	public static final String WOOD_TYPE_TAG = "woodType";
	public static final String PACKED_TAG = "packed";

	public WoodStorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static void setPacked(ItemStack storageStack, boolean packed) {
		storageStack.getOrCreateTag().putBoolean(PACKED_TAG, packed);
	}

	public static boolean isPacked(ItemStack storageStack) {
		return NBTHelper.getBoolean(storageStack, PACKED_TAG).orElse(false);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, context, tooltip, flagIn);
		if (isPacked(stack)) {
			if (flagIn == TooltipFlag.ADVANCED) {
				StackStorageWrapper.fromData(stack).getContentsUuid().ifPresent(uuid -> tooltip.add(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY)));
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
		storageStack.getOrCreateTag().remove(WoodStorageBlockItem.WOOD_TYPE_TAG);
	}

	public static Optional<WoodType> getWoodType(ItemStack storageStack) {
		return NBTHelper.getString(storageStack, WOOD_TYPE_TAG)
				.flatMap(woodType -> WoodType.values().filter(wt -> wt.name().equals(woodType)).findFirst());
	}

	public static ItemStack setWoodType(ItemStack storageStack, WoodType woodType) {
		storageStack.getOrCreateTag().putString(WOOD_TYPE_TAG, woodType.name());
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
