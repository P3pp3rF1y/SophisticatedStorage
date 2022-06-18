package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		generator.addProvider(evt.includeServer(), new BlockTagProvider(generator, evt.getExistingFileHelper()));
		generator.addProvider(evt.includeServer(), new StorageBlockLootProvider(generator));
		generator.addProvider(evt.includeServer(), new StorageRecipeProvider(generator));
	}
}
