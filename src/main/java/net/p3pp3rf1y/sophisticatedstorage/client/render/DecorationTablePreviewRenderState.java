package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;

public record DecorationTablePreviewRenderState(TrackingItemStackRenderState itemStackRenderState, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea,
		int x0, int y0, int x1, int y1) implements PictureInPictureRenderState {
	@Override
	public float scale() {
		return 16;
	}

	@Override
	@Nullable
	public ScreenRectangle bounds() {
		return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
	}
}
