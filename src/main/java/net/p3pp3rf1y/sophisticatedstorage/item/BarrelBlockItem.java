package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.Map;
import java.util.Optional;

public class BarrelBlockItem extends WoodStorageBlockItem {
	private static final String FLAT_TOP_TAG = "flatTop";
	private static final String MATERIALS_TAG = "materials";

	public BarrelBlockItem(Block block) {
		super(block);
	}

	public static void toggleFlatTop(ItemStack stack) {
		boolean flatTop = isFlatTop(stack);
		setFlatTop(stack, !flatTop);
	}

	public static void setFlatTop(ItemStack stack, boolean flatTop) {
		if (flatTop) {
			NBTHelper.setBoolean(stack, FLAT_TOP_TAG, true);
		} else {
			NBTHelper.removeTag(stack, FLAT_TOP_TAG);
		}
	}

	public static boolean isFlatTop(ItemStack stack) {
		return NBTHelper.getBoolean(stack, FLAT_TOP_TAG).orElse(false);
	}

	public static void setMaterials(ItemStack barrel, Map<BarrelMaterial, ResourceLocation> materials) {
		NBTHelper.putMap(barrel.getOrCreateTag(), MATERIALS_TAG, materials, BarrelMaterial::getSerializedName, resourceLocation -> StringTag.valueOf(resourceLocation.toString()));
	}

	public static Map<BarrelMaterial, ResourceLocation> getMaterials(ItemStack barrel) {
		return NBTHelper.getMap(barrel, MATERIALS_TAG, BarrelMaterial::fromName, (bm, tag) -> Optional.of(new ResourceLocation(tag.getAsString()))).orElse(Map.of());
	}

	public static void removeMaterials(ItemStack stack) {
		NBTHelper.removeTag(stack, MATERIALS_TAG);
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name = super.getName(stack);
		if (isFlatTop(stack)) {
			return name.copy().append(Component.translatable(StorageTranslationHelper.INSTANCE.translBlockTooltipKey("barrel") + ".flat_top"));
		}
		return name;
	}

	@Override
	public boolean isTintable(ItemStack stack) {
		return getMaterials(stack).isEmpty();
	}
}
