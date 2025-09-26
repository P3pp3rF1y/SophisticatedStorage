package net.p3pp3rf1y.sophisticatedstorage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestControllerTargetHighlightsMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;

import java.util.List;

public class ControllerTargetHighlighter {
	private static final int HIGHLIGHT_CHECK_INTERVAL = 30;
	private static int highlightCooldown = 0;
	private static ItemStack lastHighlightedStack = ItemStack.EMPTY;

	private ControllerTargetHighlighter() {
	}

	public static void highlightTargets() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || (--highlightCooldown > 0 && ItemStack.isSameItemSameTags(lastHighlightedStack, player.getMainHandItem()))) {
			return;
		}
		highlightCooldown = HIGHLIGHT_CHECK_INTERVAL;
		if (player.getOffhandItem().getItem() == ModItems.STORAGE_TOOL.get() && !player.getMainHandItem().isEmpty()) {
			List<BlockPos> controllerPositions = WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), Config.SERVER.controllerRange.get(), ControllerBlockEntity.class).stream().map(ControllerBlockEntity::getBlockPos).toList();
			StoragePacketHandler.INSTANCE.sendToServer(new RequestControllerTargetHighlightsMessage(player.getMainHandItem(), controllerPositions));
			lastHighlightedStack = player.getMainHandItem();
		}
	}
}
