package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OpenStorageInventoryMessageTest {
	private static final BlockPos STORAGE_POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void handleMessageDoesNotTransferOpenersWhenSettingsMenuPositionDoesNotMatch() {
		ServerPlayer player = mock(ServerPlayer.class);
		StorageSettingsContainerMenu settingsMenu = mock(StorageSettingsContainerMenu.class);
		player.containerMenu = settingsMenu;
		when(settingsMenu.getBlockPosition()).thenReturn(STORAGE_POS.above());

		OpenStorageInventoryMessage.handleMessage(player, new OpenStorageInventoryMessage(STORAGE_POS));

		verify(settingsMenu, never()).stillValid(player);
		verify(settingsMenu, never()).transferOpenersToStorageMenu();
	}

	@Test
	void handleMessageDoesNotTransferOpenersWhenSettingsMenuIsInvalid() {
		ServerPlayer player = mock(ServerPlayer.class);
		StorageSettingsContainerMenu settingsMenu = mock(StorageSettingsContainerMenu.class);
		player.containerMenu = settingsMenu;
		when(settingsMenu.getBlockPosition()).thenReturn(STORAGE_POS);
		when(settingsMenu.stillValid(player)).thenReturn(false);

		OpenStorageInventoryMessage.handleMessage(player, new OpenStorageInventoryMessage(STORAGE_POS));

		verify(settingsMenu, never()).transferOpenersToStorageMenu();
	}

	@Test
	void handleMessageTransfersOpenersForValidCurrentSettingsMenu() {
		ServerPlayer player = mock(ServerPlayer.class);
		StorageSettingsContainerMenu settingsMenu = mock(StorageSettingsContainerMenu.class);
		player.containerMenu = settingsMenu;
		when(settingsMenu.getBlockPosition()).thenReturn(STORAGE_POS);
		when(settingsMenu.stillValid(player)).thenReturn(true);

		OpenStorageInventoryMessage.handleMessage(player, new OpenStorageInventoryMessage(STORAGE_POS));

		verify(settingsMenu).transferOpenersToStorageMenu();
	}
}
