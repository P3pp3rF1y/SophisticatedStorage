package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		BlockTagProvider blockTagProvider = new BlockTagProvider(packOutput, evt.getLookupProvider(), evt.getExistingFileHelper());
		generator.addProvider(evt.includeServer(), blockTagProvider);
		generator.addProvider(evt.includeServer(), new ItemTagProvider(packOutput, evt.getLookupProvider(), blockTagProvider.contentsGetter(), evt.getExistingFileHelper()));
		generator.addProvider(evt.includeServer(), new StorageBlockLootProvider(packOutput, evt.getLookupProvider()));
		generator.addProvider(evt.includeServer(), new StorageRecipeProvider(generator, evt.getLookupProvider()));
	}
}
