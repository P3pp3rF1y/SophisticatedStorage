package net.p3pp3rf1y.sophisticatedstorage.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.HashMap;
import java.util.Map;

public class BarrelBlockItem extends WoodStorageBlockItem {
	public BarrelBlockItem(Block block) {
		this(block, new Properties());
	}

	public BarrelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static final Codec<Map<BarrelMaterial, ResourceLocation>> MATERIALS_CODEC =
			Codec.simpleMap(BarrelMaterial.CODEC, ResourceLocation.CODEC, StringRepresentable.keys(BarrelMaterial.values())).codec();

	public static final StreamCodec<FriendlyByteBuf, Map<BarrelMaterial, ResourceLocation>> MATERIALS_STREAM_CODEC =
			StreamCodec.of((buf, map) -> buf.writeMap(map, BarrelMaterial.STREAM_CODEC, ResourceLocation.STREAM_CODEC),
					buf -> buf.readMap(BarrelMaterial.STREAM_CODEC, ResourceLocation.STREAM_CODEC));

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

	public static boolean isFlatTop(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.FLAT_TOP, false);
	}

	public static void setMaterials(ItemStack barrel, Map<BarrelMaterial, ResourceLocation> materials) {
		barrel.set(ModDataComponents.BARREL_MATERIALS, Map.copyOf(materials));
	}

	public static Map<BarrelMaterial, ResourceLocation> getMaterials(ItemStack barrel) {
		return new HashMap<>(barrel.getOrDefault(ModDataComponents.BARREL_MATERIALS, Map.of()));
	}

	public static void removeMaterials(ItemStack stack) {
		stack.remove(ModDataComponents.BARREL_MATERIALS);
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
