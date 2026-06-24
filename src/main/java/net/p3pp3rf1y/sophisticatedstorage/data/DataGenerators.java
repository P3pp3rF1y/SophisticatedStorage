package net.p3pp3rf1y.sophisticatedstorage.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class DataGenerators {
	private DataGenerators() {
	}

	public static void gatherData(GatherDataEvent.Client evt) {
		evt.createBlockAndItemTags(BlockTagProvider::new, ItemTagProvider::new);
		evt.createProvider(StorageBlockLootProvider::new);
		evt.createProvider(StorageRecipeProvider.Runner::new);
		evt.createProvider(StorageModelProvider::new);
	}
}
