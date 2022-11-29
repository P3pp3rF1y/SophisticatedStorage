package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ScrolledToolMessage {
	private final boolean next;

	public ScrolledToolMessage(boolean next) {
		this.next = next;
	}

	public static void encode(ScrolledToolMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(msg.next);
	}

	public static ScrolledToolMessage decode(FriendlyByteBuf packetBuffer) {
		return new ScrolledToolMessage(packetBuffer.readBoolean());
	}

	static void onMessage(ScrolledToolMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, ScrolledToolMessage msg) {
		if (player == null) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() == ModItems.STORAGE_TOOL.get()) {
			StorageToolItem.cycleMode(stack, msg.next);
		}
	}
}
