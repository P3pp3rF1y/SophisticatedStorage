package net.p3pp3rf1y.sophisticatedstorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.PaintbrushOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.ToolInfoOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedstorage.client.init.StorageTintSources;
import net.p3pp3rf1y.sophisticatedstorage.client.render.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestPlayerSettingsPayload;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolPayload;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;

public class ClientEventHandler {
	private ClientEventHandler() {
	}

	private static final String KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY = "keybind.sophisticatedstorage.category";
	private static final int MIDDLE_BUTTON = 2;
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(StorageTranslationHelper.INSTANCE.translKeybind("sort"),
			StorageGuiKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDSTORAGE_CATEGORY);

	private static final Set<Predicate<StorageScreenBase<?>>> SORT_SCREEN_MATCHERS = new HashSet<>();

	static {
		SORT_SCREEN_MATCHERS.add(screen -> screen instanceof StorageScreen);
	}

	public static void addSortScreenMatcher(Predicate<StorageScreenBase<?>> matcher) {
		SORT_SCREEN_MATCHERS.add(matcher);
	}

	@SuppressWarnings("java:S6548") //singleton is intended here
	private static class StorageGuiKeyConflictContext implements IKeyConflictContext {
		public static final StorageGuiKeyConflictContext INSTANCE = new StorageGuiKeyConflictContext();

		@Override
		public boolean isActive() {
			return GUI.isActive() && Minecraft.getInstance().screen instanceof StorageScreenBase<?> storageScreen
					&& SORT_SCREEN_MATCHERS.stream().anyMatch(matcher -> matcher.test(storageScreen));
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
		modBus.addListener(ClientEventHandler::onRegisterModelLoaders);
		modBus.addListener(ClientEventHandler::registerLayer);
		modBus.addListener(ClientEventHandler::registerTooltipComponent);
		modBus.addListener(ClientEventHandler::registerOverlay);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerProviders);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(StorageTintSources::register);
		modBus.addListener(ModBlockColors::registerBlockColorHandlers);
		modBus.addListener(ClientEventHandler::registerStorageLayerLoader);
		modBus.addListener(ClientEventHandler::onRegisterReloadListeners);
		modBus.addListener(ClientEventHandler::registerStorageClientExtensions);
		modBus.addListener(ClientEventHandler::registerBarrelItemModel);
		modBus.addListener(ClientEventHandler::registerSpecialModelRenderers);
		modBus.addListener(ClientEventHandler::registerSpecialBlockModelRenderers);
		modBus.addListener(ClientEventHandler::registerBlockStateModels);
		modBus.addListener(ClientEventHandler::registerRenderPipelines);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(ClientStorageContentsTooltip::onWorldLoad);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(EventPriority.HIGH, ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::onLimitedBarrelClicked);
		eventBus.addListener(ClientEventHandler::onMouseScrolled);
		eventBus.addListener(ClientEventHandler::onRenderHighlight);
		eventBus.addListener(ClientEventHandler::onPlayerLoggingIn);
	}

