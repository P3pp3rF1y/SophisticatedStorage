package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentsFilteredFluidHandlerTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void fillRejectsEmptyLockedTankForSimulationAndExecutionWithoutCallingNativeFill() {
		IStorageFluidHandler tank = mock(IStorageFluidHandler.class);
		when(tank.size()).thenReturn(1);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		FluidResource water = FluidResource.of(Fluids.WATER);

		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(0, handler.insert(0, water, 1000, tx, false));
			assertEquals(0, handler.insert(0, water, 1000, tx, false));
			assertFalse(handler.isValid(0, water));
			assertEquals(0, handler.getCapacityAsLong(0, water));
		}
		verify(tank, never()).insert(anyInt(), any(), anyInt(), any(), anyBoolean());
	}

	@Test
	void fillAcceptsExistingFluidButRejectsDifferentFluidAndComponents() {
		IStorageFluidHandler tank = mock(IStorageFluidHandler.class);
		when(tank.size()).thenReturn(1);
		when(tank.getAmountAsLong(0)).thenReturn(1000L);
		FluidResource water = FluidResource.of(Fluids.WATER);
		when(tank.getResource(0)).thenReturn(water);
		when(tank.isValid(0, water)).thenReturn(true);
		when(tank.getCapacityAsLong(0, water)).thenReturn(4000L);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		FluidStack differentComponents = new FluidStack(Fluids.WATER, 500);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("different", true);
		differentComponents.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		FluidResource differentData = FluidResource.of(differentComponents);

		try (Transaction tx = Transaction.openRoot()) {
			when(tank.insert(0, water, 500, tx, false)).thenReturn(500);
			assertEquals(500, handler.insert(0, water, 500, tx, false));
			assertEquals(0, handler.insert(0, FluidResource.of(Fluids.LAVA), 500, tx, false));
			assertEquals(0, handler.insert(0, differentData, 500, tx, false));
			verify(tank).insert(0, water, 500, tx, false);
		}
		assertTrue(handler.isValid(0, water));
		assertFalse(handler.isValid(0, differentData));
		assertEquals(1, handler.size());
		assertEquals(4000, handler.getCapacityAsLong(0, water));
		assertSame(water, handler.getResource(0));
		assertEquals(1000, handler.getAmountAsLong(0));
	}

	@Test
	void drainRemainsAvailableForBothOverloadsWhileLocked() {
		IStorageFluidHandler tank = mock(IStorageFluidHandler.class);
		FluidResource water = FluidResource.of(Fluids.WATER);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);

		try (Transaction tx = Transaction.openRoot()) {
			when(tank.extract(0, water, 250, tx, false)).thenReturn(250);
			when(tank.extract(0, water, 750, tx, false)).thenReturn(750);
			assertEquals(250, handler.extract(0, water, 250, tx, false));
			assertEquals(750, handler.extract(0, water, 750, tx, false));
		}
	}

	@Test
	void fillRejectsFullyDrainedLockedTank() {
		IStorageFluidHandler tank = mock(IStorageFluidHandler.class);
		when(tank.size()).thenReturn(1);
		FluidResource water = FluidResource.of(Fluids.WATER);
		when(tank.getResource(0)).thenReturn(water);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);

		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(0, handler.insert(0, water, 1000, tx, false));
		}
		verify(tank, never()).insert(anyInt(), any(), anyInt(), any(), anyBoolean());
	}

	@Test
	void fillFollowsLiveEndpointLockWithoutRestrictingAnotherEndpointSharingTheTank() {
		IStorageFluidHandler tank = mock(IStorageFluidHandler.class);
		when(tank.size()).thenReturn(1);
		AtomicBoolean locked = new AtomicBoolean(true);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, locked::get);
		ContentsFilteredFluidHandler otherEndpoint = new ContentsFilteredFluidHandler(tank, () -> false);
		FluidResource water = FluidResource.of(Fluids.WATER);

		try (Transaction tx = Transaction.openRoot()) {
			when(tank.insert(0, water, 1000, tx, false)).thenReturn(1000);
			assertEquals(0, handler.insert(0, water, 1000, tx, false));
			assertEquals(1000, otherEndpoint.insert(0, water, 1000, tx, false));
			when(tank.getResource(0)).thenReturn(water);
			when(tank.getAmountAsLong(0)).thenReturn(1000L);
			assertEquals(1000, handler.insert(0, water, 1000, tx, false));
			when(tank.getAmountAsLong(0)).thenReturn(0L);
			assertEquals(0, handler.insert(0, water, 1000, tx, false));
			locked.set(false);
			assertEquals(1000, handler.insert(0, water, 1000, tx, false));
		}
	}
}
