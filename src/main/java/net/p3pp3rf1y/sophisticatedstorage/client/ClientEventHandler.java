package net.p3pp3rf1y.sophisticatedstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.PaintbrushOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.ToolInfoOverlay;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModBlockColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModItemColors;
import net.p3pp3rf1y.sophisticatedstorage.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedstorage.client.render.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;

import java.util.Map;

public class ClientEventHandler {
	private ClientEventHandler() {}

	private static final ResourceLocation CHEST_RL = new ResourceLocation(SophisticatedStorage.MOD_ID, "chest");
	private static final ResourceLocation CHEST_LEFT_RL = new ResourceLocation(SophisticatedStorage.MOD_ID, "chest_left");
	private static final ResourceLocation CHEST_RIGHT_RL = new ResourceLocation(SophisticatedStorage.MOD_ID, "chest_right");
	public static final ModelLayerLocation CHEST_LAYER = new ModelLayerLocation(CHEST_RL, "main");
	public static final ModelLayerLocation CHEST_LEFT_LAYER = new ModelLayerLocation(CHEST_LEFT_RL, "main");
	public static final ModelLayerLocation CHEST_RIGHT_LAYER = new ModelLayerLocation(CHEST_RIGHT_RL, "main");

	public static void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(ClientEventHandler::onModelRegistry);
		modBus.addListener(ClientEventHandler::registerLayer);
		modBus.addListener(ClientEventHandler::registerTooltipComponent);
		modBus.addListener(ClientEventHandler::registerOverlay);
		modBus.addListener(ClientEventHandler::registerEntityRenderers);
		modBus.addListener(ModParticles::registerProviders);
		modBus.addListener(ModItemColors::registerItemColorHandlers);
		modBus.addListener(ModBlockColors::registerBlockColorHandlers);
		modBus.addListener(ClientEventHandler::registerStorageLayerLoader);
		modBus.addListener(ClientEventHandler::onRegisterAdditionalModels);
		modBus.addListener(ClientEventHandler::onRegisterReloadListeners);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ClientStorageContentsTooltip::onWorldLoad);
		eventBus.addListener(ClientEventHandler::onLimitedBarrelClicked);
		eventBus.addListener(ClientEventHandler::onMouseScrolled);
		eventBus.addListener(ClientEventHandler::onRenderHighlight);
		eventBus.addListener(ClientEventHandler::onTick);
	}

	private static void onTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		ControllerTargetHighlighter.highlightTargets();
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
				LevelRenderer.renderShape(event.getPoseStack(), vertexConsumer, blockState.getShape(level, otherPos, CollisionContext.of(event.getCamera().getEntity())),
						otherPos.getX() - cameraPos.x, otherPos.getY() - cameraPos.y, otherPos.getZ() - cameraPos.z, 0.0F, 0.0F, 0.0F, 0.4F);
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
					LevelRenderer.renderShape(poseStack, vertexConsumer, blockState.getShape(level, pos, CollisionContext.of(event.getCamera().getEntity())),
							pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z, red, green, 0.0F, 1);
					event.setCanceled(true);
				});
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
				event.register(new ResourceLocation(modelName.getNamespace(), modelName.getPath().substring("models/".length()).replace(".json", "")));
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
		StoragePacketHandler.INSTANCE.sendToServer(new ScrolledToolMessage(evt.getScrollDelta() > 0));
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

	private static void registerStorageLayerLoader(AddPackFindersEvent event) {
		ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		if (resourceManager instanceof ReloadableResourceManager reloadableResourceManager) {
			reloadableResourceManager.registerReloadListener(StorageTextureManager.INSTANCE);
		}
	}

	private static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
		event.register("barrel", BarrelDynamicModel.Loader.INSTANCE);
		event.register("limited_barrel", LimitedBarrelDynamicModel.Loader.INSTANCE);
		event.register("chest", ChestDynamicModel.Loader.INSTANCE);
		event.register("shulker_box", ShulkerBoxDynamicModel.Loader.INSTANCE);
		event.register("simple_composite", SimpleCompositeModel.Loader.INSTANCE);
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

	private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(StorageContentsTooltip.class, ClientStorageContentsTooltip::new);
	}

	private static void registerOverlay(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "storage_tool_info", ToolInfoOverlay.HUD_TOOL_INFO);
		event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "paintbrush_info", PaintbrushOverlay.HUD_PAINTBRUSH_INFO);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get(), context -> new BarrelRenderer<>());
		event.registerBlockEntityRenderer(ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), context -> new LimitedBarrelRenderer());
		event.registerBlockEntityRenderer(ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get(), ChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxRenderer::new);
		event.registerBlockEntityRenderer(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), context -> new ControllerRenderer());
		event.registerBlockEntityRenderer(ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get(), DecorationTableRenderer::new);
	}
}
