package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.LegacyUpgradeDataMigration;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeItem;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class LegacyStorageUpgradeDataMigration {
	private LegacyStorageUpgradeDataMigration() {
	}

	public static void registerMigrators() {
		LegacyUpgradeDataMigration.registerMigrator(HopperUpgradeItem.class, LegacyStorageUpgradeDataMigration::migrateHopperData);
		LegacyUpgradeDataMigration.registerMigrator(CompressionUpgradeItem.class,
				(upgradeStack, legacyData, registryAccess) -> migrateCompressionData(upgradeStack, legacyData));
	}

	public static boolean migrateHopperData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = LegacyUpgradeDataMigration.migrateComponent(upgradeStack, legacyData, "inputFilter",
				ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, registryAccess));
		migrated |= LegacyUpgradeDataMigration.migrateComponent(upgradeStack, legacyData, "outputFilter", ModDataComponents.OUTPUT_FILTER_ATTRIBUTES.get(),
				tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, registryAccess));
		migrated |= migrateDirections(upgradeStack, legacyData, "pullDirections", ModDataComponents.PULL_DIRECTIONS.get());
		migrated |= migrateDirections(upgradeStack, legacyData, "pushDirections", ModDataComponents.PUSH_DIRECTIONS.get());
		return migrated;
	}

	public static boolean migrateCompressionData(ItemStack upgradeStack, CompoundTag legacyData) {
		return LegacyUpgradeDataMigration.migrateComponent(upgradeStack, legacyData, "firstInventorySlot", ModDataComponents.FIRST_INVENTORY_SLOT.get(),
				tag -> tag instanceof IntTag intTag ? Optional.of(intTag.getAsInt()) : Optional.empty());
	}

	private static boolean migrateDirections(ItemStack upgradeStack, CompoundTag legacyData, String legacyKey, DataComponentType<Set<Direction>> component) {
		return LegacyUpgradeDataMigration.migrateComponent(upgradeStack, legacyData, legacyKey, component, LegacyStorageUpgradeDataMigration::parseDirections);
	}

	public static Optional<Set<Direction>> parseDirections(Tag tag) {
		if (!(tag instanceof ListTag directions)) {
			return Optional.empty();
		}

		Set<Direction> parsedDirections = new LinkedHashSet<>();
		for (Tag directionTag : directions) {
			if (!(directionTag instanceof StringTag stringTag)) {
				return Optional.empty();
			}
			Direction direction = Direction.byName(stringTag.getAsString());
			if (direction == null || !parsedDirections.add(direction)) {
				return Optional.empty();
			}
		}
		return Optional.of(Set.copyOf(parsedDirections));
	}
}
