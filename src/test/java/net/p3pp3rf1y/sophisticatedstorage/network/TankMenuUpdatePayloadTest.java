package net.p3pp3rf1y.sophisticatedstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankMenuUpdatePayloadTest {
	@ParameterizedTest
	@CsvSource({"0,0,0,3", "1,127,128,4", "128,16384,2097152,9", "-1,-1,-1,15", "2147483647,0,0,7", "0,2147483647,-2147483648,11"})
	void codecRoundTripsThreeIndependentVarInts(int containerId, int amount, int capacity, int expectedBytes) {
		TankMenuUpdatePayload payload = new TankMenuUpdatePayload(containerId, amount, capacity);
		ByteBuf bytes = Unpooled.buffer();
		try {
			TankMenuUpdatePayload.STREAM_CODEC.encode(bytes, payload);
			assertEquals(expectedBytes, bytes.readableBytes());
			assertEquals(payload, TankMenuUpdatePayload.STREAM_CODEC.decode(bytes));
			assertEquals(0, bytes.readableBytes());
			assertEquals(TankMenuUpdatePayload.TYPE, payload.type());
		} finally {
			bytes.release();
		}
	}

	@Test
	void onlyMatchingTankMenuReceivesFluidUpdate() {
		IPayloadContext context = mock(IPayloadContext.class);
		Player player = mock(Player.class);
		TankContainerMenu menu = mock(TankContainerMenu.class);
		player.containerMenu = menu;
		when(context.player()).thenReturn(player);
		TankMenuUpdatePayload.handlePayload(new TankMenuUpdatePayload(menu.containerId, 17000, 64000), context);
		verify(menu).setFluidData(17000, 64000);
		TankMenuUpdatePayload.handlePayload(new TankMenuUpdatePayload(menu.containerId + 1, 3, 4), context);
		verifyNoMoreInteractions(menu);
	}

	@Test
	void otherMenuNeverReceivesTankFluidUpdate() {
		IPayloadContext context = mock(IPayloadContext.class);
		Player player = mock(Player.class);
		player.containerMenu = mock(AbstractContainerMenu.class);
		when(context.player()).thenReturn(player);
		assertDoesNotThrow(() -> TankMenuUpdatePayload.handlePayload(new TankMenuUpdatePayload(0, 1, 2), context));
	}
}
