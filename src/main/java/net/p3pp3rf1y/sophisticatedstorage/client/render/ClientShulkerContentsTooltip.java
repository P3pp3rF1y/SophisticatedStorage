package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.world.WorldEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestShulkerBoxContentsMessage;

import java.util.UUID;

public class ClientShulkerContentsTooltip extends net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltip {
	private final ItemStack shulkerItem;

	@SuppressWarnings("unused") //parameter needs to be there so that addListener logic would know which event this method listens to
	public static void onWorldLoad(WorldEvent.Load event) {
		refreshContents();
		lastRequestTime = 0;
	}

	@Override
	public void renderImage(Font font, int leftX, int topY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
		shulkerItem.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(wrapper -> renderTooltip(wrapper, font, leftX, topY, poseStack, itemRenderer, blitOffset));
	}

	public ClientShulkerContentsTooltip(ShulkerBoxItem.ContentsTooltip tooltip) {
		shulkerItem = tooltip.getShulkerItem();
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		SophisticatedStorage.PACKET_HANDLER.sendToServer(new RequestShulkerBoxContentsMessage(uuid));
	}
}
