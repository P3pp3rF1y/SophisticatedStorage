package net.p3pp3rf1y.sophisticatedstorage.block;

import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;

public interface IDynamicRenderTracker {
	default void onRenderDataUpdated(RenderDataHandler ri) {}

	default boolean isDynamicRenderer() {
		return false;
	}

	default boolean isFullyDynamicRenderer() {
		return false;
	}

	IDynamicRenderTracker NOOP = new IDynamicRenderTracker() {};
}
