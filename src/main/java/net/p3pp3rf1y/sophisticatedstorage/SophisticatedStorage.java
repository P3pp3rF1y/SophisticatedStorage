package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

@Mod(SophisticatedStorage.MOD_ID)
public class SophisticatedStorage {
	public static final String MOD_ID = "sophisticatedstorage";

	public SophisticatedStorage() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModBlocks.registerHandlers(modBus);
	}
}
