package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

public class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity> {
	private final DisplayItemRenderer displayItemRenderer = new DisplayItemRenderer(0.5, 0.5);
	@Override
	public void render(BarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.hasDynamicRenderer() || blockEntity.isPacked()) {
			return;
		}

		displayItemRenderer.renderDisplayItems(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, !blockEntity.hasFullyDynamicRenderer());
	}

	@Override
	public int getViewDistance() {
		return 32;
	}
}
