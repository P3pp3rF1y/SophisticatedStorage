package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainer;
import net.p3pp3rf1y.sophisticatedstorage.crafting.SmithingStorageUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageDyeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class ModBlocks {
	private ModBlocks() {}

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REG_NAME = "barrel";
	public static final RegistryObject<BarrelBlock> BARREL = BLOCKS.register(BARREL_REG_NAME, () -> new BarrelBlock(Config.COMMON.woodBarrel.inventorySlotCount.get(), Config.COMMON.woodBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> IRON_BARREL = BLOCKS.register("iron_barrel", () -> new BarrelBlock(Config.COMMON.ironBarrel.inventorySlotCount.get(), Config.COMMON.ironBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> GOLD_BARREL = BLOCKS.register("gold_barrel", () -> new BarrelBlock(Config.COMMON.goldBarrel.inventorySlotCount.get(), Config.COMMON.goldBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> DIAMOND_BARREL = BLOCKS.register("diamond_barrel", () -> new BarrelBlock(Config.COMMON.diamondBarrel.inventorySlotCount.get(), Config.COMMON.diamondBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> NETHERITE_BARREL = BLOCKS.register("netherite_barrel", () -> new BarrelBlock(Config.COMMON.netheriteBarrel.inventorySlotCount.get(), Config.COMMON.netheriteBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> BARREL_ITEM = ITEMS.register(BARREL_REG_NAME, () -> new WoodStorageBlockItem(BARREL.get()));
	public static final RegistryObject<BlockItem> IRON_BARREL_ITEM = ITEMS.register("iron_barrel", () -> new WoodStorageBlockItem(IRON_BARREL.get()));
	public static final RegistryObject<BlockItem> GOLD_BARREL_ITEM = ITEMS.register("gold_barrel", () -> new WoodStorageBlockItem(GOLD_BARREL.get()));
	public static final RegistryObject<BlockItem> DIAMOND_BARREL_ITEM = ITEMS.register("diamond_barrel", () -> new WoodStorageBlockItem(DIAMOND_BARREL.get()));
	public static final RegistryObject<BlockItem> NETHERITE_BARREL_ITEM = ITEMS.register("netherite_barrel", () -> new WoodStorageBlockItem(NETHERITE_BARREL.get()));

	private static final String CHEST_REG_NAME = "chest";
	public static final RegistryObject<ChestBlock> CHEST = BLOCKS.register(CHEST_REG_NAME, () -> new ChestBlock(Config.COMMON.woodChest.inventorySlotCount.get(), Config.COMMON.woodChest.upgradeSlotCount.get()));
	public static final RegistryObject<ChestBlock> IRON_CHEST = BLOCKS.register("iron_chest", () -> new ChestBlock(Config.COMMON.ironChest.inventorySlotCount.get(), Config.COMMON.ironChest.upgradeSlotCount.get()));
	public static final RegistryObject<ChestBlock> GOLD_CHEST = BLOCKS.register("gold_chest", () -> new ChestBlock(Config.COMMON.goldChest.inventorySlotCount.get(), Config.COMMON.goldChest.upgradeSlotCount.get()));
	public static final RegistryObject<ChestBlock> DIAMOND_CHEST = BLOCKS.register("diamond_chest", () -> new ChestBlock(Config.COMMON.diamondChest.inventorySlotCount.get(), Config.COMMON.diamondChest.upgradeSlotCount.get()));
	public static final RegistryObject<ChestBlock> NETHERITE_CHEST = BLOCKS.register("netherite_chest", () -> new ChestBlock(Config.COMMON.netheriteChest.inventorySlotCount.get(), Config.COMMON.netheriteChest.upgradeSlotCount.get()));
	public static final RegistryObject<BlockItem> CHEST_ITEM = ITEMS.register(CHEST_REG_NAME, () -> new ChestBlockItem(CHEST.get()));
	public static final RegistryObject<BlockItem> IRON_CHEST_ITEM = ITEMS.register("iron_chest", () -> new ChestBlockItem(IRON_CHEST.get()));
	public static final RegistryObject<BlockItem> GOLD_CHEST_ITEM = ITEMS.register("gold_chest", () -> new ChestBlockItem(GOLD_CHEST.get()));
	public static final RegistryObject<BlockItem> DIAMOND_CHEST_ITEM = ITEMS.register("diamond_chest", () -> new ChestBlockItem(DIAMOND_CHEST.get()));
	public static final RegistryObject<BlockItem> NETHERITE_CHEST_ITEM = ITEMS.register("netherite_chest", () -> new ChestBlockItem(NETHERITE_CHEST.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(BARREL_REG_NAME, () ->
			BlockEntityType.Builder.of(BarrelBlockEntity::new, BARREL.get(), IRON_BARREL.get(), GOLD_BARREL.get(), DIAMOND_BARREL.get(), NETHERITE_BARREL.get())
					.build(null));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<ChestBlockEntity>> CHEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register(CHEST_REG_NAME, () ->
			BlockEntityType.Builder.of(ChestBlockEntity::new, CHEST.get(), IRON_CHEST.get(), GOLD_CHEST.get(), DIAMOND_CHEST.get(), NETHERITE_CHEST.get())
					.build(null));

	public static final RegistryObject<MenuType<StorageContainerMenu>> STORAGE_CONTAINER_TYPE = CONTAINERS.register("storage",
			() -> IForgeMenuType.create(StorageContainerMenu::fromBuffer));

	public static final RegistryObject<MenuType<StorageSettingsContainer>> SETTINGS_CONTAINER_TYPE = CONTAINERS.register("settings",
			() -> IForgeMenuType.create(StorageSettingsContainer::fromBuffer));

	public static void registerHandlers(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		CONTAINERS.register(modBus);
		modBus.addGenericListener(MenuType.class, ModBlocks::registerContainers);
		modBus.addGenericListener(RecipeSerializer.class, ModBlocks::registerRecipeSerializers);
	}

	public static void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> evt) {
		evt.getRegistry().register(StorageDyeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "storage_dye"));
		evt.getRegistry().register(StorageTierUpgradeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "storage_tier_upgrade"));
		evt.getRegistry().register(SmithingStorageUpgradeRecipe.SERIALIZER.setRegistryName(SophisticatedStorage.MOD_ID, "smithing_storage_upgrade"));
	}

	private static void registerContainers(RegistryEvent.Register<MenuType<?>> evt) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			MenuScreens.register(STORAGE_CONTAINER_TYPE.get(), StorageScreen::constructScreen);
			MenuScreens.register(SETTINGS_CONTAINER_TYPE.get(), StorageSettingsScreen::constructScreen);
		});
	}
}
