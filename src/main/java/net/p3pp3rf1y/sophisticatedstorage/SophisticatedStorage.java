package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.data.DataGenerators;
import net.p3pp3rf1y.sophisticatedstorage.init.*;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedStorage.MOD_ID)
public class SophisticatedStorage {
	public static final String MOD_ID = "sophisticatedstorage";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static final CreativeModeTab CREATIVE_TAB = new SophisticatedStorageTab();

	private final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") //needs to be public for mod to work
	public SophisticatedStorage() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		Config.SERVER.initListeners(modBus);
		commonEventHandler.registerHandlers();
		ModCompat.initCompats();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers();
		}
		ModBlocks.registerHandlers(modBus);
		ModItems.registerHandlers(modBus);
		modBus.addListener(SophisticatedStorage::setup);
		modBus.addListener(DataGenerators::gatherData);
		modBus.addListener(CapabilityStorageWrapper::onRegister);
		ModParticles.registerParticles(modBus);
		modBus.addGenericListener(Block.class, ModLoot::registerLootFunction);
	}

	private static void setup(FMLCommonSetupEvent event) {
		StoragePacketHandler.INSTANCE.init();
		ModCompat.compatsSetup();
		event.enqueueWork(ModBlocks::registerDispenseBehavior);
		event.enqueueWork(ModBlocks::registerCauldronInteractions);
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
