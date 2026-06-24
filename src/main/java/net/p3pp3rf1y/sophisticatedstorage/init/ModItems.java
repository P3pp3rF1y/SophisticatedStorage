package net.p3pp3rf1y.sophisticatedstorage.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.infinity.InfinityUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DropPackedDisabledCondition;
import net.p3pp3rf1y.sophisticatedstorage.data.CopyStorageDataFunction;
import net.p3pp3rf1y.sophisticatedstorage.item.*;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeContainer;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeWrapper;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ModItems {
	private ModItems() {
	}

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SophisticatedStorage.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB.identifier(),
			SophisticatedStorage.MOD_ID);
	public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTION_TYPES = DeferredRegister
			.create(Registries.LOOT_FUNCTION_TYPE.identifier(), SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,
			SophisticatedStorage.MOD_ID);
	public static final Identifier STORAGE_UPGRADE_TAG_NAME = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "upgrade");

	public static final TagKey<Item> STORAGE_UPGRADE_TAG = TagKey.create(Registries.ITEM, STORAGE_UPGRADE_TAG_NAME);

	public static final DeferredHolder<Item, PickupUpgradeItem> PICKUP_UPGRADE = ITEMS.registerItem("pickup_upgrade",
			properties -> new PickupUpgradeItem(Config.SERVER.pickupUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, PickupUpgradeItem> ADVANCED_PICKUP_UPGRADE = ITEMS.registerItem("advanced_pickup_upgrade",
			properties -> new PickupUpgradeItem(Config.SERVER.advancedPickupUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, FilterUpgradeItem> FILTER_UPGRADE = ITEMS.registerItem("filter_upgrade",
			properties -> new FilterUpgradeItem(Config.SERVER.filterUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, FilterUpgradeItem> ADVANCED_FILTER_UPGRADE = ITEMS.registerItem("advanced_filter_upgrade",
			properties -> new FilterUpgradeItem(Config.SERVER.advancedFilterUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, MagnetUpgradeItem> MAGNET_UPGRADE = ITEMS.registerItem("magnet_upgrade",
			properties -> new MagnetUpgradeItem(Config.SERVER.magnetUpgrade.magnetRange::get, Config.SERVER.magnetUpgrade.filterSlots::get,
					Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, MagnetUpgradeItem> ADVANCED_MAGNET_UPGRADE = ITEMS.registerItem("advanced_magnet_upgrade",
			properties -> new MagnetUpgradeItem(Config.SERVER.advancedMagnetUpgrade.magnetRange::get, Config.SERVER.advancedMagnetUpgrade.filterSlots::get,
					Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, FeedingUpgradeItem> FEEDING_UPGRADE = ITEMS.registerItem("feeding_upgrade",
			properties -> new FeedingUpgradeItem(Config.SERVER.feedingUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, FeedingUpgradeItem> ADVANCED_FEEDING_UPGRADE = ITEMS.registerItem("advanced_feeding_upgrade",
			properties -> new FeedingUpgradeItem(Config.SERVER.advancedFeedingUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, CompactingUpgradeItem> COMPACTING_UPGRADE = ITEMS.registerItem("compacting_upgrade",
			properties -> new StorageCompactingUpgradeItem(false, Config.SERVER.compactingUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage,
					properties, Config.SERVER.compactingUpgrade::getCompactingResult));
	public static final DeferredHolder<Item, CompactingUpgradeItem> ADVANCED_COMPACTING_UPGRADE = ITEMS.registerItem("advanced_compacting_upgrade",
			properties -> new StorageCompactingUpgradeItem(true, Config.SERVER.advancedCompactingUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage,
					properties, Config.SERVER.compactingUpgrade::getCompactingResult));
	public static final DeferredHolder<Item, VoidUpgradeItem> VOID_UPGRADE = ITEMS.registerItem("void_upgrade",
			properties -> new VoidUpgradeItem(Config.SERVER.voidUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, VoidUpgradeItem> ADVANCED_VOID_UPGRADE = ITEMS.registerItem("advanced_void_upgrade",
			properties -> new VoidUpgradeItem(Config.SERVER.advancedVoidUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, SmeltingUpgradeItem> SMELTING_UPGRADE = ITEMS.registerItem("smelting_upgrade",
			properties -> new SmeltingUpgradeItem(Config.SERVER.smeltingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, AutoSmeltingUpgradeItem> AUTO_SMELTING_UPGRADE = ITEMS.registerItem("auto_smelting_upgrade",
			properties -> new AutoSmeltingUpgradeItem(Config.SERVER.autoSmeltingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, SmokingUpgradeItem> SMOKING_UPGRADE = ITEMS.registerItem("smoking_upgrade",
			properties -> new SmokingUpgradeItem(Config.SERVER.smokingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, AutoSmokingUpgradeItem> AUTO_SMOKING_UPGRADE = ITEMS.registerItem("auto_smoking_upgrade",
			properties -> new AutoSmokingUpgradeItem(Config.SERVER.autoSmokingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, BlastingUpgradeItem> BLASTING_UPGRADE = ITEMS.registerItem("blasting_upgrade",
			properties -> new BlastingUpgradeItem(Config.SERVER.blastingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, AutoBlastingUpgradeItem> AUTO_BLASTING_UPGRADE = ITEMS.registerItem("auto_blasting_upgrade",
			properties -> new AutoBlastingUpgradeItem(Config.SERVER.autoBlastingUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, CraftingUpgradeItem> CRAFTING_UPGRADE = ITEMS.registerItem("crafting_upgrade",
			properties -> new CraftingUpgradeItem(Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StonecutterUpgradeItem> STONECUTTER_UPGRADE = ITEMS.registerItem("stonecutter_upgrade",
			properties -> new StonecutterUpgradeItem(Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_1 = ITEMS.registerItem("stack_upgrade_tier_1",
			properties -> new StackUpgradeItem(2, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_1_PLUS = ITEMS.registerItem("stack_upgrade_tier_1_plus",
			properties -> new StackUpgradeItem(3, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_2 = ITEMS.registerItem("stack_upgrade_tier_2",
			properties -> new StackUpgradeItem(4, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_3 = ITEMS.registerItem("stack_upgrade_tier_3",
			properties -> new StackUpgradeItem(8, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_4 = ITEMS.registerItem("stack_upgrade_tier_4",
			properties -> new StackUpgradeItem(16, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_TIER_5 = ITEMS.registerItem("stack_upgrade_tier_5",
			properties -> new StackUpgradeItem(32, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_DOWNGRADE_TIER_1 = ITEMS.registerItem("stack_downgrade_tier_1",
			properties -> new StackUpgradeItem(0.125, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_DOWNGRADE_TIER_2 = ITEMS.registerItem("stack_downgrade_tier_2",
			properties -> new StackUpgradeItem(0.0625, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_DOWNGRADE_TIER_3 = ITEMS.registerItem("stack_downgrade_tier_3",
			properties -> new StackUpgradeItem(0.03125, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, StackUpgradeItem> STACK_UPGRADE_OMEGA_TIER = ITEMS.registerItem("stack_upgrade_omega_tier",
			properties -> new StackUpgradeItem(Integer.MAX_VALUE, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, JukeboxUpgradeItem> JUKEBOX_UPGRADE = ITEMS.registerItem("jukebox_upgrade",
			properties -> new JukeboxUpgradeItem(Config.SERVER.maxUpgradesPerStorage, () -> 1, () -> 1, properties));
	public static final DeferredHolder<Item, JukeboxUpgradeItem> ADVANCED_JUKEBOX_UPGRADE = ITEMS.registerItem("advanced_jukebox_upgrade",
			properties -> new JukeboxUpgradeItem(Config.SERVER.maxUpgradesPerStorage, Config.SERVER.advancedJukeboxUpgrade.numberOfSlots,
					Config.SERVER.advancedJukeboxUpgrade.slotsInRow, properties));
	public static final DeferredHolder<Item, PumpUpgradeItem> PUMP_UPGRADE = ITEMS.registerItem("pump_upgrade",
			properties -> new PumpUpgradeItem(false, false, true, Config.SERVER.pumpUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, PumpUpgradeItem> ADVANCED_PUMP_UPGRADE = ITEMS.registerItem("advanced_pump_upgrade",
			properties -> new PumpUpgradeItem(true, false, true, Config.SERVER.pumpUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, XpPumpUpgradeItem> XP_PUMP_UPGRADE = ITEMS.registerItem("xp_pump_upgrade",
			properties -> new XpPumpUpgradeItem(Config.SERVER.xpPumpUpgrade, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, CompressionUpgradeItem> COMPRESSION_UPGRADE = ITEMS.registerItem("compression_upgrade",
			CompressionUpgradeItem::new);
	public static final DeferredHolder<Item, HopperUpgradeItem> HOPPER_UPGRADE = ITEMS.registerItem("hopper_upgrade",
			properties -> new HopperUpgradeItem(Config.SERVER.hopperUpgrade.inputFilterSlots::get, Config.SERVER.hopperUpgrade.outputFilterSlots::get,
					Config.SERVER.hopperUpgrade.transferSpeedTicks::get, Config.SERVER.hopperUpgrade.maxTransferStackSize::get, properties));
	public static final DeferredHolder<Item, HopperUpgradeItem> ADVANCED_HOPPER_UPGRADE = ITEMS.registerItem("advanced_hopper_upgrade",
			properties -> new HopperUpgradeItem(Config.SERVER.advancedHopperUpgrade.inputFilterSlots::get,
					Config.SERVER.advancedHopperUpgrade.outputFilterSlots::get, Config.SERVER.advancedHopperUpgrade.transferSpeedTicks::get,
					Config.SERVER.advancedHopperUpgrade.maxTransferStackSize::get, properties));
	public static final DeferredHolder<Item, InfinityUpgradeItem> INFINITY_UPGRADE = ITEMS.registerItem("infinity_upgrade",
			properties -> new InfinityUpgradeItem(Config.SERVER.maxUpgradesPerStorage, true, properties));
	public static final DeferredHolder<Item, InfinityUpgradeItem> SURVIVAL_INFINITY_UPGRADE = ITEMS.registerItem("survival_infinity_upgrade",
			properties -> new InfinityUpgradeItem(Config.SERVER.maxUpgradesPerStorage, false, properties));
	public static final DeferredHolder<Item, AlchemyUpgradeItem> ALCHEMY_UPGRADE = ITEMS.registerItem("alchemy_upgrade",
			properties -> new AlchemyUpgradeItem(Config.SERVER.alchemyUpgrade.filterSlots, Config.SERVER.maxUpgradesPerStorage, properties));
	public static final DeferredHolder<Item, AlchemyUpgradeItem> ADVANCED_ALCHEMY_UPGRADE = ITEMS.registerItem("advanced_alchemy_upgrade",
			properties -> new AlchemyUpgradeItem(Config.SERVER.advancedAlchemyUpgrade.filterSlots, Config.SERVER.maxUpgradesPerStorage, properties));

	public static final Supplier<StorageTierUpgradeItem> BASIC_TIER_UPGRADE = ITEMS.registerItem("basic_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC, true, properties));
	public static final Supplier<StorageTierUpgradeItem> BASIC_TO_COPPER_TIER_UPGRADE = ITEMS.registerItem("basic_to_copper_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_COPPER, properties));
	public static final Supplier<StorageTierUpgradeItem> BASIC_TO_IRON_TIER_UPGRADE = ITEMS.registerItem("basic_to_iron_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_IRON, properties));
	public static final Supplier<StorageTierUpgradeItem> BASIC_TO_GOLD_TIER_UPGRADE = ITEMS.registerItem("basic_to_gold_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_GOLD, properties));
	public static final Supplier<StorageTierUpgradeItem> BASIC_TO_DIAMOND_TIER_UPGRADE = ITEMS.registerItem("basic_to_diamond_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_DIAMOND, properties));
	public static final Supplier<StorageTierUpgradeItem> BASIC_TO_NETHERITE_TIER_UPGRADE = ITEMS.registerItem("basic_to_netherite_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.BASIC_TO_NETHERITE, properties));
	public static final Supplier<StorageTierUpgradeItem> COPPER_TO_IRON_TIER_UPGRADE = ITEMS.registerItem("copper_to_iron_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_IRON, properties));
	public static final Supplier<StorageTierUpgradeItem> COPPER_TO_GOLD_TIER_UPGRADE = ITEMS.registerItem("copper_to_gold_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_GOLD, properties));
	public static final Supplier<StorageTierUpgradeItem> COPPER_TO_DIAMOND_TIER_UPGRADE = ITEMS.registerItem("copper_to_diamond_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_DIAMOND, properties));
	public static final Supplier<StorageTierUpgradeItem> COPPER_TO_NETHERITE_TIER_UPGRADE = ITEMS.registerItem("copper_to_netherite_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.COPPER_TO_NETHERITE, properties));
	public static final Supplier<StorageTierUpgradeItem> IRON_TO_GOLD_TIER_UPGRADE = ITEMS.registerItem("iron_to_gold_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.IRON_TO_GOLD, properties));
	public static final Supplier<StorageTierUpgradeItem> IRON_TO_DIAMOND_TIER_UPGRADE = ITEMS.registerItem("iron_to_diamond_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.IRON_TO_DIAMOND, properties));
	public static final Supplier<StorageTierUpgradeItem> IRON_TO_NETHERITE_TIER_UPGRADE = ITEMS.registerItem("iron_to_netherite_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.IRON_TO_NETHERITE, properties));
	public static final Supplier<StorageTierUpgradeItem> GOLD_TO_DIAMOND_TIER_UPGRADE = ITEMS.registerItem("gold_to_diamond_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_DIAMOND, properties));
	public static final Supplier<StorageTierUpgradeItem> GOLD_TO_NETHERITE_TIER_UPGRADE = ITEMS.registerItem("gold_to_netherite_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.GOLD_TO_NETHERITE, properties));
	public static final Supplier<StorageTierUpgradeItem> DIAMOND_TO_NETHERITE_TIER_UPGRADE = ITEMS.registerItem("diamond_to_netherite_tier_upgrade",
			properties -> new StorageTierUpgradeItem(StorageTierUpgradeItem.TierUpgrade.DIAMOND_TO_NETHERITE, properties));

	public static final Supplier<ItemBase> UPGRADE_BASE = ITEMS.registerItem("upgrade_base", properties -> new ItemBase(properties.stacksTo(16)));

	public static final Supplier<ItemBase> PACKING_TAPE = ITEMS.registerItem("packing_tape", properties -> new PackingTapeItem(properties, 8, false));
	public static final Supplier<ItemBase> SUPER_PACKING_TAPE = ITEMS.registerItem("super_packing_tape",
			properties -> new PackingTapeItem(properties, 32, true));
	public static final Supplier<ItemBase> STORAGE_TOOL = ITEMS.registerItem("storage_tool", StorageToolItem::new);
	public static final Supplier<ItemBase> DEBUG_TOOL = ITEMS.registerItem("debug_tool", properties -> new ItemBase(properties.stacksTo(1)));
	public static final Supplier<ItemBase> PAINTBRUSH = ITEMS.registerItem("paintbrush", PaintbrushItem::new);
	public static final Supplier<Item> INACCESSIBLE_SLOT = ITEMS.registerItem("inaccessible_slot", properties -> new Item(properties.stacksTo(1)));
	public static final Supplier<MapCodec<? extends LootItemFunction>> COPY_STORAGE_DATA = LOOT_FUNCTION_TYPES.register("copy_storage_data",
			() -> CopyStorageDataFunction.CODEC);

	private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
			SophisticatedStorage.MOD_ID);

	public static Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main",
			() -> CreativeModeTab.builder().icon(() -> WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.GOLD_BARREL_ITEM.get()), WoodType.SPRUCE))
					.title(Component.translatable("itemGroup.sophisticatedstorage")).displayItems((featureFlags, output) -> {
						ITEMS.getEntries().stream().filter(i -> i.get() instanceof ItemBase)
								.forEach(i -> ((ItemBase) i.get()).addCreativeTabItems(output::accept));
						ModBlocks.ITEMS.getEntries().stream().filter(i -> i.get() instanceof BlockItemBase)
								.forEach(i -> ((BlockItemBase) i.get()).addCreativeTabItems(output::accept));
					}).build());

	public static void registerHandlers(IEventBus modBus) {
		ITEMS.register(modBus);
		CREATIVE_MODE_TABS.register(modBus);
		LOOT_FUNCTION_TYPES.register(modBus);
		ATTACHMENT_TYPES.register(modBus);
		CONDITION_CODECS.register(modBus);
		ModDataComponents.register(modBus);
		modBus.addListener(ModItems::registerContainers);
		if (FMLEnvironment.getDist().isClient()) {
			ModItemsClient.init(modBus);
		}
	}

	public static final Supplier<MapCodec<DropPackedDisabledCondition>> DROP_PACKED_DISABLED_CONDITION = CONDITION_CODECS.register("drop_packed_disabled",
			() -> DropPackedDisabledCondition.CODEC);

	public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_BASIC_TYPE = new UpgradeContainerType<>(
			ContentsFilteredUpgradeContainer::new);
	public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_ADVANCED_TYPE = new UpgradeContainerType<>(
			ContentsFilteredUpgradeContainer::new);
	public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_BASIC_TYPE = new UpgradeContainerType<>(
			MagnetUpgradeContainer::new);
	public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_ADVANCED_TYPE = new UpgradeContainerType<>(
			MagnetUpgradeContainer::new);
	public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_TYPE = new UpgradeContainerType<>(
			FeedingUpgradeContainer::new);
	public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> ADVANCED_FEEDING_TYPE = new UpgradeContainerType<>(
			FeedingUpgradeContainer::new);
	public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_TYPE = new UpgradeContainerType<>(
			CompactingUpgradeContainer::new);
	public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> ADVANCED_COMPACTING_TYPE = new UpgradeContainerType<>(
			CompactingUpgradeContainer::new);
	public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_TYPE = new UpgradeContainerType<>(VoidUpgradeContainer::new);
	public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> ADVANCED_VOID_TYPE = new UpgradeContainerType<>(
			VoidUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper.SmeltingUpgradeWrapper, CookingUpgradeContainer<SmeltingRecipe, CookingUpgradeWrapper.SmeltingUpgradeWrapper>> SMELTING_TYPE = new UpgradeContainerType<>(
			CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper, AutoCookingUpgradeContainer<SmeltingRecipe, AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper>> AUTO_SMELTING_TYPE = new UpgradeContainerType<>(
			AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper.SmokingUpgradeWrapper, CookingUpgradeContainer<SmokingRecipe, CookingUpgradeWrapper.SmokingUpgradeWrapper>> SMOKING_TYPE = new UpgradeContainerType<>(
			CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper, AutoCookingUpgradeContainer<SmokingRecipe, AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper>> AUTO_SMOKING_TYPE = new UpgradeContainerType<>(
			AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper.BlastingUpgradeWrapper, CookingUpgradeContainer<BlastingRecipe, CookingUpgradeWrapper.BlastingUpgradeWrapper>> BLASTING_TYPE = new UpgradeContainerType<>(
			CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper, AutoCookingUpgradeContainer<BlastingRecipe, AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper>> AUTO_BLASTING_TYPE = new UpgradeContainerType<>(
			AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> CRAFTING_TYPE = new UpgradeContainerType<>(
			CraftingUpgradeContainer::new);
	public static final UpgradeContainerType<StonecutterUpgradeItem.Wrapper, StonecutterUpgradeContainer> STONECUTTER_TYPE = new UpgradeContainerType<>(
			StonecutterUpgradeContainer::new);
	public static final UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> JUKEBOX_TYPE = new UpgradeContainerType<>(
			JukeboxUpgradeContainer::new);
	public static final UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> ADVANCED_JUKEBOX_TYPE = new UpgradeContainerType<>(
			JukeboxUpgradeContainer::new);
	public static final UpgradeContainerType<TankUpgradeWrapper, TankUpgradeContainer> TANK_TYPE = new UpgradeContainerType<>(TankUpgradeContainer::new);
	public static final UpgradeContainerType<BatteryUpgradeWrapper, BatteryUpgradeContainer> BATTERY_TYPE = new UpgradeContainerType<>(
			BatteryUpgradeContainer::new);
	public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_TYPE = new UpgradeContainerType<>(PumpUpgradeContainer::new);
	public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> ADVANCED_PUMP_TYPE = new UpgradeContainerType<>(
			PumpUpgradeContainer::new);
	public static final UpgradeContainerType<XpPumpUpgradeWrapper, XpPumpUpgradeContainer> XP_PUMP_TYPE = new UpgradeContainerType<>(
			XpPumpUpgradeContainer::new);
	public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> HOPPER_TYPE = new UpgradeContainerType<>(
			HopperUpgradeContainer::new);
	public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> ADVANCED_HOPPER_TYPE = new UpgradeContainerType<>(
			HopperUpgradeContainer::new);
	public static final UpgradeContainerType<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> ALCHEMY_TYPE = new UpgradeContainerType<>(
			AlchemyUpgradeContainer::new);
	public static final UpgradeContainerType<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> ADVANCED_ALCHEMY_TYPE = new UpgradeContainerType<>(
			AlchemyUpgradeContainer::new);

	public static void registerContainers(RegisterEvent event) {
		if (!event.getRegistryKey().equals(Registries.MENU)) {
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
		UpgradeContainerRegistry.register(ADVANCED_JUKEBOX_UPGRADE.getId(), ADVANCED_JUKEBOX_TYPE);
		UpgradeContainerRegistry.register(PUMP_UPGRADE.getId(), PUMP_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_PUMP_UPGRADE.getId(), ADVANCED_PUMP_TYPE);
		UpgradeContainerRegistry.register(XP_PUMP_UPGRADE.getId(), XP_PUMP_TYPE);
		UpgradeContainerRegistry.register(HOPPER_UPGRADE.getId(), HOPPER_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_HOPPER_UPGRADE.getId(), ADVANCED_HOPPER_TYPE);
		UpgradeContainerRegistry.register(ALCHEMY_UPGRADE.getId(), ALCHEMY_TYPE);
		UpgradeContainerRegistry.register(ADVANCED_ALCHEMY_UPGRADE.getId(), ADVANCED_ALCHEMY_TYPE);
	}

	private static class StorageCompactingUpgradeItem extends CompactingUpgradeItem {
		public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(new UpgradeConflictDefinition(
				CompressionUpgradeItem.class::isInstance, 0, StorageTranslationHelper.INSTANCE.translError("add.compression_exists")));

		public StorageCompactingUpgradeItem(boolean shouldCompactThreeByThree, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeCountLimitConfig,
				Properties properties, ConfiguredCompactingResultProvider configuredCompactingResultProvider) {
			super(shouldCompactThreeByThree, filterSlotCount, upgradeCountLimitConfig, properties, configuredCompactingResultProvider);
		}

		@Override
		public List<UpgradeConflictDefinition> getUpgradeConflicts() {
			return UPGRADE_CONFLICT_DEFINITIONS;
		}
	}
}
