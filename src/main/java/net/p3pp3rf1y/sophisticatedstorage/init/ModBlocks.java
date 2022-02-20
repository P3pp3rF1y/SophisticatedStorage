package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainer;

public class ModBlocks {
	private ModBlocks() {}

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REGISTRY_NAME = "barrel";

	public static final RegistryObject<BarrelBlock> BARREL = BLOCKS.register(BARREL_REGISTRY_NAME, () -> new BarrelBlock(Config.COMMON.woodBarrel.inventorySlotCount.get(), Config.COMMON.woodBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> IRON_BARREL = BLOCKS.register("iron_barrel", () -> new BarrelBlock(Config.COMMON.ironBarrel.inventorySlotCount.get(), Config.COMMON.ironBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> GOLD_BARREL = BLOCKS.register("gold_barrel", () -> new BarrelBlock(Config.COMMON.goldBarrel.inventorySlotCount.get(), Config.COMMON.goldBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> DIAMOND_BARREL = BLOCKS.register("diamond_barrel", () -> new BarrelBlock(Config.COMMON.diamondBarrel.inventorySlotCount.get(), Config.COMMON.diamondBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BarrelBlock> NETHERITE_BARREL = BLOCKS.register("netherite_barrel", () -> new BarrelBlock(Config.COMMON.netheriteBarrel.inventorySlotCount.get(), Config.COMMON.netheriteBarrel.upgradeSlotCount.get(),
			BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));
	public static final RegistryObject<BlockItem> BARREL_ITEM = ITEMS.register(BARREL_REGISTRY_NAME, () -> new BlockItemBase(BARREL.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<BlockItem> IRON_BARREL_ITEM = ITEMS.register("iron_barrel", () -> new BlockItemBase(IRON_BARREL.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<BlockItem> GOLD_BARREL_ITEM = ITEMS.register("gold_barrel", () -> new BlockItemBase(GOLD_BARREL.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<BlockItem> DIAMOND_BARREL_ITEM = ITEMS.register("diamond_barrel", () -> new BlockItemBase(DIAMOND_BARREL.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));
	public static final RegistryObject<BlockItem> NETHERITE_BARREL_ITEM = ITEMS.register("netherite_barrel", () -> new BlockItemBase(NETHERITE_BARREL.get(), new Item.Properties(), SophisticatedStorage.CREATIVE_TAB));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<StorageBlockEntity>> BARREL_TILE_TYPE = BLOCK_ENTITIES.register(BARREL_REGISTRY_NAME, () ->
			BlockEntityType.Builder.of(StorageBlockEntity::new, BARREL.get(), IRON_BARREL.get(), GOLD_BARREL.get(), DIAMOND_BARREL.get(), NETHERITE_BARREL.get())
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
	}

	private static void registerContainers(RegistryEvent.Register<MenuType<?>> evt) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			MenuScreens.register(STORAGE_CONTAINER_TYPE.get(), StorageScreen::constructScreen);
			MenuScreens.register(SETTINGS_CONTAINER_TYPE.get(), StorageSettingsScreen::constructScreen);
		});
	}
}
