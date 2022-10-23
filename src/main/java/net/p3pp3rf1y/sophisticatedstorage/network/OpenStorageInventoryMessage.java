package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
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

		NetworkHooks.openScreen(player, new SimpleMenuProvider((w, p, pl) -> instantiateContainerMenu(msg, w, pl),
				WorldHelper.getBlockEntity(player.level, msg.pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty())), msg.pos);
	}

	private static StorageContainerMenu instantiateContainerMenu(OpenStorageInventoryMessage msg, int windowId, Player player) {
		if (player.level.getBlockState(msg.pos).getBlock() instanceof LimitedBarrelBlock) {
			return new LimitedBarrelContainerMenu(windowId, player, msg.pos);
		} else {
			return new StorageContainerMenu(windowId, player, msg.pos);
		}
	}
}
