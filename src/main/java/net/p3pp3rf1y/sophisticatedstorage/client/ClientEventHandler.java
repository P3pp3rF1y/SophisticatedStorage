package net.p3pp3rf1y.sophisticatedstorage.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModItemColors;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelBlockEntityRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModParticles;

public class ClientEventHandler {
	private ClientEventHandler() {}

	public static void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(ClientEventHandler::stitchTextures);
		modBus.addListener(ClientEventHandler::onModelRegistry);
		modBus.addListener(ClientEventHandler::loadComplete);
		modBus.addListener(ClientEventHandler::clientSetup);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerFactories);
	}

	private static void onModelRegistry(ModelRegistryEvent event) {
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("barrel"), BarrelDynamicModel.Loader.INSTANCE);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.IRON_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLD_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.DIAMOND_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.NETHERITE_BARREL.get(), RenderType.cutout());
	}

	private static void stitchTextures(TextureStitchEvent.Pre event) {
		if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
			return;
		}

		BarrelDynamicModel.WOOD_TEXTURES.forEach((name, textures) -> textures.values().forEach(event::addSprite));
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_TILE_TYPE.get(), BarrelBlockEntityRenderer::new);
	}

	private static void loadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> {
			ModItemColors.init();
			ModBlockColors.init();
		});
	}
}
