package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StorageHolderTierUpgradeHandler {

	public static final Map<StorageTierUpgradeItem.TierUpgrade, Map<Item, StorageHolderUpgradeDefinition>> STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS = new HashMap<>();

	public static InteractionResult upgrade(Player player, StorageHolderBase storageHolder, ItemStack itemInHand, StorageTierUpgradeItem tierUpgradeItem) {
		Map<Item, StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition> tierDefinitions = STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS
				.get(tierUpgradeItem.getTier());
		if (tierDefinitions == null) {
			SophisticatedStorage.LOGGER.warn("No tier upgrade definitions found for {}", tierUpgradeItem.getTier());
			return InteractionResult.PASS;
		}

		ItemStack storageStack = storageHolder.getSyncedStorageStack();
		if (!storageHolder.isOpen() && !storageHolder.isPacked()) {
			if (storageHolder.isLinkedStorage() && !storageHolder.isPrimaryLinkedStorage()) {
				if (!player.level().isClientSide()) {
					player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1, 0.7F);
					player.displayClientMessage(StorageTranslationHelper.INSTANCE.translStatusMessage("tier_upgrade_main_linked_storage_only"), true);
				}
				return InteractionResult.FAIL;
			}
			StorageHolderUpgradeDefinition upgradeDefinition = tierDefinitions.get(storageStack.getItem());
			boolean cannotBeUpgradedWithThisUpgradeItem = upgradeDefinition == null;
			if (cannotBeUpgradedWithThisUpgradeItem) {
				return InteractionResult.PASS;
			}

			int countRequired = upgradeDefinition.getCountRequired(storageStack);
			if (countRequired > itemInHand.getCount()) {
				player.displayClientMessage(Component.translatable(StorageTranslationHelper.INSTANCE.translGui("status.too_low_tier_upgrade_count"),
						countRequired, itemInHand.getHoverName()), true);
				return InteractionResult.FAIL;
			}

			if (!player.level().isClientSide()) {
				if (storageHolder.isLinkedStorage()) {
					if (!(player.level() instanceof ServerLevel serverLevel)
							|| !upgradeDefinition.upgradeLinkedStorageHolder(serverLevel, storageHolder, storageStack)) {
						return InteractionResult.FAIL;
					}
				} else {
					upgradeDefinition.upgradeStorageHolder(storageHolder, storageStack);
				}

				if (!player.isCreative()) {
					itemInHand.shrink(countRequired);
				}
			}

			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	static {
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_COPPER,
				Map.of(ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_BARREL_ITEM.get()),
						ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_CHEST_ITEM.get()),
						ModBlocks.SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.COPPER_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON,
				Map.of(ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_BARREL_ITEM.get()),
						ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_CHEST_ITEM.get()),
						ModBlocks.SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_GOLD,
				Map.of(ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
						ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
						ModBlocks.SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_DIAMOND,
				Map.of(ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
						ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
						ModBlocks.SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_NETHERITE,
				Map.of(ModBlocks.BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
						ModBlocks.CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
						ModBlocks.SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_IRON,
				Map.of(ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_BARREL_ITEM.get()),
						ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_CHEST_ITEM.get()),
						ModBlocks.COPPER_SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_GOLD,
				Map.of(ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
						ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
						ModBlocks.COPPER_SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_DIAMOND, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_NETHERITE, Map.of(
				ModBlocks.COPPER_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.COPPER_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.COPPER_SHULKER_BOX_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_GOLD,
				Map.of(ModBlocks.IRON_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_BARREL_ITEM.get()),
						ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_CHEST_ITEM.get()),
						ModBlocks.IRON_SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_DIAMOND,
				Map.of(ModBlocks.IRON_BARREL_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
						ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
						ModBlocks.IRON_SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.IRON_TO_NETHERITE, Map.of(
				ModBlocks.IRON_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.IRON_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.IRON_SHULKER_BOX_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_DIAMOND,
				Map.of(ModBlocks.GOLD_BARREL_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_BARREL_ITEM.get()),
						ModBlocks.GOLD_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_CHEST_ITEM.get()),
						ModBlocks.GOLD_SHULKER_BOX_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
						ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get()),
						ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get()),
						ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get()),
						ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(),
						new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get())));
		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_NETHERITE, Map.of(
				ModBlocks.GOLD_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.GOLD_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.GOLD_SHULKER_BOX_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())));

		StorageHolderTierUpgradeHandler.STORAGE_HOLDER_TIER_UPGRADE_DEFINITIONS.put(StorageTierUpgradeItem.TierUpgrade.DIAMOND_TO_NETHERITE, Map.of(
				ModBlocks.DIAMOND_BARREL_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_BARREL_ITEM.get()),
				ModBlocks.DIAMOND_CHEST_ITEM.get(), new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_CHEST_ITEM.get()),
				ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(),
				new StorageHolderTierUpgradeHandler.StorageHolderUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get())));
	}

	public static class StorageHolderUpgradeDefinition {
		private final BlockItem upgradedItem;

		private StorageHolderUpgradeDefinition(BlockItem upgradedItem) {
			this.upgradedItem = upgradedItem;
		}

		public void upgradeStorageHolder(StorageHolderBase storageHolder, ItemStack storageItem) {
			if (upgradedItem.getBlock() instanceof StorageBlockBase storageBlock) {
				if (isDoubleChest(storageItem)) {
					upgradeIndividualStorageHolder(storageHolder.getMainStorageHolder(), storageItem, storageBlock.getNumberOfInventorySlots() * 2,
							storageBlock.getNumberOfUpgradeSlots());
					storageHolder.getAuxiliaryStorageHolder().ifPresent(auxiliaryStorageHolder -> {
						upgradeIndividualStorageHolder(auxiliaryStorageHolder, storageItem, storageBlock.getNumberOfInventorySlots() * 2,
								storageBlock.getNumberOfUpgradeSlots());
					});
				} else {
					upgradeIndividualStorageHolder(storageHolder, storageItem, storageBlock.getNumberOfInventorySlots(),
							storageBlock.getNumberOfUpgradeSlots());
				}
			}
		}

		public boolean upgradeLinkedStorageHolder(ServerLevel level, StorageHolderBase storageHolder, ItemStack storageItem) {
			LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(storageItem);
			if (endpoint == null || !(upgradedItem.getBlock() instanceof StorageBlockBase storageBlock)) {
				return false;
			}
			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
			if (!manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
				return false;
			}
			Optional<StorageLinkedStorageHostWrapper> host = manager.resolveVirtualHost(endpoint.groupId())
					.filter(StorageLinkedStorageHostWrapper.class::isInstance).map(StorageLinkedStorageHostWrapper.class::cast);
			Optional<LinkedStorageHostDescriptor> descriptor = manager.getHostDescriptor(endpoint.groupId());
			if (host.isEmpty() || descriptor.isEmpty() || !descriptor.get().factoryId().equals(StorageLinkedStorageHostWrapper.FACTORY_ID)) {
				return false;
			}
			int inventorySlots = storageBlock.getNumberOfInventorySlots() * (isDoubleChest(storageItem) ? 2 : 1);
			int upgradeSlots = storageBlock.getNumberOfUpgradeSlots();
			ItemStack upgradedStack = new ItemStack(upgradedItem);
			upgradedStack.setTag(storageItem.getTag());
			StorageBlockItem.setNumberOfInventorySlots(upgradedStack, inventorySlots);
			StorageBlockItem.setNumberOfUpgradeSlots(upgradedStack, upgradeSlots);
			CompoundTag carrier = StorageLinkedStorageHostWrapper.withDisplayName(descriptor.get().virtualCarrier(), upgradedStack.getHoverName());
			carrier.putInt("inventorySlots", inventorySlots);
			carrier.putInt("upgradeSlots", upgradeSlots);
			carrier.putInt("baseStackSizeMultiplier", storageBlock.getBaseStackSizeMultiplier());
			host.get().changeSize(inventorySlots - host.get().getInventoryHandler().getSlots(), upgradeSlots - host.get().getUpgradeHandler().getSlots());
			host.get().getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
			CompoundTag renderInfo = host.get().getRenderInfo().getNbt();
			carrier.put(StorageWrapper.RENDER_INFO_TAG, renderInfo.copy());
			upgradedStack.getOrCreateTag().put(StorageWrapper.RENDER_INFO_TAG, renderInfo.copy());
			host.get().persistCanonicalContents();
			if (!manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(),
					new LinkedStorageHostDescriptor(StorageLinkedStorageHostWrapper.FACTORY_ID, carrier))) {
				return false;
			}
			storageHolder.setStorageItem(upgradedStack);
			if (isDoubleChest(storageItem)) {
				storageHolder.getAuxiliaryStorageHolder().ifPresent(auxiliaryStorageHolder -> upgradeIndividualStorageHolder(auxiliaryStorageHolder,
						auxiliaryStorageHolder.getSyncedStorageStack(), inventorySlots, upgradeSlots));
			}
			return true;
		}

		private void upgradeIndividualStorageHolder(StorageHolderBase storageHolder, ItemStack storageItem, int newNumberOfInventorySlots,
				int newNumberOfUpgradeSlots) {
			ItemStack newStorageItem = new ItemStack(upgradedItem);
			newStorageItem.setTag(storageItem.getTag());

			storageHolder.setStorageItem(newStorageItem);

			IStorageWrapper storageWrapper = storageHolder.getStorageWrapper();
			if (storageWrapper instanceof MovingStorageWrapper movingStorageWrapper) {
				int additionalInventorySlots = newNumberOfInventorySlots - storageWrapper.getInventoryHandler().getSlots();
				int additionalUpgradeSlots = newNumberOfUpgradeSlots - storageWrapper.getUpgradeHandler().getSlots();
				movingStorageWrapper.changeSize(additionalInventorySlots, additionalUpgradeSlots);
			}
		}

		public int getCountRequired(ItemStack storageStack) {
			if (isDoubleChest(storageStack)) {
				return 2;
			}

			return 1;
		}

		private boolean isDoubleChest(ItemStack storageStack) {
			return storageStack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(storageStack);
		}
	}
}
