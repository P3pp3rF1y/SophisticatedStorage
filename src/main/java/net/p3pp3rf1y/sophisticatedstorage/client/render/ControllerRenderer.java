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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
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
				renderBlockOutline(controller.getBlockPos(), controller.getBlockPos(), level, poseStack, bufferSource, DyeColor.LIGHT_BLUE.getTextColor());
			}
			renderLinkedBlocksOutline(controller, level, poseStack, bufferSource);
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

	private void renderLinkedBlocksOutline(ControllerBlockEntity controller, ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource) {
		controller.getLinkedBlocks().forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
			renderLineBetweenBlocks(controller.getBlockPos(), pos, shape, poseStack, bufferSource, DyeColor.LIME.getTextColor());
			renderBlockOutline(controller.getBlockPos(), pos, shape, poseStack, bufferSource, DyeColor.LIME.getTextColor());
		});
	}

	private void renderLineBetweenBlocks(BlockPos initialPos, BlockPos pos, VoxelShape shape, PoseStack poseStack, MultiBufferSource bufferSource, int color) {
		if (shape.isEmpty()) {
			return;
		}

		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;

		Vec3 center = shape.bounds().getCenter();

		VertexConsumer buffer = bufferSource.getBuffer(LineRenderType.LINES);

		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix4f = pose.pose();
		float normalX = (float) (pos.getX() - initialPos.getX() + (0.5F - center.x()));
		float normalY = (float) (pos.getY() - initialPos.getY() + (0.5F - center.y()));
		float normalZ = (float) (pos.getZ() - initialPos.getZ() + (0.5F - center.z()));
		buffer.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(red, green, blue, 255)
				.setNormal(pose, normalX, normalY, normalZ);
		buffer.addVertex(matrix4f, (float) (pos.getX() - initialPos.getX() + center.x()), (float) (pos.getY() - initialPos.getY() + center.y()), (float) (pos.getZ() - initialPos.getZ() + center.z())).setColor(red, green, blue, 255)
				.setNormal(pose, normalX, normalY, normalZ);
	}

	private void renderBlockOutline(BlockPos controllerPos, BlockPos pos, ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource, int color) {
		BlockState state = level.getBlockState(pos);
		VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
		renderBlockOutline(controllerPos, pos, shape, poseStack, bufferSource, color);
	}

	private void renderBlockOutline(BlockPos controllerPos, BlockPos pos, VoxelShape shape, PoseStack poseStack, MultiBufferSource bufferSource, int color) {
		VertexConsumer vertexConsumer = bufferSource.getBuffer(LineRenderType.LINES);
		float red = (float) (color >> 16 & 255) / 255.0F;
		float green = (float) (color >> 8 & 255) / 255.0F;
		float blue = (float) (color & 255) / 255.0F;

		LevelRenderer.renderShape(poseStack, vertexConsumer, shape, (double) -controllerPos.getX() + pos.getX(), (double) -controllerPos.getY() + pos.getY(), (double) -controllerPos.getZ() + pos.getZ(), red, green, blue, 1);
	}

	@Override
	public boolean shouldRenderOffScreen(ControllerBlockEntity blockEntity) {
		return true;
	}

	private static class LineRenderType extends RenderType {
		public LineRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
			super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
		}

		private static final RenderType LINES = RenderType.create("storage_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
				CompositeState.builder()
						.setShaderState(RENDERTYPE_LINES_SHADER)
						.setDepthTestState(NO_DEPTH_TEST)
						.setLineState(new LineStateShard(OptionalDouble.empty()))
						.setLayeringState(VIEW_OFFSET_Z_LAYERING)
						.setCullState(RenderStateShard.NO_CULL)
						.createCompositeState(false));
	}

	@Override
	public AABB getRenderBoundingBox(ControllerBlockEntity blockEntity) {
		return new AABB(blockEntity.getBlockPos()).inflate(ControllerBlockEntityBase.SEARCH_RANGE);
	}
}
