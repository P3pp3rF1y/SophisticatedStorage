package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryInventoryPart;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoBlastingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoSmeltingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoSmokingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.BlastingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.SmeltingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.SmokingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankInventoryPart;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

public class ModItems {
	private ModItems() {}

	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);

	public static final ResourceLocation STORAGE_UPGRADE_TAG_NAME = new ResourceLocation(SophisticatedStorage.MOD_ID, "upgrade");

	public static final TagKey<Item> STORAGE_UPGRADE_TAG = TagKey.create(Registry.ITEM_REGISTRY, STORAGE_UPGRADE_TAG_NAME);

	public static final RegistryObject<PickupUpgradeItem> PICKUP_UPGRADE = ITEMS.register("pickup_upgrade",
			() -> new PickupUpgradeItem(Config.COMMON.pickupUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<PickupUpgradeItem> ADVANCED_PICKUP_UPGRADE = ITEMS.register("advanced_pickup_upgrade",
			() -> new PickupUpgradeItem(Config.COMMON.advancedPickupUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<FilterUpgradeItem> FILTER_UPGRADE = ITEMS.register("filter_upgrade",
			() -> new FilterUpgradeItem(Config.COMMON.filterUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<FilterUpgradeItem> ADVANCED_FILTER_UPGRADE = ITEMS.register("advanced_filter_upgrade",
			() -> new FilterUpgradeItem(Config.COMMON.advancedFilterUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<MagnetUpgradeItem> MAGNET_UPGRADE = ITEMS.register("magnet_upgrade",
			() -> new MagnetUpgradeItem(Config.COMMON.magnetUpgrade.magnetRange::get, Config.COMMON.magnetUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<MagnetUpgradeItem> ADVANCED_MAGNET_UPGRADE = ITEMS.register("advanced_magnet_upgrade",
			() -> new MagnetUpgradeItem(Config.COMMON.advancedMagnetUpgrade.magnetRange::get, Config.COMMON.advancedMagnetUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<FeedingUpgradeItem> FEEDING_UPGRADE = ITEMS.register("feeding_upgrade",
			() -> new FeedingUpgradeItem(Config.COMMON.feedingUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<FeedingUpgradeItem> ADVANCED_FEEDING_UPGRADE = ITEMS.register("advanced_feeding_upgrade",
			() -> new FeedingUpgradeItem(Config.COMMON.advancedFeedingUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<CompactingUpgradeItem> COMPACTING_UPGRADE = ITEMS.register("compacting_upgrade",
			() -> new CompactingUpgradeItem(false, Config.COMMON.compactingUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<CompactingUpgradeItem> ADVANCED_COMPACTING_UPGRADE = ITEMS.register("advanced_compacting_upgrade",
			() -> new CompactingUpgradeItem(true, Config.COMMON.advancedCompactingUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<VoidUpgradeItem> VOID_UPGRADE = ITEMS.register("void_upgrade",
			() -> new VoidUpgradeItem(Config.COMMON.voidUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<VoidUpgradeItem> ADVANCED_VOID_UPGRADE = ITEMS.register("advanced_void_upgrade",
			() -> new VoidUpgradeItem(Config.COMMON.advancedVoidUpgrade.filterSlots::get, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<SmeltingUpgradeItem> SMELTING_UPGRADE = ITEMS.register("smelting_upgrade",
			() -> new SmeltingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.smeltingUpgrade));
	public static final RegistryObject<AutoSmeltingUpgradeItem> AUTO_SMELTING_UPGRADE = ITEMS.register("auto_smelting_upgrade",
			() -> new AutoSmeltingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.autoSmeltingUpgrade));
	public static final RegistryObject<SmokingUpgradeItem> SMOKING_UPGRADE = ITEMS.register("smoking_upgrade",
			() -> new SmokingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.smokingUpgrade));
	public static final RegistryObject<AutoSmokingUpgradeItem> AUTO_SMOKING_UPGRADE = ITEMS.register("auto_smoking_upgrade",
			() -> new AutoSmokingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.autoSmokingUpgrade));
	public static final RegistryObject<BlastingUpgradeItem> BLASTING_UPGRADE = ITEMS.register("blasting_upgrade",
			() -> new BlastingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.blastingUpgrade));
	public static final RegistryObject<AutoBlastingUpgradeItem> AUTO_BLASTING_UPGRADE = ITEMS.register("auto_blasting_upgrade",
			() -> new AutoBlastingUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.autoBlastingUpgrade));
	public static final RegistryObject<CraftingUpgradeItem> CRAFTING_UPGRADE = ITEMS.register("crafting_upgrade",
			() -> new CraftingUpgradeItem(SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<StonecutterUpgradeItem> STONECUTTER_UPGRADE = ITEMS.register("stonecutter_upgrade",
			() -> new StonecutterUpgradeItem(SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<StackUpgradeItem> STACK_UPGRADE_TIER_1 = ITEMS.register("stack_upgrade_tier_1", () ->
			new StackUpgradeItem(2, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<StackUpgradeItem> STACK_UPGRADE_TIER_2 = ITEMS.register("stack_upgrade_tier_2", () ->
			new StackUpgradeItem(4, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<StackUpgradeItem> STACK_UPGRADE_TIER_3 = ITEMS.register("stack_upgrade_tier_3", () ->
			new StackUpgradeItem(8, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<StackUpgradeItem> STACK_UPGRADE_TIER_4 = ITEMS.register("stack_upgrade_tier_4", () ->
			new StackUpgradeItem(16, SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<JukeboxUpgradeItem> JUKEBOX_UPGRADE = ITEMS.register("jukebox_upgrade",
			() -> new JukeboxUpgradeItem(SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<PumpUpgradeItem> PUMP_UPGRADE = ITEMS.register("pump_upgrade", () -> new PumpUpgradeItem(false, false, SophisticatedStorage.CREATIVE_TAB, Config.COMMON.pumpUpgrade));
	public static final RegistryObject<PumpUpgradeItem> ADVANCED_PUMP_UPGRADE = ITEMS.register("advanced_pump_upgrade", () -> new PumpUpgradeItem(true, true, SophisticatedStorage.CREATIVE_TAB, Config.COMMON.pumpUpgrade));
	public static final RegistryObject<XpPumpUpgradeItem> XP_PUMP_UPGRADE = ITEMS.register("xp_pump_upgrade", () -> new XpPumpUpgradeItem(SophisticatedStorage.CREATIVE_TAB, Config.COMMON.xpPumpUpgrade));
	public static final RegistryObject<StorageTierUpgradeItem> BASIC_TIER_UPGRADE = ITEMS.register("basic_tier_upgrade", () -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC, true));
	public static final RegistryObject<StorageTierUpgradeItem> BASIC_TO_IRON_TIER_UPGRADE = ITEMS.register("basic_to_iron_tier_upgrade", () -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON));
	public static final RegistryObject<StorageTierUpgradeItem> IRON_TO_GOLD_TIER_UPGRADE = ITEMS.register("iron_to_gold_tier_upgrade", () -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.IRON_TO_GOLD));
	public static final RegistryObject<StorageTierUpgradeItem> GOLD_TO_DIAMOND_TIER_UPGRADE = ITEMS.register("gold_to_diamond_tier_upgrade", () -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_DIAMOND));
	public static final RegistryObject<StorageTierUpgradeItem> DIAMOND_TO_NETHERITE_TIER_UPGRADE = ITEMS.register("diamond_to_netherite_tier_upgrade", () -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.DIAMOND_TO_NETHERITE));

	public static final RegistryObject<ItemBase> UPGRADE_BASE = ITEMS.register("upgrade_base", () -> new ItemBase(new Item.Properties().stacksTo(16), SophisticatedStorage.CREATIVE_TAB));

	public static final RegistryObject<ItemBase> PACKING_TAPE = ITEMS.register("packing_tape", ()-> new ItemBase(new Item.Properties().stacksTo(1).durability(4), SophisticatedStorage.CREATIVE_TAB));

	public static void registerHandlers(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(ModItems::registerContainers);
	}

	private static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_BASIC_TYPE = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
	private static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_ADVANCED_TYPE = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
	private static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_BASIC_TYPE = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
	private static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_ADVANCED_TYPE = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
	private static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_TYPE = new UpgradeContainerType<>(FeedingUpgradeContainer::new);
	private static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> ADVANCED_FEEDING_TYPE = new UpgradeContainerType<>(FeedingUpgradeContainer::new);
	private static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_TYPE = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
	private static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> ADVANCED_COMPACTING_TYPE = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
	private static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_TYPE = new UpgradeContainerType<>(VoidUpgradeContainer::new);
	private static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> ADVANCED_VOID_TYPE = new UpgradeContainerType<>(VoidUpgradeContainer::new);
	private static final UpgradeContainerType<CookingUpgradeWrapper.SmeltingUpgradeWrapper, CookingUpgradeContainer<SmeltingRecipe, CookingUpgradeWrapper.SmeltingUpgradeWrapper>> SMELTING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	private static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper, AutoCookingUpgradeContainer<SmeltingRecipe, AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper>> AUTO_SMELTING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	private static final UpgradeContainerType<CookingUpgradeWrapper.SmokingUpgradeWrapper, CookingUpgradeContainer<SmokingRecipe, CookingUpgradeWrapper.SmokingUpgradeWrapper>> SMOKING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	private static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper, AutoCookingUpgradeContainer<SmokingRecipe, AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper>> AUTO_SMOKING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	private static final UpgradeContainerType<CookingUpgradeWrapper.BlastingUpgradeWrapper, CookingUpgradeContainer<BlastingRecipe, CookingUpgradeWrapper.BlastingUpgradeWrapper>> BLASTING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	private static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper, AutoCookingUpgradeContainer<BlastingRecipe, AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper>> AUTO_BLASTING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	private static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> CRAFTING_TYPE = new UpgradeContainerType<>(CraftingUpgradeContainer::new);
	private static final UpgradeContainerType<StonecutterUpgradeWrapper, StonecutterUpgradeContainer> STONECUTTER_TYPE = new UpgradeContainerType<>(StonecutterUpgradeContainer::new);
	private static final UpgradeContainerType<JukeboxUpgradeItem.Wrapper, JukeboxUpgradeContainer> JUKEBOX_TYPE = new UpgradeContainerType<>(JukeboxUpgradeContainer::new);
	private static final UpgradeContainerType<TankUpgradeWrapper, TankUpgradeContainer> TANK_TYPE = new UpgradeContainerType<>(TankUpgradeContainer::new);
	private static final UpgradeContainerType<BatteryUpgradeWrapper, BatteryUpgradeContainer> BATTERY_TYPE = new UpgradeContainerType<>(BatteryUpgradeContainer::new);
	private static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_TYPE = new UpgradeContainerType<>(PumpUpgradeContainer::new);
	private static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> ADVANCED_PUMP_TYPE = new UpgradeContainerType<>(PumpUpgradeContainer::new);
	private static final UpgradeContainerType<XpPumpUpgradeWrapper, XpPumpUpgradeContainer> XP_PUMP_TYPE = new UpgradeContainerType<>(XpPumpUpgradeContainer::new);

	public static void registerContainers(RegisterEvent event) {
		if (!event.getRegistryKey().equals(ForgeRegistries.Keys.CONTAINER_TYPES)) {
			return;
		}

		UpgradeContainerRegistry.register(PICKUP_UPGRADE.getId(), PICKUP_BASIC_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_PICKUP_UPGRADE.getId(), PICKUP_ADVANCED_TYPE);
		UpgradeContainerRegistry.register(FILTER_UPGRADE.getId(), FilterUpgradeContainer.BASIC_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_FILTER_UPGRADE.getId(), FilterUpgradeContainer.ADVANCED_TYPE);
		UpgradeContainerRegistry.register(MAGNET_UPGRADE.getId(), MAGNET_BASIC_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_MAGNET_UPGRADE.getId(), MAGNET_ADVANCED_TYPE);
		UpgradeContainerRegistry.register(FEEDING_UPGRADE.getId(), FEEDING_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_FEEDING_UPGRADE.getId(), ADVANCED_FEEDING_TYPE);
		UpgradeContainerRegistry.register(COMPACTING_UPGRADE.getId(), COMPACTING_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_COMPACTING_UPGRADE.getId(), ADVANCED_COMPACTING_TYPE);
		UpgradeContainerRegistry.register(VOID_UPGRADE.getId(), VOID_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_VOID_UPGRADE.getId(), ADVANCED_VOID_TYPE);
		UpgradeContainerRegistry.register(SMELTING_UPGRADE.getId(), SMELTING_TYPE);
		UpgradeContainerRegistry.register(AUTO_SMELTING_UPGRADE.getId(), AUTO_SMELTING_TYPE);
		UpgradeContainerRegistry.register(SMOKING_UPGRADE.getId(), SMOKING_TYPE);
		UpgradeContainerRegistry.register(AUTO_SMOKING_UPGRADE.getId(), AUTO_SMOKING_TYPE);
		UpgradeContainerRegistry.register(BLASTING_UPGRADE.getId(), BLASTING_TYPE);
		UpgradeContainerRegistry.register(AUTO_BLASTING_UPGRADE.getId(), AUTO_BLASTING_TYPE);
		UpgradeContainerRegistry.register(CRAFTING_UPGRADE.getId(), CRAFTING_TYPE);
		UpgradeContainerRegistry.register(STONECUTTER_UPGRADE.getId(), STONECUTTER_TYPE);
		UpgradeContainerRegistry.register(JUKEBOX_UPGRADE.getId(), JUKEBOX_TYPE);
		UpgradeContainerRegistry.register(PUMP_UPGRADE.getId(), PUMP_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_PUMP_UPGRADE.getId(), ADVANCED_PUMP_TYPE);
		UpgradeContainerRegistry.register(XP_PUMP_UPGRADE.getId(), XP_PUMP_TYPE);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			UpgradeGuiManager.registerTab(FEEDING_TYPE, (FeedingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new FeedingUpgradeTab.Basic(uc, p, s, Config.COMMON.feedingUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(ADVANCED_FEEDING_TYPE, (FeedingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new FeedingUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedFeedingUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(PICKUP_BASIC_TYPE, (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
					new PickupUpgradeTab.Basic(uc, p, s, Config.COMMON.pickupUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(PICKUP_ADVANCED_TYPE, (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
					new PickupUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedPickupUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(FilterUpgradeContainer.BASIC_TYPE, (FilterUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new FilterUpgradeTab.Basic(uc, p, s, Config.COMMON.filterUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(FilterUpgradeContainer.ADVANCED_TYPE, (FilterUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new FilterUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedFilterUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(MAGNET_BASIC_TYPE, (MagnetUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new MagnetUpgradeTab.Basic(uc, p, s, Config.COMMON.magnetUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(MAGNET_ADVANCED_TYPE, (MagnetUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new MagnetUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedMagnetUpgrade.slotsInRow.get(), StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			UpgradeGuiManager.registerTab(COMPACTING_TYPE, (CompactingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new CompactingUpgradeTab.Basic(uc, p, s, Config.COMMON.compactingUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(ADVANCED_COMPACTING_TYPE, (CompactingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new CompactingUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedCompactingUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(VOID_TYPE, (VoidUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new VoidUpgradeTab.Basic(uc, p, s, Config.COMMON.voidUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(ADVANCED_VOID_TYPE, (VoidUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new VoidUpgradeTab.Advanced(uc, p, s, Config.COMMON.advancedVoidUpgrade.slotsInRow.get()));
			UpgradeGuiManager.registerTab(SMELTING_TYPE, CookingUpgradeTab.SmeltingUpgradeTab::new);
			UpgradeGuiManager.registerTab(AUTO_SMELTING_TYPE, (AutoCookingUpgradeContainer<SmeltingRecipe, AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
					new AutoCookingUpgradeTab.AutoSmeltingUpgradeTab(uc, p, s, Config.COMMON.autoSmeltingUpgrade.inputFilterSlotsInRow.get(), Config.COMMON.autoSmeltingUpgrade.fuelFilterSlotsInRow.get()));
			UpgradeGuiManager.registerTab(SMOKING_TYPE, CookingUpgradeTab.SmokingUpgradeTab::new);
			UpgradeGuiManager.registerTab(AUTO_SMOKING_TYPE, (AutoCookingUpgradeContainer<SmokingRecipe, AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
					new AutoCookingUpgradeTab.AutoSmokingUpgradeTab(uc, p, s, Config.COMMON.autoSmokingUpgrade.inputFilterSlotsInRow.get(), Config.COMMON.autoSmokingUpgrade.fuelFilterSlotsInRow.get()));
			UpgradeGuiManager.registerTab(BLASTING_TYPE, CookingUpgradeTab.BlastingUpgradeTab::new);
			UpgradeGuiManager.registerTab(AUTO_BLASTING_TYPE, (AutoCookingUpgradeContainer<BlastingRecipe, AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
					new AutoCookingUpgradeTab.AutoBlastingUpgradeTab(uc, p, s, Config.COMMON.autoBlastingUpgrade.inputFilterSlotsInRow.get(), Config.COMMON.autoBlastingUpgrade.fuelFilterSlotsInRow.get()));
			UpgradeGuiManager.registerTab(CRAFTING_TYPE, (CraftingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
					new CraftingUpgradeTab(uc, p, s, StorageButtonDefinitions.SHIFT_CLICK_TARGET));
			UpgradeGuiManager.registerTab(STONECUTTER_TYPE, (StonecutterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) ->
					new StonecutterUpgradeTab(upgradeContainer, position, screen, StorageButtonDefinitions.SHIFT_CLICK_TARGET));
			UpgradeGuiManager.registerTab(JUKEBOX_TYPE, JukeboxUpgradeTab::new);
			UpgradeGuiManager.registerTab(TANK_TYPE, TankUpgradeTab::new);
			UpgradeGuiManager.registerTab(BATTERY_TYPE, BatteryUpgradeTab::new);
			UpgradeGuiManager.registerInventoryPart(TANK_TYPE, TankInventoryPart::new);
			UpgradeGuiManager.registerInventoryPart(BATTERY_TYPE, BatteryInventoryPart::new);
			UpgradeGuiManager.registerTab(PUMP_TYPE, PumpUpgradeTab.Basic::new);
			UpgradeGuiManager.registerTab(ADVANCED_PUMP_TYPE, PumpUpgradeTab.Advanced::new);
			UpgradeGuiManager.registerTab(XP_PUMP_TYPE, (XpPumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) ->
					new XpPumpUpgradeTab(upgradeContainer, position, screen, Config.COMMON.xpPumpUpgrade.mendingOn.get()));
		});
	}
}
