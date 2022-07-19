package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

public class ModItemColors {
	private ModItemColors() {}

	public static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex < 1000 || tintIndex > 1001) {
				return -1;
			}
			if (tintIndex == 1000) {
				return StorageBlockItem.getMainColorFromStack(stack).orElse(-1);
			} else {
				return StorageBlockItem.getAccentColorFromStack(stack).orElse(-1);
			}
		}, ModBlocks.BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
	}
}
