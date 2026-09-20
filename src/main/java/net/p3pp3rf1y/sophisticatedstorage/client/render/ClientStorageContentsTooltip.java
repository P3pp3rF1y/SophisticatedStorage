package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.RequestLinkedStorageContentsPayload;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.LinkedStorageTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsPayload;

import javax.annotation.Nullable;

import java.util.UUID;

public class ClientStorageContentsTooltip extends ClientStorageContentsTooltipBase {
	private final ItemStack storageItem;
	@Nullable
	private final ClientLinkedStorageTooltip linkedStorageTooltip;

	@SuppressWarnings("unused")
	// parameter needs to be there so that addListener logic would know which event this method listens to
	public static void onWorldLoad(LevelEvent.Load event) {
		refreshContents();
		ClientLinkedStorageContents.clear();
		lastRequestTime = 0;
	}

	@Override
	public void renderImage(Font font, int leftX, int topY, GuiGraphics guiGraphics) {
		if (linkedStorageTooltip != null) {
			linkedStorageTooltip.renderImage(font, leftX, topY, guiGraphics);
			topY += linkedStorageTooltip.getHeight();
		}
		// noinspection DataFlowIssue - level definitely exists here
		renderTooltip(StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(), storageItem), font, leftX, topY, guiGraphics);
	}

	public ClientStorageContentsTooltip(StorageContentsTooltip tooltip) {
		storageItem = tooltip.getStorageItem();
		LinkedStorageTooltip linkedStorageTooltip = tooltip.linkedStorageTooltip();
		this.linkedStorageTooltip = linkedStorageTooltip == null ? null : new ClientLinkedStorageTooltip(linkedStorageTooltip);
	}

	@Override
	public int getWidth(Font font) {
		return linkedStorageTooltip == null ? super.getWidth(font) : Math.max(super.getWidth(font), linkedStorageTooltip.getWidth(font));
	}

	@Override
	public int getHeight() {
		return super.getHeight() + (linkedStorageTooltip == null ? 0 : linkedStorageTooltip.getHeight());
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		StorageBlockEntity.getLinkedStorageEndpointData(storageItem).ifPresentOrElse(
				endpoint -> PacketDistributor.sendToServer(
						new RequestLinkedStorageContentsPayload(endpoint.groupId(), ClientLinkedStorageContents.getRevision(endpoint.groupId()).orElse(-1L))),
				() -> PacketDistributor.sendToServer(new RequestStorageContentsPayload(uuid)));
	}
}
