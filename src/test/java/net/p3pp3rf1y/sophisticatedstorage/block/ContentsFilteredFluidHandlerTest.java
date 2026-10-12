package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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
	}

	@Test
	void fillRejectsEmptyLockedTankForSimulationAndExecutionWithoutCallingNativeFill() {
		FluidTank tank = spy(new FluidTank(4000));
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		FluidStack water = new FluidStack(Fluids.WATER, 1000);
		assertEquals(0, handler.fill(water, FluidAction.SIMULATE));
		assertEquals(0, handler.fill(water, FluidAction.EXECUTE));
		assertFalse(handler.isFluidValid(0, water));
		assertTrue(tank.isEmpty());
		verify(tank, never()).fill(any(), any());
	}

	@Test
	void fillAcceptsExistingFluidButRejectsDifferentFluidAndComponents() {
		FluidTank tank = new FluidTank(4000);
		tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		FluidStack water = new FluidStack(Fluids.WATER, 500);
		FluidStack differentComponents = water.copy();
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("different", true);
		differentComponents.setTag(tag);
		assertEquals(500, handler.fill(water, FluidAction.SIMULATE));
		assertEquals(1000, tank.getFluidAmount());
		assertEquals(500, handler.fill(water, FluidAction.EXECUTE));
		assertEquals(0, handler.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE));
		assertEquals(0, handler.fill(differentComponents, FluidAction.EXECUTE));
		assertEquals(1500, tank.getFluidAmount());
		assertTrue(handler.isFluidValid(0, water));
		assertFalse(handler.isFluidValid(0, differentComponents));
		assertEquals(1, handler.getTanks());
		assertEquals(4000, handler.getTankCapacity(0));
	}

	@Test
	void drainRemainsAvailableForBothOverloadsWhileLocked() {
		FluidTank tank = new FluidTank(4000);
		tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		assertEquals(1000, handler.drain(1000, FluidAction.SIMULATE).getAmount());
		assertEquals(250, handler.drain(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE).getAmount());
		assertEquals(750, handler.drain(1000, FluidAction.EXECUTE).getAmount());
		assertTrue(tank.isEmpty());
	}

	@Test
	void fillRejectsFullyDrainedLockedTank() {
		FluidTank tank = new FluidTank(4000);
		tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		handler.drain(1000, FluidAction.EXECUTE);
		assertEquals(0, handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertTrue(tank.isEmpty());
	}

	@Test
	void fillFollowsLiveEndpointLockWithoutRestrictingAnotherEndpointSharingTheTank() {
		FluidTank tank = new FluidTank(4000);
		AtomicBoolean locked = new AtomicBoolean(true);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, locked::get);
		ContentsFilteredFluidHandler otherEndpoint = new ContentsFilteredFluidHandler(tank, () -> false);
		FluidStack water = new FluidStack(Fluids.WATER, 1000);
		assertEquals(0, handler.fill(water, FluidAction.EXECUTE));
		assertEquals(1000, otherEndpoint.fill(water, FluidAction.EXECUTE));
		assertEquals(1000, handler.fill(water, FluidAction.EXECUTE));
		handler.drain(2000, FluidAction.EXECUTE);
		locked.set(false);
		assertEquals(1000, handler.fill(water, FluidAction.EXECUTE));
		assertEquals(1000, tank.getFluidAmount());
	}
}
