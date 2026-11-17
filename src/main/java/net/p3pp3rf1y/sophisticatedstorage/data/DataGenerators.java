package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

public class DataGenerators {
	private DataGenerators() {
	}

	public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent evt) {
		evt.add(Registries.LOOT_TABLE, new StorageBlockLootProvider()).recipe(StorageRecipeProvider::new);
	}

	public static void gatherData(GatherDataEvent.Client evt) {
		evt.createBlockAndItemTags(BlockTagProvider::new, (packOutput, registries, blockTagProvider) -> new ItemTagProvider(packOutput, registries));
		evt.createProvider(StorageModelProvider::new);
	}
}
