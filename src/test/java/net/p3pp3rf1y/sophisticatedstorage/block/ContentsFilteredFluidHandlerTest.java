package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
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

		int simulated = handler.fill(water, FluidAction.SIMULATE);
		int executed = handler.fill(water, FluidAction.EXECUTE);

		assertEquals(0, simulated);
		assertEquals(0, executed);
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
		differentComponents.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

		int simulated = handler.fill(water, FluidAction.SIMULATE);
		int amountAfterSimulation = tank.getFluidAmount();
		int executed = handler.fill(water, FluidAction.EXECUTE);
		int differentFluid = handler.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
		int differentData = handler.fill(differentComponents, FluidAction.EXECUTE);

		assertEquals(500, simulated);
		assertEquals(1000, amountAfterSimulation);
		assertEquals(500, executed);
		assertEquals(0, differentFluid);
		assertEquals(0, differentData);
		assertEquals(1500, tank.getFluidAmount());
		assertTrue(handler.isFluidValid(0, water));
		assertFalse(handler.isFluidValid(0, differentComponents));
		assertEquals(1, handler.getTanks());
		assertEquals(4000, handler.getTankCapacity(0));
		assertSame(tank.getFluid(), handler.getFluidInTank(0));
	}

	@Test
	void drainRemainsAvailableForBothOverloadsWhileLocked() {
		FluidTank tank = new FluidTank(4000);
		tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);

		FluidStack simulated = handler.drain(1000, FluidAction.SIMULATE);
		FluidStack matching = handler.drain(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE);
		FluidStack remaining = handler.drain(1000, FluidAction.EXECUTE);

		assertEquals(1000, simulated.getAmount());
		assertEquals(250, matching.getAmount());
		assertEquals(750, remaining.getAmount());
		assertTrue(tank.isEmpty());
	}

	@Test
	void fillRejectsFullyDrainedLockedTank() {
		FluidTank tank = new FluidTank(4000);
		tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, () -> true);
		handler.drain(1000, FluidAction.EXECUTE);

		int refilled = handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);

		assertEquals(0, refilled);
		assertTrue(tank.isEmpty());
	}

	@Test
	void fillFollowsLiveEndpointLockWithoutRestrictingAnotherEndpointSharingTheTank() {
		FluidTank tank = new FluidTank(4000);
		AtomicBoolean locked = new AtomicBoolean(true);
		ContentsFilteredFluidHandler handler = new ContentsFilteredFluidHandler(tank, locked::get);
		ContentsFilteredFluidHandler otherEndpoint = new ContentsFilteredFluidHandler(tank, () -> false);
		FluidStack water = new FluidStack(Fluids.WATER, 1000);

		int rejected = handler.fill(water, FluidAction.EXECUTE);
		int acceptedByOther = otherEndpoint.fill(water, FluidAction.EXECUTE);
		int matching = handler.fill(water, FluidAction.EXECUTE);
		handler.drain(2000, FluidAction.EXECUTE);
		locked.set(false);
		int unlocked = handler.fill(water, FluidAction.EXECUTE);

		assertEquals(0, rejected);
		assertEquals(1000, acceptedByOther);
		assertEquals(1000, matching);
		assertEquals(1000, unlocked);
		assertEquals(1000, tank.getFluidAmount());
	}
}
