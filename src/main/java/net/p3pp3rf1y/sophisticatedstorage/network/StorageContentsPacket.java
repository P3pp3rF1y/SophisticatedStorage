package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;

import javax.annotation.Nullable;
import java.util.UUID;

public class StorageContentsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "storage_contents");
	private final UUID shulkerBoxUuid;
	@Nullable
	private final CompoundTag contents;

	public StorageContentsPacket(UUID shulkerBoxUuid, @Nullable CompoundTag contents) {
		this.shulkerBoxUuid = shulkerBoxUuid;
		this.contents = contents;
	}

	public StorageContentsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID(), buffer.readNbt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(this::handlePacket);
	}

	private void handlePacket() {
		if (contents == null) {
			return;
		}

		ItemContentsStorage.get().setStorageContents(shulkerBoxUuid, contents);
		ClientStorageContentsTooltipBase.refreshContents();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(shulkerBoxUuid);
		buffer.writeNbt(contents);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
