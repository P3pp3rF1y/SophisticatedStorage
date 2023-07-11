package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeConfig;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeConfig;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
	private Config() {}

	public static final Client CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	public static final Server SERVER;
	public static final ForgeConfigSpec SERVER_SPEC;

	static {
		final Pair<Server, ForgeConfigSpec> commonSpec = new ForgeConfigSpec.Builder().configure(Server::new);
		SERVER_SPEC = commonSpec.getRight();
		SERVER = commonSpec.getLeft();
		final Pair<Client, ForgeConfigSpec> clientSpec = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpec.getRight();
		CLIENT = clientSpec.getLeft();
	}

	public static class Client {
		public final ForgeConfigSpec.BooleanValue showHigherTierTintedVariants;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client-side Settings").push("client");

			showHigherTierTintedVariants = builder.comment("Determines whether JEI and creative tab will show tinted storage items for iron and higher tiers. Can help with easily removing many of these items from there.")
					.worldRestart().define("showHigherTierTintedVariants", true);

			builder.pop();
		}
	}

	public static class Server {
		public final StorageConfig woodBarrel;
		public final StorageConfig ironBarrel;
		public final StorageConfig goldBarrel;
		public final StorageConfig diamondBarrel;
		public final StorageConfig netheriteBarrel;

		public final LimitedBarrelConfig limitedBarrel1;
		public final LimitedBarrelConfig ironLimitedBarrel1;
		public final LimitedBarrelConfig goldLimitedBarrel1;
		public final LimitedBarrelConfig diamondLimitedBarrel1;
		public final LimitedBarrelConfig netheriteLimitedBarrel1;

		public final LimitedBarrelConfig limitedBarrel2;
		public final LimitedBarrelConfig ironLimitedBarrel2;
		public final LimitedBarrelConfig goldLimitedBarrel2;
		public final LimitedBarrelConfig diamondLimitedBarrel2;
		public final LimitedBarrelConfig netheriteLimitedBarrel2;

		public final LimitedBarrelConfig limitedBarrel3;
		public final LimitedBarrelConfig ironLimitedBarrel3;
		public final LimitedBarrelConfig goldLimitedBarrel3;
		public final LimitedBarrelConfig diamondLimitedBarrel3;
		public final LimitedBarrelConfig netheriteLimitedBarrel3;

		public final LimitedBarrelConfig limitedBarrel4;
		public final LimitedBarrelConfig ironLimitedBarrel4;
		public final LimitedBarrelConfig goldLimitedBarrel4;
		public final LimitedBarrelConfig diamondLimitedBarrel4;
		public final LimitedBarrelConfig netheriteLimitedBarrel4;

		public final StorageConfig woodChest;
		public final StorageConfig ironChest;
		public final StorageConfig goldChest;
		public final StorageConfig diamondChest;
		public final StorageConfig netheriteChest;

		public final StorageConfig shulkerBox;
		public final StorageConfig ironShulkerBox;
		public final StorageConfig goldShulkerBox;
		public final StorageConfig diamondShulkerBox;
		public final StorageConfig netheriteShulkerBox;
		public final ShulkerBoxDisallowedItems shulkerBoxDisallowedItems;

		public final StackUpgradeConfig stackUpgrade;
		public final FilteredUpgradeConfig compactingUpgrade;
		public final FilteredUpgradeConfig advancedCompactingUpgrade;
		public final FilteredUpgradeConfig depositUpgrade;
		public final FilteredUpgradeConfig advancedDepositUpgrade;
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

		public final ForgeConfigSpec.IntValue tooManyItemEntityDrops;

		@SuppressWarnings("unused") //need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigReload(ModConfigEvent.Reloading event) {
			stackUpgrade.clearNonStackableItems();
		}

		public Server(ForgeConfigSpec.Builder builder) {
			builder.comment("Server Settings").push("server");

			woodBarrel = new StorageConfig(builder, "Wood Barrel", 27, 1);
			ironBarrel = new StorageConfig(builder, "Iron Barrel", 54, 2);
			goldBarrel = new StorageConfig(builder, "Gold Barrel", 81, 3);
			diamondBarrel = new StorageConfig(builder, "Diamond Barrel", 108, 4);
			netheriteBarrel = new StorageConfig(builder, "Netherite Barrel", 132, 5);

			limitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Barrel I", 32, 1);
			ironLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Iron Barrel I", 64, 2);
			goldLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Gold Barrel I", 96, 3);
			diamondLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel I", 128, 4);
			netheriteLimitedBarrel1 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel I", 160, 5);

			limitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Barrel II", 16, 1);
			ironLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Iron Barrel II", 32, 2);
			goldLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Gold Barrel II", 48, 3);
			diamondLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel II", 64, 4);
			netheriteLimitedBarrel2 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel II", 80, 5);

			limitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Barrel III", 10, 1);
			ironLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Iron Barrel III", 20, 2);
			goldLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Gold Barrel III", 30, 3);
			diamondLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel III", 40, 4);
			netheriteLimitedBarrel3 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel III", 50, 5);

			limitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Barrel IV", 8, 1);
			ironLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Iron Barrel IV", 16, 2);
			goldLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Gold Barrel IV", 24, 3);
			diamondLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Diamond Barrel IV", 32, 4);
			netheriteLimitedBarrel4 = new LimitedBarrelConfig(builder, "Limited Netherite Barrel IV", 40, 5);

			woodChest = new StorageConfig(builder, "Wood Chest", 27, 1);
			ironChest = new StorageConfig(builder, "Iron Chest", 54, 2);
			goldChest = new StorageConfig(builder, "Gold Chest", 81, 3);
			diamondChest = new StorageConfig(builder, "Diamond Chest", 108, 4);
			netheriteChest = new StorageConfig(builder, "Netherite Chest", 132, 5);

			shulkerBox = new StorageConfig(builder, "Shulker Box", 27, 1);
			ironShulkerBox = new StorageConfig(builder, "Iron Shulker Box", 54, 2);
			goldShulkerBox = new StorageConfig(builder, "Gold Shulker Box", 81, 3);
			diamondShulkerBox = new StorageConfig(builder, "Diamond Shulker Box", 108, 4);
			netheriteShulkerBox = new StorageConfig(builder, "Netherite Shulker Box", 132, 5);
			shulkerBoxDisallowedItems = new ShulkerBoxDisallowedItems(builder);

			stackUpgrade = new StackUpgradeConfig(builder);
			compactingUpgrade = new FilteredUpgradeConfig(builder, "Compacting Upgrade", "compactingUpgrade", 9, 3);
			advancedCompactingUpgrade = new FilteredUpgradeConfig(builder, "Advanced Compacting Upgrade", "advancedCompactingUpgrade", 16, 4);
			depositUpgrade = new FilteredUpgradeConfig(builder, "Deposit Upgrade", "depositUpgrade", 9, 3);
			advancedDepositUpgrade = new FilteredUpgradeConfig(builder, "Advanced Deposit Upgrade", "advancedDepositUpgrade", 16, 4);
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

			tooManyItemEntityDrops = builder.comment("Threshold of number of item entities dropped from chest / barrel above which break is canceled (unless shift key is pressed) and message is displayed explaining to player many drops and packing tape use").defineInRange("tooManyItemEntityDrops", 200, 0, 1000);
			builder.pop();
		}

		public static class StorageConfig {
			public final ForgeConfigSpec.IntValue inventorySlotCount;
			public final ForgeConfigSpec.IntValue upgradeSlotCount;

			public StorageConfig(ForgeConfigSpec.Builder builder, String storagePrefix, int inventorySlotCountDefault, int upgradeSlotCountDefault) {
				builder.comment(storagePrefix + " Settings").push(storagePrefix.replace(" ", ""));
				inventorySlotCount = builder.comment("Number of inventory slots in the storage").defineInRange("inventorySlotCount", inventorySlotCountDefault, 1, 180);
				upgradeSlotCount = builder.comment("Number of upgrade slots in the storage").defineInRange("upgradeSlotCount", upgradeSlotCountDefault, 0, 10);
				builder.pop();
			}
		}

		public static class LimitedBarrelConfig {
			public final ForgeConfigSpec.IntValue baseSlotLimitMultiplier;
			public final ForgeConfigSpec.IntValue upgradeSlotCount;

			public LimitedBarrelConfig(ForgeConfigSpec.Builder builder, String storagePrefix, int baseSlotLimitMultiplierDefault, int upgradeSlotCountDefault) {
				builder.comment(storagePrefix + " Settings").push(storagePrefix.replace(" ", ""));
				baseSlotLimitMultiplier = builder.comment("Multiplier that's used to calculate base slot limit").defineInRange("baseSlotLimitMultiplier", baseSlotLimitMultiplierDefault, 1, 8192);
				upgradeSlotCount = builder.comment("Number of upgrade slots in the storage").defineInRange("upgradeSlotCount", upgradeSlotCountDefault, 0, 10);
				builder.pop();
			}
		}

		public static class ShulkerBoxDisallowedItems {
			private final ForgeConfigSpec.BooleanValue containerItemsDisallowed;
			private final ForgeConfigSpec.ConfigValue<List<String>> disallowedItemsList;
			private boolean initialized = false;
			private Set<Item> disallowedItemsSet = null;

			ShulkerBoxDisallowedItems(ForgeConfigSpec.Builder builder) {
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
					ResourceLocation registryName = new ResourceLocation(disallowedItemName);
					if (ForgeRegistries.ITEMS.containsKey(registryName)) {
						disallowedItemsSet.add(ForgeRegistries.ITEMS.getValue(registryName));
					}
				}
			}
		}
	}
}
