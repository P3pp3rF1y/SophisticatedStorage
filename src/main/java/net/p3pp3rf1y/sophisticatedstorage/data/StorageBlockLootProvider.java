package net.p3pp3rf1y.sophisticatedstorage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class StorageBlockLootProvider implements DataProvider {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final DataGenerator generator;

	StorageBlockLootProvider(DataGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void run(HashCache cache) throws IOException {
		Map<ResourceLocation, LootTable.Builder> tables = new HashMap<>();

		tables.put(ModBlocks.BARREL.getId(), getStorage(ModBlocks.BARREL_ITEM.get()));
		tables.put(ModBlocks.COPPER_BARREL.getId(), getStorage(ModBlocks.COPPER_BARREL_ITEM.get()));
		tables.put(ModBlocks.IRON_BARREL.getId(), getStorage(ModBlocks.IRON_BARREL_ITEM.get()));
		tables.put(ModBlocks.GOLD_BARREL.getId(), getStorage(ModBlocks.GOLD_BARREL_ITEM.get()));
		tables.put(ModBlocks.DIAMOND_BARREL.getId(), getStorage(ModBlocks.DIAMOND_BARREL_ITEM.get()));
		tables.put(ModBlocks.NETHERITE_BARREL.getId(), getStorage(ModBlocks.NETHERITE_BARREL_ITEM.get()));
		tables.put(ModBlocks.LIMITED_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_COPPER_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_IRON_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_GOLD_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_DIAMOND_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_NETHERITE_BARREL_1.getId(), getStorage(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()));
		tables.put(ModBlocks.LIMITED_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_COPPER_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_IRON_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_GOLD_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_DIAMOND_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_NETHERITE_BARREL_2.getId(), getStorage(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()));
		tables.put(ModBlocks.LIMITED_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_COPPER_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_IRON_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_GOLD_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_DIAMOND_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_NETHERITE_BARREL_3.getId(), getStorage(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()));
		tables.put(ModBlocks.LIMITED_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.LIMITED_COPPER_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.LIMITED_IRON_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.LIMITED_GOLD_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.LIMITED_DIAMOND_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.LIMITED_NETHERITE_BARREL_4.getId(), getStorage(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get()));
		tables.put(ModBlocks.CHEST.getId(), getStorage(ModBlocks.CHEST_ITEM.get()));
		tables.put(ModBlocks.COPPER_CHEST.getId(), getStorage(ModBlocks.COPPER_CHEST_ITEM.get()));
		tables.put(ModBlocks.IRON_CHEST.getId(), getStorage(ModBlocks.IRON_CHEST_ITEM.get()));
		tables.put(ModBlocks.GOLD_CHEST.getId(), getStorage(ModBlocks.GOLD_CHEST_ITEM.get()));
		tables.put(ModBlocks.DIAMOND_CHEST.getId(), getStorage(ModBlocks.DIAMOND_CHEST_ITEM.get()));
		tables.put(ModBlocks.NETHERITE_CHEST.getId(), getStorage(ModBlocks.NETHERITE_CHEST_ITEM.get()));
		tables.put(ModBlocks.SHULKER_BOX.getId(), getStorage(ModBlocks.SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.COPPER_SHULKER_BOX.getId(), getStorage(ModBlocks.COPPER_SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.IRON_SHULKER_BOX.getId(), getStorage(ModBlocks.IRON_SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.GOLD_SHULKER_BOX.getId(), getStorage(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.DIAMOND_SHULKER_BOX.getId(), getStorage(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.NETHERITE_SHULKER_BOX.getId(), getStorage(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()));
		tables.put(ModBlocks.CONTROLLER.getId(), createSingleItemTable(ModBlocks.CONTROLLER_ITEM.get()));
		tables.put(ModBlocks.STORAGE_LINK.getId(), createSingleItemTable(ModBlocks.STORAGE_LINK_ITEM.get()));

		for (Map.Entry<ResourceLocation, LootTable.Builder> e : tables.entrySet()) {
			Path path = getPath(generator.getOutputFolder(), e.getKey());
			DataProvider.save(GSON, cache, LootTables.serialize(e.getValue().setParamSet(LootContextParamSets.BLOCK).build()), path);
		}
	}

	@Override
	public String getName() {
		return "SophisticatedStorage block loot tables";
	}

	private static Path getPath(Path root, ResourceLocation id) {
		return root.resolve("data/" + id.getNamespace() + "/loot_tables/blocks/" + id.getPath() + ".json");
	}

	private static LootTable.Builder getStorage(Item storageItem) {
		LootPoolEntryContainer.Builder<?> entry = LootItem.lootTableItem(storageItem);
		LootPool.Builder pool = LootPool.lootPool().name("main").setRolls(ConstantValue.exactly(1)).add(entry)
				.apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
				.apply(CopyStorageDataFunction.builder());
		return LootTable.lootTable().withPool(pool);
	}

	protected static LootTable.Builder createSingleItemTable(ItemLike item) {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(item)));
	}
}
