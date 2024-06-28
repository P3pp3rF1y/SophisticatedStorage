package net.p3pp3rf1y.sophisticatedstorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.ToolInfoOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModItemColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedstorage.client.render.*;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestPlayerSettingsPacket;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolPacket;

import java.util.Map;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;

public class ClientEventHandler {
	private ClientEventHandler() {}

	private static final String KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY = "keybind.sophisticatedstorage.category";
	private static final int MIDDLE_BUTTON = 2;
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(StorageTranslationHelper.INSTANCE.translKeybind("sort"),
			StorageGuiKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY);

	@SuppressWarnings("java:S6548") //singleton is intended here
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

	private static final ResourceLocation CHEST_RL = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest");
	private static final ResourceLocation CHEST_LEFT_RL = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest_left");
	private static final ResourceLocation CHEST_RIGHT_RL = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest_right");
	public static final ModelLayerLocation CHEST_LAYER = new ModelLayerLocation(CHEST_RL, "main");
	public static final ModelLayerLocation CHEST_LEFT_LAYER = new ModelLayerLocation(CHEST_LEFT_RL, "main");
	public static final ModelLayerLocation CHEST_RIGHT_LAYER = new ModelLayerLocation(CHEST_RIGHT_RL, "main");

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ClientEventHandler::onModelRegistry);
		modBus.addListener(ClientEventHandler::registerLayer);
		modBus.addListener(ClientEventHandler::registerTooltipComponent);
		modBus.addListener(ClientEventHandler::registerOverlay);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerProviders);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(ModItemColors::registerItemColorHandlers);
		modBus.addListener(ModBlockColors::registerBlockColorHandlers);
		modBus.addListener(ClientEventHandler::registerStorageLayerLoader);
		modBus.addListener(ClientEventHandler::onRegisterAdditionalModels);
		modBus.addListener(ClientEventHandler::onRegisterReloadListeners);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(ClientStorageContentsTooltip::onWorldLoad);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::onLimitedBarrelClicked);
		eventBus.addListener(ClientEventHandler::onMouseScrolled);
		eventBus.addListener(ClientEventHandler::onRenderHighlight);
		eventBus.addListener(ClientEventHandler::onPlayerLoggingIn);
	}

	private static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		PacketDistributor.SERVER.noArg().send(new RequestPlayerSettingsPacket());
	}

	private static void onRenderHighlight(RenderHighlightEvent.Block event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		if (player.getMainHandItem().getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(player.getMainHandItem())) {
			BlockHitResult hitresult = event.getTarget();
			BlockPos otherPos = hitresult.getBlockPos().relative(player.getDirection().getClockWise());
			Level level = player.level();
			BlockState blockState = level.getBlockState(otherPos);
			if (!blockState.isAir() && level.getWorldBorder().isWithinBounds(otherPos)) {
				VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
				Vec3 cameraPos = event.getCamera().getPosition();
				LevelRenderer.renderShape(event.getPoseStack(), vertexConsumer, blockState.getShape(level, otherPos, CollisionContext.of(event.getCamera().getEntity())),
						otherPos.getX() - cameraPos.x, otherPos.getY() - cameraPos.y, otherPos.getZ() - cameraPos.z, 0.0F, 0.0F, 0.0F, 0.4F);
			}
		}
	}

	private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
		addBarrelPartModelsToBake(event);
	}

	private static void addBarrelPartModelsToBake(ModelEvent.RegisterAdditional event) {
		Map<ResourceLocation, Resource> models = Minecraft.getInstance().getResourceManager().listResources("models/block/barrel_part", fileName -> fileName.getPath().endsWith(".json"));
		models.forEach((modelName, resource) -> {
			if (modelName.getNamespace().equals(SophisticatedStorage.MOD_ID)) {
				event.register(ResourceLocation.fromNamespaceAndPath(modelName.getNamespace(), modelName.getPath().substring("models/".length()).replace(".json", "")));
			}
		});
	}

	private static void onMouseScrolled(InputEvent.MouseScrollingEvent evt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) {
			return;
		}
		LocalPlayer player = mc.player;
		if (player == null || !player.isShiftKeyDown()) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() != ModItems.STORAGE_TOOL.get()) {
			return;
		}
		PacketDistributor.SERVER.noArg().send(new ScrolledToolPacket(evt.getScrollDeltaY() > 0));
		evt.setCanceled(true);
	}

	private static void onLimitedBarrelClicked(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();

		BlockPos pos = event.getPos();
		Level level = event.getLevel();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LimitedBarrelBlock limitedBarrel)) {
			return;
		}
		if (limitedBarrel.isLookingAtFront(player, pos, state)) {
			if (player.isCreative()) {
				event.setCanceled(true);
			} else {
				if (event.getEntity().getDigSpeed(state, event.getPos()) < 2) {
					event.setUseItem(Event.Result.DENY);
					Minecraft.getInstance().gameMode.destroyDelay = 5;
				}
			}
		}
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
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

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
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

	private static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "barrel"), BarrelDynamicModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "limited_barrel"), LimitedBarrelDynamicModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest"), ChestDynamicModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "shulker_box"), ShulkerBoxDynamicModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "simple_composite"), SimpleCompositeModel.Loader.INSTANCE);
	}

	private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
			BarrelDynamicModelBase.invalidateCache();
			BarrelBakedModelBase.invalidateCache();
		});
	}

	public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CHEST_LAYER, () -> ChestRenderer.createSingleBodyLayer(true));
		event.registerLayerDefinition(CHEST_LEFT_LAYER, ChestRenderer::createDoubleBodyLeftLayer);
		event.registerLayerDefinition(CHEST_RIGHT_LAYER, ChestRenderer::createDoubleBodyRightLayer);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(SORT_KEYBIND);
	}

	private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(StorageContentsTooltip.class, ClientStorageContentsTooltip::new);
	}

	private static void registerOverlay(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "storage_tool_info"), ToolInfoOverlay.HUD_TOOL_INFO);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), context -> new BarrelRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), context -> new LimitedBarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), context -> new ControllerRenderer());
	}
}
