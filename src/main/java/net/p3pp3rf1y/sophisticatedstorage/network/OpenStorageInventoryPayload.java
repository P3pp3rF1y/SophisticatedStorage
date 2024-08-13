package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

public record OpenStorageInventoryPayload(BlockPos pos) implements CustomPacketPayload {
	public static final Type<OpenStorageInventoryPayload> TYPE = new Type<>(SophisticatedCore.getRL("open_storage_inventory"));
	public static final StreamCodec<ByteBuf, OpenStorageInventoryPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			OpenStorageInventoryPayload::pos,
			OpenStorageInventoryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(OpenStorageInventoryPayload payload, IPayloadContext context) {
		context.player().openMenu(
				new SophisticatedMenuProvider(
						(w, p, pl) -> instantiateContainerMenu(w, pl, payload.pos),
						WorldHelper.getBlockEntity(context.player().level(), payload.pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty()),
						false
				),
				payload.pos
		);
	}

	private static StorageContainerMenu instantiateContainerMenu(int windowId, Player player, BlockPos pos) {
		if (player.level().getBlockState(pos).getBlock() instanceof LimitedBarrelBlock) {
			return new LimitedBarrelContainerMenu(windowId, player, pos);
		} else {
			return new StorageContainerMenu(windowId, player, pos);
		}
	}
}
