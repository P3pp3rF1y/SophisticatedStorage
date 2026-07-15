package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;

import java.nio.ByteBuffer;

public class StorageBlockPreviewPipRenderer extends PictureInPictureRenderer<StorageBlockPreviewRenderState> {
	private static final Vector3f ITEM_DISPLAY_PREVIEW_LIGHT_0 = new Vector3f(-0.65F, 0.25F, 0.8F).normalize();
	private static final Vector3f ITEM_DISPLAY_PREVIEW_LIGHT_1 = new Vector3f(-0.15F, 0.05F, 1F).normalize();

	@Nullable
	private GpuBuffer previewLightingBuffer;

	public StorageBlockPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<StorageBlockPreviewRenderState> getRenderStateClass() {
		return StorageBlockPreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(StorageBlockPreviewRenderState renderState, PoseStack poseStack) {
		setupPreviewLighting();
		poseStack.mulPose(Axis.XN.rotationDegrees(-renderState.xAxisRotation()));
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yAxisRotation()));
		poseStack.scale(renderState.previewScale(), -renderState.previewScale(), -renderState.previewScale());
		applyStoragePreviewCounterTransform(poseStack, renderState.storageBlockEntity().getBlockState());
		poseStack.translate(-0.5, -0.5, -0.5);
		BlockPos otherStorageOffset = renderState.otherStorageOffset();
		if (otherStorageOffset != null) {
			poseStack.translate(-otherStorageOffset.getX() / 2D, 0, -otherStorageOffset.getZ() / 2D);
		}

		StorageBlockPreviewRenderer.render(renderState.storageBlockEntity(), renderState.partialTicks(), poseStack, bufferSource, 15728880,
				OverlayTexture.NO_OVERLAY);
		if (renderState.otherStorageBlockEntity() != null && otherStorageOffset != null) {
			poseStack.pushPose();
			poseStack.translate(otherStorageOffset.getX(), 0, otherStorageOffset.getZ());
			StorageBlockPreviewRenderer.render(renderState.otherStorageBlockEntity(), renderState.partialTicks(), poseStack, bufferSource, 15728880,
					OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}
	}

	@Override
	protected boolean textureIsReadyToBlit(StorageBlockPreviewRenderState renderState) {
		return false;
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2.0F;
	}

	@Override
	protected String getTextureLabel() {
		return "storage_block_preview";
	}

	@Override
	public void close() {
		super.close();
		if (previewLightingBuffer != null) {
			previewLightingBuffer.close();
		}
	}

	private void setupPreviewLighting() {
		if (previewLightingBuffer == null) {
			previewLightingBuffer = RenderSystem.getDevice().createBuffer(() -> "Storage item display preview lighting", 136, Lighting.UBO_SIZE);
			try (MemoryStack memoryStack = MemoryStack.stackPush()) {
				ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(ITEM_DISPLAY_PREVIEW_LIGHT_0)
						.putVec3(ITEM_DISPLAY_PREVIEW_LIGHT_1).get();
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(previewLightingBuffer.slice(0, Lighting.UBO_SIZE), byteBuffer);
			}
		}
		RenderSystem.setShaderLights(previewLightingBuffer.slice(0, Lighting.UBO_SIZE));
	}

	private void applyStoragePreviewCounterTransform(PoseStack poseStack, BlockState state) {
		if (state.getBlock() instanceof LimitedBarrelBlock) {
			VerticalFacing verticalFacing = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
			if (verticalFacing != VerticalFacing.NO) {
				poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(verticalFacing.getDirection()).conjugate());
			}
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(state.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)).conjugate());
		} else if (state.hasProperty(BarrelBlock.FACING)) {
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(state.getValue(BarrelBlock.FACING)).conjugate());
		} else if (state.hasProperty(ShulkerBoxBlock.FACING)) {
			poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(state.getValue(ShulkerBoxBlock.FACING)).conjugate());
		} else if (state.hasProperty(ChestBlock.FACING)) {
			poseStack.mulPose(Axis.YP.rotationDegrees(state.getValue(ChestBlock.FACING).toYRot() - Direction.NORTH.toYRot()));
		}
	}
}
