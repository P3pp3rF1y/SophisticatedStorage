package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;

@Mod(SophisticatedStorage.MOD_ID)
public class SophisticatedStorage {
	public static final String MOD_ID = "sophisticatedstorage";
	public static final StoragePacketHandler PACKET_HANDLER = new StoragePacketHandler(MOD_ID);
	public static final CreativeModeTab CREATIVE_TAB = new SophisticatedStorageTab();

	private final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") //needs to be public for mod to work
	public SophisticatedStorage() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(Config.COMMON::onConfigReload);
		commonEventHandler.registerHandlers();
		ModBlocks.registerHandlers(modBus);
		ModItems.registerHandlers(modBus);
		modBus.addListener(SophisticatedStorage::setup);
	}

	private static void setup(FMLCommonSetupEvent event) {
		PACKET_HANDLER.init();
	}
}
