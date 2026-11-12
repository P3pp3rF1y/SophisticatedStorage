package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TankFluidHandlerTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void insertRejectsAnEmptyLockedTankThroughEitherOverload(boolean explicitLimit) {
		AtomicBoolean locked = new AtomicBoolean(true);
		TankFluidHandler handler = handler(locked);
		FluidResource water = FluidResource.of(componentWater(1000));

		assertEquals(0, insert(handler, water, 1000, explicitLimit, true));
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void insertUsesTheCurrentLockState() {
		AtomicBoolean locked = new AtomicBoolean(true);
		TankFluidHandler handler = handler(locked);
		locked.set(false);

		int filled = insert(handler, FluidResource.of(componentWater(1000)), 1000, false, true);

		assertEquals(1000, filled);
		assertTrue(FluidStack.matches(componentWater(1000), handler.getFluidInTank(0)));
	}

	@Test
	void insertRejectsDifferentComponentsWhileLocked() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		insert(handler, FluidResource.of(componentWater(1000)), 1000, false, true);
		locked.set(true);

		int filled = insert(handler, FluidResource.of(Fluids.WATER), 1000, true, true);

		assertEquals(0, filled);
		assertTrue(FluidStack.matches(componentWater(1000), handler.getFluidInTank(0)));
	}

	@Test
	void insertSimulationAcceptsMatchingLockedFluidWithoutMutation() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		insert(handler, FluidResource.of(componentWater(1000)), 1000, false, true);
		locked.set(true);

		int filled = insert(handler, FluidResource.of(componentWater(1000)), 1000, false, false);

		assertEquals(1000, filled);
		assertTrue(FluidStack.matches(componentWater(1000), handler.getFluidInTank(0)));
	}

	@Test
	void extractPreservesMatchingComponentsWhileLocked() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		insert(handler, FluidResource.of(componentWater(1000)), 1000, false, true);
		locked.set(true);

		int drained = extract(handler, FluidResource.of(componentWater(1000)), 1000, true, true);

		assertEquals(1000, drained);
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void insertRejectsAnEmptyLockedTankAfterItsPreviousFluidWasDrained() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = handler(locked);
		FluidResource water = FluidResource.of(componentWater(1000));
		insert(handler, water, 1000, false, true);
		locked.set(true);
		extract(handler, water, 1000, true, true);

		assertEquals(0, insert(handler, water, 1000, false, true));
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	@Test
	void loadRestoresSavedFluidAmountAndComponents() {
		TankFluidHandler original = handler();
		insert(original, FluidResource.of(componentWater(1234)), 1234, false, true);
		RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		CompoundTag saved = original.save(registries);
		TankFluidHandler restored = handler();

		restored.load(registries, saved);

		assertTrue(FluidStack.matches(componentWater(1234), restored.getFluidInTank(0)));
	}

	@Test
	void getFluidInTankReturnsADefensiveCopy() {
		TankFluidHandler handler = handler();
		insert(handler, FluidResource.of(componentWater(1234)), 1234, false, true);
		FluidStack view = handler.getFluidInTank(0);

		view.shrink(1000);

		assertTrue(FluidStack.matches(componentWater(1234), handler.getFluidInTank(0)));
	}

	@Test
	void insertDoesNotRetainTheCallerFluidStack() {
		TankFluidHandler handler = handler();
		FluidStack water = componentWater(1234);
		insert(handler, FluidResource.of(water), 1234, false, true);

		water.shrink(1000);

		assertTrue(FluidStack.matches(componentWater(1234), handler.getFluidInTank(0)));
	}

	@ParameterizedTest
	@MethodSource("mutationParams")
	void insertAndExtractPersistMutationsAndOnlySynchronizeVisibleChanges(MutationParams params) {
		AtomicInteger saves = new AtomicInteger();
		AtomicInteger updates = new AtomicInteger();
		TankFluidHandler handler = new TankFluidHandler(() -> 8000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, saves::incrementAndGet,
				updates::incrementAndGet);
		if (params.initialAmount > 0) {
			insert(handler, FluidResource.of(Fluids.WATER), params.initialAmount, false, true);
		}
		saves.set(0);
		updates.set(0);

		if (params.drain) {
			extract(handler, FluidResource.of(Fluids.WATER), 2, false, params.commit);
		} else {
			insert(handler, FluidResource.of(params.lava ? Fluids.LAVA : Fluids.WATER), 1, false, params.commit);
		}

		assertEquals(params.saves, saves.get());
		assertEquals(params.updates, updates.get());
	}

	private static Stream<MutationParams> mutationParams() {
		return Stream.of(new MutationParams(0, false, false, true, 1, 1), new MutationParams(1, false, false, true, 1, 0),
				new MutationParams(2, false, false, false, 0, 0), new MutationParams(2, true, false, true, 1, 1),
				new MutationParams(0, false, true, true, 1, 1));
	}

	@Test
	void insertAndExtractOrdinaryTransfersBypassPumpLimitButExplicitAutomationTransfersDoNot() {
		AtomicInteger saves = new AtomicInteger();
		TankFluidHandler handler = new TankFluidHandler(() -> 8000, () -> 1000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false,
				saves::incrementAndGet, () -> {
				});
		FluidResource water = FluidResource.of(Fluids.WATER);
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(3500, handler.insert(0, water, 3500, tx));
		}
		assertEquals(0, handler.getFluidInTank(0).getAmount());
		assertEquals(0, saves.get());
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(3500, handler.insert(0, water, 3500, tx));
			tx.commit();
		}
		assertEquals(3500, handler.getFluidInTank(0).getAmount());
		assertEquals(1, saves.get());
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(2000, handler.extract(0, water, 2000, tx));
		}
		assertEquals(3500, handler.getFluidInTank(0).getAmount());
		assertEquals(1, saves.get());
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(2000, handler.extract(0, water, 2000, tx));
			tx.commit();
		}
		assertEquals(1500, handler.getFluidInTank(0).getAmount());
		assertEquals(2, saves.get());
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1000, handler.insert(0, water, 3500, tx, false));
			assertEquals(1000, handler.extract(0, water, 2000, tx, false));
		}
		assertEquals(1500, handler.getFluidInTank(0).getAmount());
		assertEquals(2, saves.get());
	}

	private record MutationParams(int initialAmount, boolean drain, boolean lava, boolean commit, int saves, int updates) {
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void insertVoidsMatchingOverflowAndHonorsSimulation(boolean commit) {
		TankFluidHandler handler = overflowingHandler(new AtomicBoolean(false));

		int filled = insert(handler, FluidResource.of(Fluids.WATER), 2500, false, commit);

		assertEquals(2500, filled);
		assertEquals(commit ? 1000 : 0, handler.getFluidInTank(0).getAmount());
	}

	@Test
	void insertRejectsDifferentFluidEvenWhenOverflowVoidingIsEnabled() {
		TankFluidHandler handler = overflowingHandler(new AtomicBoolean(false));
		insert(handler, FluidResource.of(Fluids.WATER), 2500, false, true);

		assertEquals(0, insert(handler, FluidResource.of(Fluids.LAVA), 2500, false, true));
		assertEquals(1000, handler.getFluidInTank(0).getAmount());
	}

	@Test
	void insertDoesNotBypassAnEmptyLockWhenOverflowVoidingIsEnabled() {
		AtomicBoolean locked = new AtomicBoolean(false);
		TankFluidHandler handler = overflowingHandler(locked);
		insert(handler, FluidResource.of(Fluids.WATER), 2500, false, true);
		extract(handler, FluidResource.of(Fluids.WATER), 1000, false, true);
		locked.set(true);

		assertEquals(0, insert(handler, FluidResource.of(Fluids.WATER), 2500, false, true));
		assertTrue(handler.getFluidInTank(0).isEmpty());
	}

	private static int insert(TankFluidHandler handler, FluidResource resource, int amount, boolean ignoreLimit, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int inserted = handler.insert(0, resource, amount, tx, ignoreLimit);
			if (commit) {
				tx.commit();
			}
			return inserted;
		}
	}

	private static int extract(TankFluidHandler handler, FluidResource resource, int amount, boolean ignoreLimit, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int extracted = handler.extract(0, resource, amount, tx, ignoreLimit);
			if (commit) {
				tx.commit();
			}
			return extracted;
		}
	}

	private static TankFluidHandler overflowingHandler(AtomicBoolean locked) {
		return new TankFluidHandler(() -> 1000, () -> ItemStack.EMPTY, locked::get,
				(fluid, type) -> type == VoidType.STORAGE_OVERFLOW && fluid.is(Fluids.WATER), () -> {
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
		water.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		return water;
	}
}
