package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.network.SyncItemHighlightsPayload;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;

import java.util.ArrayList;
import java.util.List;

public record RequestControllerTargetHighlightsPayload(ItemStack stack,
													   List<BlockPos> controllerPositions) implements CustomPacketPayload {
	public static final Type<RequestControllerTargetHighlightsPayload> TYPE = new Type<>(SophisticatedStorage.getRL("request_controller_target_highlights"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestControllerTargetHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			RequestControllerTargetHighlightsPayload::stack,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			RequestControllerTargetHighlightsPayload::controllerPositions,
			RequestControllerTargetHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestControllerTargetHighlightsPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (player instanceof ServerPlayer serverPlayer) {
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
			PacketDistributor.sendToPlayer(serverPlayer, new SyncItemHighlightsPayload(stackStorages, itemStorages, emptyTargetSlotStorages));
		}
	}
}
