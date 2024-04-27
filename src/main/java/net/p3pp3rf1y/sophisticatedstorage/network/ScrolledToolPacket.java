package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

public class ScrolledToolPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedStorage.MOD_ID, "scrolled_tool");
	private final boolean next;

	public ScrolledToolPacket(boolean next) {
		this.next = next;
	}

	public ScrolledToolPacket(FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() == ModItems.STORAGE_TOOL.get()) {
			StorageToolItem.cycleMode(stack, next);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(next);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
