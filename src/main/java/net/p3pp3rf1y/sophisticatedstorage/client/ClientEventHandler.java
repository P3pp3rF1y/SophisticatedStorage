package net.p3pp3rf1y.sophisticatedstorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.ToolInfoOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModItemColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelDynamicModelBase;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ClientStorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ControllerRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.LimitedBarrelDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.LimitedBarrelRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.LockRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxDynamicModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxRenderer;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeContainer;

import static net.minecraftforge.client.gui.ForgeIngameGui.HOTBAR_ELEMENT;
import static net.minecraftforge.client.settings.KeyConflictContext.GUI;

public class ClientEventHandler {
	private ClientEventHandler() {}

	private static final String KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY = "keybind.sophisticatedstorage.category";
	private static final int MIDDLE_BUTTON = 2;
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(StorageTranslationHelper.INSTANCE.translKeybind("sort"),
			StorageGuiKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY);

	private static class StorageGuiKeyConflictContext implements IKeyConflictContext {
		public static final StorageGuiKeyConflictContext INSTANCE = new StorageGuiKeyConflictContext();

		@Override
		public boolean isActive() {
			return GUI.isActive() && Minecraft.getInstance().screen instanceof StorageScreen;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}

	private static final ResourceLocation CHEST_RL = new ResourceLocation(SophisticatedStorage.MOD_ID, "chest");
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
		modBus.addListener(ClientEventHandler::registerStorageLayerLoader);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ClientStorageContentsTooltip::onWorldLoad);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::onLimitedBarrelClicked);
		eventBus.addListener(ClientEventHandler::onMouseScrolled);
	}

	private static void onMouseScrolled(InputEvent.MouseScrollEvent evt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null || !Screen.hasShiftDown()) {
			return;
		}
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() != ModItems.STORAGE_TOOL.get()) {
			return;
		}
		StoragePacketHandler.INSTANCE.sendToServer(new ScrolledToolMessage(evt.getScrollDelta() > 0));
		evt.setCanceled(true);
	}

	private static void onLimitedBarrelClicked(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getPlayer();
		if (!player.isCreative()) {
			return;
		}

		BlockPos pos = event.getPos();
		Level level = event.getWorld();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LimitedBarrelBlock limitedBarrel)) {
			return;
		}
		if (limitedBarrel.isLookingAtFront(player, pos, state)) {
			event.setCanceled(true);
		}
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyboardKeyPressedEvent.Pre event) {
		if (SORT_KEYBIND.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode())) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static void registerStorageLayerLoader(AddPackFindersEvent event) {
		ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		if (resourceManager instanceof ReloadableResourceManager reloadableResourceManager) {
			reloadableResourceManager.registerReloadListener(StorageTextureManager.INSTANCE);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseClickedEvent.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static boolean tryCallSort(Screen gui) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu instanceof StorageContainerMenu container && gui instanceof StorageScreen screen) {
			MouseHandler mh = mc.mouseHandler;
			double mouseX = mh.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
			double mouseY = mh.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
			Slot selectedSlot = screen.findSlot(mouseX, mouseY);
			if (selectedSlot != null && container.isNotPlayersInventorySlot(selectedSlot.index)) {
				container.sort();
				return true;
			}
		}
		return false;
	}

	private static void onModelRegistry(ModelRegistryEvent event) {
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("barrel"), BarrelDynamicModel.Loader.INSTANCE);
		ModelLoaderRegistry.registerLoader(SophisticatedStorage.getRL("limited_barrel"), LimitedBarrelDynamicModel.Loader.INSTANCE);
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

		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_BARREL_1.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_BARREL_2.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_BARREL_3.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_BARREL_4.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_IRON_BARREL_1.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_IRON_BARREL_2.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_IRON_BARREL_3.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_IRON_BARREL_4.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_GOLD_BARREL_1.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_GOLD_BARREL_2.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_GOLD_BARREL_3.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_GOLD_BARREL_4.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_NETHERITE_BARREL_1.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_NETHERITE_BARREL_2.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_NETHERITE_BARREL_3.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIMITED_NETHERITE_BARREL_4.get(), RenderType.cutout());

		MinecraftForgeClient.registerTooltipComponentFactory(StorageContentsTooltip.class, ClientStorageContentsTooltip::new);

		event.enqueueWork(() -> ClientRegistry.registerKeyBinding(SORT_KEYBIND));
		OverlayRegistry.registerOverlayAbove(HOTBAR_ELEMENT, "storage_tool_info", ToolInfoOverlay.HUD_TOOL_INFO);
	}

	private static void stitchTextures(TextureStitchEvent.Pre event) {
		stitchBlockAtlasTextures(event);
		stitchChestTextures(event);
		stitchShulkerBoxTextures(event);
		event.addSprite(LockRenderer.LOCK_TEXTURE.texture());
		event.addSprite(CompressionInventoryPart.EMPTY_COMPRESSION_SLOT.getSecond());
		event.addSprite(HopperUpgradeContainer.EMPTY_INPUT_FILTER_SLOT_BACKGROUND.getSecond());
		event.addSprite(HopperUpgradeContainer.EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND.getSecond());
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

		StorageTextureManager.INSTANCE.getUniqueChestMaterials().forEach(mat -> event.addSprite(mat.texture()));
	}

	private static void stitchBlockAtlasTextures(TextureStitchEvent.Pre event) {
		if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
			return;
		}

		BarrelDynamicModelBase.getTextures().forEach(mat -> event.addSprite(mat.texture()));
		ChestDynamicModel.getWoodBreakTextures().forEach(event::addSprite);
		event.addSprite(ChestDynamicModel.TINTABLE_BREAK_TEXTURE);
		event.addSprite(ShulkerBoxDynamicModel.TINTABLE_BREAK_TEXTURE);
		event.addSprite(ShulkerBoxDynamicModel.MAIN_BREAK_TEXTURE);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), context -> new BarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), context -> new LimitedBarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), context -> new ControllerRenderer());
	}

	private static void loadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> {
			ModItemColors.init();
			ModBlockColors.init();
		});
	}
}
