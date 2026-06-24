package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
	private static String networkProtocolVersion;
	private final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") // needs to be public for mod to work
	public SophisticatedStorage(IEventBus modBus, Dist dist, ModContainer container) {
		networkProtocolVersion = container.getModInfo().getVersion().toString();
		container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		if (dist == Dist.CLIENT && !ModList.get().isLoaded("configured")) {
			container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
		Config.SERVER.initListeners(modBus);
		commonEventHandler.registerHandlers();
		ModCompat.register();
		if (dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers(modBus);
			modBus.addListener(DataGenerators::gatherData);
		}
		ModBlocks.registerHandlers(modBus);
		ModItems.registerHandlers(modBus);
		modBus.addListener(ModPayloads::registerPayloads);
		modBus.addListener(SophisticatedStorage::setup);
		ModParticles.registerParticles(modBus);
	}

	private static void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(ModBlocks::registerDispenseBehavior);
		event.enqueueWork(ModBlocks::registerCauldronInteractions);
	}

	public static Identifier getIdentifier(String regName) {
		return Identifier.parse(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}

	public static String getNetworkProtocolVersion() {
		return networkProtocolVersion;
	}
}
