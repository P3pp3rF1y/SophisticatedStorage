package net.p3pp3rf1y.sophisticatedstorage.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsMessage;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class CommonEventHandler {
	private static final int AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT = 20;

	public void registerHandlers() {
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(this::onPlayerLoggedIn);
		eventBus.addListener(this::onPlayerChangedDimension);
		eventBus.addListener(this::onBlockBreak);
	}

	private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		SophisticatedCore.PACKET_HANDLER.sendToClient((ServerPlayer) event.getEntity(), new SyncPlayerSettingsMessage(playerTagName, SettingsManager.getPlayerSettingsTag(event.getEntity(), playerTagName)));
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		SophisticatedCore.PACKET_HANDLER.sendToClient((ServerPlayer) event.getEntity(), new SyncPlayerSettingsMessage(playerTagName, SettingsManager.getPlayerSettingsTag(event.getEntity(), playerTagName)));
	}

	private void onBlockBreak(BlockEvent.BreakEvent event) {
		Player player = event.getPlayer();
		if (!(event.getState().getBlock() instanceof WoodStorageBlockBase) || player.isShiftKeyDown()) {
			return;
		}

		Level level = player.getLevel();
		WorldHelper.getBlockEntity(level, event.getPos(), WoodStorageBlockEntity.class).ifPresent(wbe -> {
			if (wbe.isPacked()) {
				return;
			}

			AtomicInteger droppedItemEntityCount = new AtomicInteger(0);
			InventoryHelper.iterate(wbe.getStorageWrapper().getInventoryHandler(), (slot, stack) -> {
				if (stack.isEmpty()) {
					return;
				}
				droppedItemEntityCount.addAndGet((int) Math.ceil(stack.getCount() / (double) Math.min(stack.getMaxStackSize(), AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT)));
			});

			if (droppedItemEntityCount.get() > Config.COMMON.tooManyItemEntityDrops.get()) {
				event.setCanceled(true);
				ItemBase packingTapeItem = ModItems.PACKING_TAPE.get();
				Component packingTapeItemName = packingTapeItem.getName(new ItemStack(packingTapeItem)).copy().withStyle(ChatFormatting.GREEN);
				player.sendSystemMessage(StorageTranslationHelper.INSTANCE.translStatusMessage("too_many_item_entity_drops",
						event.getState().getBlock().getName().withStyle(ChatFormatting.GREEN),
						Component.literal(String.valueOf(droppedItemEntityCount.get())).withStyle(ChatFormatting.RED),
						packingTapeItemName)
				);
			}
		});
	}
}
