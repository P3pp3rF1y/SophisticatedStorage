package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.function.Consumer;

public class StorageConnectorBlockItem extends SimpleMaterialBlockItem {
	public static final String REGISTRY_NAME_WITHOUT_WOOD_TYPE = "storage_connector";
	public static final String BLOCK_TRANSLATION_KEY = Util.makeDescriptionId("block", SophisticatedStorage.getRL(REGISTRY_NAME_WITHOUT_WOOD_TYPE));
	public static final String TOOLTIP_TRANSLATION_KEY = BLOCK_TRANSLATION_KEY + TranslationHelper.TOOLTIP_SUFFIX;

	public StorageConnectorBlockItem(Block block, Properties properties) {
		super(block, properties);
	}


	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
		StorageTranslationHelper.INSTANCE.getTranslatedLines(TOOLTIP_TRANSLATION_KEY, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
	}

	@Override
	public Component getName(ItemStack stack) {
		if (getMaterial(stack).isPresent()) {
			return WoodStorageBlockItem.getDisplayName(BLOCK_TRANSLATION_KEY, null);
		}
		return WoodStorageBlockItem.getDisplayName(BLOCK_TRANSLATION_KEY, getWoodType(BuiltInRegistries.ITEM.getKey(this).getPath()));
	}

	private WoodType getWoodType(String registryName) {
		if (registryName.length() <= REGISTRY_NAME_WITHOUT_WOOD_TYPE.length() + 1) {
			return WoodType.ACACIA;
		}

		String woodName = registryName.substring(0, registryName.length() - (REGISTRY_NAME_WITHOUT_WOOD_TYPE.length() + 1));

		return WoodType.values().filter(wt -> wt.name().equals(woodName)).findFirst().orElse(WoodType.ACACIA);
	}
}
