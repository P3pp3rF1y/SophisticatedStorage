package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class OpenStorageInventoryMessage {
	private final BlockPos pos;

	public OpenStorageInventoryMessage(BlockPos pos) {
		this.pos = pos;
	}

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

	static void handleMessage(@Nullable ServerPlayer player, OpenStorageInventoryMessage msg) {
		if (player == null) {
			return;
		}

		if (!(player.containerMenu instanceof StorageSettingsContainerMenu settingsContainerMenu) || !settingsContainerMenu.getBlockPosition().equals(msg.pos)
				|| !settingsContainerMenu.stillValid(player)) {
			return;
		}

		settingsContainerMenu.transferOpenersToStorageMenu();
		WorldHelper.getBlockEntity(player.level(), msg.pos, StorageBlockEntity.class).ifPresent(storage -> {
			NetworkHooks.openScreen(player, new SimpleMenuProvider((w, p, pl) -> instantiateContainerMenu(msg, w, pl, true), storage.getMenuDisplayName()),
					buffer -> writeMenuData(player, msg.pos, buffer));
		});
	}

	private static void writeMenuData(ServerPlayer player, BlockPos pos, FriendlyByteBuf buffer) {
		if (player.level().getBlockState(pos).getBlock() instanceof LimitedBarrelBlock) {
			buffer.writeBlockPos(pos);
		} else {
			StorageContainerMenu.writeMenuData(buffer, player, pos);
		}
	}

	private static StorageContainerMenu instantiateContainerMenu(OpenStorageInventoryMessage msg, int windowId, Player player, boolean openersAlreadyActive) {
		if (player.level().getBlockState(msg.pos).getBlock() instanceof LimitedBarrelBlock) {
			return new LimitedBarrelContainerMenu(windowId, player, msg.pos, openersAlreadyActive);
		} else {
			return new StorageContainerMenu(windowId, player, msg.pos, openersAlreadyActive);
		}
	}
}
