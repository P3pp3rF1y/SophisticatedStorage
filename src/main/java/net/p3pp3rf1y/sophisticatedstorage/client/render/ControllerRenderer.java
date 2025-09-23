package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.OptionalDouble;

public class ControllerRenderer implements BlockEntityRenderer<ControllerBlockEntity> {
	@Override
	public void render(ControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;

		if (level == null) {
			return;
		}

		InventoryHelper.getItemFromEitherHand(player, ModItems.DEBUG_TOOL.get()).ifPresent(storageTool -> renderConnectedStorageBlocksInfo(controller, Direction.orderedByNearest(player)[0].getOpposite(), poseStack, bufferSource));
		InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get()).ifPresent(storageTool -> {
			if (StorageToolItem.getMode(storageTool) != StorageToolItem.Mode.LINK) {
				return;
			}

			if (StorageToolItem.getControllerLink(storageTool).map(controllerPos -> controllerPos.equals(controller.getBlockPos())).orElse(false)) {
				renderControllerOutline(controller, poseStack, bufferSource);
			}
			renderLinkedBlocks(controller, level, poseStack, bufferSource);
			renderStorageBlocksOutline(controller, poseStack, bufferSource);
		});
	}

	private void renderConnectedStorageBlocksInfo(ControllerBlockEntity controller, Direction playerLookDirection, PoseStack poseStack, MultiBufferSource bufferSource) {
		Font fontRenderer = Minecraft.getInstance().font;
		double zScale = 0.001;
		float scale = 0.015f;
		int storageOrder = 1;
		for (BlockPos position : controller.getStoragePositions()) {
			BlockPos controllerPos = controller.getBlockPos();

			double translateX = position.getX() + 0.5 - controllerPos.getX() + 0.501 * (playerLookDirection.getNormal().getX());
			double translateY = position.getY() + 0.5 - controllerPos.getY() + 0.501 * (playerLookDirection.getNormal().getY());
			double translateZ = position.getZ() + 0.5 - controllerPos.getZ() + 0.501 * (playerLookDirection.getNormal().getZ());

			poseStack.pushPose();
			poseStack.translate(translateX, translateY, translateZ);
			Quaternionf rotation = playerLookDirection.getRotation();
			rotation.mul(Axis.XP.rotationDegrees(-90.0F));
			poseStack.mulPose(rotation);
			poseStack.translate(-0.45f, 0.45f, 0);

			poseStack.scale(scale, -scale, (float) zScale);
			fontRenderer.drawInBatch("Order: " + storageOrder, 0, 0, DyeColor.WHITE.getTextColor(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
			poseStack.translate(0, 10, 0);
			fontRenderer.drawInBatch("Slots: " + controller.getSlots(storageOrder - 1), 0, 0, DyeColor.WHITE.getTextColor(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
			poseStack.popPose();

			storageOrder++;
		}
	}

	private void renderLinkedBlocks(ControllerBlockEntity controller, ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource) {
		controller.getLinkedBlocks().forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
			renderLineBetweenBlocks(controller.getBlockPos(), pos, shape, poseStack, bufferSource, DyeColor.LIME.getTextureDiffuseColors());
		});
		BlockHighlightRenderHelper.renderThickEdges(poseStack, bufferSource, DyeColor.LIME.getTextColor(), controller.getLinkedBlockEdges(), controller.getBlockPos());
	}

	private void renderStorageBlocksOutline(ControllerBlockEntity controller, PoseStack poseStack, MultiBufferSource bufferSource) {
		BlockHighlightRenderHelper.renderThickEdges(poseStack, bufferSource, 0x69c53b, controller.getStorageBlockEdges(), controller.getBlockPos());
	}

	private void renderLineBetweenBlocks(BlockPos initialPos, BlockPos pos, VoxelShape shape, PoseStack poseStack, MultiBufferSource bufferSource, float[] color) {
		if (shape.isEmpty()) {
			return;
		}

		float red = color[0];
		float green = color[1];
		float blue = color[2];

		Vec3 center = shape.bounds().getCenter();

		VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.LINES);

		Matrix4f matrix4f = poseStack.last().pose();
		Matrix3f matrix3f = poseStack.last().normal();
		float normalX = (float) (pos.getX() - initialPos.getX() + (0.5F - center.x()));
		float normalY = (float) (pos.getY() - initialPos.getY() + (0.5F - center.y()));
		float normalZ = (float) (pos.getZ() - initialPos.getZ() + (0.5F - center.z()));
		buffer.vertex(matrix4f, 0.5F, 0.5F, 0.5F).color(red, green, blue, 255)
				.normal(matrix3f, normalX, normalY, normalZ).endVertex();
		buffer.vertex(matrix4f, (float) (pos.getX() - initialPos.getX() + center.x()), (float) (pos.getY() - initialPos.getY() + center.y()), (float) (pos.getZ() - initialPos.getZ() + center.z())).color(red, green, blue, 255)
				.normal(matrix3f, normalX, normalY, normalZ).endVertex();
	}

	private void renderControllerOutline(ControllerBlockEntity controller, PoseStack poseStack, MultiBufferSource bufferSource) {
		BlockHighlightRenderHelper.renderThickEdges(poseStack, bufferSource, 0x2ebbff, controller.getControllerEdges(), controller.getBlockPos());
	}

	@Override
	public boolean shouldRenderOffScreen(ControllerBlockEntity pBlockEntity) {
		return true;
	}

	private static class RenderTypes extends RenderType {
		private static final RenderType LINES = create(
				"storage_lines",
				DefaultVertexFormat.POSITION_COLOR_NORMAL,
				VertexFormat.Mode.LINES,
				256,
				false,
				false,
				CompositeState.builder()
						.setShaderState(RENDERTYPE_LINES_SHADER)
						.setDepthTestState(NO_DEPTH_TEST)
						.setLineState(new LineStateShard(OptionalDouble.empty()))
						.setLayeringState(VIEW_OFFSET_Z_LAYERING)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		public RenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
			super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
		}

	}
}
