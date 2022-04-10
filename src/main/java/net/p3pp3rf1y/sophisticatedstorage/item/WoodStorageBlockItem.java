package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WoodStorageBlockItem extends StorageBlockItem {
	public static final String WOOD_TYPE_TAG = "woodType";

	private final Map<WoodType, String> woodDescriptionIds = new HashMap<>();

	public WoodStorageBlockItem(Block block) {
		super(block);
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
		if (StorageBlockItem.getMaincolorFromStack(storageStack).isPresent()) {
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

	private String makeWoodStorageDescriptionId(WoodType wt) {
		ResourceLocation id = Objects.requireNonNull(getRegistryName());
		return "item." + id.getNamespace() + "." + wt.name() + "_" + id.getPath().replace('/', '.');
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getWoodType(stack).map(wt -> woodDescriptionIds.computeIfAbsent(wt, this::makeWoodStorageDescriptionId)).orElse(super.getDescriptionId(stack));
	}
}
