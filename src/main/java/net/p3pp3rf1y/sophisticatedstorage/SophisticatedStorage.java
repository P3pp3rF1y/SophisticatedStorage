package net.p3pp3rf1y.sophisticatedstorage;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

@Mod(SophisticatedStorage.MOD_ID)
public class SophisticatedStorage {
	public static final String MOD_ID = "sophisticatedstorage";

	public static final CreativeModeTab CREATIVE_TAB = new SophisticatedStorageTab();

	private final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") //needs to be public for mod to work
	public SophisticatedStorage() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		commonEventHandler.registerHandlers();
		ModBlocks.registerHandlers(modBus);
	}
}
