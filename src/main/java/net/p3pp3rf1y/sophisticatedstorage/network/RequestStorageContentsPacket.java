package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import java.util.UUID;

public class RequestStorageContentsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "request_storage_contents");
	private final UUID storageUuid;

	public RequestStorageContentsPacket(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public RequestStorageContentsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		PacketHelper.sendToPlayer(new StorageContentsPacket(storageUuid, ItemContentsStorage.get().getOrCreateStorageContents(storageUuid)), serverPlayer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(storageUuid);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
