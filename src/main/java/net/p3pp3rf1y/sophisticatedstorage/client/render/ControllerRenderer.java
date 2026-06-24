package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ControllerRenderer implements BlockEntityRenderer<ControllerBlockEntity, ControllerRenderer.ControllerRenderState> {
	private static final RenderType LINES = RenderTypes.lines();

	private void submitConnectedStorageBlocksInfo(SubmitNodeCollector submitNodeCollector, Direction playerLookDirection, PoseStack poseStack,
			List<BlockPos> storagePositions, List<Integer> storageSlots, BlockPos controllerPos) {
		double zScale = 0.001;
		float scale = 0.015f;
		int storageOrder = 1;
		for (BlockPos position : storagePositions) {
			double translateX = position.getX() + 0.5 - controllerPos.getX() + 0.501 * (playerLookDirection.getUnitVec3i().getX());
			double translateY = position.getY() + 0.5 - controllerPos.getY() + 0.501 * (playerLookDirection.getUnitVec3i().getY());
			double translateZ = position.getZ() + 0.5 - controllerPos.getZ() + 0.501 * (playerLookDirection.getUnitVec3i().getZ());

			poseStack.pushPose();
			poseStack.translate(translateX, translateY, translateZ);
			Quaternionf rotation = playerLookDirection.getRotation();
			rotation.mul(Axis.XP.rotationDegrees(-90.0F));
			poseStack.mulPose(rotation);
			poseStack.translate(-0.45f, 0.45f, 0);

			poseStack.scale(scale, -scale, (float) zScale);
			submitNodeCollector.submitText(poseStack, 0, 0, Component.literal("Order: " + storageOrder).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
					15728880, DyeColor.WHITE.getTextColor(), 0, 0);
			poseStack.translate(0, 10, 0);
			submitNodeCollector.submitText(poseStack, 0, 0, Component.literal("Slots: " + storageSlots.get(storageOrder - 1)).getVisualOrderText(), false,
					Font.DisplayMode.NORMAL, 15728880, DyeColor.WHITE.getTextColor(), 0, 0);
			poseStack.popPose();

			storageOrder++;
		}
	}

	private void submitLinkedBlocks(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, BlockPos controllerPos,
			List<VoxelOutliner.Edge> linkedBlockEdges, List<ControllerRenderState.LinkedBlockInfo> linkedBlocks) {
		linkedBlocks.forEach(linkedBlockInfo -> submitLineBetweenBlocks(submitNodeCollector, controllerPos, linkedBlockInfo.pos, linkedBlockInfo.center,
				poseStack, DyeColor.LIME.getTextColor()));
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, DyeColor.LIME.getTextColor(), linkedBlockEdges, controllerPos);
	}

	private void submitStorageBlocksOutline(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, List<VoxelOutliner.Edge> storageBlockEdges,
			BlockPos controllerPos) {
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, 0x69c53b, storageBlockEdges, controllerPos);
	}

	private void submitLineBetweenBlocks(SubmitNodeCollector submitNodeCollector, BlockPos initialPos, BlockPos pos, Vec3 center, PoseStack poseStack,
			int color) {
		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;

		float normalX = (float) (pos.getX() - initialPos.getX() + (0.5F - center.x()));
		float normalY = (float) (pos.getY() - initialPos.getY() + (0.5F - center.y()));
		float normalZ = (float) (pos.getZ() - initialPos.getZ() + (0.5F - center.z()));
		submitNodeCollector.submitCustomGeometry(poseStack, LINES, (pose, buffer) -> {
			buffer.addVertex(pose, 0.5F, 0.5F, 0.5F).setColor(red, green, blue, 255).setNormal(pose, normalX, normalY, normalZ).setLineWidth(2);
			buffer.addVertex(pose, (float) (pos.getX() - initialPos.getX() + center.x()), (float) (pos.getY() - initialPos.getY() + center.y()),
					(float) (pos.getZ() - initialPos.getZ() + center.z())).setColor(red, green, blue, 255).setNormal(pose, normalX, normalY, normalZ)
					.setLineWidth(2);
		});
	}

	private void submitControllerOutline(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, List<VoxelOutliner.Edge> controllerEdges,
			BlockPos controllerPos) {
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, 0x2ebbff, controllerEdges, controllerPos);
	}

	@Override
	public ControllerRenderState createRenderState() {
		return new ControllerRenderState();
	}

	@Override
	public void extractRenderState(ControllerBlockEntity controller, ControllerRenderState renderState, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(controller, renderState, partialTick, cameraPos, crumblingOverlay);

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}

		InventoryHelper.getItemFromEitherHand(player, ModItems.DEBUG_TOOL.get()).ifPresent(storageTool -> {
			renderState.storagePositions = controller.getStoragePositions();
			renderState.storageSlots = new ArrayList<>();
			for (int i = 0; i < renderState.storagePositions.size(); i++) {
				renderState.storageSlots.add(controller.getSlots(i));
			}
		});

		InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get()).ifPresent(storageTool -> {
			if (StorageToolItem.getMode(storageTool) != StorageToolItem.Mode.LINK) {
				return;
			}

			renderState.linkedBlockEdges = controller.getLinkedBlockEdges();
			renderState.storageBlockEdges = controller.getStorageBlockEdges();
			renderState.linkedBlocks = controller.getLinkedBlocks().stream().flatMap(pos -> {
				VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
				if (shape.isEmpty()) {
					return Stream.empty();
				}
				return Stream.of(new ControllerRenderState.LinkedBlockInfo(pos, shape.bounds().getCenter()));
			}).toList();

			if (StorageToolItem.getControllerLink(storageTool).map(toolControllerPos -> toolControllerPos.equals(controller.getBlockPos())).orElse(false)) {
				renderState.controllerEdges = controller.getControllerEdges();
			} else {
				renderState.controllerEdges = Collections.emptyList();
			}
		});

		renderState.hiddenOverlayQuads = Collections.emptyList();
		if (controller.isOverlayHidden() && SimpleMaterialOverlayRenderer.holdsStorageToolThatShowsHiddenOverlay()) {
			controller.getMaterial().ifPresent(material -> {
				BlockStateModel blockModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(controller.getBlockState());
				if (blockModel instanceof SimpleMaterialModel.SimpleMaterialBlockStateModel simpleMaterialModel) {
					renderState.hiddenOverlayQuads = simpleMaterialModel.getOverlayOnlyQuads(material, true);
				}
			});
		}
	}

	@Override
	public void submit(ControllerRenderState controllerRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState) {
		BlockPos controllerPos = controllerRenderState.blockPos;

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		submitConnectedStorageBlocksInfo(submitNodeCollector, Direction.orderedByNearest(player)[0].getOpposite(), poseStack,
				controllerRenderState.storagePositions, controllerRenderState.storageSlots, controllerPos);
		submitControllerOutline(submitNodeCollector, poseStack, controllerRenderState.controllerEdges, controllerPos);
		submitLinkedBlocks(submitNodeCollector, poseStack, controllerPos, controllerRenderState.linkedBlockEdges, controllerRenderState.linkedBlocks);
		submitStorageBlocksOutline(submitNodeCollector, poseStack, controllerRenderState.storageBlockEdges, controllerPos);
		SimpleMaterialOverlayRenderer.submitHiddenOverlayQuads(submitNodeCollector, poseStack, controllerRenderState.lightCoords,
				controllerRenderState.hiddenOverlayQuads, false);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(ControllerBlockEntity blockEntity) {
		return new AABB(blockEntity.getBlockPos()).inflate(Config.SERVER.controllerRange.getAsInt());
	}

	public static class ControllerRenderState extends BlockEntityRenderState {
		public List<BlockPos> storagePositions = Collections.emptyList();
		public List<Integer> storageSlots = Collections.emptyList();
		public List<VoxelOutliner.Edge> linkedBlockEdges = Collections.emptyList();
		public List<VoxelOutliner.Edge> storageBlockEdges = Collections.emptyList();
		public List<LinkedBlockInfo> linkedBlocks = Collections.emptyList();
		public List<VoxelOutliner.Edge> controllerEdges = Collections.emptyList();
		public List<BakedQuad> hiddenOverlayQuads = Collections.emptyList();

		public record LinkedBlockInfo(BlockPos pos, Vec3 center) {
		}
	}
}
