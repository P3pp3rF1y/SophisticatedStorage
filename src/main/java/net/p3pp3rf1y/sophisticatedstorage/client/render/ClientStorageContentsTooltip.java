package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsPayload;

import java.util.UUID;

public class ClientStorageContentsTooltip extends ClientStorageContentsTooltipBase {
	private final ItemStack storageItem;

	@SuppressWarnings("unused")
	//parameter needs to be there so that addListener logic would know which event this method listens to
	public static void onWorldLoad(LevelEvent.Load event) {
		refreshContents();
		lastRequestTime = 0;
	}

	public void renderImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor guiGraphics) {
		//noinspection DataFlowIssue - level definitely exists here
		extractTooltip(StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(), storageItem), font, x, y, guiGraphics);
	}

	public ClientStorageContentsTooltip(StorageContentsTooltip tooltip) {
		storageItem = tooltip.getStorageItem();
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		ClientPacketDistributor.sendToServer(new RequestStorageContentsPayload(uuid));
	}
}
