package net.p3pp3rf1y.sophisticatedstorage.common;

import com.google.common.collect.Queues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncPlayerSettingsPayload;
import net.p3pp3rf1y.sophisticatedcore.settings.main.PlayerMainSettingsSavedData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.infinity.InfinityUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class CommonEventHandler {
	private static final int AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT = 20;

	private final Queue<TickTask> pendingTickTasks = Queues.newConcurrentLinkedQueue();

	public void registerHandlers() {
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(this::onPlayerChangedDimension);
		eventBus.addListener(this::onPlayerRespawn);
		eventBus.addListener(this::handleTooManyDropsBreak);
		eventBus.addListener(this::handleBreakStorageWithInfinityUpgrade);
		eventBus.addListener(this::onLimitedBarrelLeftClicked);
		eventBus.addListener(this::onSneakItemBlockInteraction);
		eventBus.addListener(this::onLevelTick);
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

	private void sendPlayerSettingsToClient(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			String name = SophisticatedStorage.MOD_ID;
			PacketDistributor.sendToPlayer(serverPlayer, new SyncPlayerSettingsPayload(name, PlayerMainSettingsSavedData.get().get(player.getUUID(), name)));
		}
	}

	private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		sendPlayerSettingsToClient(event.getEntity());
	}

	private void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel) || pendingTickTasks.isEmpty()) {
			return;
		}

		Iterator<TickTask> it = pendingTickTasks.iterator();

		while (it.hasNext()) {
			TickTask tickTask = it.next();
			if (tickTask.getTick() <= serverLevel.getServer().getTickCount()) {
				tickTask.run();
				it.remove();
			}
		}
	}

	private void handleBreakStorageWithInfinityUpgrade(BreakBlockEvent event) {
		Player player = event.getPlayer();

		if (!(event.getState().getBlock() instanceof StorageBlockBase)) {
			return;
		}

		WorldHelper.getBlockEntity(event.getLevel(), event.getPos(), StorageBlockEntity.class)
				.ifPresent(storageBlockEntity -> {
					if (storageBlockEntity.getStorageWrapper().getUpgradeHandler().getTypeWrappers(InfinityUpgradeItem.TYPE).stream().anyMatch(w -> !w.checkPermission(player))) {
						event.setCanceled(true);
						if (!event.getLevel().isClientSide()) {
							event.setNotifyClient(true);
							player.sendOverlayMessage(StorageTranslationHelper.INSTANCE.translStatusMessage("infinity_upgrade_only_admin_break").withStyle(ChatFormatting.RED));
						}
						scheduleRenderUpdate(storageBlockEntity, event.getLevel(), event.getPos(), event.getState());
					}
				});
	}

	private void handleTooManyDropsBreak(BreakBlockEvent event) {
		Player player = event.getPlayer();
		if (!(event.getState().getBlock() instanceof WoodStorageBlockBase) || player.isShiftKeyDown()) {
			return;
		}

		Level level = player.level();
		BlockPos pos = event.getPos();
		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(wbe -> {
			if (wbe.isPacked() || Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
				return;
			}

			AtomicInteger droppedItemEntityCount = new AtomicInteger(0);

			int startCountingFromSlot;
			InventoryHandler inventoryHandler;
			if (wbe instanceof ChestBlockEntity cbe && !cbe.isMainChest() && level.getBlockState(pos).getBlock() instanceof ChestBlock chestBlock) {
				startCountingFromSlot = chestBlock.getNumberOfInventorySlots();
				inventoryHandler = cbe.getMainStorageWrapper().getInventoryHandler();
			} else {
				startCountingFromSlot = 0;
				inventoryHandler = wbe.getStorageWrapper().getInventoryHandler();
			}

			InventoryHelper.iterate(inventoryHandler, (slot, stack) -> {
				if (stack.isEmpty() || slot < startCountingFromSlot) {
					return;
				}
				droppedItemEntityCount.addAndGet((int) Math.ceil(stack.getCount() / (double) Math.min(stack.getMaxStackSize(), AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT)));
			}, () -> false, false);

			if (droppedItemEntityCount.get() > Config.SERVER.tooManyItemEntityDrops.get()) {
				event.setCanceled(true);
				if (!event.getLevel().isClientSide()) {
					event.setNotifyClient(true);
				}
				ItemBase packingTapeItem = ModItems.PACKING_TAPE.get();
				Component packingTapeItemName = packingTapeItem.getName(new ItemStack(packingTapeItem)).copy().withStyle(ChatFormatting.GREEN);
				BlockState state = event.getState();
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(StorageTranslationHelper.INSTANCE.translStatusMessage("too_many_item_entity_drops",
							state.getBlock().getCloneItemStack(player.level(), pos, state, true, player).getHoverName().copy().withStyle(ChatFormatting.GREEN),
							Component.literal(String.valueOf(droppedItemEntityCount.get())).withStyle(ChatFormatting.RED),
							packingTapeItemName)
					);
				}
				scheduleRenderUpdate(wbe, level, pos, state);
			}
		});
	}

	private void scheduleRenderUpdate(StorageBlockEntity storageBe, LevelAccessor level, BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.scheduleTick(pos, state.getBlock(), 2);
			pendingTickTasks.add(new TickTask(serverLevel.getServer().getTickCount() + 2, () -> {
				storageBe.setUpdateBlockRender();
				WorldHelper.notifyBlockUpdate(storageBe);
			}));
		}
	}
}
