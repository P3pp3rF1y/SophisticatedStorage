package net.p3pp3rf1y.sophisticatedstorage.compat.quark;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import org.violetmoon.quark.content.building.module.VariantChestsModule;

import java.util.Map;

public class QuarkCompat implements ICompat {
	private static final Map<Block, WoodType> VANILLA_PLANK_TO_WOOD_TYPE = Map.ofEntries(Map.entry(Blocks.OAK_PLANKS, WoodType.OAK),
			Map.entry(Blocks.ACACIA_PLANKS, WoodType.ACACIA), Map.entry(Blocks.BIRCH_PLANKS, WoodType.BIRCH),
			Map.entry(Blocks.CRIMSON_PLANKS, WoodType.CRIMSON), Map.entry(Blocks.DARK_OAK_PLANKS, WoodType.DARK_OAK),
			Map.entry(Blocks.JUNGLE_PLANKS, WoodType.JUNGLE), Map.entry(Blocks.MANGROVE_PLANKS, WoodType.MANGROVE),
			Map.entry(Blocks.SPRUCE_PLANKS, WoodType.SPRUCE), Map.entry(Blocks.WARPED_PLANKS, WoodType.WARPED),
			Map.entry(Blocks.BAMBOO_PLANKS, WoodType.BAMBOO), Map.entry(Blocks.CHERRY_PLANKS, WoodType.CHERRY));

	@Override
	public void setup() {
		VariantChestsModule.regularChests.forEach((sourceBlock, quarkChest) -> {
			WoodType woodType = VANILLA_PLANK_TO_WOOD_TYPE.get(sourceBlock);
			if (woodType == null) {
				return;
			}
			StorageTierUpgradeItem.TierUpgrade.BASIC.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							chestBlockEntity -> chestBlockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
			StorageTierUpgradeItem.TierUpgrade.BASIC_TO_COPPER.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.COPPER_CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
			StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
			StorageTierUpgradeItem.TierUpgrade.BASIC_TO_GOLD.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.GOLD_CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
			StorageTierUpgradeItem.TierUpgrade.BASIC_TO_DIAMOND.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.DIAMOND_CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
			StorageTierUpgradeItem.TierUpgrade.BASIC_TO_NETHERITE.addTierUpgradeDefinition(quarkChest,
					new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class,
							blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.NETHERITE_CHEST.get(), woodType, ChestBlock.FACING,
							ChestBlock.WATERLOGGED, ChestBlock.TYPE));
		});
	}
}
