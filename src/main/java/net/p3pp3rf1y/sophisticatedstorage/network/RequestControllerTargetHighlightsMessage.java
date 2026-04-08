package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncBlockHighlightsMessage;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StoragePositionGroups;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record RequestControllerTargetHighlightsMessage(ItemStack stack,
													   List<BlockPos> controllerPositions) {
	public static final int MATCHING_STACK_HIGHLIGHT_COLOR = 0x4CAF50;
	public static final int MATCHING_ITEM_HIGHLIGHT_COLOR = 0x42A5F5;
	public static final int EMPTY_TARGET_HIGHLIGHT_COLOR = 0xFFEB3B;

	public static void encode(RequestControllerTargetHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeItemStack(msg.stack(), false);
		packetBuffer.writeCollection(msg.controllerPositions(), FriendlyByteBuf::writeBlockPos);
	}

	public static RequestControllerTargetHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestControllerTargetHighlightsMessage(packetBuffer.readItem(), packetBuffer.readList(FriendlyByteBuf::readBlockPos));
	}

	static void onMessage(RequestControllerTargetHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context.getSender()));
		context.setPacketHandled(true);
	}

	public static void handleMessage(RequestControllerTargetHighlightsMessage payload, @Nullable ServerPlayer player) {
		List<BlockPos> stackStorages = new ArrayList<>();
		List<BlockPos> itemStorages = new ArrayList<>();
		List<BlockPos> emptyTargetSlotStorages = new ArrayList<>();
		ItemStackKey stackKey = ItemStackKey.of(payload.stack());
		payload.controllerPositions().forEach(pos -> {
			Level level = player.level();
			WorldHelper.getLoadedBlockEntity(level, pos, ControllerBlockEntity.class).ifPresent(controller -> {
				stackStorages.addAll(controller.getStackStorages(stackKey));
				itemStorages.addAll(controller.getItemStorages(stackKey));
				emptyTargetSlotStorages.addAll(controller.getEmptyTargetSlotStorages(stackKey));
			});
		});
		PacketHandler.INSTANCE.sendToClient(player, new SyncBlockHighlightsMessage(
				Map.of(
						MATCHING_STACK_HIGHLIGHT_COLOR, StoragePositionGroups.getGroupPositions(player.level(), stackStorages),
						MATCHING_ITEM_HIGHLIGHT_COLOR, StoragePositionGroups.getGroupPositions(player.level(), itemStorages),
						EMPTY_TARGET_HIGHLIGHT_COLOR, StoragePositionGroups.getGroupPositions(player.level(), emptyTargetSlotStorages)
				)
		));
	}
}
