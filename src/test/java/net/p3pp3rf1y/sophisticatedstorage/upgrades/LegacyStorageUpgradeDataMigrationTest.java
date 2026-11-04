package net.p3pp3rf1y.sophisticatedstorage.upgrades;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.util.LegacyUpgradeDataMigration;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyStorageUpgradeDataMigrationTest {
	private static final int EXPECTED_UPGRADE_ITEM_COUNT = 42;
	private static final RegistryAccess REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		LegacyStorageUpgradeDataMigration.registerMigrators();
	}

	@Test
	void hopperMigrationMapsFiltersDirectionsAndNestedLegacyItems() {
		ItemStack stack = new ItemStack(Items.STICK);
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("inputFilter", filterData("minecraft:stone", true));
		legacyData.put("outputFilter", filterData("minecraft:dirt", false));
		legacyData.put("pullDirections", directions("down", "up", "north"));
		legacyData.put("pushDirections", directions("south", "west", "east"));
		legacyData.putString("unknown", "preserved");

		assertTrue(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, legacyData, REGISTRIES));

		FilterAttributes inputAttributes = stack.get(ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES);
		FilterAttributes outputAttributes = stack.get(ModDataComponents.OUTPUT_FILTER_ATTRIBUTES);
		assertNotNull(inputAttributes);
		assertNotNull(outputAttributes);
		assertTrue(inputAttributes.matchAnyTag());
		assertFalse(outputAttributes.matchAnyTag());
		assertEquals(Items.STONE, inputAttributes.filterItems().getStackInSlot(1).getItem());
		assertEquals("legacy", inputAttributes.filterItems().getStackInSlot(1).get(DataComponents.CUSTOM_DATA).copyTag().getString("nested"));
		assertEquals(Items.DIRT, outputAttributes.filterItems().getStackInSlot(1).getItem());
		assertEquals(Set.of(Direction.DOWN, Direction.UP, Direction.NORTH), stack.get(ModDataComponents.PULL_DIRECTIONS));
		assertEquals(Set.of(Direction.SOUTH, Direction.WEST, Direction.EAST), stack.get(ModDataComponents.PUSH_DIRECTIONS));
		assertEquals("preserved", legacyData.getString("unknown"));
		assertFalse(legacyData.contains("inputFilter"));
		assertFalse(legacyData.contains("outputFilter"));
		assertFalse(legacyData.contains("pullDirections"));
		assertFalse(legacyData.contains("pushDirections"));
		assertFalse(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, legacyData, REGISTRIES));
	}

	@Test
	void hopperMigrationDropsLegacyDataForExistingComponentsAndRetainsMalformedValuesWithoutTargets() {
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES, new FilterAttributes(Set.of(), false, false, false, PrimaryMatch.ITEM, false,
				net.minecraft.world.item.component.ItemContainerContents.EMPTY, false, false));
		stack.set(ModDataComponents.PULL_DIRECTIONS, Set.of(Direction.UP));
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("inputFilter", filterData("minecraft:stone", true));
		legacyData.put("pullDirections", directions("down"));
		legacyData.put("pushDirections", directions("north", "north"));
		legacyData.put("outputFilter", StringTag.valueOf("malformed"));

		assertTrue(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, legacyData, REGISTRIES));

		assertFalse(stack.get(ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES).matchAnyTag());
		assertEquals(Set.of(Direction.UP), stack.get(ModDataComponents.PULL_DIRECTIONS));
		assertFalse(legacyData.contains("inputFilter"));
		assertFalse(legacyData.contains("pullDirections"));
		assertTrue(legacyData.contains("pushDirections"));
		assertTrue(legacyData.contains("outputFilter"));

		CompoundTag malformedExistingTargetData = new CompoundTag();
		malformedExistingTargetData.put("inputFilter", StringTag.valueOf("malformed"));
		assertTrue(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, malformedExistingTargetData, REGISTRIES));
		assertTrue(malformedExistingTargetData.isEmpty());

		legacyData.put("pushDirections", directions("unknown"));
		assertFalse(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, legacyData, REGISTRIES));
		assertTrue(legacyData.contains("pushDirections"));

		legacyData.put("pushDirections", directionsWithNonString());
		assertFalse(LegacyStorageUpgradeDataMigration.migrateHopperData(stack, legacyData, REGISTRIES));
		assertTrue(legacyData.contains("pushDirections"));
	}

	@Test
	void compressionMigrationMapsOnlyAnIntFirstInventorySlot() {
		ItemStack stack = new ItemStack(Items.STICK);
		CompoundTag legacyData = new CompoundTag();
		legacyData.putInt("firstInventorySlot", 7);
		legacyData.putString("unknown", "preserved");

		assertTrue(LegacyStorageUpgradeDataMigration.migrateCompressionData(stack, legacyData));
		assertEquals(7, stack.get(ModDataComponents.FIRST_INVENTORY_SLOT));
		assertFalse(legacyData.contains("firstInventorySlot"));
		assertEquals("preserved", legacyData.getString("unknown"));
		assertFalse(LegacyStorageUpgradeDataMigration.migrateCompressionData(stack, legacyData));

		ItemStack currentStack = new ItemStack(Items.STICK);
		currentStack.set(ModDataComponents.FIRST_INVENTORY_SLOT, 3);
		CompoundTag currentLegacyData = new CompoundTag();
		currentLegacyData.putInt("firstInventorySlot", 7);
		assertTrue(LegacyStorageUpgradeDataMigration.migrateCompressionData(currentStack, currentLegacyData));
		assertEquals(3, currentStack.get(ModDataComponents.FIRST_INVENTORY_SLOT));
		assertFalse(currentLegacyData.contains("firstInventorySlot"));

		CompoundTag malformedData = new CompoundTag();
		malformedData.putString("firstInventorySlot", "seven");
		assertFalse(LegacyStorageUpgradeDataMigration.migrateCompressionData(new ItemStack(Items.STICK), malformedData));
		assertTrue(malformedData.contains("firstInventorySlot"));

		assertTrue(LegacyStorageUpgradeDataMigration.migrateCompressionData(currentStack, malformedData));
		assertEquals(3, currentStack.get(ModDataComponents.FIRST_INVENTORY_SLOT));
		assertTrue(malformedData.isEmpty());
	}

	@Test
	void dispatchesRegisteredStorageMigrations() {
		CompoundTag hopperData = new CompoundTag();
		hopperData.put("pullDirections", directions("down"));
		assertDispatched(ModItems.HOPPER_UPGRADE.get(), hopperData.copy());
		assertDispatched(ModItems.ADVANCED_HOPPER_UPGRADE.get(), hopperData.copy());

		CompoundTag compressionData = new CompoundTag();
		compressionData.putInt("firstInventorySlot", 4);
		assertDispatched(ModItems.COMPRESSION_UPGRADE.get(), compressionData);
	}

	@TestFactory
	Stream<DynamicTest> migrateAllRegisteredUpgradeItemsFrom1201Data() {
		List<MigrationCase> migrationCases = List.of(
				filterCase(ModItems.PICKUP_UPGRADE.get(), "filters", emptyInventory(), false, ModCoreDataComponents.FILTER_ATTRIBUTES.get()),
				filterCase(ModItems.ADVANCED_PICKUP_UPGRADE.get(), "filters", emptyInventory(), false, ModCoreDataComponents.FILTER_ATTRIBUTES.get()),
				new MigrationCase(ModItems.FILTER_UPGRADE.get(), "direction", StringTag.valueOf("both"),
						stack -> assertEquals(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.BOTH, stack.get(ModCoreDataComponents.DIRECTION))),
				new MigrationCase(ModItems.ADVANCED_FILTER_UPGRADE.get(), "direction", StringTag.valueOf("both"),
						stack -> assertEquals(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.BOTH, stack.get(ModCoreDataComponents.DIRECTION))),
				booleanCase(ModItems.MAGNET_UPGRADE.get(), "pickupItems", ModCoreDataComponents.PICKUP_ITEMS.get()),
				booleanCase(ModItems.ADVANCED_MAGNET_UPGRADE.get(), "pickupItems", ModCoreDataComponents.PICKUP_ITEMS.get()),
				booleanCase(ModItems.FEEDING_UPGRADE.get(), "feedImmediatelyWhenHurt", ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT.get()),
				booleanCase(ModItems.ADVANCED_FEEDING_UPGRADE.get(), "feedImmediatelyWhenHurt", ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT.get()),
				booleanCase(ModItems.COMPACTING_UPGRADE.get(), "compactNonUncraftable", ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE.get()),
				booleanCase(ModItems.ADVANCED_COMPACTING_UPGRADE.get(), "compactNonUncraftable", ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE.get()),
				booleanCase(ModItems.VOID_UPGRADE.get(), "shouldWorkInGUI", ModCoreDataComponents.SHOULD_WORK_IN_GUI.get()),
				booleanCase(ModItems.ADVANCED_VOID_UPGRADE.get(), "shouldWorkInGUI", ModCoreDataComponents.SHOULD_WORK_IN_GUI.get()),
				floatCase(ModItems.SMELTING_UPGRADE.get()),
				filterCase(ModItems.AUTO_SMELTING_UPGRADE.get(), "inputFilter", autoCookingFilterData(), true,
						ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get()),
				floatCase(ModItems.SMOKING_UPGRADE.get()),
				filterCase(
						ModItems.AUTO_SMOKING_UPGRADE.get(), "inputFilter", autoCookingFilterData(), true, ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get()),
				floatCase(ModItems.BLASTING_UPGRADE.get()),
				filterCase(ModItems.AUTO_BLASTING_UPGRADE.get(), "inputFilter", autoCookingFilterData(), true,
						ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get()),
				booleanCase(ModItems.CRAFTING_UPGRADE.get(), "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get()),
				new MigrationCase(ModItems.STONECUTTER_UPGRADE.get(), "recipeId", StringTag.valueOf("minecraft:stone"),
						stack -> assertEquals(ResourceLocation.withDefaultNamespace("stone"), stack.get(ModCoreDataComponents.RECIPE_ID))),
				booleanCase(ModItems.JUKEBOX_UPGRADE.get(), "isPlaying", ModCoreDataComponents.IS_PLAYING.get()),
				booleanCase(ModItems.ADVANCED_JUKEBOX_UPGRADE.get(), "isPlaying", ModCoreDataComponents.IS_PLAYING.get()),
				booleanCase(ModItems.PUMP_UPGRADE.get(), "input", ModCoreDataComponents.IS_INPUT.get()),
				booleanCase(ModItems.ADVANCED_PUMP_UPGRADE.get(), "input", ModCoreDataComponents.IS_INPUT.get()),
				booleanCase(ModItems.XP_PUMP_UPGRADE.get(), "mendItems", ModCoreDataComponents.MEND_ITEMS.get()),
				new MigrationCase(ModItems.COMPRESSION_UPGRADE.get(), "firstInventorySlot", IntTag.valueOf(7),
						stack -> assertEquals(7, stack.get(ModDataComponents.FIRST_INVENTORY_SLOT))),
				new MigrationCase(ModItems.HOPPER_UPGRADE.get(), "pullDirections", directions("down"),
						stack -> assertEquals(Set.of(Direction.DOWN), stack.get(ModDataComponents.PULL_DIRECTIONS))),
				new MigrationCase(ModItems.ADVANCED_HOPPER_UPGRADE.get(), "pullDirections", directions("down"),
						stack -> assertEquals(Set.of(Direction.DOWN), stack.get(ModDataComponents.PULL_DIRECTIONS))),
				booleanCase(ModItems.ALCHEMY_UPGRADE.get(), "matchAllEffects", ModCoreDataComponents.MATCH_ALL_EFFECTS.get()),
				booleanCase(ModItems.ADVANCED_ALCHEMY_UPGRADE.get(), "matchAllEffects", ModCoreDataComponents.MATCH_ALL_EFFECTS.get()),
				commonEnabledCase(ModItems.STACK_UPGRADE_TIER_1.get()), commonEnabledCase(ModItems.STACK_UPGRADE_TIER_1_PLUS.get()),
				commonEnabledCase(ModItems.STACK_UPGRADE_TIER_2.get()), commonEnabledCase(ModItems.STACK_UPGRADE_TIER_3.get()),
				commonEnabledCase(ModItems.STACK_UPGRADE_TIER_4.get()), commonEnabledCase(ModItems.STACK_UPGRADE_TIER_5.get()),
				commonEnabledCase(ModItems.STACK_DOWNGRADE_TIER_1.get()), commonEnabledCase(ModItems.STACK_DOWNGRADE_TIER_2.get()),
				commonEnabledCase(ModItems.STACK_DOWNGRADE_TIER_3.get()), commonEnabledCase(ModItems.STACK_UPGRADE_OMEGA_TIER.get()),
				commonEnabledCase(ModItems.INFINITY_UPGRADE.get()), commonEnabledCase(ModItems.SURVIVAL_INFINITY_UPGRADE.get()));

		assertMigrationCaseCompleteness(migrationCases);
		return migrationCases.stream().map(migrationCase -> DynamicTest.dynamicTest(migrationCase.item().toString(), () -> migrate(migrationCase)));
	}

	private static MigrationCase booleanCase(Item item, String legacyKey, DataComponentType<Boolean> component) {
		return new MigrationCase(item, legacyKey, ByteTag.valueOf(false), stack -> {
			assertTrue(stack.has(component));
			assertEquals(false, stack.get(component));
		});
	}

	private static MigrationCase commonEnabledCase(Item item) {
		return booleanCase(item, "enabled", ModCoreDataComponents.ENABLED.get());
	}

	private static MigrationCase filterCase(Item item, String legacyKey, CompoundTag legacyValue, boolean isAllowList,
			DataComponentType<FilterAttributes> component) {
		return new MigrationCase(item, legacyKey, legacyValue, stack -> assertEquals(filterAttributes(isAllowList), stack.get(component)));
	}

	private static MigrationCase floatCase(Item item) {
		return new MigrationCase(item, "storedXp", FloatTag.valueOf(2.5F), stack -> assertEquals(2.5F, stack.get(ModCoreDataComponents.STORED_XP)));
	}

	private static void assertMigrationCaseCompleteness(List<MigrationCase> migrationCases) {
		Set<Item> caseItems = migrationCases.stream().map(MigrationCase::item).collect(Collectors.toSet());
		Set<Item> registeredUpgradeItems = ModItems.ITEMS.getEntries().stream().map(entry -> entry.get()).filter(IUpgradeItem.class::isInstance)
				.collect(Collectors.toSet());

		assertEquals(EXPECTED_UPGRADE_ITEM_COUNT, migrationCases.size(), "Expected 30 stateful and 12 common-enabled-only upgrade migration cases");
		assertEquals(migrationCases.size(), caseItems.size(), "Upgrade migration cases must not contain duplicate items");
		assertEquals(EXPECTED_UPGRADE_ITEM_COUNT, registeredUpgradeItems.size(), "Expected concrete registered IUpgradeItem count");
		assertEquals(registeredUpgradeItems, caseItems, "Upgrade migration cases must cover every registered IUpgradeItem");
	}

	private static void migrate(MigrationCase migrationCase) {
		ItemStack stack = new ItemStack(migrationCase.item());
		CompoundTag legacyData = new CompoundTag();
		legacyData.put(migrationCase.legacyKey(), migrationCase.legacyValue().copy());
		legacyData.putString("unknown", "preserved");
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertTrue(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
		migrationCase.targetAssertion().accept(stack);

		CompoundTag remainingData = stack.get(DataComponents.CUSTOM_DATA).copyTag();
		assertFalse(remainingData.contains(migrationCase.legacyKey()));
		assertEquals("preserved", remainingData.getString("unknown"));
		assertFalse(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
	}

	private static CompoundTag rootFilterData() {
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("filters", emptyInventory());
		return legacyData;
	}

	private static CompoundTag autoCookingFilterData() {
		return rootFilterData();
	}

	private static CompoundTag emptyInventory() {
		CompoundTag inventory = new CompoundTag();
		inventory.putInt("Size", 0);
		inventory.put("Items", new ListTag());
		return inventory;
	}

	private static FilterAttributes filterAttributes(boolean isAllowList) {
		return new FilterAttributes(Set.of(), isAllowList, false, false, PrimaryMatch.ITEM, true, ItemContainerContents.EMPTY, false, false);
	}

	private static void assertDispatched(net.minecraft.world.item.Item upgradeItem, CompoundTag legacyData) {
		ItemStack stack = new ItemStack(upgradeItem);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertTrue(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
		assertFalse(stack.has(DataComponents.CUSTOM_DATA));
	}

	private static CompoundTag filterData(String itemId, boolean matchAnyTag) {
		CompoundTag filterData = new CompoundTag();
		filterData.putBoolean("isAllowList", true);
		filterData.putBoolean("matchAnyTag", matchAnyTag);
		CompoundTag inventory = new CompoundTag();
		inventory.putInt("Size", 2);
		CompoundTag item = new CompoundTag();
		item.putString("id", itemId);
		item.putByte("Count", (byte) 1);
		item.putInt("Slot", 1);
		CompoundTag itemTag = new CompoundTag();
		itemTag.putString("nested", "legacy");
		item.put("tag", itemTag);
		ListTag items = new ListTag();
		items.add(item);
		inventory.put("Items", items);
		filterData.put("filters", inventory);
		return filterData;
	}

	private static ListTag directions(String... directions) {
		ListTag result = new ListTag();
		for (String direction : directions) {
			result.add(StringTag.valueOf(direction));
		}
		return result;
	}

	private static ListTag directionsWithNonString() {
		ListTag result = new ListTag();
		result.add(IntTag.valueOf(1));
		return result;
	}

	private record MigrationCase(Item item, String legacyKey, Tag legacyValue, Consumer<ItemStack> targetAssertion) {
	}
}
