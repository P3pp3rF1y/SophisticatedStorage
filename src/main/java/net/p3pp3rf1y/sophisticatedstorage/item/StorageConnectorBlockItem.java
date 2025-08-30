package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class StorageConnectorBlockItem extends BlockItemBase {
	public static final String REGISTRY_NAME_WITHOUT_WOOD_TYPE = "storage_connector";
	public static final String BLOCK_TRANSLATION_KEY = Util.makeDescriptionId("block", SophisticatedStorage.getRL(REGISTRY_NAME_WITHOUT_WOOD_TYPE));

	public StorageConnectorBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack stack) {
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
