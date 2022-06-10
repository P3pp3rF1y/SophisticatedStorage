package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeConfig;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
	private Config() {}

	public static final Config.Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;

	static {
		final Pair<Config.Common, ForgeConfigSpec> commonSpec = new ForgeConfigSpec.Builder().configure(Config.Common::new);
		COMMON_SPEC = commonSpec.getRight();
		COMMON = commonSpec.getLeft();
	}

	public static class Common {
		public final StorageConfig woodBarrel;
		public final StorageConfig ironBarrel;
		public final StorageConfig goldBarrel;
		public final StorageConfig diamondBarrel;
		public final StorageConfig netheriteBarrel;

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
		public final FilteredUpgradeConfig voidUpgrade;
		public final FilteredUpgradeConfig advancedVoidUpgrade;
		public final CookingUpgradeConfig smeltingUpgrade;
		public final CookingUpgradeConfig smokingUpgrade;
		public final CookingUpgradeConfig blastingUpgrade;
		public final AutoCookingUpgradeConfig autoSmeltingUpgrade;
		public final AutoCookingUpgradeConfig autoSmokingUpgrade;
		public final AutoCookingUpgradeConfig autoBlastingUpgrade;
		public final PumpUpgradeConfig pumpUpgrade;
		public final XpPumpUpgradeConfig xpPumpUpgrade;

		public final ForgeConfigSpec.IntValue tooManyItemEntityDrops;

		@SuppressWarnings("unused") //need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigReload(ModConfigEvent.Reloading event) {
			stackUpgrade.clearNonStackableItems();
		}

		public Common(ForgeConfigSpec.Builder builder) {
			builder.comment("Common Settings").push("common");

			woodBarrel = new StorageConfig(builder, "Wood Barrel", 27, 1);
			ironBarrel = new StorageConfig(builder, "Iron Barrel", 54, 1);
			goldBarrel = new StorageConfig(builder, "Gold Barrel", 81, 2);
			diamondBarrel = new StorageConfig(builder, "Diamond Barrel", 108, 3);
			netheriteBarrel = new StorageConfig(builder, "Netherite Barrel", 132, 4);

			woodChest = new StorageConfig(builder, "Wood Chest", 27, 1);
			ironChest = new StorageConfig(builder, "Iron Chest", 54, 1);
			goldChest = new StorageConfig(builder, "Gold Chest", 81, 2);
			diamondChest = new StorageConfig(builder, "Diamond Chest", 108, 3);
			netheriteChest = new StorageConfig(builder, "Netherite Chest", 132, 4);

			shulkerBox = new StorageConfig(builder, "Shulker Box", 27, 1);
			ironShulkerBox = new StorageConfig(builder, "Iron Shulker Box", 54, 1);
			goldShulkerBox = new StorageConfig(builder, "Gold Shulker Box", 81, 2);
			diamondShulkerBox = new StorageConfig(builder, "Diamond Shulker Box", 108, 3);
			netheriteShulkerBox = new StorageConfig(builder, "Netherite Shulker Box", 132, 4);

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
			voidUpgrade = new FilteredUpgradeConfig(builder, "Void Upgrade", "voidUpgrade", 9, 3);
			advancedVoidUpgrade = new FilteredUpgradeConfig(builder, "Advanced Void Upgrade", "advancedVoidUpgrade", 16, 4);
			smeltingUpgrade = CookingUpgradeConfig.getInstance(builder, "Smelting Upgrade", "smeltingUpgrade");
			smokingUpgrade = CookingUpgradeConfig.getInstance(builder, "Smoking Upgrade", "smokingUpgrade");
			blastingUpgrade = CookingUpgradeConfig.getInstance(builder, "Blasting Upgrade", "blastingUpgrade");
			autoSmeltingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Smelting Upgrade", "autoSmeltingUpgrade");
			autoSmokingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Smoking Upgrade", "autoSmokingUpgrade");
			autoBlastingUpgrade = new AutoCookingUpgradeConfig(builder, "Auto-Blasting Upgrade", "autoBlastingUpgrade");
			pumpUpgrade = new PumpUpgradeConfig(builder);
			xpPumpUpgrade = new XpPumpUpgradeConfig(builder);
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
	}
}