	private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(ControllerRenderer.NO_DEPTH_LINES_PIPELINE);
		event.registerPipeline(ControllerRenderer.THICK_HIGHLIGHT_PIPELINE);
	}

	private static void registerBlockStateModels(RegisterBlockStateModels event) {
		event.registerModel(BarrelUnbakedModelBase.UnbakedBlockStateModel.ID, BarrelUnbakedModelBase.UnbakedBlockStateModel.CODEC);
		event.registerModel(ChestBlockStateModel.Unbaked.ID, ChestBlockStateModel.Unbaked.CODEC);
		event.registerModel(ShulkerBoxBlockStateModel.Unbaked.ID, ShulkerBoxBlockStateModel.Unbaked.CODEC);
	}

	private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
		event.register(ModBlocks.CHEST_ITEM.getId(), ChestItemRenderer.Unbaked.MAP_CODEC);
		event.register(ModBlocks.SHULKER_BOX_ITEM.getId(), ShulkerBoxItemRenderer.Unbaked.MAP_CODEC);
	}

	private static void registerSpecialBlockModelRenderers(RegisterSpecialBlockModelRendererEvent event) {
		ModBlocks.BLOCKS.getEntries().stream()
				.filter(b -> b.get() instanceof ChestBlock)
				.forEach(b -> event.register(b.get(), new ChestItemRenderer.Unbaked()));

		ModBlocks.BLOCKS.getEntries().stream()
				.filter(b -> b.get() instanceof ShulkerBoxBlock)
				.forEach(b -> event.register(b.get(), new ShulkerBoxItemRenderer.Unbaked()));
	}

	private static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		PacketDistributor.sendToServer(new RequestPlayerSettingsPayload());
	}

	private static void onRenderHighlight(RenderHighlightEvent.Block event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(stack)) {
			BlockHitResult hitresult = event.getTarget();
			BlockPos otherPos = hitresult.getBlockPos().relative(player.getDirection().getClockWise());
			Level level = player.level();
			BlockState blockState = level.getBlockState(otherPos);
			if (!blockState.isAir() && level.getWorldBorder().isWithinBounds(otherPos)) {
				VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
				Vec3 cameraPos = event.getCamera().getPosition();
				ShapeRenderer.renderShape(event.getPoseStack(), vertexConsumer, blockState.getShape(level, otherPos, CollisionContext.of(event.getCamera().getEntity())),
						otherPos.getX() - cameraPos.x, otherPos.getY() - cameraPos.y, otherPos.getZ() - cameraPos.z, ARGB.colorFromFloat(0.4F, 0, 0, 0));
			}
		}

		if (stack.getItem() instanceof PaintbrushItem) {
			BlockHitResult hitresult = event.getTarget();
			Level level = player.level();
			BlockPos pos = hitresult.getBlockPos();
			BlockState blockState = level.getBlockState(pos);

			if (blockState.getBlock() instanceof StorageBlockBase || blockState.getBlock() == ModBlocks.CONTROLLER.get()) {
				PaintbrushOverlay.getItemRequirementsFor(stack, player, level, pos).ifPresent(itemRequirements -> {
					float red = !itemRequirements.itemsMissing().isEmpty() ? 1 : 0;
					float green = itemRequirements.itemsMissing().isEmpty() ? 1 : 0;
					VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
					Vec3 cameraPos = event.getCamera().getPosition();
					PoseStack poseStack = event.getPoseStack();
					ShapeRenderer.renderShape(poseStack, vertexConsumer, blockState.getShape(level, pos, CollisionContext.of(event.getCamera().getEntity())),
							pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z, ARGB.colorFromFloat(1, red, green, 0));
					event.setCanceled(true);
				});
			}
		}
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
		PacketDistributor.sendToServer(new ScrolledToolPayload(evt.getScrollDeltaY() > 0));
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
				if (event.getEntity().getDestroySpeed(state, event.getPos()) < 2) {
					event.setUseItem(TriState.FALSE);
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

	private static void registerStorageLayerLoader(AddClientReloadListenersEvent event) {
		event.addListener(SophisticatedStorage.getRL("chest_texture_manager"), StorageTextureManager.INSTANCE);
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static boolean tryCallSort(Screen gui) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu instanceof StorageContainerMenuBase<?> container && gui instanceof StorageScreenBase<?> screen) {
			MouseHandler mh = mc.mouseHandler;
			double mouseX = mh.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
			double mouseY = mh.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
			Slot selectedSlot = screen.getHoveredSlot(mouseX, mouseY);
			if (selectedSlot == null || container.isNotPlayersInventorySlot(selectedSlot.index)) {
				container.sort();
				return true;
			}
		}
		return false;
	}

	private static void onRegisterModelLoaders(ModelEvent.RegisterLoaders event) {
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "barrel"), BarrelUnbakedModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "limited_barrel"), LimitedBarrelUnbakedModel.Loader.INSTANCE);
		event.register(ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "simple_composite"), SimpleCompositeUnbakedModel.Loader.INSTANCE);
	}

	private static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(SophisticatedStorage.getRL("barrel_cache_invalidation"), (ResourceManagerReloadListener) ClientEventHandler::invalidateBarrelCache);
	}

	private static void invalidateBarrelCache(ResourceManager resourceManager) {
		BarrelUnbakedModelBase.invalidateCache();
		BarrelBlockStateModelBase.invalidateCache();
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

	private static void registerOverlay(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "storage_tool_info"), ToolInfoOverlay.HUD_TOOL_INFO);
		event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "paintbrush_info"), PaintbrushOverlay.HUD_PAINTBRUSH_INFO);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), context -> new BarrelRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), context -> new LimitedBarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), context -> new ControllerRenderer());
		event.registerBlockEntityRenderer(ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get(), DecorationTableRenderer::new);
	}

	private static void registerStorageClientExtensions(RegisterClientExtensionsEvent event) {
		registerBarrelClientExtensions(event,
				ModBlocks.BARREL.get(), ModBlocks.COPPER_BARREL.get(), ModBlocks.IRON_BARREL.get(), ModBlocks.GOLD_BARREL.get(), ModBlocks.DIAMOND_BARREL.get(), ModBlocks.NETHERITE_BARREL.get(),
				ModBlocks.LIMITED_BARREL_1.get(), ModBlocks.LIMITED_COPPER_BARREL_1.get(), ModBlocks.LIMITED_IRON_BARREL_1.get(), ModBlocks.LIMITED_GOLD_BARREL_1.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1.get(),
				ModBlocks.LIMITED_BARREL_2.get(), ModBlocks.LIMITED_COPPER_BARREL_2.get(), ModBlocks.LIMITED_IRON_BARREL_2.get(), ModBlocks.LIMITED_GOLD_BARREL_2.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), ModBlocks.LIMITED_NETHERITE_BARREL_2.get(),
				ModBlocks.LIMITED_BARREL_3.get(), ModBlocks.LIMITED_COPPER_BARREL_3.get(), ModBlocks.LIMITED_IRON_BARREL_3.get(), ModBlocks.LIMITED_GOLD_BARREL_3.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3.get(),
				ModBlocks.LIMITED_BARREL_4.get(), ModBlocks.LIMITED_COPPER_BARREL_4.get(), ModBlocks.LIMITED_IRON_BARREL_4.get(), ModBlocks.LIMITED_GOLD_BARREL_4.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), ModBlocks.LIMITED_NETHERITE_BARREL_4.get()
		);
	}

	private static void registerBarrelClientExtensions(RegisterClientExtensionsEvent event, BarrelBlock... barrelBlocks) {
		for (BarrelBlock barrelBlock : barrelBlocks) {
			event.registerBlock(new BarrelBlockClientExtensions(barrelBlock), barrelBlock);
		}
	}

	private static void registerBarrelItemModel(RegisterItemModelsEvent event) {
		event.register(SophisticatedStorage.getRL("barrel"), BarrelItemModel.Unbaked.MAP_CODEC);
	}
}
