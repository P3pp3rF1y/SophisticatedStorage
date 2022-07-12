package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		BlockTagProvider blockTagProvider = new BlockTagProvider(generator, evt.getExistingFileHelper());
		generator.addProvider(evt.includeServer(), blockTagProvider);
		generator.addProvider(evt.includeServer(), new ItemTagProvider(generator, blockTagProvider, evt.getExistingFileHelper()));
		generator.addProvider(evt.includeServer(), new StorageBlockLootProvider(generator));
		generator.addProvider(evt.includeServer(), new StorageRecipeProvider(generator));
	}
}
