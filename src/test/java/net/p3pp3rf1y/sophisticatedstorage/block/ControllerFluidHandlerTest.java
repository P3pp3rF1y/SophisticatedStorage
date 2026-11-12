package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ControllerFluidHandlerTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void insertPrefersMatchingBeforeEmptyAndHonorsSimulation(boolean commit) {
		TankFluidHandler empty = tank();
		TankFluidHandler matching = tank();
		insert(matching, FluidResource.of(Fluids.WATER), 600);
		ControllerFluidHandler controller = new ControllerFluidHandler(() -> List.of(empty, matching));

		int filled;
		try (Transaction tx = Transaction.openRoot()) {
			filled = controller.insert(FluidResource.of(Fluids.WATER), 700, tx, false);
			if (commit) {
				tx.commit();
			}
		}

		assertEquals(700, filled);
		assertEquals(commit ? 1000 : 600, matching.getAmountAsLong(0));
		assertEquals(commit ? 300 : 0, empty.getAmountAsLong(0));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void extractCombinesOnlyExactIdentityAndHonorsSimulation(boolean commit) {
		TankFluidHandler plain = tank();
		TankFluidHandler component = tank();
		TankFluidHandler secondPlain = tank();
		insert(plain, FluidResource.of(Fluids.WATER), 300);
		FluidStack special = new FluidStack(Fluids.WATER, 500);
		CompoundTag data = new CompoundTag();
		data.putString("identity", "special");
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		insert(component, FluidResource.of(special), 500);
		insert(secondPlain, FluidResource.of(Fluids.WATER), 400);
		AtomicReference<List<IStorageFluidHandler>> members = new AtomicReference<>(List.of(plain, component, secondPlain));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);

		FluidResource first = controller.getResource(0);
		int drained;
		try (Transaction tx = Transaction.openRoot()) {
			drained = controller.extract(first, 1000, tx, false);
			if (commit) {
				tx.commit();
			}
		}

		assertEquals(700, drained);
		assertEquals(FluidResource.of(Fluids.WATER), first);
		assertEquals(commit ? 0 : 300, plain.getAmountAsLong(0));
		assertEquals(commit ? 0 : 400, secondPlain.getAmountAsLong(0));
		assertTrue(FluidStack.matches(special, component.getFluidInTank(0)));
	}

	@Test
	void extractByResourceUsesCurrentMembershipAndPreservesComponents() {
		TankFluidHandler component = tank();
		FluidStack special = new FluidStack(Fluids.WATER, 500);
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
		insert(component, FluidResource.of(special), 500);
		AtomicReference<List<IStorageFluidHandler>> members = new AtomicReference<>(List.of(tank(), component));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of(component));

		int exact;
		try (Transaction tx = Transaction.openRoot()) {
			exact = controller.extract(FluidResource.of(special), 200, tx, false);
			tx.commit();
		}

		assertEquals(FluidResource.of(special), component.getResource(0));
		assertEquals(200, exact);
		assertEquals(300, component.getAmountAsLong(0));
	}

	@Test
	void sizeReflectsCurrentMembership() {
		AtomicReference<List<IStorageFluidHandler>> members = new AtomicReference<>(List.of(tank(), tank()));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of(tank()));

		assertEquals(1, controller.size());
	}

	@Test
	void insertRejectsFluidAfterAllMembersAreRemoved() {
		AtomicReference<List<IStorageFluidHandler>> members = new AtomicReference<>(List.of(tank()));
		ControllerFluidHandler controller = new ControllerFluidHandler(members::get);
		members.set(List.of());

		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(0, controller.insert(FluidResource.of(Fluids.WATER), 100, tx, false));
			tx.commit();
		}
		assertEquals(0, controller.size());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void insertPublicCapabilityFillsMatchingBeforeEmptyAcrossMembers(boolean commit) {
		TankFluidHandler empty = tank();
		TankFluidHandler matching = tank();
		insert(matching, FluidResource.of(Fluids.WATER), 600);
		ResourceHandler<FluidResource> capability = new ControllerFluidHandler(() -> List.of(empty, matching));
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(700, capability.insert(FluidResource.of(Fluids.WATER), 700, tx));
			if (commit) {
				tx.commit();
			}
		}
		assertEquals(commit ? 1000 : 600, matching.getAmountAsLong(0));
		assertEquals(commit ? 300 : 0, empty.getAmountAsLong(0));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void insertPublicCapabilityRespectsInputOutputAndExactFluidIdentity(boolean commit) {
		TankFluidHandler plain = tank();
		TankFluidHandler second = tank();
		TankFluidHandler component = tank();
		FluidStack special = new FluidStack(Fluids.WATER, 200);
		CompoundTag data = new CompoundTag();
		data.putString("identity", "special");
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		insert(component, FluidResource.of(special), 200);
		ResourceHandler<FluidResource> input = new RestrictedFluidHandler(new ControllerFluidHandler(() -> List.of(plain, second, component)), true, false);
		ResourceHandler<FluidResource> output = new RestrictedFluidHandler(new ControllerFluidHandler(() -> List.of(plain, second, component)), false, true);
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(2000, input.insert(FluidResource.of(Fluids.WATER), 2000, tx));
			assertEquals(0, output.insert(FluidResource.of(Fluids.WATER), 100, tx));
			assertEquals(0, input.extract(FluidResource.of(Fluids.WATER), 100, tx));
			assertEquals(2000, output.extract(FluidResource.of(Fluids.WATER), 2000, tx));
			assertEquals(0, output.extract(FluidResource.of(Fluids.WATER), 201, tx));
			if (commit) {
				tx.commit();
			}
		}
		assertEquals(0, plain.getAmountAsLong(0));
		assertEquals(0, second.getAmountAsLong(0));
		assertTrue(FluidStack.matches(special, component.getFluidInTank(0)));
	}

	@Test
	void insertRestrictedPublicIndexedOperationsIgnorePumpRateButExplicitFalsePreservesIt() {
		TankFluidHandler limited = new TankFluidHandler(() -> 2000, () -> 1000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, () -> {
		}, () -> {
		});
		IStorageFluidHandler input = new RestrictedFluidHandler(limited, true, false);
		IStorageFluidHandler output = new RestrictedFluidHandler(limited, false, true);
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1000, input.insert(0, FluidResource.of(Fluids.WATER), 2000, tx, false));
			assertEquals(1000, input.insert(0, FluidResource.of(Fluids.WATER), 1000, tx));
			tx.commit();
		}
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1000, output.extract(0, FluidResource.of(Fluids.WATER), 2000, tx, false));
			assertEquals(1000, output.extract(0, FluidResource.of(Fluids.WATER), 1000, tx));
			tx.commit();
		}
		assertEquals(0, limited.getAmountAsLong(0));
	}

	private static void insert(TankFluidHandler handler, FluidResource resource, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(amount, handler.insert(0, resource, amount, tx, false));
			tx.commit();
		}
	}

	private static TankFluidHandler tank() {
		return new TankFluidHandler(() -> 1000, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, () -> {
		}, () -> {
		});
	}
}
