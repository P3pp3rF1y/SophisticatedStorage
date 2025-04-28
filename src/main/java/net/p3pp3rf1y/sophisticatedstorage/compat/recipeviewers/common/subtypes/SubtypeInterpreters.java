package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SubtypeInterpreters {
	private static final PropertyBasedSubtypeInterpreter chestSubtypeInterpreter = new ChestSubtypeInterpreter();
	private static final PropertyBasedSubtypeInterpreter barrelSubtypeInterpreter = new BarrelSubtypeInterpreter();
	private static final PropertyBasedSubtypeInterpreter shulkerBoxSubtypeInterpreter = new ShulkerBoxSubtypeInterpreter();

	public static Map<BlockItem, PropertyBasedSubtypeInterpreter> getSubtypeInterpreters() {
		return new HashMap<>(){{
			ModBlocks.ITEMS.getEntries().stream()
					.filter(holder -> holder.get() instanceof StorageBlockItem)
					.forEach(item -> {
						Item i = item.get();
						if (i instanceof BarrelBlockItem blockItem) {
							put(blockItem, barrelSubtypeInterpreter);
						} else if (i instanceof ChestBlockItem blockItem) {
							put(blockItem, chestSubtypeInterpreter);
						} else if (i instanceof ShulkerBoxItem blockItem) {
							put(blockItem, shulkerBoxSubtypeInterpreter);
						}
					});
		}};
	}

	public static Optional<PropertyBasedSubtypeInterpreter> getSubtypeInterpreter(Map<BlockItem, PropertyBasedSubtypeInterpreter> subtypeInterpreters, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return Optional.empty();
		}

		return Optional.ofNullable(subtypeInterpreters.get(blockItem));
	}
}
