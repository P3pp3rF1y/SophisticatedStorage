package net.p3pp3rf1y.sophisticatedstorage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.PaintbrushOverlay;
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

public class ClientEventHandler {
	private ClientEventHandler() {
	}

	private static final Identifier CHEST_RL = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest");
	private static final Identifier CHEST_LEFT_RL = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest_left");
	private static final Identifier CHEST_RIGHT_RL = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "chest_right");
	public static final ModelLayerLocation CHEST_LAYER = new ModelLayerLocation(CHEST_RL, "main");
	public static final ModelLayerLocation CHEST_LEFT_LAYER = new ModelLayerLocation(CHEST_LEFT_RL, "main");
	public static final ModelLayerLocation CHEST_RIGHT_LAYER = new ModelLayerLocation(CHEST_RIGHT_RL, "main");
	public static final ModelLayerLocation CHEST_LOCK_LAYER = new ModelLayerLocation(CHEST_RL, "lock");
	public static final ModelLayerLocation CHEST_LOCK_LEFT_LAYER = new ModelLayerLocation(CHEST_LEFT_RL, "lock");
	public static final ModelLayerLocation CHEST_LOCK_RIGHT_LAYER = new ModelLayerLocation(CHEST_RIGHT_RL, "lock");

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ClientEventHandler::onRegisterModelLoaders);
		modBus.addListener(ClientEventHandler::registerLayer);
		modBus.addListener(ClientEventHandler::registerTooltipComponent);
		modBus.addListener(ClientEventHandler::registerOverlay);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerProviders);
		modBus.addListener(StorageTintSources::register);
		modBus.addListener(ModBlockColors::registerBlockColorHandlers);
		modBus.addListener(ClientEventHandler::registerStorageLayerLoader);
		modBus.addListener(ClientEventHandler::onRegisterReloadListeners);
		modBus.addListener(ClientEventHandler::registerStorageClientExtensions);
		modBus.addListener(ClientEventHandler::registerBarrelItemModel);
		modBus.addListener(ClientEventHandler::registerSpecialModelRenderers);
		modBus.addListener(ClientEventHandler::registerBlockStateModels);
		modBus.addListener(ClientEventHandler::registerRenderPipelines);
		modBus.addListener(ClientEventHandler::registerPictureInPictuterRenderers);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(ClientStorageContentsTooltip::onWorldLoad);
		eventBus.addListener(ClientEventHandler::onLimitedBarrelClicked);
		eventBus.addListener(ClientEventHandler::onMouseScrolled);
		eventBus.addListener(ClientEventHandler::onExtractBlockOutline);
		eventBus.addListener(ClientEventHandler::onPlayerLoggingIn);
		eventBus.addListener(ClientEventHandler::onTick);
	}

	private static void onTick(ClientTickEvent.Pre event) {
		ControllerTargetHighlighter.highlightTargets();
	}

	private static void registerPictureInPictuterRenderers(RegisterPictureInPictureRenderersEvent event) {
		event.register(OversizedItemRenderState.class, OversizedItemRenderer::new);
	}

	private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
		// no custom pipeline needed for temporary lines fallback
	}

	private static void registerBlockStateModels(RegisterBlockStateModels event) {
		event.registerModel(BarrelUnbakedModelBase.UnbakedBlockStateModel.ID, BarrelUnbakedModelBase.UnbakedBlockStateModel.CODEC);
		event.registerModel(ChestBlockStateModel.Unbaked.ID, ChestBlockStateModel.Unbaked.CODEC);
		event.registerModel(ShulkerBoxBlockStateModel.Unbaked.ID, ShulkerBoxBlockStateModel.Unbaked.CODEC);
		event.registerModel(SimpleMaterialModel.UnbakedBlockStateModel.ID, SimpleMaterialModel.UnbakedBlockStateModel.CODEC);
	}

	private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
		event.register(ModBlocks.CHEST_ITEM.getId(), ChestItemRenderer.Unbaked.MAP_CODEC);
		event.register(ModBlocks.SHULKER_BOX_ITEM.getId(), ShulkerBoxItemRenderer.Unbaked.MAP_CODEC);
	}

	private static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		ClientPacketDistributor.sendToServer(new RequestPlayerSettingsPayload());
	}

	private static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		CollisionContext collisionContext = CollisionContext.of(event.getCamera().entity());
		if (stack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(stack)) {
			BlockHitResult hitresult = event.getHitResult();
			BlockPos otherPos = hitresult.getBlockPos().relative(player.getDirection().getClockWise());
			Level level = player.level();
			BlockState blockState = level.getBlockState(otherPos);
			if (!blockState.isAir() && level.getWorldBorder().isWithinBounds(otherPos)) {
				event.addCustomRenderer((blockOutlineRenderState, bufferSource, poseStack, b, levelRenderState) -> {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());
					Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
					ShapeRenderer.renderShape(poseStack, vertexConsumer, blockState.getShape(level, otherPos, collisionContext),
							otherPos.getX() - cameraPos.x, otherPos.getY() - cameraPos.y, otherPos.getZ() - cameraPos.z, ARGB.colorFromFloat(0.4F, 0, 0, 0),
							minecraft.getWindow().getAppropriateLineWidth());
					return false;
				});
			}
		}

		if (stack.getItem() instanceof PaintbrushItem) {
			BlockHitResult hitresult = event.getHitResult();
			Level level = player.level();
			BlockPos pos = hitresult.getBlockPos();
			BlockState blockState = level.getBlockState(pos);

			if (blockState.getBlock() instanceof StorageBlockBase || blockState.getBlock() == ModBlocks.CONTROLLER.get() || level.getBlockEntity(pos) instanceof ISimpleMaterialHolder) {
				PaintbrushOverlay.getItemRequirementsFor(stack, player, level, pos).ifPresent(itemRequirements -> {
					event.addCustomRenderer((blockOutlineRenderState, bufferSource, poseStack, b, levelRenderState) -> {
						float red = !itemRequirements.itemsMissing().isEmpty() ? 1 : 0;
						float green = itemRequirements.itemsMissing().isEmpty() ? 1 : 0;
						VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());
						Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
						ShapeRenderer.renderShape(poseStack, vertexConsumer, blockState.getShape(level, pos, collisionContext),
								pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z, ARGB.colorFromFloat(1, red, green, 0),
								minecraft.getWindow().getAppropriateLineWidth());
						return true;
					});
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
		ClientPacketDistributor.sendToServer(new ScrolledToolPayload(evt.getScrollDeltaY() > 0));
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

	private static void registerStorageLayerLoader(AddClientReloadListenersEvent event) {
		event.addListener(SophisticatedStorage.getIdentifier("chest_texture_manager"), StorageTextureManager.INSTANCE);
	}

	private static void onRegisterModelLoaders(ModelEvent.RegisterLoaders event) {
		event.register(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "barrel"), BarrelUnbakedModel.Loader.INSTANCE);
		event.register(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "limited_barrel"), LimitedBarrelUnbakedModel.Loader.INSTANCE);
		event.register(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "simple_composite"), SimpleCompositeUnbakedModel.Loader.INSTANCE);
		event.register(Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "simple_material"), SimpleMaterialModel.Loader.INSTANCE);
	}

	private static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(SophisticatedStorage.getIdentifier("barrel_cache_invalidation"), (ResourceManagerReloadListener) ClientEventHandler::invalidateBarrelCache);
	}

	private static void invalidateBarrelCache(ResourceManager resourceManager) {
		BarrelUnbakedModelBase.invalidateCache();
		BarrelBlockStateModelBase.invalidateCache();
	}

	public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CHEST_LAYER, ChestRenderer::createSingleBodyLayer);
		event.registerLayerDefinition(CHEST_LEFT_LAYER, ChestRenderer::createDoubleBodyLeftLayer);
		event.registerLayerDefinition(CHEST_RIGHT_LAYER, ChestRenderer::createDoubleBodyRightLayer);
		event.registerLayerDefinition(CHEST_LOCK_LAYER, ChestRenderer::createSingleLockLayer);
		event.registerLayerDefinition(CHEST_LOCK_LEFT_LAYER, ChestRenderer::createDoubleLockLeftLayer);
		event.registerLayerDefinition(CHEST_LOCK_RIGHT_LAYER, ChestRenderer::createDoubleLockRightLayer);
	}

	private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(StorageContentsTooltip.class, ClientStorageContentsTooltip::new);
	}

	private static void registerOverlay(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "storage_tool_info"), ToolInfoOverlay.HUD_TOOL_INFO);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "paintbrush_info"), PaintbrushOverlay.HUD_PAINTBRUSH_INFO);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), BarrelRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), LimitedBarrelRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), context -> new ControllerRenderer());
		event.registerBlockEntityRenderer(ModBlocks.STORAGE_LINK_BLOCK_ENTITY_TYPE.get(), context -> new SimpleMaterialOverlayRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.STORAGE_IO_BLOCK_ENTITY_TYPE.get(), context -> new SimpleMaterialOverlayRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.STORAGE_INPUT_BLOCK_ENTITY_TYPE.get(), context -> new SimpleMaterialOverlayRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.STORAGE_OUTPUT_BLOCK_ENTITY_TYPE.get(), context -> new SimpleMaterialOverlayRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.STORAGE_CONNECTOR_BLOCK_ENTITY_TYPE.get(), context -> new SimpleMaterialOverlayRenderer<>());
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
		event.register(SophisticatedStorage.getIdentifier("barrel"), BarrelItemModel.Unbaked.MAP_CODEC);
		event.register(SophisticatedStorage.getIdentifier("simple_material"), SimpleMaterialModel.SimpleMaterialItemModel.Unbaked.MAP_CODEC);
	}
}
