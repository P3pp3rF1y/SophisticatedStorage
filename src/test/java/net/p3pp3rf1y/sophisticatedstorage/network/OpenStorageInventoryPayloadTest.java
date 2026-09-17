package net.p3pp3rf1y.sophisticatedstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class OpenStorageInventoryPayloadTest {
	@Test
	void handlePayloadDoesNotOpenStorageMenuWhenSettingsMenuIsInvalidAtPayloadPosition() {
		BlockPos pos = new BlockPos(1, 2, 3);
		Player player = mock(Player.class);
		StorageSettingsContainerMenu settingsMenu = mock(StorageSettingsContainerMenu.class);
		IPayloadContext context = mock(IPayloadContext.class);
		player.containerMenu = settingsMenu;
		when(context.player()).thenReturn(player);
		when(settingsMenu.getBlockPosition()).thenReturn(pos);
		when(settingsMenu.stillValid(player)).thenReturn(false);

		OpenStorageInventoryPayload.handlePayload(new OpenStorageInventoryPayload(pos), context);

		verify(settingsMenu, never()).transferOpenersToStorageMenu();
		verify(player, never()).openMenu(any(), ArgumentMatchers.<Consumer<RegistryFriendlyByteBuf>>any());
	}
}
