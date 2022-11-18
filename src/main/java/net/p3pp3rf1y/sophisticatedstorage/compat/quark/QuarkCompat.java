package net.p3pp3rf1y.sophisticatedstorage.compat.quark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import java.util.Map;

public class QuarkCompat implements ICompat {
	private static final Map<String, WoodType> CHESTS = Map.of(
			"oak_chest", WoodType.OAK,
			"acacia_chest", WoodType.ACACIA,
			"birch_chest", WoodType.BIRCH,
			"crimson_chest", WoodType.CRIMSON,
			"dark_oak_chest", WoodType.DARK_OAK,
			"jungle_chest", WoodType.JUNGLE,
			"spruce_chest", WoodType.SPRUCE,
			"warped_chest", WoodType.WARPED
	);

	@Override
	public void setup() {
		CHESTS.forEach((name, woodType) -> {
			Block chest = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(CompatModIds.QUARK, name));
			if (chest != null) {
				StorageTierUpgradeItem.addTierUpgradeDefinition(StorageTierUpgradeItem.TierUpgrade.BASIC, chest,
						new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class, chestBlockEntity -> chestBlockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.CHEST.get(), woodType, ChestBlock.FACING));
				StorageTierUpgradeItem.addTierUpgradeDefinition(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON, chest,
						new StorageTierUpgradeItem.VanillaTierUpgradeDefinition<>(ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_CHEST.get(), woodType, ChestBlock.FACING));
			}
		});
	}
}
