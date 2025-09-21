package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
import net.minecraft.client.renderer.RenderPipelines;
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
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
import net.p3pp3rf1y.sophisticatedstorage.util.VoxelOutliner;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.OptionalDouble;

public class ControllerRenderer implements BlockEntityRenderer<ControllerBlockEntity> {
	public static final RenderPipeline NO_DEPTH_LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withLocation(SophisticatedStorage.getRL("pipeline/controller_lines"))
			.build();

	public static final RenderPipeline THICK_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
			.withCull(false)
			.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
			.withDepthWrite(true)
			.withLocation(SophisticatedStorage.getRL("pipeline/outline_quads"))
			.build();

	private static final RenderType LINES = RenderType.create("storage_lines", 1536, NO_DEPTH_LINES_PIPELINE,
			RenderType.CompositeState.builder()
					.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
					.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
					.setOutputState(RenderType.ITEM_ENTITY_TARGET)
					.createCompositeState(false)
	);

	public static final RenderType THICK_HIGHLIGHT_QUADS = RenderType.create(
			"storage_outline_quads",
			1536,
			THICK_HIGHLIGHT_PIPELINE,
			RenderType.CompositeState.builder()
					.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
					.setOutputState(RenderType.ITEM_ENTITY_TARGET)
					.createCompositeState(false)
	);

	@Override
	public void render(ControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
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
			fontRenderer.drawInBatch("Order: " + storageOrder, 0, 0, DyeColor.WHITE.getTextColor(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
			poseStack.translate(0, 10, 0);
			fontRenderer.drawInBatch("Slots: " + controller.getSlots(storageOrder - 1), 0, 0, DyeColor.WHITE.getTextColor(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
			poseStack.popPose();

			storageOrder++;
		}
	}

	private void renderLinkedBlocks(ControllerBlockEntity controller, ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource) {
		controller.getLinkedBlocks().forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
			renderLineBetweenBlocks(controller.getBlockPos(), pos, shape, poseStack, bufferSource, DyeColor.LIME.getTextColor());
		});
		renderThickEdges(controller, poseStack, bufferSource, DyeColor.LIME.getTextColor(), controller.getLinkedBlockEdges());
	}

	private static void renderThickEdges(ControllerBlockEntity controller, PoseStack poseStack, MultiBufferSource bufferSource, int color, List<VoxelOutliner.Edge> edges) {
		VertexConsumer vertexConsumer = bufferSource.getBuffer(THICK_HIGHLIGHT_QUADS);
		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;
		PoseStack.Pose pose = poseStack.last();
		BlockPos controllerPos = controller.getBlockPos();

		edges.forEach(edge -> {
			emitThickLineOrtho(vertexConsumer, pose, controllerPos, edge.a, edge.b, 1 / 32f, red, green, blue, 255);
		});
	}

	private void renderStorageBlocksOutline(ControllerBlockEntity controller, PoseStack poseStack, MultiBufferSource bufferSource) {
		renderThickEdges(controller, poseStack, bufferSource, 0x69c53b, controller.getStorageBlockEdges());
	}

	private void renderLineBetweenBlocks(BlockPos initialPos, BlockPos pos, VoxelShape shape, PoseStack poseStack, MultiBufferSource bufferSource, int color) {
		if (shape.isEmpty()) {
			return;
		}

		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;

		Vec3 center = shape.bounds().getCenter();

		VertexConsumer buffer = bufferSource.getBuffer(LINES);

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

	private void renderControllerOutline(ControllerBlockEntity controller, PoseStack poseStack, MultiBufferSource bufferSource) {
		renderThickEdges(controller, poseStack, bufferSource, 0x2ebbff, controller.getControllerEdges());
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
	
	@Override
	public AABB getRenderBoundingBox(ControllerBlockEntity blockEntity) {
		return new AABB(blockEntity.getBlockPos()).inflate(Config.SERVER.controllerRange.getAsInt());
	}

	public static void emitThickLineOrtho(
			VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
			Vec3 a, Vec3 b, float thickness,
			int r, int g, int bl, int alpha
	) {
		final float rh = thickness * 0.5f;

		// Axis-aligned unit along the segment (handles +/- direction)
		Vec3 d = b.subtract(a);
		Vec3 u = new Vec3(Math.signum(d.x), Math.signum(d.y), Math.signum(d.z));

		// Extend both ends by rh along the axis
		Vec3 aEx = a.subtract(u.scale(rh));
		Vec3 bEx = b.add(u.scale(rh));

		// Stable world-aligned cross section so adjacent segments match
		Vec3 v, w;
		if (u.x != 0) {
			v = new Vec3(0, 1, 0);
			w = new Vec3(0, 0, 1);
		} else if (u.y != 0) {
			v = new Vec3(1, 0, 0);
			w = new Vec3(0, 0, 1);
		} else {
			v = new Vec3(1, 0, 0);
			w = new Vec3(0, 1, 0);
		}

		Vec3 vOff = v.scale(rh), wOff = w.scale(rh);

		// 8 prism corners around extended endpoints
		Vec3 aVpWm = aEx.add(vOff).subtract(wOff);
		Vec3 aVpWp = aEx.add(vOff).add(wOff);
		Vec3 aVmWp = aEx.subtract(vOff).add(wOff);
		Vec3 aVmWm = aEx.subtract(vOff).subtract(wOff);

		Vec3 bVpWm = bEx.add(vOff).subtract(wOff);
		Vec3 bVpWp = bEx.add(vOff).add(wOff);
		Vec3 bVmWp = bEx.subtract(vOff).add(wOff);
		Vec3 bVmWm = bEx.subtract(vOff).subtract(wOff);

		// 4 side faces (POSITION_COLOR, no normals needed)
		emitQuad(vc, pose, origin, aVpWm, aVpWp, bVpWp, bVpWm, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVmWp, aVmWm, bVmWm, bVmWp, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVmWp, aVpWp, bVpWp, bVmWp, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVpWm, aVmWm, bVmWm, bVpWm, r, g, bl, alpha);
	}

	private static void emitQuad(
			VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
			Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
			int r, int g, int b, int a
	) {
		add(vc, pose, origin, p0, r, g, b, a);
		add(vc, pose, origin, p1, r, g, b, a);
		add(vc, pose, origin, p2, r, g, b, a);
		add(vc, pose, origin, p3, r, g, b, a);
	}

	private static void add(VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
							Vec3 p, int r, int g, int b, int a) {
		vc.addVertex(pose,
						(float) (p.x - origin.getX()),
						(float) (p.y - origin.getY()),
						(float) (p.z - origin.getZ()))
				.setColor(r, g, b, a);
	}
}
