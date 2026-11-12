package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
import static org.junit.jupiter.api.Assertions.*;

class ControllerFluidHandlerTest {
	@ParameterizedTest
	@EnumSource(IFluidHandler.FluidAction.class)
	void fillPrefersMatchingBeforeEmptyAndHonorsSimulation(IFluidHandler.FluidAction action) {
		TankFluidHandler empty = tank();
		TankFluidHandler matching = tank();
		matching.fill(new FluidStack(Fluids.WATER, 600), EXECUTE);
		ControllerFluidHandler controller = new ControllerFluidHandler(() -> List.of(empty, matching));

		int filled = controller.fill(new FluidStack(Fluids.WATER, 700), action);

		assertEquals(700, filled);
		assertEquals(action.execute() ? 1000 : 600, matching.getFluidInTank(0).getAmount());
		assertEquals(action.execute() ? 300 : 0, empty.getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(IFluidHandler.FluidAction.class)
	void drainCombinesOnlyExactIdentityAndHonorsSimulation(IFluidHandler.FluidAction action) {
		TankFluidHandler plain = tank();
		TankFluidHandler component = tank();
		TankFluidHandler secondPlain = tank();
		plain.fill(new FluidStack(Fluids.WATER, 300), EXECUTE);
		FluidStack special = new FluidStack(Fluids.WATER, 500);
		CompoundTag data = new CompoundTag();
		data.putString("identity", "special");
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		component.fill(special, EXECUTE);
		secondPlain.fill(new FluidStack(Fluids.WATER, 400), EXECUTE);
		AtomicReference<List<IFluidHandler>> members = new AtomicReference<>(List.of(plain, component, secondPlain));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);

		FluidStack drained = controller.drain(1000, action);

		assertEquals(700, drained.getAmount());
		assertTrue(FluidStack.isSameFluidSameComponents(new FluidStack(Fluids.WATER, 1), drained));
		assertEquals(action.execute() ? 0 : 300, plain.getFluidInTank(0).getAmount());
		assertEquals(action.execute() ? 0 : 400, secondPlain.getFluidInTank(0).getAmount());
		assertTrue(FluidStack.matches(special, component.getFluidInTank(0)));
	}

	@Test
	void drainByResourceUsesCurrentMembershipAndPreservesComponents() {
		TankFluidHandler component = tank();
		FluidStack special = new FluidStack(Fluids.WATER, 500);
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
		component.fill(special, EXECUTE);
		AtomicReference<List<IFluidHandler>> members = new AtomicReference<>(List.of(tank(), component));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of(component));

		FluidStack exact = controller.drain(special.copyWithAmount(200), EXECUTE);

		assertTrue(FluidStack.isSameFluidSameComponents(special, exact));
		assertEquals(200, exact.getAmount());
		assertEquals(300, component.getFluidInTank(0).getAmount());
	}

	@Test
	void getTanksReflectsCurrentMembership() {
		AtomicReference<List<IFluidHandler>> members = new AtomicReference<>(List.of(tank(), tank()));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of(tank()));

		int tanks = controller.getTanks();

		assertEquals(1, tanks);
	}

	@Test
	void fillRejectsFluidAfterAllMembersAreRemoved() {
		AtomicReference<List<IFluidHandler>> members = new AtomicReference<>(List.of(tank()));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of());

		int filled = controller.fill(new FluidStack(Fluids.WATER, 100), EXECUTE);

		assertEquals(0, filled);
		assertEquals(0, controller.getTanks());
	}

	private static TankFluidHandler tank() {
		return new TankFluidHandler(() -> 1000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, () -> {
		}, () -> {
		});
	}
}
