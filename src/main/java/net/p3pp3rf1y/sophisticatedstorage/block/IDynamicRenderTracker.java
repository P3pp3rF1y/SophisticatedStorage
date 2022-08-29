package net.p3pp3rf1y.sophisticatedstorage.block;

import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;

public interface IDynamicRenderTracker {
	default void onRenderInfoUpdated(RenderInfo ri) {}

	default boolean isDynamicRenderer() {
		return false;
	}

	default boolean isFullyDynamicRenderer() {
		return false;
	}

	IDynamicRenderTracker NOOP = new IDynamicRenderTracker() {};
}
