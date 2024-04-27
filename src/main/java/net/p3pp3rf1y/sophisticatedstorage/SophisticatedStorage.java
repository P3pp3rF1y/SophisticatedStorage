package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedstorage.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.data.DataGenerators;
import net.p3pp3rf1y.sophisticatedstorage.init.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedStorage.MOD_ID)
public class SophisticatedStorage {
	public static final String MOD_ID = "sophisticatedstorage";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	private final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") //needs to be public for mod to work
	public SophisticatedStorage(IEventBus modBus) {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		Config.SERVER.initListeners(modBus);
		commonEventHandler.registerHandlers();
		ModCompat.register();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers(modBus);
		}
		ModBlocks.registerHandlers(modBus);
		ModItems.registerHandlers(modBus);
		modBus.addListener(ModPackets::registerPackets);
		modBus.addListener(SophisticatedStorage::setup);
		modBus.addListener(DataGenerators::gatherData);
		ModParticles.registerParticles(modBus);
	}

	private static void setup(FMLCommonSetupEvent event) {
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
