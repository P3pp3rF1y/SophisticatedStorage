package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.function.Supplier;

public class DynamicRenderTracker implements IDynamicRenderTracker {
	private static final int DYNAMIC_CHECK_WINDOW_TICKS = 100;
	private static final int MAX_ITEM_CHANGES_IN_WINDOW = 4;

	private long lastItemChangeTime = 0;
	private final int[] itemChangeExpirationTimes = new int[MAX_ITEM_CHANGES_IN_WINDOW];
	private boolean dynamicRenderer = false;
	private ItemStack lastRenderItem = ItemStack.EMPTY;
	private final StorageBlockEntity storageBlockEntity;

	public DynamicRenderTracker(StorageBlockEntity storageBlockEntity) {
		this.storageBlockEntity = storageBlockEntity;
	}

	@Override
	public void onRenderInfoUpdated(RenderInfo ri) {
		if (getLevel().isClientSide) {
			ItemStack item = ri.getItemDisplayRenderInfo().getItem();
			if (ItemHandlerHelper.canItemStacksStack(lastRenderItem, item)) {
				return;
			}
			lastRenderItem = item;

			boolean wasDynamic = dynamicRenderer;
			checkForItemModelCustomRenderer(item);

			if (!updateItemChangeExpirations()) {
				dynamicRenderer = true;
			}

			if (!dynamicRenderer || !wasDynamic) {
				WorldHelper.notifyBlockUpdate(storageBlockEntity);
			}
		} else {
			WorldHelper.notifyBlockUpdate(storageBlockEntity);
		}
	}

	private void checkForItemModelCustomRenderer(ItemStack item) {
		Supplier<Runnable> modelUpdate = () -> () -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemRenderer itemRenderer = minecraft.getItemRenderer();
			BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
			dynamicRenderer = model.isCustomRenderer();
		};
		modelUpdate.get().run();
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
		return IDynamicRenderTracker.super.isDynamicRenderer();
	}
}
