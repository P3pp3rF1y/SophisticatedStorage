package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record StorageBlockPreviewRenderState(StorageBlockEntity storageBlockEntity, @Nullable StorageBlockEntity otherStorageBlockEntity,
		@Nullable BlockPos otherStorageOffset, float xAxisRotation, float yAxisRotation, float previewScale, float partialTicks, Matrix3x2f pose,
		@Nullable ScreenRectangle scissorArea, int x0, int y0, int x1, int y1) implements PictureInPictureRenderState {
	@Override
	public float scale() {
		return 16.0F;
	}

	@Override
	public @Nullable ScreenRectangle bounds() {
		return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
	}
}
