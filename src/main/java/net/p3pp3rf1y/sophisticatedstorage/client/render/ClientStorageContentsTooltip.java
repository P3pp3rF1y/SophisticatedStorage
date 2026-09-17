package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.level.LevelEvent;
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
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		if (linkedStorageTooltip != null) {
			linkedStorageTooltip.extractImage(font, x, y, w, h, graphics);
			y += linkedStorageTooltip.getHeight(font);
		}
		extractTooltip(StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(), storageItem), font, x, y, graphics);
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
	public int getHeight(Font font) {
		return super.getHeight(font) + (linkedStorageTooltip == null ? 0 : linkedStorageTooltip.getHeight(font));
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		StorageBlockEntity.getLinkedStorageEndpointData(storageItem).ifPresentOrElse(
				endpoint -> ClientPacketDistributor.sendToServer(
						new RequestLinkedStorageContentsPayload(endpoint.groupId(), ClientLinkedStorageContents.getRevision(endpoint.groupId()).orElse(-1L))),
				() -> ClientPacketDistributor.sendToServer(new RequestStorageContentsPayload(uuid)));
	}
}
