package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

public class DecorationTableRenderer implements BlockEntityRenderer<DecorationTableBlockEntity, DecorationTableRenderer.DecorationTableRenderState> {
	private final ItemModelResolver itemModelResolver;

	public DecorationTableRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public DecorationTableRenderState createRenderState() {
		return new DecorationTableRenderState();
	}

	@Override
	public void extractRenderState(DecorationTableBlockEntity blockEntity, DecorationTableRenderState renderState, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		ItemStack result = blockEntity.getResult();
		if (!result.isEmpty() && result.getItem() != ModItems.PAINTBRUSH.get()) {
			itemModelResolver.updateForTopItem(renderState.result, result, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
		}
		renderState.facing = blockEntity.getBlockState().getValue(DecorationTableBlock.FACING);
	}

	@Override
	public void submit(DecorationTableRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState) {
		if (renderState.result.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.5, 1.125, 0.5);
		poseStack.mulPose(renderState.facing.getOpposite().getRotation());
		poseStack.mulPose(Axis.XN.rotationDegrees(90));
		poseStack.translate(0, 0, -0.1);
		poseStack.scale(0.5f, 0.5f, 0.5f);
		renderState.result.submit(poseStack, submitNodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	public static class DecorationTableRenderState extends BlockEntityRenderState {
		public ItemStackRenderState result = new ItemStackRenderState();
		public Direction facing;
	}
}
