package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.crafting.FlatTopBarrelToggleRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.ShulkerBoxFromChestRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.SmithingStorageUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageDyeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class ModBlocks {
	private static final String LIMITED_BARREL_NAME = "limited_barrel";

	private ModBlocks() {}

	public static final TagKey<Item> BASE_TIER_WOODEN_STORAGE_TAG = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), SophisticatedStorage.getRL("base_tier_wooden_storage"));
	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REG_NAME = "barrel";
	public static final RegistryObject<BarrelBlock> BARREL = BLOCKS.register(BARREL_REG_NAME, () -> new BarrelBlock(Config.SERVER.woodBarrel.inventorySlotCount::get, Config.SERVER.woodBarrel.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> IRON_BARREL = BLOCKS.register("iron_barrel", () -> new BarrelBlock(Config.SERVER.ironBarrel.inventorySlotCount::get, Config.SERVER.ironBarrel.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> GOLD_BARREL = BLOCKS.register("gold_barrel", () -> new BarrelBlock(Config.SERVER.goldBarrel.inventorySlotCount::get, Config.SERVER.goldBarrel.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> DIAMOND_BARREL = BLOCKS.register("diamond_barrel", () -> new BarrelBlock(Config.SERVER.diamondBarrel.inventorySlotCount::get, Config.SERVER.diamondBarrel.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> NETHERITE_BARREL = BLOCKS.register("netherite_barrel", () -> new BarrelBlock(Config.SERVER.netheriteBarrel.inventorySlotCount::get, Config.SERVER.netheriteBarrel.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> BARREL_ITEM = ITEMS.register(BARREL_REG_NAME, () -> new WoodStorageBlockItem(BARREL.get()));
	public static final RegistryObject<BlockItem> IRON_BARREL_ITEM = ITEMS.register("iron_barrel", () -> new WoodStorageBlockItem(IRON_BARREL.get()));
	public static final RegistryObject<BlockItem> GOLD_BARREL_ITEM = ITEMS.register("gold_barrel", () -> new WoodStorageBlockItem(GOLD_BARREL.get()));
	public static final RegistryObject<BlockItem> DIAMOND_BARREL_ITEM = ITEMS.register("diamond_barrel", () -> new WoodStorageBlockItem(DIAMOND_BARREL.get()));
	public static final RegistryObject<BlockItem> NETHERITE_BARREL_ITEM = ITEMS.register("netherite_barrel", () -> new WoodStorageBlockItem(NETHERITE_BARREL.get()));

	private static final String LIMITED_BARREL_REG_NAME = LIMITED_BARREL_NAME;
	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_1 = BLOCKS.register("limited_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.limitedBarrel1.baseSlotLimitMultiplier::get, Config.SERVER.limitedBarrel1.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_1 = BLOCKS.register("limited_iron_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.ironLimitedBarrel1.baseSlotLimitMultiplier::get, Config.SERVER.ironLimitedBarrel1.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_1 = BLOCKS.register("limited_gold_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.goldLimitedBarrel1.baseSlotLimitMultiplier::get, Config.SERVER.goldLimitedBarrel1.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_1 = BLOCKS.register("limited_diamond_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.diamondLimitedBarrel1.baseSlotLimitMultiplier::get, Config.SERVER.diamondLimitedBarrel1.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_1 = BLOCKS.register("limited_netherite_barrel_1", () -> new LimitedBarrelBlock(1, Config.SERVER.netheriteLimitedBarrel1.baseSlotLimitMultiplier::get, Config.SERVER.netheriteLimitedBarrel1.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_1_ITEM = ITEMS.register("limited_barrel_1", () -> new WoodStorageBlockItem(LIMITED_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_1_ITEM = ITEMS.register("limited_iron_barrel_1", () -> new WoodStorageBlockItem(LIMITED_IRON_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_1_ITEM = ITEMS.register("limited_gold_barrel_1", () -> new WoodStorageBlockItem(LIMITED_GOLD_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_1_ITEM = ITEMS.register("limited_diamond_barrel_1", () -> new WoodStorageBlockItem(LIMITED_DIAMOND_BARREL_1.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_1_ITEM = ITEMS.register("limited_netherite_barrel_1", () -> new WoodStorageBlockItem(LIMITED_NETHERITE_BARREL_1.get()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_2 = BLOCKS.register("limited_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.limitedBarrel2.baseSlotLimitMultiplier::get, Config.SERVER.limitedBarrel2.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_2 = BLOCKS.register("limited_iron_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.ironLimitedBarrel2.baseSlotLimitMultiplier::get, Config.SERVER.ironLimitedBarrel2.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_2 = BLOCKS.register("limited_gold_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.goldLimitedBarrel2.baseSlotLimitMultiplier::get, Config.SERVER.goldLimitedBarrel2.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_2 = BLOCKS.register("limited_diamond_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.diamondLimitedBarrel2.baseSlotLimitMultiplier::get, Config.SERVER.diamondLimitedBarrel2.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_2 = BLOCKS.register("limited_netherite_barrel_2", () -> new LimitedBarrelBlock(2, Config.SERVER.netheriteLimitedBarrel2.baseSlotLimitMultiplier::get, Config.SERVER.netheriteLimitedBarrel2.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_2_ITEM = ITEMS.register("limited_barrel_2", () -> new WoodStorageBlockItem(LIMITED_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_2_ITEM = ITEMS.register("limited_iron_barrel_2", () -> new WoodStorageBlockItem(LIMITED_IRON_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_2_ITEM = ITEMS.register("limited_gold_barrel_2", () -> new WoodStorageBlockItem(LIMITED_GOLD_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_2_ITEM = ITEMS.register("limited_diamond_barrel_2", () -> new WoodStorageBlockItem(LIMITED_DIAMOND_BARREL_2.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_2_ITEM = ITEMS.register("limited_netherite_barrel_2", () -> new WoodStorageBlockItem(LIMITED_NETHERITE_BARREL_2.get()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_3 = BLOCKS.register("limited_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.limitedBarrel3.baseSlotLimitMultiplier::get, Config.SERVER.limitedBarrel3.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_3 = BLOCKS.register("limited_iron_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.ironLimitedBarrel3.baseSlotLimitMultiplier::get, Config.SERVER.ironLimitedBarrel3.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_3 = BLOCKS.register("limited_gold_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.goldLimitedBarrel3.baseSlotLimitMultiplier::get, Config.SERVER.goldLimitedBarrel3.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_3 = BLOCKS.register("limited_diamond_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.diamondLimitedBarrel3.baseSlotLimitMultiplier::get, Config.SERVER.diamondLimitedBarrel3.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_3 = BLOCKS.register("limited_netherite_barrel_3", () -> new LimitedBarrelBlock(3, Config.SERVER.netheriteLimitedBarrel3.baseSlotLimitMultiplier::get, Config.SERVER.netheriteLimitedBarrel3.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_3_ITEM = ITEMS.register("limited_barrel_3", () -> new WoodStorageBlockItem(LIMITED_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_3_ITEM = ITEMS.register("limited_iron_barrel_3", () -> new WoodStorageBlockItem(LIMITED_IRON_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_3_ITEM = ITEMS.register("limited_gold_barrel_3", () -> new WoodStorageBlockItem(LIMITED_GOLD_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_3_ITEM = ITEMS.register("limited_diamond_barrel_3", () -> new WoodStorageBlockItem(LIMITED_DIAMOND_BARREL_3.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_3_ITEM = ITEMS.register("limited_netherite_barrel_3", () -> new WoodStorageBlockItem(LIMITED_NETHERITE_BARREL_3.get()));

	public static final RegistryObject<BarrelBlock> LIMITED_BARREL_4 = BLOCKS.register("limited_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.limitedBarrel4.baseSlotLimitMultiplier::get, Config.SERVER.limitedBarrel4.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_IRON_BARREL_4 = BLOCKS.register("limited_iron_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.ironLimitedBarrel4.baseSlotLimitMultiplier::get, Config.SERVER.ironLimitedBarrel4.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_GOLD_BARREL_4 = BLOCKS.register("limited_gold_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.goldLimitedBarrel4.baseSlotLimitMultiplier::get, Config.SERVER.goldLimitedBarrel4.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_DIAMOND_BARREL_4 = BLOCKS.register("limited_diamond_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.diamondLimitedBarrel4.baseSlotLimitMultiplier::get, Config.SERVER.diamondLimitedBarrel4.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> LIMITED_NETHERITE_BARREL_4 = BLOCKS.register("limited_netherite_barrel_4", () -> new LimitedBarrelBlock(4, Config.SERVER.netheriteLimitedBarrel4.baseSlotLimitMultiplier::get, Config.SERVER.netheriteLimitedBarrel4.upgradeSlotCount::get,
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> LIMITED_BARREL_4_ITEM = ITEMS.register("limited_barrel_4", () -> new WoodStorageBlockItem(LIMITED_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_IRON_BARREL_4_ITEM = ITEMS.register("limited_iron_barrel_4", () -> new WoodStorageBlockItem(LIMITED_IRON_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_GOLD_BARREL_4_ITEM = ITEMS.register("limited_gold_barrel_4", () -> new WoodStorageBlockItem(LIMITED_GOLD_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_DIAMOND_BARREL_4_ITEM = ITEMS.register("limited_diamond_barrel_4", () -> new WoodStorageBlockItem(LIMITED_DIAMOND_BARREL_4.get()));
	public static final RegistryObject<BlockItem> LIMITED_NETHERITE_BARREL_4_ITEM = ITEMS.register("limited_netherite_barrel_4", () -> new WoodStorageBlockItem(LIMITED_NETHERITE_BARREL_4.get()));

	private static final String CHEST_REG_NAME = "chest";
	public static final RegistryObject<ChestBlock> CHEST = BLOCKS.register(CHEST_REG_NAME, () -> new ChestBlock(Config.SERVER.woodChest.inventorySlotCount::get, Config.SERVER.woodChest.upgradeSlotCount::get));
	public static final RegistryObject<ChestBlock> IRON_CHEST = BLOCKS.register("iron_chest", () -> new ChestBlock(Config.SERVER.ironChest.inventorySlotCount::get, Config.SERVER.ironChest.upgradeSlotCount::get));
	public static final RegistryObject<ChestBlock> GOLD_CHEST = BLOCKS.register("gold_chest", () -> new ChestBlock(Config.SERVER.goldChest.inventorySlotCount::get, Config.SERVER.goldChest.upgradeSlotCount::get));
	public static final RegistryObject<ChestBlock> DIAMOND_CHEST = BLOCKS.register("diamond_chest", () -> new ChestBlock(Config.SERVER.diamondChest.inventorySlotCount::get, Config.SERVER.diamondChest.upgradeSlotCount::get));
	public static final RegistryObject<ChestBlock> NETHERITE_CHEST = BLOCKS.register("netherite_chest", () -> new ChestBlock(Config.SERVER.netheriteChest.inventorySlotCount::get, Config.SERVER.netheriteChest.upgradeSlotCount::get));
	public static final RegistryObject<BlockItem> CHEST_ITEM = ITEMS.register(CHEST_REG_NAME, () -> new ChestBlockItem(CHEST.get()));
	public static final RegistryObject<BlockItem> IRON_CHEST_ITEM = ITEMS.register("iron_chest", () -> new ChestBlockItem(IRON_CHEST.get()));
	public static final RegistryObject<BlockItem> GOLD_CHEST_ITEM = ITEMS.register("gold_chest", () -> new ChestBlockItem(GOLD_CHEST.get()));
	public static final RegistryObject<BlockItem> DIAMOND_CHEST_ITEM = ITEMS.register("diamond_chest", () -> new ChestBlockItem(DIAMOND_CHEST.get()));
	public static final RegistryObject<BlockItem> NETHERITE_CHEST_ITEM = ITEMS.register("netherite_chest", () -> new ChestBlockItem(NETHERITE_CHEST.get()));

	private static final String SHULKER_BOX_REG_NAME = "shulker_box";
	public static final RegistryObject<ShulkerBoxBlock> SHULKER_BOX = BLOCKS.register(SHULKER_BOX_REG_NAME, () -> new ShulkerBoxBlock(Config.SERVER.shulkerBox.inventorySlotCount::get, Config.SERVER.shulkerBox.upgradeSlotCount::get));
	public static final RegistryObject<ShulkerBoxBlock> IRON_SHULKER_BOX = BLOCKS.register("iron_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.ironShulkerBox.inventorySlotCount::get, Config.SERVER.ironShulkerBox.upgradeSlotCount::get));
	public static final RegistryObject<ShulkerBoxBlock> GOLD_SHULKER_BOX = BLOCKS.register("gold_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.goldShulkerBox.inventorySlotCount::get, Config.SERVER.goldShulkerBox.upgradeSlotCount::get));
	public static final RegistryObject<ShulkerBoxBlock> DIAMOND_SHULKER_BOX = BLOCKS.register("diamond_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.diamondShulkerBox.inventorySlotCount::get, Config.SERVER.diamondShulkerBox.upgradeSlotCount::get));
	public static final RegistryObject<ShulkerBoxBlock> NETHERITE_SHULKER_BOX = BLOCKS.register("netherite_shulker_box", () -> new ShulkerBoxBlock(Config.SERVER.netheriteShulkerBox.inventorySlotCount::get, Config.SERVER.netheriteShulkerBox.upgradeSlotCount::get));
	public static final RegistryObject<BlockItem> SHULKER_BOX_ITEM = ITEMS.register(SHULKER_BOX_REG_NAME, () -> new ShulkerBoxItem(SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> IRON_SHULKER_BOX_ITEM = ITEMS.register("iron_shulker_box", () -> new ShulkerBoxItem(IRON_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> GOLD_SHULKER_BOX_ITEM = ITEMS.register("gold_shulker_box", () -> new ShulkerBoxItem(GOLD_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> DIAMOND_SHULKER_BOX_ITEM = ITEMS.register("diamond_shulker_box", () -> new ShulkerBoxItem(DIAMOND_SHULKER_BOX.get()));
	public static final RegistryObject<BlockItem> NETHERITE_SHULKER_BOX_ITEM = ITEMS.register("netherite_shulker_box", () -> new ShulkerBoxItem(NETHERITE_SHULKER_BOX.get()));

	private static final String CONTROLLER_REG_NAME = "controller";
	public static final RegistryObject<ControllerBlock> CONTROLLER = BLOCKS.register(CONTROLLER_REG_NAME, ControllerBlock::new);
	private static final String STORAGE_LINK_REG_NAME = "storage_link";
	public static final RegistryObject<StorageLinkBlock> STORAGE_LINK = BLOCKS.register(STORAGE_LINK_REG_NAME, StorageLinkBlock::new);
	public static final RegistryObject<BlockItem> CONTROLLER_ITEM = ITEMS.register(CONTROLLER_REG_NAME, () -> new BlockItemBase(CONTROLLER.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<BlockItem> STORAGE_LINK_ITEM = ITEMS.register(STORAGE_LINK_REG_NAME, () -> new BlockItemBase(STORAGE_LINK.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(BARREL_REG_NAME, () ->
			BlockEntityType.Builder.of(BarrelBlockEntity::new, BARREL.get(), IRON_BARREL.get(), GOLD_BARREL.get(), DIAMOND_BARREL.get(), NETHERITE_BARREL.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<LimitedBarrelBlockEntity>> LIMITED_BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(LIMITED_BARREL_REG_NAME, () ->
			BlockEntityType.Builder.of(LimitedBarrelBlockEntity::new,
							LIMITED_BARREL_1.get(), LIMITED_IRON_BARREL_1.get(), LIMITED_GOLD_BARREL_1.get(), LIMITED_DIAMOND_BARREL_1.get(), LIMITED_NETHERITE_BARREL_1.get(),
							LIMITED_BARREL_2.get(), LIMITED_IRON_BARREL_2.get(), LIMITED_GOLD_BARREL_2.get(), LIMITED_DIAMOND_BARREL_2.get(), LIMITED_NETHERITE_BARREL_2.get(),
							LIMITED_BARREL_3.get(), LIMITED_IRON_BARREL_3.get(), LIMITED_GOLD_BARREL_3.get(), LIMITED_DIAMOND_BARREL_3.get(), LIMITED_NETHERITE_BARREL_3.get(),
							LIMITED_BARREL_4.get(), LIMITED_IRON_BARREL_4.get(), LIMITED_GOLD_BARREL_4.get(), LIMITED_DIAMOND_BARREL_4.get(), LIMITED_NETHERITE_BARREL_4.get()
					)
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ChestBlockEntity>> CHEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(CHEST_REG_NAME, () ->
			BlockEntityType.Builder.of(ChestBlockEntity::new, CHEST.get(), IRON_CHEST.get(), GOLD_CHEST.get(), DIAMOND_CHEST.get(), NETHERITE_CHEST.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ShulkerBoxBlockEntity>> SHULKER_BOX_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(SHULKER_BOX_REG_NAME, () ->
			BlockEntityType.Builder.of(ShulkerBoxBlockEntity::new, SHULKER_BOX.get(), IRON_SHULKER_BOX.get(), GOLD_SHULKER_BOX.get(), DIAMOND_SHULKER_BOX.get(), NETHERITE_SHULKER_BOX.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ControllerBlockEntity>> CONTROLLER_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(CONTROLLER_REG_NAME, () ->
			BlockEntityType.Builder.of(ControllerBlockEntity::new, CONTROLLER.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageLinkBlockEntity>> STORAGE_LINK_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(STORAGE_LINK_REG_NAME, () ->
			BlockEntityType.Builder.of(StorageLinkBlockEntity::new, STORAGE_LINK.get())
					.build(null));

	public static final RegistryObject<MenuType<StorageContainerMenu>> STORAGE_CONTAINER_TYPE = CONTAINERS.register("storage",
			() -> IForgeMenuType.create(StorageContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<LimitedBarrelContainerMenu>> LIMITED_BARREL_CONTAINER_TYPE = CONTAINERS.register(LIMITED_BARREL_NAME,
			() -> IForgeMenuType.create(LimitedBarrelContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<StorageSettingsContainerMenu>> SETTINGS_CONTAINER_TYPE = CONTAINERS.register("settings",
			() -> IForgeMenuType.create(StorageSettingsContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<LimitedBarrelSettingsContainerMenu>> LIMITED_BARREL_SETTINGS_CONTAINER_TYPE = CONTAINERS.register("limited_barrel_settings",
			() -> IForgeMenuType.create(LimitedBarrelSettingsContainerMenu::fromBuffer));

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SophisticatedStorage.MOD_ID);
	public static final RegistryObject<SimpleRecipeSerializer<?>> FLAT_TOP_BARREL_TOGGLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("flat_top_barrel_toggle", () -> new SimpleRecipeSerializer<>(FlatTopBarrelToggleRecipe::new));

	public static void registerHandlers(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		CONTAINERS.register(modBus);
		RECIPE_SERIALIZERS.register(modBus);
		modBus.addGenericListener(MenuType.class, ModBlocks::registerContainers);
		modBus.addGenericListener(RecipeSerializer.class, ModBlocks::registerRecipeSerializers);
		MinecraftForge.EVENT_BUS.addListener(ModBlocks::onResourceReload);
	}

	public static void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> evt) {
		evt.getRegistry().register(StorageDyeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "storage_dye"));
		evt.getRegistry().register(StorageTierUpgradeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "storage_tier_upgrade"));
		evt.getRegistry().register(SmithingStorageUpgradeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "smithing_storage_upgrade"));
		evt.getRegistry().register(ShulkerBoxFromChestRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "shulker_box_from_chest"));
	}

	private static void onResourceReload(AddReloadListenerEvent event) {
		ShulkerBoxFromChestRecipe.REGISTERED_RECIPES.clear();
	}

	private static void registerContainers(RegistryEvent.Register<MenuType<?>> evt) {
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
}
