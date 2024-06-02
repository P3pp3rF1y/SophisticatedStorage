package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.world.WorldEvent;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;

import java.util.UUID;

public class ClientStorageContentsTooltip extends net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltip {
	private final ItemStack storageItem;

	@SuppressWarnings("unused") //parameter needs to be there so that addListener logic would know which event this method listens to
	public static void onWorldLoad(WorldEvent.Load event) {
		refreshContents();
		lastRequestTime = 0;
	}

	@Override
	public void renderImage(Font font, int leftX, int topY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
		storageItem.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(wrapper -> renderTooltip(wrapper, font, leftX, topY, poseStack, itemRenderer, blitOffset));
	}

	public ClientStorageContentsTooltip(StorageContentsTooltip tooltip) {
		storageItem = tooltip.getStorageItem();
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		StoragePacketHandler.INSTANCE.sendToServer(new RequestStorageContentsMessage(uuid));
	}
}
