package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankLightingTest {
	private MockedStatic<RegistryHelper> registryMock;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.WATER_BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.EXPERIENCE_BOTTLE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.GLASS_BOTTLE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.POTION.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@BeforeEach
	void mockRegistryAccess() {
		registryMock = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registryMock.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
	}

	@AfterEach
	void closeRegistryMock() {
		registryMock.close();
	}

	@Test
	void tickDoesNotEmitLightForSimulatedFluid() {
		Fixture fixture = fixture();
		try (Transaction tx = Transaction.openRoot()) {
			fixture.fluid().insert(0, FluidResource.of(Fluids.LAVA), 16001, tx, false);
		}
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		assertTrue(fixture.values.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(ints = {16001, 48000})
	void tickEmitsLightOnlyFromOccupiedSegments(int amount) {
		Fixture fixture = fixture();
		fill(fixture.fluid(), new FluidStack(Fluids.LAVA, amount));
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		assertEquals(15, ModBlocks.IRON_TANK.get().getLightEmission(fixture.primary.getBlockState(), fixture.level, BlockPos.ZERO));
		assertEquals(15, fixture.values.get(BlockPos.ZERO.above()));
		assertEquals(amount == 48000 ? 15 : 0, fixture.values.getOrDefault(BlockPos.ZERO.above(2), 0));
		assertTrue(ModBlocks.IRON_TANK.get().hasDynamicLightEmission(fixture.primary.getBlockState()));
	}

	@Test
	void tickClearsTheSegmentLightWhenFluidDropsBelowItsBoundary() {
		Fixture fixture = fixture();
		fill(fixture.fluid(), new FluidStack(Fluids.LAVA, 16001));
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		drain(fixture.fluid(), 1);
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		assertEquals(0, fixture.values.get(BlockPos.ZERO.above()));
		assertEquals(15, fixture.values.get(BlockPos.ZERO));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void tickClearsLightingAfterPackingOrReplacingLavaWithWater(boolean packed) {
		Fixture fixture = fixture();
		fill(fixture.fluid(), new FluidStack(Fluids.LAVA, 48000));
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		if (packed) {
			fixture.primary.pack();
		} else {
			drain(fixture.fluid(), Integer.MAX_VALUE);
			fill(fixture.fluid(), new FluidStack(Fluids.WATER, 48000));
		}
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		assertEquals(3, fixture.values.size());
		assertTrue(fixture.values.values().stream().allMatch(value -> value == 0));
	}

	private static void fill(TankFluidHandler handler, FluidStack fluid) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(fluid.getAmount(), handler.insert(0, FluidResource.of(fluid), fluid.getAmount(), tx, true));
			tx.commit();
		}
	}

	private static void drain(TankFluidHandler handler, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			handler.extract(0, handler.getResource(0), amount, tx, true);
			tx.commit();
		}
	}

	private static Fixture fixture() {
		RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries);
		when(level.hasChunkAt(any())).thenReturn(true);
		Map<BlockPos, TankBlockEntity> segments = new HashMap<>();
		Map<BlockPos, Integer> values = new HashMap<>();
		AuxiliaryLightManager lights = mock(AuxiliaryLightManager.class);
		when(level.getAuxLightManager(any(BlockPos.class))).thenReturn(lights);
		when(lights.getLightAt(any())).thenAnswer(call -> values.getOrDefault(call.getArgument(0), 0));
		doAnswer(call -> {
			values.put(call.getArgument(0), call.getArgument(1));
			return null;
		}).when(lights).setLightAt(any(), anyInt());
		for (int index = 0; index < 3; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.IRON_TANK.get().defaultBlockState());
			tank.setLevel(level);
			segments.put(pos, tank);
		}
		when(level.getBlockEntity(any())).thenAnswer(call -> segments.get(call.getArgument(0)));
		TankBlockEntity primary = segments.get(BlockPos.ZERO);
		primary.configureGroup(3);
		return new Fixture(level, primary, values);
	}

	private record Fixture(ServerLevel level, TankBlockEntity primary, Map<BlockPos, Integer> values) {
		TankFluidHandler fluid() {
			return primary.getStorageWrapper().getTankFluidHandler();
		}
	}
}
