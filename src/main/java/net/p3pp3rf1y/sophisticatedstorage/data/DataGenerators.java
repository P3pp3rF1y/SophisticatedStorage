package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class DataGenerators {
	private DataGenerators() {
	}

	public static void gatherData(GatherDataEvent.Client evt) {
		evt.createBlockAndItemTags(BlockTagProvider::new, (packOutput, registries, blockTagProvider) -> new ItemTagProvider(packOutput, registries));
		evt.createReloadableRegistryObjects(new RegistrySetBuilder().add(Registries.LOOT_TABLE, new StorageBlockLootProvider())
				.add(RecipeProvider.asBootstrap(StorageRecipeProvider::new)));
		evt.createProvider(StorageModelProvider::new);
	}
}
