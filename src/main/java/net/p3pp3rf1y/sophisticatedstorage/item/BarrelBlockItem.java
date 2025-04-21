package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class BarrelBlockItem extends WoodStorageBlockItem {
	public static final String FLAT_TOP_TAG = "flatTop";
	public static final String MATERIALS_TAG = "materials";

	public BarrelBlockItem(Block block) {
		this(block, new Properties());
	}
	public BarrelBlockItem(Block block, Properties properties) {
		super(block, properties);
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

	public static void uncompactMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		if (materials.isEmpty()) {
			return;
		}

		Map<BarrelMaterial, ResourceLocation> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		materials.forEach((mat, texture) -> {
			for (BarrelMaterial child : mat.getChildren()) {
				uncompactedMaterials.put(child, texture);
			}
		});

		materials.clear();
		materials.putAll(uncompactedMaterials);
	}

	public static void compactMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (!material.isLeaf()) {
				//if all children have the same texture remove them and convert to the parent
				ResourceLocation firstChildTexture = null;
				boolean allChildrenHaveSameTexture = true;
				for (BarrelMaterial child : material.getChildren()) {
					ResourceLocation texture = materials.get(child);
					if (texture == null || (firstChildTexture != null && !firstChildTexture.equals(texture))) {
						allChildrenHaveSameTexture = false;
						break;
					} else if (firstChildTexture == null) {
						firstChildTexture = texture;
					}
				}

				if (firstChildTexture != null && allChildrenHaveSameTexture) {
					materials.put(material, firstChildTexture);
					for (BarrelMaterial child : material.getChildren()) {
						materials.remove(child);
					}
				}
			}
		}
	}

	public static void removeCoveredTints(ItemStack barrelStackCopy, Map<BarrelMaterial, ResourceLocation> materials) {
		if (barrelStackCopy.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			boolean hasMainTint = tintableBlockItem.getMainColor(barrelStackCopy).isPresent();
			boolean hasAccentTint = tintableBlockItem.getAccentColor(barrelStackCopy).isPresent();

			if (hasMainTint || hasAccentTint) {
				if (hasMainTint && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_BUT_TRIM))) {
					tintableBlockItem.removeMainColor(barrelStackCopy);
				}
				if (hasAccentTint && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_TRIM))) {
					tintableBlockItem.removeAccentColor(barrelStackCopy);
				}
			}
		}
	}

	public static Map<BarrelMaterial, ResourceLocation> getUncompactedMaterials(ItemStack storageStack) {
		Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
		materials.putAll(getMaterials(storageStack));
		uncompactMaterials(materials);
		return materials;
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name;
		if (getMaterials(stack).isEmpty()) {
			name = super.getName(stack);
		} else {
			name = getDisplayName(getDescriptionId(), null);
		}
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
