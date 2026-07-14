package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;

public record OpenStorageInventoryPayload(BlockPos pos) implements CustomPacketPayload {
	public static final Type<OpenStorageInventoryPayload> TYPE = new Type<>(SophisticatedStorage.getRL("open_storage_inventory"));
	public static final StreamCodec<ByteBuf, OpenStorageInventoryPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
			OpenStorageInventoryPayload::pos, OpenStorageInventoryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(OpenStorageInventoryPayload payload, IPayloadContext context) {
		Player player = context.player();
		boolean shouldTransferOpeners = player.containerMenu instanceof StorageSettingsContainerMenu settingsContainerMenu
				&& settingsContainerMenu.getBlockPosition().equals(payload.pos);
		if (shouldTransferOpeners) {
			StorageSettingsContainerMenu settingsContainerMenu = (StorageSettingsContainerMenu) player.containerMenu;
			settingsContainerMenu.transferOpenersToStorageMenu();
		}
		boolean openersAlreadyActive = shouldTransferOpeners;

		player.openMenu(new SophisticatedMenuProvider((w, p, pl) -> instantiateContainerMenu(w, pl, payload.pos, openersAlreadyActive), WorldHelper
				.getBlockEntity(player.level(), payload.pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty()),
				false), payload.pos);
	}

	private static StorageContainerMenu instantiateContainerMenu(int windowId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		if (player.level().getBlockState(pos).getBlock() instanceof LimitedBarrelBlock) {
			return new LimitedBarrelContainerMenu(windowId, player, pos, openersAlreadyActive);
		} else {
			return new StorageContainerMenu(windowId, player, pos, openersAlreadyActive);
		}
	}
}
