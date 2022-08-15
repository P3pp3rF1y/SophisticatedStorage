package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;

public class BarrelRenderer implements BlockEntityRenderer<WoodStorageBlockEntity> {
	@Override
	public void render(WoodStorageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!blockEntity.hasDynamicRenderer() || blockEntity.isPacked()) {
			return;
		}

		DisplayItemRenderer.renderDisplayItem(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, 0.5, 0.51);
	}
}
