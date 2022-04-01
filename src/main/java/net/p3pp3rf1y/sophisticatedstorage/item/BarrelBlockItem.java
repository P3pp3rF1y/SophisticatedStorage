package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BarrelBlockItem extends BlockItemBase {
	public static final String WOOD_TYPE_TAG = "woodType";

	private Map<WoodType, String> woodDescriptionIds = new HashMap<>();

	public BarrelBlockItem(Block block, Properties properties, CreativeModeTab tab) {
		super(block, properties, tab);
	}

	public static Optional<WoodType> getWoodType(ItemStack barrelStack) {
		return NBTHelper.getString(barrelStack, WOOD_TYPE_TAG)
				.flatMap(woodType -> WoodType.values().filter(wt -> wt.name().equals(woodType)).findFirst());
	}

	public static ItemStack setWoodType(ItemStack barrelStack, WoodType woodType) {
		barrelStack.getOrCreateTag().putString(WOOD_TYPE_TAG, woodType.name());
		return barrelStack;
	}

	public static Optional<Integer> getMaincolorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "mainColor");
	}

	public static Optional<Integer> getAccentColorFromStack(ItemStack barrelStack) {
		return NBTHelper.getInt(barrelStack, "accentColor");
	}

	private String makeWoodBarrelDescriptionId(WoodType wt) {
		ResourceLocation id = Objects.requireNonNull(getRegistryName());
		return "item." + id.getNamespace() + "." + wt.name() + "_" + id.getPath().replace('/', '.');
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getWoodType(stack).map(wt -> woodDescriptionIds.computeIfAbsent(wt, this::makeWoodBarrelDescriptionId)).orElse(super.getDescriptionId(stack));
	}
}
