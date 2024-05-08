package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class ModBlocks {
	private static final String LIMITED_BARREL_NAME = "limited_barrel";

	private ModBlocks() {}

	public static final TagKey<Item> BASE_TIER_WOODEN_STORAGE_TAG = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), SophisticatedStorage.getRL("base_tier_wooden_storage"));
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SophisticatedStorage.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REG_NAME = "barrel";
	public static final RegistryObject<BarrelBlock> BARREL = BLOCKS.register(BARREL_REG_NAME, () -> new BarrelBlock(Config.SERVER.woodBarrel.inventorySlotCount, Config.SERVER.woodBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> COPPER_BARREL = BLOCKS.register("copper_barrel", () -> new BarrelBlock(Config.SERVER.copperBarrel.inventorySlotCount, Config.SERVER.copperBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> IRON_BARREL = BLOCKS.register("iron_barrel", () -> new BarrelBlock(Config.SERVER.ironBarrel.inventorySlotCount, Config.SERVER.ironBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> GOLD_BARREL = BLOCKS.register("gold_barrel", () -> new BarrelBlock(Config.SERVER.goldBarrel.inventorySlotCount, Config.SERVER.goldBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> DIAMOND_BARREL = BLOCKS.register("diamond_barrel", () -> new BarrelBlock(Config.SERVER.diamondBarrel.inventorySlotCount, Config.SERVER.diamondBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> NETHERITE_BARREL = BLOCKS.register("netherite_barrel", () -> new BarrelBlock(Config.SERVER.netheriteBarrel.inventorySlotCount, Config.SERVER.netheriteBarrel.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> BARREL_ITEM = ITEMS.register(BARREL_REG_NAME, () -> new BarrelBlockItem(BARREL.get()));
	public static final RegistryObject<BlockItem> COPPER_BARREL_ITEM = ITEMS.register("copper_barrel", () -> new BarrelBlockItem(COPPER_BARREL.get()));
	public static final RegistryObject<BlockItem> IRON_BARREL_ITEM = ITEMS.register("iron_barrel", () -> new BarrelBlockItem(IRON_BARREL.get()));
	public static final RegistryObject<BlockItem> GOLD_BARREL_ITEM = ITEMS.register("gold_barrel", () -> new BarrelBlockItem(GOLD_BARREL.get()));
	public static final RegistryObject<BlockItem> DIAMOND_BARREL_ITEM = ITEMS.register("diamond_barrel", () -> new BarrelBlockItem(DIAMOND_BARREL.get()));
	public static final RegistryObject<BlockItem> NETHERITE_BARREL_ITEM = ITEMS.register("netherite_barrel", () -> new BarrelBlockItem(NETHERITE_BARREL.get(), new Properties().fireResistant()));

	private static final String LIMITED_BARREL_REG_NAME = LIMITED_BARREL_NAME;
	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_1 = BLOCKS.register("limited_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.limitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_COPPER_BARREL_1 = BLOCKS.register("limited_copper_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.copperLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_1 = BLOCKS.register("limited_iron_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.ironLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_1 = BLOCKS.register("limited_gold_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.goldLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_1 = BLOCKS.register("limited_diamond_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.diamondLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_1 = BLOCKS.register("limited_netherite_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.netheriteLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel1.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_1_ITEM = ITEMS.register("limited_barrel_1", () -> new BarrelBlockItem(LIMITED_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_1_ITEM = ITEMS.register("limited_iron_barrel_1", () -> new BarrelBlockItem(LIMITED_IRON_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_COPPER_BARREL_1_ITEM = ITEMS.register("limited_copper_barrel_1", () -> new BarrelBlockItem(LIMITED_COPPER_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_1_ITEM = ITEMS.register("limited_gold_barrel_1", () -> new BarrelBlockItem(LIMITED_GOLD_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_1_ITEM = ITEMS.register("limited_diamond_barrel_1", () -> new BarrelBlockItem(LIMITED_DIAMOND_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_1_ITEM = ITEMS.register("limited_netherite_barrel_1", () -> new BarrelBlockItem(LIMITED_NETHERITE_BARREL_1.get(), new Properties().fireResistant()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_2 = BLOCKS.register("limited_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.limitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_COPPER_BARREL_2 = BLOCKS.register("limited_copper_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.copperLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_2 = BLOCKS.register("limited_iron_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.ironLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_2 = BLOCKS.register("limited_gold_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.goldLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_2 = BLOCKS.register("limited_diamond_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.diamondLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_2 = BLOCKS.register("limited_netherite_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.netheriteLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel2.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_2_ITEM = ITEMS.register("limited_barrel_2", () -> new BarrelBlockItem(LIMITED_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_COPPER_BARREL_2_ITEM = ITEMS.register("limited_copper_barrel_2", () -> new BarrelBlockItem(LIMITED_COPPER_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_2_ITEM = ITEMS.register("limited_iron_barrel_2", () -> new BarrelBlockItem(LIMITED_IRON_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_2_ITEM = ITEMS.register("limited_gold_barrel_2", () -> new BarrelBlockItem(LIMITED_GOLD_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_2_ITEM = ITEMS.register("limited_diamond_barrel_2", () -> new BarrelBlockItem(LIMITED_DIAMOND_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_2_ITEM = ITEMS.register("limited_netherite_barrel_2", () -> new BarrelBlockItem(LIMITED_NETHERITE_BARREL_2.get(), new Properties().fireResistant()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_3 = BLOCKS.register("limited_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.limitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_COPPER_BARREL_3 = BLOCKS.register("limited_copper_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.copperLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_3 = BLOCKS.register("limited_iron_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.ironLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_3 = BLOCKS.register("limited_gold_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.goldLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_3 = BLOCKS.register("limited_diamond_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.diamondLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_3 = BLOCKS.register("limited_netherite_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.netheriteLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel3.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_3_ITEM = ITEMS.register("limited_barrel_3", () -> new BarrelBlockItem(LIMITED_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_COPPER_BARREL_3_ITEM = ITEMS.register("limited_copper_barrel_3", () -> new BarrelBlockItem(LIMITED_COPPER_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_3_ITEM = ITEMS.register("limited_iron_barrel_3", () -> new BarrelBlockItem(LIMITED_IRON_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_3_ITEM = ITEMS.register("limited_gold_barrel_3", () -> new BarrelBlockItem(LIMITED_GOLD_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_3_ITEM = ITEMS.register("limited_diamond_barrel_3", () -> new BarrelBlockItem(LIMITED_DIAMOND_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_3_ITEM = ITEMS.register("limited_netherite_barrel_3", () -> new BarrelBlockItem(LIMITED_NETHERITE_BARREL_3.get(), new Properties().fireResistant()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_4 = BLOCKS.register("limited_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.limitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_COPPER_BARREL_4 = BLOCKS.register("limited_copper_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.copperLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_4 = BLOCKS.register("limited_iron_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.ironLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_4 = BLOCKS.register("limited_gold_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.goldLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_4 = BLOCKS.register("limited_diamond_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.diamondLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_4 = BLOCKS.register("limited_netherite_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.netheriteLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel4.upgradeSlotCount,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_4_ITEM = ITEMS.register("limited_barrel_4", () -> new BarrelBlockItem(LIMITED_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_COPPER_BARREL_4_ITEM = ITEMS.register("limited_copper_barrel_4", () -> new BarrelBlockItem(LIMITED_COPPER_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_4_ITEM = ITEMS.register("limited_iron_barrel_4", () -> new BarrelBlockItem(LIMITED_IRON_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_4_ITEM = ITEMS.register("limited_gold_barrel_4", () -> new BarrelBlockItem(LIMITED_GOLD_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_4_ITEM = ITEMS.register("limited_diamond_barrel_4", () -> new BarrelBlockItem(LIMITED_DIAMOND_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_4_ITEM = ITEMS.register("limited_netherite_barrel_4", () -> new BarrelBlockItem(LIMITED_NETHERITE_BARREL_4.get(), new Properties().fireResistant()));

	private static final String CHEST_REG_NAME = "chest";
	public static final RegistryObject<ChestBlock> CHEST = BLOCKS.register(CHEST_REG_NAME, () -> new ChestBlock(Config.SERVER.woodChest.inventorySlotCount, Config.SERVER.woodChest.upgradeSlotCount));
    public static final RegistryObject<ChestBlock> COPPER_CHEST = BLOCKS.register("copper_chest", () -> new ChestBlock(Config.SERVER.copperChest.inventorySlotCount, Config.SERVER.copperChest.upgradeSlotCount));
	public static final RegistryObject<ChestBlock> IRON_CHEST = BLOCKS.register("iron_chest", () -> new ChestBlock(Config.SERVER.ironChest.inventorySlotCount, Config.SERVER.ironChest.upgradeSlotCount));
	public static final RegistryObject<ChestBlock> GOLD_CHEST = BLOCKS.register("gold_chest", () -> new ChestBlock(Config.SERVER.goldChest.inventorySlotCount, Config.SERVER.goldChest.upgradeSlotCount));
	public static final RegistryObject<ChestBlock> DIAMOND_CHEST = BLOCKS.register("diamond_chest", () -> new ChestBlock(Config.SERVER.diamondChest.inventorySlotCount, Config.SERVER.diamondChest.upgradeSlotCount));
	public static final RegistryObject<ChestBlock> NETHERITE_CHEST = BLOCKS.register("netherite_chest", () -> new ChestBlock(Config.SERVER.netheriteChest.inventorySlotCount, Config.SERVER.netheriteChest.upgradeSlotCount, 1200));
	public static final RegistryObject<BlockItem> CHEST_ITEM = ITEMS.register(CHEST_REG_NAME, () -> new ChestBlockItem(CHEST.get()));
	public static final RegistryObject<BlockItem> COPPER_CHEST_ITEM = ITEMS.register("copper_chest", () -> new ChestBlockItem(COPPER_CHEST.get()));
	public static final RegistryObject<BlockItem> IRON_CHEST_ITEM = ITEMS.register("iron_chest", () -> new ChestBlockItem(IRON_CHEST.get()));
	public static final RegistryObject<BlockItem> GOLD_CHEST_ITEM = ITEMS.register("gold_chest", () -> new ChestBlockItem(GOLD_CHEST.get()));
	public static final RegistryObject<BlockItem> DIAMOND_CHEST_ITEM = ITEMS.register("diamond_chest", () -> new ChestBlockItem(DIAMOND_CHEST.get()));
	public static final RegistryObject<BlockItem> NETHERITE_CHEST_ITEM = ITEMS.register("netherite_chest", () -> new ChestBlockItem(NETHERITE_CHEST.get(), new Properties().fireResistant()));

	private static final String SHULKER_BOX_REG_NAME = "shulker_box";
	public static final RegistryObject<ShulkerBoxBlock> SHULKER_BOX = BLOCKS.register(SHULKER_BOX_REG_NAME, () -> new ShulkerBoxBlock(Config.SERVER.shulkerBox.inventorySlotCount, Config.SERVER.shulkerBox.upgradeSlotCount));
    public static final RegistryObject<ShulkerBoxBlock> COPPER_SHULKER_BOX = BLOCKS.register("copper_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.copperShulkerBox.inventorySlotCount, Config.SERVER.copperShulkerBox.upgradeSlotCount));
	public static final RegistryObject<ShulkerBoxBlock> IRON_SHULKER_BOX = BLOCKS.register("iron_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.ironShulkerBox.inventorySlotCount, Config.SERVER.ironShulkerBox.upgradeSlotCount));
	public static final RegistryObject<ShulkerBoxBlock> GOLD_SHULKER_BOX = BLOCKS.register("gold_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.goldShulkerBox.inventorySlotCount, Config.SERVER.goldShulkerBox.upgradeSlotCount));
	public static final RegistryObject<ShulkerBoxBlock> DIAMOND_SHULKER_BOX = BLOCKS.register("diamond_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.diamondShulkerBox.inventorySlotCount, Config.SERVER.diamondShulkerBox.upgradeSlotCount));
	public static final RegistryObject<ShulkerBoxBlock> NETHERITE_SHULKER_BOX = BLOCKS.register("netherite_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.netheriteShulkerBox.inventorySlotCount, Config.SERVER.netheriteShulkerBox.upgradeSlotCount, 1200));
	public static final RegistryObject<BlockItem> SHULKER_BOX_ITEM = ITEMS.register(SHULKER_BOX_REG_NAME, () -> new ShulkerBoxItem(SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> COPPER_SHULKER_BOX_ITEM = ITEMS.register("copper_shulker_box", () -> new ShulkerBoxItem(COPPER_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> IRON_SHULKER_BOX_ITEM = ITEMS.register("iron_shulker_box", () -> new ShulkerBoxItem(IRON_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> GOLD_SHULKER_BOX_ITEM = ITEMS.register("gold_shulker_box", () -> new ShulkerBoxItem(GOLD_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> DIAMOND_SHULKER_BOX_ITEM = ITEMS.register("diamond_shulker_box", () -> new ShulkerBoxItem(DIAMOND_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> NETHERITE_SHULKER_BOX_ITEM = ITEMS.register("netherite_shulker_box", () -> new ShulkerBoxItem(NETHERITE_SHULKER_BOX.get(), new Properties().stacksTo(1).fireResistant()));

	private static final String CONTROLLER_REG_NAME = "controller";
	public static final RegistryObject<ControllerBlock> CONTROLLER = BLOCKS.register(CONTROLLER_REG_NAME, ControllerBlock::new);
	private static final String STORAGE_LINK_REG_NAME = "storage_link";
	public static final RegistryObject<StorageLinkBlock> STORAGE_LINK = BLOCKS.register(STORAGE_LINK_REG_NAME, StorageLinkBlock::new);
	public static final RegistryObject<BlockItem> CONTROLLER_ITEM = ITEMS.register(CONTROLLER_REG_NAME, () -> new BlockItemBase(CONTROLLER.get(), new Item.Properties()));
	public static final RegistryObject<BlockItem> STORAGE_LINK_ITEM = ITEMS.register(STORAGE_LINK_REG_NAME, () -> new BlockItemBase(STORAGE_LINK.get(), new Item.Properties()));
	public static final String STORAGE_IO_REG_NAME = "storage_io";
	public static final RegistryObject<StorageIOBlock> STORAGE_IO = BLOCKS.register(STORAGE_IO_REG_NAME, StorageIOBlock::new);
	public static final String STORAGE_INPUT_REG_NAME = "storage_input";
	public static final RegistryObject<StorageIOBlock> STORAGE_INPUT = BLOCKS.register(STORAGE_INPUT_REG_NAME, () -> new StorageIOBlock() {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new StorageInputBlockEntity(pos, state);
		}
	});
	public static final String STORAGE_OUTPUT_REG_NAME = "storage_output";
	public static final RegistryObject<StorageIOBlock> STORAGE_OUTPUT = BLOCKS.register(STORAGE_OUTPUT_REG_NAME, () -> new StorageIOBlock() {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new StorageOutputBlockEntity(pos, state);
		}
	});

	public static final RegistryObject<BlockItem> STORAGE_IO_ITEM = ITEMS.register(STORAGE_IO_REG_NAME, () -> new BlockItemBase(STORAGE_IO.get(), new Item.Properties()));
	public static final RegistryObject<BlockItem> STORAGE_INPUT_ITEM = ITEMS.register(STORAGE_INPUT_REG_NAME, () -> new BlockItemBase(STORAGE_INPUT.get(), new Item.Properties()));
	public static final RegistryObject<BlockItem> STORAGE_OUTPUT_ITEM = ITEMS.register(STORAGE_OUTPUT_REG_NAME, () -> new BlockItemBase(STORAGE_OUTPUT.get(), new Item.Properties()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(BARREL_REG_NAME, () ->
			BlockEntityType.Builder.of(BarrelBlockEntity::new, BARREL.get(), COPPER_BARREL.get(), IRON_BARREL.get(), GOLD_BARREL.get(), DIAMOND_BARREL.get(), NETHERITE_BARREL.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<LimitedBarrelBlockEntity>> LIMITED_BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(LIMITED_BARREL_REG_NAME, () ->
			BlockEntityType.Builder.of(LimitedBarrelBlockEntity::new,
							LIMITED_BARREL_1.get(), LIMITED_COPPER_BARREL_1.get(), LIMITED_IRON_BARREL_1.get(), LIMITED_GOLD_BARREL_1.get(), LIMITED_DIAMOND_BARREL_1.get(), LIMITED_NETHERITE_BARREL_1.get(),
							LIMITED_BARREL_2.get(), LIMITED_COPPER_BARREL_2.get(), LIMITED_IRON_BARREL_2.get(), LIMITED_GOLD_BARREL_2.get(), LIMITED_DIAMOND_BARREL_2.get(), LIMITED_NETHERITE_BARREL_2.get(),
							LIMITED_BARREL_3.get(), LIMITED_COPPER_BARREL_3.get(), LIMITED_IRON_BARREL_3.get(), LIMITED_GOLD_BARREL_3.get(), LIMITED_DIAMOND_BARREL_3.get(), LIMITED_NETHERITE_BARREL_3.get(),
							LIMITED_BARREL_4.get(), LIMITED_COPPER_BARREL_4.get(), LIMITED_IRON_BARREL_4.get(), LIMITED_GOLD_BARREL_4.get(), LIMITED_DIAMOND_BARREL_4.get(), LIMITED_NETHERITE_BARREL_4.get()
					)
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ChestBlockEntity>> CHEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(CHEST_REG_NAME, () ->
			BlockEntityType.Builder.of(ChestBlockEntity::new, CHEST.get(), COPPER_CHEST.get(), IRON_CHEST.get(), GOLD_CHEST.get(), DIAMOND_CHEST.get(), NETHERITE_CHEST.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ShulkerBoxBlockEntity>> SHULKER_BOX_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(SHULKER_BOX_REG_NAME, () ->
			BlockEntityType.Builder.of(ShulkerBoxBlockEntity::new, SHULKER_BOX.get(), COPPER_SHULKER_BOX.get(), IRON_SHULKER_BOX.get(), GOLD_SHULKER_BOX.get(), DIAMOND_SHULKER_BOX.get(), NETHERITE_SHULKER_BOX.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ControllerBlockEntity>> CONTROLLER_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(CONTROLLER_REG_NAME, () ->
			BlockEntityType.Builder.of(ControllerBlockEntity::new, CONTROLLER.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageLinkBlockEntity>> STORAGE_LINK_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_LINK_REG_NAME, () ->
			BlockEntityType.Builder.of(StorageLinkBlockEntity::new, STORAGE_LINK.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageIOBlockEntity>> STORAGE_IO_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_IO_REG_NAME, () ->
			BlockEntityType.Builder.of(StorageIOBlockEntity::new, STORAGE_IO.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageInputBlockEntity>> STORAGE_INPUT_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_INPUT_REG_NAME, () ->
			BlockEntityType.Builder.of(StorageInputBlockEntity::new, STORAGE_INPUT.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageOutputBlockEntity>> STORAGE_OUTPUT_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_OUTPUT_REG_NAME, () ->
			BlockEntityType.Builder.of(StorageOutputBlockEntity::new, STORAGE_OUTPUT.get())
					.build(null));

	public static final RegistryObject<MenuType<StorageContainerMenu>> STORAGE_CONTAINER_TYPE = MENU_TYPES.register("storage",
			() -> IForgeMenuType.create(StorageContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<LimitedBarrelContainerMenu>> LIMITED_BARREL_CONTAINER_TYPE = MENU_TYPES.register(LIMITED_BARREL_NAME,
			() -> IForgeMenuType.create(LimitedBarrelContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<StorageSettingsContainerMenu>> SETTINGS_CONTAINER_TYPE = MENU_TYPES.register("settings",
			() -> IForgeMenuType.create(StorageSettingsContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<LimitedBarrelSettingsContainerMenu>> LIMITED_BARREL_SETTINGS_CONTAINER_TYPE = MENU_TYPES.register("limited_barrel_settings",
			() -> IForgeMenuType.create(LimitedBarrelSettingsContainerMenu::fromBuffer));

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SophisticatedStorage.MOD_ID);
	public static final RegistryObject<SimpleCraftingRecipeSerializer<?>> STORAGE_DYE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_dye", () -> new SimpleCraftingRecipeSerializer<>(StorageDyeRecipe::new));
	public static final RegistryObject<RecipeSerializer<?>> STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_tier_upgrade", StorageTierUpgradeRecipe.Serializer::new);
	public static final RegistryObject<RecipeSerializer<?>> STORAGE_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_tier_upgrade_shapeless", StorageTierUpgradeShapelessRecipe.Serializer::new);
	public static final RegistryObject<RecipeSerializer<?>> SHULKER_BOX_FROM_CHEST_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("shulker_box_from_chest", ShulkerBoxFromChestRecipe.Serializer::new);
	public static final RegistryObject<SimpleCraftingRecipeSerializer<?>> FLAT_TOP_BARREL_TOGGLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("flat_top_barrel_toggle", () -> new SimpleCraftingRecipeSerializer<>(FlatTopBarrelToggleRecipe::new));
	public static final RegistryObject<SimpleCraftingRecipeSerializer<?>> BARREL_MATERIAL_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("barrel_material", () -> new SimpleCraftingRecipeSerializer<>(BarrelMaterialRecipe::new));

	public static void registerHandlers(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITY_TYPES.register(modBus);
		MENU_TYPES.register(modBus);
		RECIPE_SERIALIZERS.register(modBus);
		modBus.addListener(ModBlocks::registerContainers);
		modBus.addListener(ModBlocks::registerRecipeSerializers);
		MinecraftForge.EVENT_BUS.addListener(ModBlocks::onResourceReload);
	}

	private static void onResourceReload(AddReloadListenerEvent event) {
		ShulkerBoxFromChestRecipe.REGISTERED_RECIPES.clear();
	}

	private static void registerRecipeSerializers(RegisterEvent event) {
		if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
			CraftingHelper.register(new ResourceLocation(SophisticatedStorage.MOD_ID, "base_tier_wooden_storage"), BaseTierWoodenStorageIngredient.Serializer.INSTANCE);
		}
	}

	private static void registerContainers(FMLClientSetupEvent evt) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			MenuScreens.register(STORAGE_CONTAINER_TYPE.get(), StorageScreen::constructScreen);
			MenuScreens.register(SETTINGS_CONTAINER_TYPE.get(), StorageSettingsScreen::constructScreen);
			MenuScreens.register(LIMITED_BARREL_CONTAINER_TYPE.get(), LimitedBarrelScreen::new);
			MenuScreens.register(LIMITED_BARREL_SETTINGS_CONTAINER_TYPE.get(), LimitedBarrelSettingsScreen::new);
		});
	}

	public static void registerDispenseBehavior() {
		DispenserBlock.registerBehavior(SHULKER_BOX_ITEM.get(), new ShulkerBoxDispenseBehavior());
	}

	public static void registerCauldronInteractions() {
		CauldronInteraction.WATER.put(BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(COPPER_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(IRON_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(GOLD_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(DIAMOND_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(NETHERITE_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.put(LIMITED_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_COPPER_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_COPPER_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_COPPER_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_COPPER_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_IRON_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_IRON_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_IRON_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_IRON_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_GOLD_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_GOLD_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_GOLD_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_GOLD_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_DIAMOND_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_DIAMOND_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_DIAMOND_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_DIAMOND_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_NETHERITE_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_NETHERITE_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_NETHERITE_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(LIMITED_NETHERITE_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.put(CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(COPPER_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(IRON_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(GOLD_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(DIAMOND_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(NETHERITE_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.put(SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(COPPER_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(IRON_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(GOLD_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(DIAMOND_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.put(NETHERITE_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class BarrelCauldronInteraction extends WoodStorageCauldronInteraction {
		private static final BarrelCauldronInteraction INSTANCE = new BarrelCauldronInteraction();

		@Override
		protected void removePaint(ItemStack stack) {
			super.removePaint(stack);
			BarrelBlockItem.removeMaterials(stack);
		}
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class WoodStorageCauldronInteraction extends StorageCauldronInteraction {
		private static final WoodStorageCauldronInteraction INSTANCE = new WoodStorageCauldronInteraction();
		@Override
		protected void removePaint(ItemStack stack) {
			super.removePaint(stack);
			if (WoodStorageBlockItem.getWoodType(stack).isEmpty()) {
				WoodStorageBlockItem.setWoodType(stack, WoodType.ACACIA);
			}
		}

		@Override
		protected boolean canRemovePaint(ItemStack stack) {
			return super.canRemovePaint(stack) && !WoodStorageBlockItem.isPacked(stack);
		}
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class StorageCauldronInteraction implements CauldronInteraction {
		private static final StorageCauldronInteraction INSTANCE = new StorageCauldronInteraction();

		@Override
		public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
			if (canRemovePaint(stack)) {

				if (!level.isClientSide()) {
					removePaint(stack);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			return InteractionResult.PASS;
		}

		protected boolean canRemovePaint(ItemStack stack) {
			return stack.getItem() instanceof ITintableBlockItem;
		}

		protected void removePaint(ItemStack stack) {
			if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.removeMainColor(stack);
				tintableBlockItem.removeAccentColor(stack);
			}
		}
	}
}
