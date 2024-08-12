package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.ICookingUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeConfig;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeConfig;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Config {
	private Config() {
	}

	private static final String MAX_UPGRADES_MATCHER = "([a-z0-9_]+\\|[a-z0-9_/.-]+\\|\\d+)";

	public static final Client CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;

	public static final Server SERVER;
	public static final ModConfigSpec SERVER_SPEC;
	public static final Common COMMON;
	public static final ModConfigSpec COMMON_SPEC;

	static {
		final Pair<Server, ModConfigSpec> serverSpec = new ModConfigSpec.Builder().configure(Server::new);
		SERVER_SPEC = serverSpec.getRight();
		SERVER = serverSpec.getLeft();
		final Pair<Client, ModConfigSpec> clientSpec = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpec.getRight();
		CLIENT = clientSpec.getLeft();
		final Pair<Common, ModConfigSpec> commonSpec = new ModConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = commonSpec.getRight();
		COMMON = commonSpec.getLeft();
	}

	public static class Common {
		public final ModConfigSpec.BooleanValue dropPacked;

		public Common(ModConfigSpec.Builder builder) {
			builder.comment("Common Settings").push("common");

			dropPacked = builder.comment("Determines whether chests / barrels are always dropped as packed with their contents when broken without the need to use packing tape").define("dropPacked", false);

			builder.pop();
		}
	}

	public static class Client {
		public final ModConfigSpec.BooleanValue showHigherTierTintedVariants;
		public final ModConfigSpec.BooleanValue showSingleWoodVariantOnly;

		public Client(ModConfigSpec.Builder builder) {
			builder.comment("Client-side Settings").push("client");

			showHigherTierTintedVariants = builder.comment("Determines whether JEI and creative tab will show tinted storage items for iron and higher tiers. Can help with easily removing many of these items from there.")
					.worldRestart().define("showHigherTierTintedVariants", true);
			showSingleWoodVariantOnly = builder.comment("Determines whether JEI and creative tab will show only single wood variant of storage item per tier. Makes creative tab and .")
					.worldRestart().define("showSingleWoodVariantOnly", false);

			builder.pop();
		}
	}

	public static class Server {
		public final StorageConfig woodBarrel;
		public final StorageConfig copperBarrel;
		public final StorageConfig ironBarrel;
		public final StorageConfig goldBarrel;
		public final StorageConfig diamondBarrel;
		public final StorageConfig netheriteBarrel;

		public final LimitedBarrelConfig limitedBarrel1;
		public final LimitedBarrelConfig copperLimitedBarrel1;
		public final LimitedBarrelConfig ironLimitedBarrel1;
		public final LimitedBarrelConfig goldLimitedBarrel1;
		public final LimitedBarrelConfig diamondLimitedBarrel1;
		public final LimitedBarrelConfig netheriteLimitedBarrel1;

		public final LimitedBarrelConfig limitedBarrel2;
		public final LimitedBarrelConfig copperLimitedBarrel2;
		public final LimitedBarrelConfig ironLimitedBarrel2;
		public final LimitedBarrelConfig goldLimitedBarrel2;
		public final LimitedBarrelConfig diamondLimitedBarrel2;
		public final LimitedBarrelConfig netheriteLimitedBarrel2;

		public final LimitedBarrelConfig limitedBarrel3;
		public final LimitedBarrelConfig copperLimitedBarrel3;
		public final LimitedBarrelConfig ironLimitedBarrel3;
		public final LimitedBarrelConfig goldLimitedBarrel3;
		public final LimitedBarrelConfig diamondLimitedBarrel3;
		public final LimitedBarrelConfig netheriteLimitedBarrel3;

		public final LimitedBarrelConfig limitedBarrel4;
		public final LimitedBarrelConfig copperLimitedBarrel4;
		public final LimitedBarrelConfig ironLimitedBarrel4;
		public final LimitedBarrelConfig goldLimitedBarrel4;
		public final LimitedBarrelConfig diamondLimitedBarrel4;
		public final LimitedBarrelConfig netheriteLimitedBarrel4;

		public final StorageConfig woodChest;
		public final StorageConfig copperChest;
		public final StorageConfig ironChest;
		public final StorageConfig goldChest;
		public final StorageConfig diamondChest;
		public final StorageConfig netheriteChest;

		public final StorageConfig shulkerBox;
		public final StorageConfig copperShulkerBox;
		public final StorageConfig ironShulkerBox;
		public final StorageConfig goldShulkerBox;
		public final StorageConfig diamondShulkerBox;
		public final StorageConfig netheriteShulkerBox;
		public final ShulkerBoxDisallowedItems shulkerBoxDisallowedItems;

		public final StackUpgradeConfig stackUpgrade;
		public final FilteredUpgradeConfig compactingUpgrade;
		public final FilteredUpgradeConfig advancedCompactingUpgrade;
		public final FilteredUpgradeConfig feedingUpgrade;
		public final FilteredUpgradeConfig advancedFeedingUpgrade;
		public final FilteredUpgradeConfig filterUpgrade;
		public final FilteredUpgradeConfig advancedFilterUpgrade;
		public final MagnetUpgradeConfig magnetUpgrade;
		public final MagnetUpgradeConfig advancedMagnetUpgrade;
		public final FilteredUpgradeConfig pickupUpgrade;
		public final FilteredUpgradeConfig advancedPickupUpgrade;
		public final VoidUpgradeConfig voidUpgrade;
		public final VoidUpgradeConfig advancedVoidUpgrade;
		public final CookingUpgradeConfig smeltingUpgrade;
		public final CookingUpgradeConfig smokingUpgrade;
		public final CookingUpgradeConfig blastingUpgrade;
		public final AutoCookingUpgradeConfig autoSmeltingUpgrade;
		public final AutoCookingUpgradeConfig autoSmokingUpgrade;
		public final AutoCookingUpgradeConfig autoBlastingUpgrade;
		public final PumpUpgradeConfig pumpUpgrade;
		public final XpPumpUpgradeConfig xpPumpUpgrade;
		public final CompressionUpgradeConfig compressionUpgrade;
		public final HopperUpgradeConfig hopperUpgrade;
		public final HopperUpgradeConfig advancedHopperUpgrade;

		public final ModConfigSpec.IntValue tooManyItemEntityDrops;
		public final MaxUgradesPerStorageConfig maxUpgradesPerStorage;

		public void initListeners(IEventBus modBus) {
			modBus.addListener(this::onConfigReload);
			modBus.addListener(this::onConfigLoad);
		}

		@SuppressWarnings("unused")
		//need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigReload(ModConfigEvent.Reloading event) {
			clearCache();
		}

		@SuppressWarnings("unused")
		//need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigLoad(ModConfigEvent.Loading event) {
			clearCache();
		}

		private void clearCache() {
			stackUpgrade.clearNonStackableItems();
			maxUpgradesPerStorage.clearCache();
			compressionUpgrade.clearCache();
		}

		public Server(ModConfigSpec.Builder builder) {
			builder.comment("Server Settings").push("server");

			woodBarrel = new StorageConfig(builder, "Wood Barrel", 27, 1);
			copperBarrel = new StorageConfig(builder, "Copper Barrel", 45, 1);
			ironBarrel = new StorageConfig(builder, "Iron Barrel", 54, 2);
			goldBarrel = new StorageConfig(builder, "Gold Barrel", 81, 3);
			diamondBarrel = new StorageConfig(builder, "Diamond Barrel", 108, 4);
			netheriteBarrel = new StorageConfig(builder, "Netherite Barrel", 132, 5);

			limitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Barrel I", 32, 1);
			copperLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Copper Barrel I", 53, 1);
			ironLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Iron Barrel I", 64, 2);
			goldLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Gold Barrel I", 96, 3);
			diamondLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel I", 128, 4);
			netheriteLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel I", 160, 5);

			limitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Barrel II", 16, 1);
			copperLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Copper Barrel II", 27, 1);
			ironLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Iron Barrel II", 32, 2);
			goldLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Gold Barrel II", 48, 3);
			diamondLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel II", 64, 4);
			netheriteLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel II", 80, 5);

			limitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Barrel III", 10, 1);
			copperLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Copper Barrel III", 17, 1);
			ironLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Iron Barrel III", 20, 2);
			goldLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Gold Barrel III", 30, 3);
			diamondLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel III", 40, 4);
			netheriteLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel III", 50, 5);

			limitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Barrel IV", 8, 1);
			copperLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Copper Barrel IV", 13, 1);
			ironLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Iron Barrel IV", 16, 2);
			goldLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Gold Barrel IV", 24, 3);
			diamondLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel IV", 32, 4);
			netheriteLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel IV", 40, 5);

			woodChest = new StorageConfig(builder, "Wood Chest", 27, 1);
			copperChest = new StorageConfig(builder, "Copper Chest", 45, 1);
			ironChest = new StorageConfig(builder, "Iron Chest", 54, 2);
			goldChest = new StorageConfig(builder, "Gold Chest", 81, 3);
			diamondChest = new StorageConfig(builder, "Diamond Chest", 108, 4);
			netheriteChest = new StorageConfig(builder, "Netherite Chest", 132, 5);

			shulkerBox = new StorageConfig(builder, "Shulker Box", 27, 1);
			copperShulkerBox = new StorageConfig(builder, "Copper Shulker Box", 45, 1);
			ironShulkerBox = new StorageConfig(builder, "Iron Shulker Box", 54, 2);
			goldShulkerBox = new StorageConfig(builder, "Gold Shulker Box", 81, 3);
			diamondShulkerBox = new StorageConfig(builder, "Diamond Shulker Box", 108, 4);
			netheriteShulkerBox = new StorageConfig(builder, "Netherite Shulker Box", 132, 5);
			shulkerBoxDisallowedItems = new ShulkerBoxDisallowedItems(builder);

			stackUpgrade = new StackUpgradeConfig(builder);
			compactingUpgrade = new FilteredUpgradeConfig(builder, "Compacting Upgrade", "compactingUpgrade", 9, 3);
			advancedCompactingUpgrade = new FilteredUpgradeConfig(builder, "Advanced Compacting Upgrade", "advancedCompactingUpgrade", 16, 4);
			feedingUpgrade = new FilteredUpgradeConfig(builder, "Feeding Upgrade", "feedingUpgrade", 9, 3);
			advancedFeedingUpgrade = new FilteredUpgradeConfig(builder, "Advanced Feeding Upgrade", "advancedFeedingUpgrade", 16, 4);
			filterUpgrade = new FilteredUpgradeConfig(builder, "Filter Upgrade", "filterUpgrade", 9, 3);
			advancedFilterUpgrade = new FilteredUpgradeConfig(builder, "Advanced Filter Upgrade", "advancedFilterUpgrade", 16, 4);
			magnetUpgrade = new MagnetUpgradeConfig(builder, "Magnet Upgrade", "magnetUpgrade", 9, 3, 3);
			advancedMagnetUpgrade = new MagnetUpgradeConfig(builder, "Advanced Magnet Upgrade", "advancedMagnetUpgrade", 16, 4, 5);
			pickupUpgrade = new FilteredUpgradeConfig(builder, "Pickup Upgrade", "pickupUpgrade", 9, 3);
			advancedPickupUpgrade = new FilteredUpgradeConfig(builder, "Advanced Pickup Upgrade", "advancedPickupUpgrade", 16, 4);
			voidUpgrade = new VoidUpgradeConfig(builder, "Void Upgrade", "voidUpgrade", 9, 3);
			advancedVoidUpgrade = new VoidUpgradeConfig(builder, "Advanced Void Upgrade", "advancedVoidUpgrade", 16, 4);
			smeltingUpgrade = CookingUpgradeConfig.getInstance(builder, "Smelting Upgrade", "smeltingUpgrade");
			smokingUpgrade = CookingUpgradeConfig.getInstance(builder, "Smoking Upgrade", "smokingUpgrade");
			blastingUpgrade = CookingUpgradeConfig.getInstance(builder, "Blasting Upgrade", "blastingUpgrade");
			autoSmeltingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Smelting Upgrade", "autoSmeltingUpgrade");
			autoSmokingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Smoking Upgrade", "autoSmokingUpgrade");
			autoBlastingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Blasting Upgrade", "autoBlastingUpgrade");
			pumpUpgrade = new PumpUpgradeConfig(builder);
			xpPumpUpgrade = new XpPumpUpgradeConfig(builder);
			compressionUpgrade = new CompressionUpgradeConfig(builder);
			hopperUpgrade = new HopperUpgradeConfig(builder, "Hopper Upgrade", "hopperUpgrade", 2, 2, 2, 2, 8, 1);
			advancedHopperUpgrade = new HopperUpgradeConfig(builder, "Advanced Hopper Upgrade", "advancedHopperUpgrade", 4, 4, 4, 4, 2, 4);

			maxUpgradesPerStorage = new MaxUgradesPerStorageConfig(builder, Map.of(
					ChestBlockEntity.STORAGE_TYPE, Map.of(
							StackUpgradeItem.UPGRADE_GROUP.name(), 2,
							ICookingUpgrade.UPGRADE_GROUP.name(), 1,
							ModItems.JUKEBOX_UPGRADE_NAME, 1
					),
					BarrelBlockEntity.STORAGE_TYPE, Map.of(
							StackUpgradeItem.UPGRADE_GROUP.name(), 2,
							ICookingUpgrade.UPGRADE_GROUP.name(), 1,
							ModItems.JUKEBOX_UPGRADE_NAME, 1
					),
					ShulkerBoxBlockEntity.STORAGE_TYPE, Map.of(
							StackUpgradeItem.UPGRADE_GROUP.name(), 2,
							ICookingUpgrade.UPGRADE_GROUP.name(), 1,
							ModItems.JUKEBOX_UPGRADE_NAME, 1
					),
					LimitedBarrelBlockEntity.STORAGE_TYPE, Map.of(
							ICookingUpgrade.UPGRADE_GROUP.name(), 1,
							ModItems.JUKEBOX_UPGRADE_NAME, 1
					)
			));

			tooManyItemEntityDrops = builder.comment("Threshold of number of item entities dropped from chest / barrel above which break is canceled (unless shift key is pressed) and message is displayed explaining to player many drops and packing tape use").defineInRange("tooManyItemEntityDrops", 200, 0, 1000);
			builder.pop();
		}

		public static class StorageConfig {
			public final ModConfigSpec.IntValue inventorySlotCount;
			public final ModConfigSpec.IntValue upgradeSlotCount;

			public StorageConfig(ModConfigSpec.Builder builder, String storagePrefix, int inventorySlotCountDefault, int upgradeSlotCountDefault) {
				builder.comment(storagePrefix + " Settings").push(storagePrefix.replace(" ", ""));
				inventorySlotCount = builder.comment("Number of inventory slots in the storage").defineInRange("inventorySlotCount", inventorySlotCountDefault, 1, 180);
				upgradeSlotCount = builder.comment("Number of upgrade slots in the storage").defineInRange("upgradeSlotCount", upgradeSlotCountDefault, 0, 10);
				builder.pop();
			}
		}

		public static class LimitedBarrelConfig {
			public final ModConfigSpec.IntValue baseSlotLimitMultiplier;
			public final ModConfigSpec.IntValue upgradeSlotCount;

			public LimitedBarrelConfig(ModConfigSpec.Builder builder, String storagePrefix, int baseSlotLimitMultiplierDefault, int upgradeSlotCountDefault) {
				builder.comment(storagePrefix + " Settings").push(storagePrefix.replace(" ", ""));
				baseSlotLimitMultiplier = builder.comment("Multiplier that's used to calculate base slot limit").defineInRange("baseSlotLimitMultiplier", baseSlotLimitMultiplierDefault, 1, 8192);
				upgradeSlotCount = builder.comment("Number of upgrade slots in the storage").defineInRange("upgradeSlotCount", upgradeSlotCountDefault, 0, 10);
				builder.pop();
			}
		}

		public static class ShulkerBoxDisallowedItems {
			private final ModConfigSpec.BooleanValue containerItemsDisallowed;
			private final ModConfigSpec.ConfigValue<List<String>> disallowedItemsList;
			private boolean initialized = false;
			private Set<Item> disallowedItemsSet = null;

			ShulkerBoxDisallowedItems(ModConfigSpec.Builder builder) {
				builder.push("shulkerBoxDisallowedItems");
				disallowedItemsList = builder.comment("List of items that are not allowed to be put in shulkerboxes - e.g. \"minecraft:bundle\"").define("disallowedItems", new ArrayList<>());
				containerItemsDisallowed = builder.comment("Determines if container items (those that override canFitInsideContainerItems to false) are able to fit in shulker boxes")
						.define("containerItemsDisallowed", false);
				builder.pop();
			}

			public boolean isItemDisallowed(Item item) {
				if (!SERVER_SPEC.isLoaded()) {
					return true;
				}

				if (!initialized) {
					loadDisallowedSet();
				}

				if (Boolean.TRUE.equals(containerItemsDisallowed.get()) && !item.canFitInsideContainerItems()) {
					return true;
				}

				return disallowedItemsSet.contains(item);
			}

			private void loadDisallowedSet() {
				initialized = true;
				disallowedItemsSet = new HashSet<>();

				for (String disallowedItemName : disallowedItemsList.get()) {
					ResourceLocation registryName = ResourceLocation.parse(disallowedItemName);
					BuiltInRegistries.ITEM.getOptional(registryName).ifPresent(disallowedItemsSet::add);
				}
			}
		}

		public static class MaxUgradesPerStorageConfig implements IUpgradeCountLimitConfig {
			private final ModConfigSpec.ConfigValue<List<? extends String>> maxUpgradesPerStorageList;
			@Nullable
			private Map<String, Map<String, Integer>> maxUpgradesPerStorage = null;

			protected MaxUgradesPerStorageConfig(ModConfigSpec.Builder builder, Map<String, Map<String, Integer>> defaultUpgradesPerStorage) {
				maxUpgradesPerStorageList = builder.comment("Limit of maximum number of upgrades of type per storage in format of \"StorageType|UpgradeRegistryName[or UpgradeGroup]|MaxNumber\"")
						.defineList("maxUpgradesPerStorage", convertToList(defaultUpgradesPerStorage), mapping -> ((String) mapping).matches(MAX_UPGRADES_MATCHER));
			}

			private List<String> convertToList(Map<String, Map<String, Integer>> defaultUpgradesPerStorage) {
				List<String> list = new ArrayList<>();
				defaultUpgradesPerStorage.forEach((storageType, upgradeMap) ->
						upgradeMap.forEach((upgradeName, maxNumber) ->
								list.add(storageType + "|" + upgradeName + "|" + maxNumber)
						)
				);
				return list;
			}

			public void clearCache() {
				maxUpgradesPerStorage = null;
			}

			@Override
			public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
				if (maxUpgradesPerStorage == null) {
					initMaxUpgradesPerStorage();
				}
				if (upgradeRegistryName == null) {
					return Integer.MAX_VALUE;
				}

				if (!maxUpgradesPerStorage.containsKey(storageType)) {
					return Integer.MAX_VALUE;
				}

				return maxUpgradesPerStorage.get(storageType).getOrDefault(upgradeRegistryName.getPath(), Integer.MAX_VALUE);
			}

			private void initMaxUpgradesPerStorage() {
				maxUpgradesPerStorage = new HashMap<>();
				for (String entry : maxUpgradesPerStorageList.get()) {
					String[] parts = entry.split("\\|");
					if (parts.length != 3) {
						continue;
					}

					String storageType = parts[0];
					String upgradeName = parts[1];
					int maxNumber = Integer.parseInt(parts[2]);

					maxUpgradesPerStorage.computeIfAbsent(storageType, k -> new HashMap<>()).put(upgradeName, maxNumber);
				}
			}

			@Override
			public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
				if (maxUpgradesPerStorage == null) {
					initMaxUpgradesPerStorage();
				}
				if (!maxUpgradesPerStorage.containsKey(storageType)) {
					return Integer.MAX_VALUE;
				}

				return maxUpgradesPerStorage.get(storageType).getOrDefault(upgradeGroup.name(), Integer.MAX_VALUE);
			}
		}
	}
}
