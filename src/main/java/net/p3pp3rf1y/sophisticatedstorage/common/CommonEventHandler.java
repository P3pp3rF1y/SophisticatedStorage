package net.p3pp3rf1y.sophisticatedstorage.common;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsMessage;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.ISneakItemInteractionBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
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
		eventBus.addListener(this::onPlayerRespawn);
		eventBus.addListener(this::onBlockBreak);
		eventBus.addListener(this::onLimitedBarrelLeftClicked);
		eventBus.addListener(this::onSneakItemBlockInteraction);
	}

	private void onLimitedBarrelLeftClicked(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		if (!player.isCreative()) {
			return;
		}

		BlockPos pos = event.getPos();
		Level level = event.getLevel();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LimitedBarrelBlock limitedBarrel)) {
			return;
		}
		if (limitedBarrel.tryToTakeItem(state, level, pos, player)) {
			event.setCanceled(true);
		}
	}

	private void onSneakItemBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getEntity().isShiftKeyDown()) {
			return;
		}

		BlockPos pos = event.getPos();
		Level level = event.getLevel();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ISneakItemInteractionBlock sneakItemInteractionBlock)) {
			return;
		}
		if (sneakItemInteractionBlock.trySneakItemInteraction(event.getEntity(), event.getHand(), state, level, pos, event.getHitVec(), event.getItemStack())) {
			event.setCanceled(true);
		}
	}

	private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		sendPlayerSettingsToClient(event.getEntity());
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		sendPlayerSettingsToClient(event.getEntity());
	}

	private void sendPlayerSettingsToClient(Player player) {
		String playerTagName = StorageSettingsHandler.SOPHISTICATED_STORAGE_SETTINGS_PLAYER_TAG;
		PacketHandler.INSTANCE.sendToClient((ServerPlayer) player, new SyncPlayerSettingsMessage(playerTagName, SettingsManager.getPlayerSettingsTag(player, playerTagName)));
	}

	private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		sendPlayerSettingsToClient(event.getEntity());
	}

	private void onBlockBreak(BlockEvent.BreakEvent event) {
		Player player = event.getPlayer();
		if (!(event.getState().getBlock() instanceof WoodStorageBlockBase) || player.isShiftKeyDown()) {
			return;
		}

		Level level = player.level();
		WorldHelper.getBlockEntity(level, event.getPos(), WoodStorageBlockEntity.class).ifPresent(wbe -> {
			if (wbe.isPacked() || Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
				return;
			}

			AtomicInteger droppedItemEntityCount = new AtomicInteger(0);
			InventoryHelper.iterate(wbe.getStorageWrapper().getInventoryHandler(), (slot, stack) -> {
				if (stack.isEmpty()) {
					return;
				}
				droppedItemEntityCount.addAndGet((int) Math.ceil(stack.getCount() / (double) Math.min(stack.getMaxStackSize(), AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT)));
			});

			if (droppedItemEntityCount.get() > Config.SERVER.tooManyItemEntityDrops.get()) {
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
