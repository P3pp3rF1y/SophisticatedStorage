package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TankFluidHandlerTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillRejectsAnEmptyLockedTankThroughEitherOverload(boolean explicitLimit) {
		AtomicBoolean locked = new AtomicBoolean(true);
		TankFluidHandler handler = handler(locked);
		FluidStack water = componentWater(1000);

		int filled = explicitLimit ? handler.fill(water, FluidAction.EXECUTE, true) : handler.fill(water, FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void fillUsesTheCurrentLockState() {
		AtomicBoolean locked = new AtomicBoolean(true);
		TankFluidHandler handler = handler(locked);
		locked.set(false);

		int filled = handler.fill(componentWater(1000), FluidAction.EXECUTE);

		assertEquals(1000, filled);
		assertTrue(componentWater(1000).isFluidStackIdentical(handler.getFluidInTank(0)));
	}

	@Test
	void fillRejectsDifferentComponentsWhileLocked() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		handler.fill(componentWater(1000), FluidAction.EXECUTE);
		locked.set(true);

		int filled = handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE, true);

		assertEquals(0, filled);
		assertTrue(componentWater(1000).isFluidStackIdentical(handler.getFluidInTank(0)));
	}

	@Test
	void fillSimulationAcceptsMatchingLockedFluidWithoutMutation() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		handler.fill(componentWater(1000), FluidAction.EXECUTE);
		locked.set(true);

		int filled = handler.fill(componentWater(1000), FluidAction.SIMULATE);

		assertEquals(1000, filled);
		assertTrue(componentWater(1000).isFluidStackIdentical(handler.getFluidInTank(0)));
	}

	@Test
	void drainPreservesMatchingComponentsWhileLocked() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		handler.fill(componentWater(1000), FluidAction.EXECUTE);
		locked.set(true);

		FluidStack drained = handler.drain(componentWater(1000), FluidAction.EXECUTE, true);

		assertTrue(componentWater(1000).isFluidStackIdentical(drained));
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void fillRejectsAnEmptyLockedTankAfterItsPreviousFluidWasDrained() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		handler.fill(componentWater(1000), FluidAction.EXECUTE);
		locked.set(true);
		handler.drain(componentWater(1000), FluidAction.EXECUTE, true);

		int filled = handler.fill(componentWater(1000), FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void loadRestoresSavedFluidAmountAndComponents() {
		TankFluidHandler original = handler();
		original.fill(componentWater(1234), FluidAction.EXECUTE);
		CompoundTag saved = original.save();
		TankFluidHandler restored = handler();

		restored.load(saved);

		assertTrue(componentWater(1234).isFluidStackIdentical(restored.getFluidInTank(0)));
	}

	@Test
	void getFluidInTankReturnsADefensiveCopy() {
		TankFluidHandler handler = handler();
		handler.fill(componentWater(1234), FluidAction.EXECUTE);
		FluidStack view = handler.getFluidInTank(0);

		view.shrink(1000);

		assertTrue(componentWater(1234).isFluidStackIdentical(handler.getFluidInTank(0)));
	}

	@Test
	void fillDoesNotRetainTheCallerFluidStack() {
		TankFluidHandler handler = handler();
		FluidStack water = componentWater(1234);
		handler.fill(water, FluidAction.EXECUTE);

		water.shrink(1000);

		assertTrue(componentWater(1234).isFluidStackIdentical(handler.getFluidInTank(0)));
	}

	@ParameterizedTest
	@MethodSource("mutationParams")
	void fillAndDrainPersistMutationsAndOnlySynchronizeVisibleChanges(MutationParams params) {
		AtomicInteger saves = new AtomicInteger();
		AtomicInteger updates = new AtomicInteger();
		TankFluidHandler handler = new TankFluidHandler(() -> 8000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, saves::incrementAndGet,
				updates::incrementAndGet);
		if (params.initialAmount > 0) {
			handler.fill(new FluidStack(Fluids.WATER, params.initialAmount), FluidAction.EXECUTE);
		}
		saves.set(0);
		updates.set(0);

		if (params.drain) {
			handler.drain(2, params.action);
		} else {
			handler.fill(new FluidStack(params.lava ? Fluids.LAVA : Fluids.WATER, 1), params.action);
		}

		assertEquals(params.saves, saves.get());
		assertEquals(params.updates, updates.get());
	}

	private static Stream<MutationParams> mutationParams() {
		return Stream.of(new MutationParams(0, false, false, FluidAction.EXECUTE, 1, 1), new MutationParams(1, false, false, FluidAction.EXECUTE, 1, 0),
				new MutationParams(2, false, false, FluidAction.SIMULATE, 0, 0), new MutationParams(2, true, false, FluidAction.EXECUTE, 1, 1),
				new MutationParams(0, false, true, FluidAction.EXECUTE, 1, 1));
	}

	private record MutationParams(int initialAmount, boolean drain, boolean lava, FluidAction action, int saves, int updates) {
	}

	@ParameterizedTest
	@EnumSource(FluidAction.class)
	void fillVoidsMatchingOverflowAndHonorsSimulation(FluidAction action) {
		TankFluidHandler handler = overflowingHandler(new AtomicBoolean(false));

		int filled = handler.fill(new FluidStack(Fluids.WATER, 2500), action);

		assertEquals(2500, filled);
		assertEquals(action.execute() ? 1000 : 0, handler.getFluidInTank(0).getAmount());
	}

	@Test
	void fillRejectsDifferentFluidEvenWhenOverflowVoidingIsEnabled() {
		TankFluidHandler handler = overflowingHandler(new AtomicBoolean(false));
		handler.fill(new FluidStack(Fluids.WATER, 2500), FluidAction.EXECUTE);

		int filled = handler.fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertEquals(1000, handler.getFluidInTank(0).getAmount());
	}

	@Test
	void fillDoesNotBypassAnEmptyLockWhenOverflowVoidingIsEnabled() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = overflowingHandler(locked);
		handler.fill(new FluidStack(Fluids.WATER, 2500), FluidAction.EXECUTE);
		handler.drain(1000, FluidAction.EXECUTE);
		locked.set(true);

		int filled = handler.fill(new FluidStack(Fluids.WATER, 2500), FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	private static TankFluidHandler overflowingHandler(AtomicBoolean locked) {
		return new TankFluidHandler(() -> 1000, () -> ItemStack.EMPTY, locked::get,
				(fluid, type) -> type == VoidType.STORAGE_OVERFLOW && fluid.getFluid() == Fluids.WATER, () -> {
				}, () -> {
				});
	}

	private static TankFluidHandler handler() {
		return handler(new AtomicBoolean(false));
	}

	private static TankFluidHandler handler(AtomicBoolean locked) {
		return new TankFluidHandler(() -> 8000, () -> ItemStack.EMPTY, locked::get, (fluid, type) -> false, () -> {
		}, () -> {
		});
	}

	private static FluidStack componentWater(int amount) {
		FluidStack water = new FluidStack(Fluids.WATER, amount);
		CompoundTag data = new CompoundTag();
		data.putString("identity", "test");
		water.setTag(data);
		return water;
	}
}
