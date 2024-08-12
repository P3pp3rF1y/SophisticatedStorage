package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.ArrayList;
import java.util.List;

public class DynamicRenderTracker implements IDynamicRenderTracker {
	private static final int DYNAMIC_CHECK_WINDOW_TICKS = 100;
	private static final int MAX_ITEM_CHANGES_IN_WINDOW = 4;

	private long lastItemChangeTime = 0;
	private final int[] itemChangeExpirationTimes = new int[MAX_ITEM_CHANGES_IN_WINDOW];
	private boolean dynamicRenderer = false;
	private boolean fullyDynamic = false;
	private final List<ItemStack> lastRenderedItems = new ArrayList<>();
	private final StorageBlockEntity storageBlockEntity;

	public DynamicRenderTracker(StorageBlockEntity storageBlockEntity) {
		this.storageBlockEntity = storageBlockEntity;
	}

	@Override
	public void onRenderInfoUpdated(RenderInfo ri) {
		if (getLevel().isClientSide) {
			RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = ri.getItemDisplayRenderInfo();
			List<RenderInfo.DisplayItem> displayItems = itemDisplayRenderInfo.getDisplayItems();
			if (displayItems.isEmpty()) {
				lastRenderedItems.clear();
				dynamicRenderer = false;
				return;
			}

			if (renderedItemsHaventChanged(displayItems)) {
				return;
			}

			updateDynamicFlags(displayItems);
		} else {
			WorldHelper.notifyBlockUpdate(storageBlockEntity);
		}
	}

	private void updateDynamicFlags(List<RenderInfo.DisplayItem> displayItems) {
		lastRenderedItems.clear();
		displayItems.forEach(displayItem -> lastRenderedItems.add(displayItem.getItem()));

		boolean wasDynamic = dynamicRenderer;
		boolean wasFullyDynamic = fullyDynamic;

		fullyDynamic = !displayItems.isEmpty();
		dynamicRenderer = false;
		for (var displayItem : displayItems) {
			if (hasItemModelCustomRenderer(displayItem.getItem())) {
				dynamicRenderer = true;
			} else {
				fullyDynamic = false;
			}
		}

		if (!updateItemChangeExpirations()) {
			dynamicRenderer = true;
			fullyDynamic = true;
		}

		if (dynamicRenderer != wasDynamic || fullyDynamic != wasFullyDynamic) {
			WorldHelper.notifyBlockUpdate(storageBlockEntity);
		}
	}

	private boolean renderedItemsHaventChanged(List<RenderInfo.DisplayItem> displayItems) {
		if (lastRenderedItems.size() != displayItems.size()) {
			return false;
		}
		for (int i = 0; i < lastRenderedItems.size(); i++) {
			if (!ItemStack.isSameItemSameComponents(lastRenderedItems.get(i), displayItems.get(i).getItem())) {
				return false;
			}
		}
		return true;
	}

	private boolean hasItemModelCustomRenderer(ItemStack item) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer itemRenderer = minecraft.getItemRenderer();
		BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
		return model.isCustomRenderer();
	}

	private boolean updateItemChangeExpirations() {
		boolean timeSet = false;
		int timeDiff = (int) Math.min(DYNAMIC_CHECK_WINDOW_TICKS, getLevel().getGameTime() - lastItemChangeTime);
		lastItemChangeTime = getLevel().getGameTime();
		for (int i = 0; i < itemChangeExpirationTimes.length; i++) {
			int val = Math.max(0, itemChangeExpirationTimes[i] - timeDiff);
			if (!timeSet && val == 0) {
				timeSet = true;
				val = DYNAMIC_CHECK_WINDOW_TICKS;
			}
			itemChangeExpirationTimes[i] = val;
		}
		return timeSet;
	}

	private Level getLevel() {
		//noinspection ConstantConditions - level is not null at the point this tracker is running
		return storageBlockEntity.getLevel();
	}

	@Override
	public boolean isDynamicRenderer() {
		return dynamicRenderer;
	}

	@Override
	public boolean isFullyDynamicRenderer() {
		return fullyDynamic;
	}
}
