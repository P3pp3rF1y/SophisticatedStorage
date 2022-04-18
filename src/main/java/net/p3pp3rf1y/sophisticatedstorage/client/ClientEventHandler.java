package net.p3pp3rf1y.sophisticatedstorage.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModItemColors;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxRenderer;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModParticles;

public class ClientEventHandler {
	private ClientEventHandler() {}

	private static final ResourceLocation CHEST_RL = new ResourceLocation(SophisticatedBackpacks.MOD_ID, "chest");
	public static final ModelLayerLocation CHEST_LAYER = new ModelLayerLocation(CHEST_RL, "main");

	public static void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(ClientEventHandler::stitchTextures);
		modBus.addListener(ClientEventHandler::onModelRegistry);
		modBus.addListener(ClientEventHandler::loadComplete);
		modBus.addListener(ClientEventHandler::registerLayer);
		modBus.addListener(ClientEventHandler::clientSetup);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerFactories);
	}

	private static void onModelRegistry(ModelRegistryEvent event) {
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("barrel"), BarrelDynamicModel.Loader.INSTANCE);
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("chest"), ChestDynamicModel.Loader.INSTANCE);
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("shulker_box"), ShulkerBoxDynamicModel.Loader.INSTANCE);
	}

	public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CHEST_LAYER, () -> ChestRenderer.createSingleBodyLayer(true));
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.IRON_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLD_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.DIAMOND_BARREL.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.NETHERITE_BARREL.get(), RenderType.cutout());
	}

	private static void stitchTextures(TextureStitchEvent.Pre event) {
		stitchBlockAtlasTextures(event);
		stitchChestTextures(event);
		stitchShulkerBoxTextures(event);
	}

	private static void stitchShulkerBoxTextures(TextureStitchEvent.Pre event) {
		if (!event.getAtlas().location().equals(Sheets.SHULKER_SHEET)) {
			return;
		}

		event.addSprite(ShulkerBoxRenderer.BASE_TIER_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.IRON_TIER_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.GOLD_TIER_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.DIAMOND_TIER_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.NETHERITE_TIER_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.TINTABLE_MAIN_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.TINTABLE_ACCENT_MATERIAL.texture());
		event.addSprite(ShulkerBoxRenderer.NO_TINT_MATERIAL.texture());
	}

	private static void stitchChestTextures(TextureStitchEvent.Pre event) {
		if (!event.getAtlas().location().equals(Sheets.CHEST_SHEET)) {
			return;
		}

		ChestRenderer.WOOD_MATERIALS.values().forEach(mat -> event.addSprite(mat.texture()));
		event.addSprite(ChestRenderer.WOOD_TIER_MATERIAL.texture());
		event.addSprite(ChestRenderer.IRON_TIER_MATERIAL.texture());
		event.addSprite(ChestRenderer.GOLD_TIER_MATERIAL.texture());
		event.addSprite(ChestRenderer.DIAMOND_TIER_MATERIAL.texture());
		event.addSprite(ChestRenderer.NETHERITE_TIER_MATERIAL.texture());
		event.addSprite(ChestRenderer.TINTABLE_MAIN_MATERIAL.texture());
		event.addSprite(ChestRenderer.TINTABLE_ACCENT_MATERIAL.texture());
	}

	private static void stitchBlockAtlasTextures(TextureStitchEvent.Pre event) {
		if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
			return;
		}

		BarrelDynamicModel.WOOD_TEXTURES.forEach((name, textures) -> textures.values().forEach(event::addSprite));
		ChestDynamicModel.WOOD_BREAK_TEXTURES.forEach((name, texture) -> event.addSprite(texture));
		event.addSprite(ChestDynamicModel.TINTABLE_BREAK_TEXTURE);
		event.addSprite(ShulkerBoxDynamicModel.TINTABLE_BREAK_TEXTURE);
		event.addSprite(ShulkerBoxDynamicModel.MAIN_BREAK_TEXTURE);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), context -> new BarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
	}

	private static void loadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> {
			ModItemColors.init();
			ModBlockColors.init();
		});
	}
}
