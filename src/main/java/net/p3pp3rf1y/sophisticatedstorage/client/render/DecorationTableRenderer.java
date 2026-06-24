package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

public class DecorationTableRenderer implements BlockEntityRenderer<DecorationTableBlockEntity> {
	private final ItemRenderer itemRenderer;

	public DecorationTableRenderer(BlockEntityRendererProvider.Context context) {
		itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(DecorationTableBlockEntity table, float v, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			Vec3 cameraPos) {
		if (table.getResult().isEmpty() || table.getResult().getItem() == ModItems.PAINTBRUSH.get()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.5, 1.125, 0.5);
		poseStack.mulPose(table.getBlockState().getValue(DecorationTableBlock.FACING).getOpposite().getRotation());
		poseStack.mulPose(Axis.XN.rotationDegrees(90));
		poseStack.translate(0, 0, -0.1);
		poseStack.scale(0.5f, 0.5f, 0.5f);
		itemRenderer.renderStatic(table.getResult(), ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, table.getLevel(), 0);

		poseStack.popPose();
	}
}
