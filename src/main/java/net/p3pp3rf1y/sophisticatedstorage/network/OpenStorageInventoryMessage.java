package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class OpenStorageInventoryMessage {
	private final BlockPos pos;

	public OpenStorageInventoryMessage(BlockPos pos) {this.pos = pos;}

	public static void encode(OpenStorageInventoryMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBlockPos(msg.pos);
	}

	public static OpenStorageInventoryMessage decode(FriendlyByteBuf packetBuffer) {
		return new OpenStorageInventoryMessage(packetBuffer.readBlockPos());
	}

	static void onMessage(OpenStorageInventoryMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, OpenStorageInventoryMessage msg) {
		if (player == null) {
			return;
		}

		NetworkHooks.openGui(player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, msg.pos),
				WorldHelper.getBlockEntity(player.level, msg.pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(TextComponent.EMPTY)), msg.pos);
	}
}
