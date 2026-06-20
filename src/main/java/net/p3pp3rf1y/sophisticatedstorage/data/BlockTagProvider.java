package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
	public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, SophisticatedStorage.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		tag(BlockTags.MINEABLE_WITH_AXE).add(
				key(ModBlocks.BARREL.get()), key(ModBlocks.COPPER_BARREL.get()), key(ModBlocks.IRON_BARREL.get()), key(ModBlocks.GOLD_BARREL.get()), key(ModBlocks.DIAMOND_BARREL.get()), key(ModBlocks.NETHERITE_BARREL.get()),
				key(ModBlocks.LIMITED_BARREL_1.get()), key(ModBlocks.LIMITED_COPPER_BARREL_1.get()), key(ModBlocks.LIMITED_IRON_BARREL_1.get()), key(ModBlocks.LIMITED_GOLD_BARREL_1.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()),
				key(ModBlocks.LIMITED_BARREL_2.get()), key(ModBlocks.LIMITED_COPPER_BARREL_2.get()), key(ModBlocks.LIMITED_IRON_BARREL_2.get()), key(ModBlocks.LIMITED_GOLD_BARREL_2.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()),
				key(ModBlocks.LIMITED_BARREL_3.get()), key(ModBlocks.LIMITED_COPPER_BARREL_3.get()), key(ModBlocks.LIMITED_IRON_BARREL_3.get()), key(ModBlocks.LIMITED_GOLD_BARREL_3.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()),
				key(ModBlocks.LIMITED_BARREL_4.get()), key(ModBlocks.LIMITED_COPPER_BARREL_4.get()), key(ModBlocks.LIMITED_IRON_BARREL_4.get()), key(ModBlocks.LIMITED_GOLD_BARREL_4.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()),
				key(ModBlocks.CHEST.get()), key(ModBlocks.COPPER_CHEST.get()), key(ModBlocks.IRON_CHEST.get()), key(ModBlocks.GOLD_CHEST.get()), key(ModBlocks.DIAMOND_CHEST.get()), key(ModBlocks.NETHERITE_CHEST.get()),
				key(ModBlocks.DECORATION_TABLE.get())
		);
		ModBlocks.STORAGE_CONNECTOR_BLOCKS.values().forEach(block -> tag(BlockTags.MINEABLE_WITH_AXE).add(key(block.get())));

		tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
				key(ModBlocks.SHULKER_BOX.get()), key(ModBlocks.COPPER_SHULKER_BOX.get()), key(ModBlocks.IRON_SHULKER_BOX.get()), key(ModBlocks.GOLD_SHULKER_BOX.get()), key(ModBlocks.DIAMOND_SHULKER_BOX.get()), key(ModBlocks.NETHERITE_SHULKER_BOX.get()),
				key(ModBlocks.CONTROLLER.get()), key(ModBlocks.STORAGE_LINK.get()), key(ModBlocks.STORAGE_IO.get()), key(ModBlocks.STORAGE_INPUT.get()), key(ModBlocks.STORAGE_OUTPUT.get())
		);
		tag(BlockTags.GUARDED_BY_PIGLINS).add(
				key(ModBlocks.BARREL.get()), key(ModBlocks.COPPER_BARREL.get()), key(ModBlocks.IRON_BARREL.get()), key(ModBlocks.GOLD_BARREL.get()), key(ModBlocks.DIAMOND_BARREL.get()), key(ModBlocks.NETHERITE_BARREL.get()),
				key(ModBlocks.LIMITED_BARREL_1.get()), key(ModBlocks.LIMITED_COPPER_BARREL_1.get()), key(ModBlocks.LIMITED_IRON_BARREL_1.get()), key(ModBlocks.LIMITED_GOLD_BARREL_1.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()),
				key(ModBlocks.LIMITED_BARREL_2.get()), key(ModBlocks.LIMITED_COPPER_BARREL_2.get()), key(ModBlocks.LIMITED_IRON_BARREL_2.get()), key(ModBlocks.LIMITED_GOLD_BARREL_2.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()),
				key(ModBlocks.LIMITED_BARREL_3.get()), key(ModBlocks.LIMITED_COPPER_BARREL_3.get()), key(ModBlocks.LIMITED_IRON_BARREL_3.get()), key(ModBlocks.LIMITED_GOLD_BARREL_3.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()),
				key(ModBlocks.LIMITED_BARREL_4.get()), key(ModBlocks.LIMITED_COPPER_BARREL_4.get()), key(ModBlocks.LIMITED_IRON_BARREL_4.get()), key(ModBlocks.LIMITED_GOLD_BARREL_4.get()), key(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()), key(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()),
				key(ModBlocks.CHEST.get()), key(ModBlocks.COPPER_CHEST.get()), key(ModBlocks.IRON_CHEST.get()), key(ModBlocks.GOLD_CHEST.get()), key(ModBlocks.DIAMOND_CHEST.get()), key(ModBlocks.NETHERITE_CHEST.get()),
				key(ModBlocks.SHULKER_BOX.get()), key(ModBlocks.COPPER_SHULKER_BOX.get()), key(ModBlocks.IRON_SHULKER_BOX.get()), key(ModBlocks.GOLD_SHULKER_BOX.get()), key(ModBlocks.DIAMOND_SHULKER_BOX.get()), key(ModBlocks.NETHERITE_SHULKER_BOX.get())
		);
		tag(Tags.Blocks.CHESTS).add(key(ModBlocks.CHEST.get()), key(ModBlocks.COPPER_CHEST.get()), key(ModBlocks.IRON_CHEST.get()), key(ModBlocks.GOLD_CHEST.get()), key(ModBlocks.DIAMOND_CHEST.get()), key(ModBlocks.NETHERITE_CHEST.get()));
		tag(Tags.Blocks.BARRELS).add(key(ModBlocks.BARREL.get()), key(ModBlocks.COPPER_BARREL.get()), key(ModBlocks.IRON_BARREL.get()), key(ModBlocks.GOLD_BARREL.get()), key(ModBlocks.DIAMOND_BARREL.get()), key(ModBlocks.NETHERITE_BARREL.get()));
		tag(Tags.Blocks.BARRELS_WOODEN).add(key(ModBlocks.BARREL.get()));
	}

	private static ResourceKey<Block> key(Block block) {
		return block.builtInRegistryHolder().key();
	}
}
