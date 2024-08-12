package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class StorageBlockLootProvider extends LootTableProvider {
	StorageBlockLootProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
		super(packOutput, Set.of(),
				List.of(
						new SubProviderEntry(SubProvider::new, LootContextParamSets.BLOCK)
				),
				registries
		);
	}

	private static class SubProvider extends BlockLootSubProvider {
		protected SubProvider(HolderLookup.Provider registries) {
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		public void generate() {
			add(ModBlocks.BARREL.get(), dropStorageWithContents(ModBlocks.BARREL_ITEM.get()));
			add(ModBlocks.COPPER_BARREL.get(), dropStorageWithContents(ModBlocks.COPPER_BARREL_ITEM.get()));
			add(ModBlocks.IRON_BARREL.get(), dropStorageWithContents(ModBlocks.IRON_BARREL_ITEM.get()));
			add(ModBlocks.GOLD_BARREL.get(), dropStorageWithContents(ModBlocks.GOLD_BARREL_ITEM.get()));
			add(ModBlocks.DIAMOND_BARREL.get(), dropStorageWithContents(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			add(ModBlocks.NETHERITE_BARREL.get(), dropStorageWithContents(ModBlocks.NETHERITE_BARREL_ITEM.get()));
			add(ModBlocks.LIMITED_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_COPPER_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_IRON_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_GOLD_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_NETHERITE_BARREL_1.get(), dropStorageWithContents(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()));
			add(ModBlocks.LIMITED_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_COPPER_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_IRON_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_GOLD_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_NETHERITE_BARREL_2.get(), dropStorageWithContents(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()));
			add(ModBlocks.LIMITED_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_COPPER_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_IRON_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_GOLD_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_NETHERITE_BARREL_3.get(), dropStorageWithContents(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()));
			add(ModBlocks.LIMITED_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_BARREL_4_ITEM.get()));
			add(ModBlocks.LIMITED_COPPER_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get()));
			add(ModBlocks.LIMITED_IRON_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get()));
			add(ModBlocks.LIMITED_GOLD_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get()));
			add(ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get()));
			add(ModBlocks.LIMITED_NETHERITE_BARREL_4.get(), dropStorageWithContents(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get()));
			add(ModBlocks.CHEST.get(), dropStorageWithContents(ModBlocks.CHEST_ITEM.get()));
			add(ModBlocks.COPPER_CHEST.get(), dropStorageWithContents(ModBlocks.COPPER_CHEST_ITEM.get()));
			add(ModBlocks.IRON_CHEST.get(), dropStorageWithContents(ModBlocks.IRON_CHEST_ITEM.get()));
			add(ModBlocks.GOLD_CHEST.get(), dropStorageWithContents(ModBlocks.GOLD_CHEST_ITEM.get()));
			add(ModBlocks.DIAMOND_CHEST.get(), dropStorageWithContents(ModBlocks.DIAMOND_CHEST_ITEM.get()));
			add(ModBlocks.NETHERITE_CHEST.get(), dropStorageWithContents(ModBlocks.NETHERITE_CHEST_ITEM.get()));
			add(ModBlocks.SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.SHULKER_BOX_ITEM.get()));
			add(ModBlocks.COPPER_SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.COPPER_SHULKER_BOX_ITEM.get()));
			add(ModBlocks.IRON_SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.IRON_SHULKER_BOX_ITEM.get()));
			add(ModBlocks.GOLD_SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()));
			add(ModBlocks.DIAMOND_SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()));
			add(ModBlocks.NETHERITE_SHULKER_BOX.get(), dropStorageWithContents(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()));

			add(ModBlocks.CONTROLLER.get(), dropBlock(ModBlocks.CONTROLLER_ITEM.get()));
			add(ModBlocks.STORAGE_LINK.get(), dropBlock(ModBlocks.STORAGE_LINK_ITEM.get()));
			add(ModBlocks.STORAGE_IO.get(), dropBlock(ModBlocks.STORAGE_IO_ITEM.get()));
			add(ModBlocks.STORAGE_INPUT.get(), dropBlock(ModBlocks.STORAGE_INPUT_ITEM.get()));
			add(ModBlocks.STORAGE_OUTPUT.get(), dropBlock(ModBlocks.STORAGE_OUTPUT_ITEM.get()));
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return BuiltInRegistries.BLOCK.entrySet().stream()
					.filter(e -> e.getKey().location().getNamespace().equals(SophisticatedStorage.MOD_ID))
					.map(Map.Entry::getValue)
					.toList();
		}

		private static LootTable.Builder dropStorageWithContents(Item storageItem) {
			LootPool.Builder pool = LootPool.lootPool().name("main").setRolls(ConstantValue.exactly(1))
					.add(LootItem.lootTableItem(storageItem))
					.apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
					.apply(CopyStorageDataFunction.builder());
			return LootTable.lootTable().withPool(pool);
		}

		public LootTable.Builder dropBlock(ItemLike item) {
			return LootTable.lootTable().withPool(applyExplosionCondition(item, LootPool.lootPool().name("main").setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(item))));
		}
	}
}
