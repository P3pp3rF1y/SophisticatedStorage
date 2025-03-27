package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import java.util.HashMap;
import java.util.Map;

public class StorageHolderTierUpgradeHandler {

	public static final Map<StorageTierUpgradeItem.TierUpgrade, Map<Item, StorageHolderUpgradeDefinition>> STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS = new HashMap<>();

	public static boolean upgrade(Player player, StorageHolderBase storageHolder, ItemStack itemInHand, StorageTierUpgradeItem tierUpgradeItem) {
		Map<Item, StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition> tierDefinitions = STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.get(tierUpgradeItem.getTier());
		if (tierDefinitions == null) {
			SophisticatedStorage.LOGGER.warn("No tier upgrade definitions found for {}", tierUpgradeItem.getTier());
			return false;
		}

		ItemStack storageStack = storageHolder.getSyncedStorageStack();
		if (!storageHolder.isOpen() && !storageHolder.isPacked()) {
			StorageHolderUpgradeDefinition upgradeDefinition = tierDefinitions.get(storageStack.getItem());
			if (upgradeDefinition == null) {
				SophisticatedStorage.LOGGER.warn("No tier upgrade definition found for {}", () -> ForgeRegistries.ITEMS.getKey(storageStack.getItem()));
				return false;
			}

			if (!player.level().isClientSide()) {
				upgradeDefinition.upgradeStorageHolder(storageHolder, storageStack);

				if (!player.isCreative()) {
					itemInHand.shrink(1);
				}
			}

			return true;
		}
		return false;
	}

	static {
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_COPPER, Map.of(
				ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_BARREL_ITEM.get()),
				ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_CHEST_ITEM.get()),
				ModBlocks.SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON, Map.of(
				ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_BARREL_ITEM.get()),
				ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_CHEST_ITEM.get()),
				ModBlocks.SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_GOLD, Map.of(
				ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
				ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
				ModBlocks.SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_DIAMOND, Map.of(
				ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				ModBlocks.SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_NETHERITE, Map.of(
				ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())
		));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_IRON, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_GOLD, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_DIAMOND, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_NETHERITE, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())
		));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_GOLD, Map.of(
				ModBlocks.IRON_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
				ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
				ModBlocks.IRON_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_DIAMOND, Map.of(
				ModBlocks.IRON_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				ModBlocks.IRON_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_NETHERITE, Map.of(
				ModBlocks.IRON_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.IRON_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())
		));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_DIAMOND, Map.of(
				ModBlocks.GOLD_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				ModBlocks.GOLD_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())
		));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_NETHERITE, Map.of(
				ModBlocks.GOLD_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.GOLD_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())
		));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.DIAMOND_TO_NETHERITE, Map.of(
				ModBlocks.DIAMOND_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.DIAMOND_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())
		));
	}

	public static class StorageHolderUpgradeDefinition {
		private final BlockItem upgradedItem;

		private StorageHolderUpgradeDefinition(BlockItem upgradedItem) {
			this.upgradedItem = upgradedItem;
		}

		public void upgradeStorageHolder(StorageHolderBase storageHolder, ItemStack storageItem) {
			ItemStack newStorageItem = new ItemStack(upgradedItem);
			newStorageItem.setTag(storageItem.getTag());

			storageHolder.setStorageItem(newStorageItem);

			if (upgradedItem.getBlock() instanceof StorageBlockBase storageBlock) {
				IStorageWrapper storageWrapper = storageHolder.getStorageWrapper();
				if (storageWrapper instanceof MovingStorageWrapper movingStorageWrapper) {
					int additionalInventorySlots = storageBlock.getNumberOfInventorySlots() - storageWrapper.getInventoryHandler().getSlots();
					int additionalUpgradeSlots = storageBlock.getNumberOfUpgradeSlots() - storageWrapper.getUpgradeHandler().getSlots();
					movingStorageWrapper.changeSize(additionalInventorySlots, additionalUpgradeSlots);
				}
			}
		}
	}
}
