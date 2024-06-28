package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

public class OpenStorageInventoryPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "open_storage_inventory");
	private final BlockPos pos;

	public OpenStorageInventoryPacket(BlockPos pos) {
		this.pos = pos;
	}

	public OpenStorageInventoryPacket(FriendlyByteBuf buffer) {
		this(buffer.readBlockPos());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		player.openMenu(
				new SimpleMenuProvider(
						(w, p, pl) -> instantiateContainerMenu(w, pl),
						WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty())
				),
				pos
		);
	}

	private StorageContainerMenu instantiateContainerMenu(int windowId, Player player) {
		if (player.level().getBlockState(pos).getBlock() instanceof LimitedBarrelBlock) {
			return new LimitedBarrelContainerMenu(windowId, player, pos);
		} else {
			return new StorageContainerMenu(windowId, player, pos);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
