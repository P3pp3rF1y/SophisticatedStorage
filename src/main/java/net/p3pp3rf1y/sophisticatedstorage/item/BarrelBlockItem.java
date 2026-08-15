package net.p3pp3rf1y.sophisticatedstorage.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class BarrelBlockItem extends WoodStorageBlockItem {
	public BarrelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static final Codec<Map<BarrelMaterial, Identifier>> MATERIALS_CODEC = Codec
			.simpleMap(BarrelMaterial.CODEC, Identifier.CODEC, StringRepresentable.keys(BarrelMaterial.values())).codec();

	public static final StreamCodec<FriendlyByteBuf, Map<BarrelMaterial, Identifier>> MATERIALS_STREAM_CODEC = ByteBufCodecs.map(HashMap::new,
			BarrelMaterial.STREAM_CODEC, Identifier.STREAM_CODEC, BarrelMaterial.values().length);

	public static void toggleFlatTop(ItemStack stack) {
		boolean flatTop = isFlatTop(stack);
		setFlatTop(stack, !flatTop);
	}

	public static void setFlatTop(ItemStack stack, boolean flatTop) {
		if (flatTop) {
			stack.set(ModDataComponents.FLAT_TOP, true);
		} else {
			stack.remove(ModDataComponents.FLAT_TOP);
		}
	}

	public static boolean isFlatTop(DataComponentHolder componentHolder) {
		return componentHolder.getOrDefault(ModDataComponents.FLAT_TOP, false);
	}

	public static void setMaterials(ItemStack barrel, Map<BarrelMaterial, Identifier> materials) {
		barrel.set(ModDataComponents.BARREL_MATERIALS, Map.copyOf(materials));
	}

	public static Map<BarrelMaterial, Identifier> getMaterials(DataComponentHolder barrel) {
		return barrel.getOrDefault(ModDataComponents.BARREL_MATERIALS, Map.of());
	}

	public static void removeMaterials(ItemStack stack) {
		stack.remove(ModDataComponents.BARREL_MATERIALS);
	}

	public static void uncompactMaterials(Map<BarrelMaterial, Identifier> materials) {
		if (materials.isEmpty()) {
			return;
		}

		Map<BarrelMaterial, Identifier> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		materials.forEach((mat, texture) -> {
			for (BarrelMaterial child : mat.getChildren()) {
				uncompactedMaterials.put(child, texture);
			}
		});

		materials.clear();
		materials.putAll(uncompactedMaterials);
	}

	public static void compactMaterials(Map<BarrelMaterial, Identifier> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (!material.isLeaf()) {
				// if all children have the same texture remove them and convert to the parent
				Identifier firstChildTexture = null;
				boolean allChildrenHaveSameTexture = true;
				for (BarrelMaterial child : material.getChildren()) {
					Identifier texture = materials.get(child);
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

	public static void removeCoveredTints(ItemStack barrelStackCopy, Map<BarrelMaterial, Identifier> materials) {
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

	public static Map<BarrelMaterial, Identifier> getUncompactedMaterials(ItemStack storageStack) {
		Map<BarrelMaterial, Identifier> materials = new EnumMap<>(BarrelMaterial.class);
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
