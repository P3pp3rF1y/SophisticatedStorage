package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageIOFluidHandlerTest {
	private MockedStatic<RegistryHelper> registryMock;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.clearConfig();
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

	@ParameterizedTest
	@MethodSource("portSides")
	void getExternalFluidHandlerExposesNetworkOnEverySideIncludingUnsidedQueries(PortSide params) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		insert(fixture.nativeFluid(), water(), 1000);
		assertNotNull(handler);
		assertEquals(1, handler.size());
		assertEquals(16000, handler.getCapacityAsLong(0, water()));
		assertEquals(params.kind == Port.INPUT ? 0 : 1000, handler.getAmountAsLong(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerReturnsNullWithoutLevelOrController(Port kind) {
		assertNull(createPort(kind).getExternalFluidHandler(null));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerReturnsNullAfterUnlinking(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		port.unregisterController();
		assertNull(port.getExternalFluidHandler(null));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerAllowsClientDiscoveryWithoutAccessingTheController(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		insert(fixture.nativeFluid(), water(), 1000);
		when(fixture.level.isClientSide()).thenReturn(true);
		clearInvocations(fixture.level);
		IStorageFluidHandler handler = port.getExternalFluidHandler(null);
		assertNotNull(handler);
		assertUnavailable(handler);
		assertEquals(1000, fixture.nativeFluid().getAmountAsLong(0));
		verify(fixture.level, never()).getChunkSource();
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void insertRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		insert(fixture.nativeFluid(), water(), 1000);
		int accepted = params.kind == Port.OUTPUT ? 0 : 500;
		assertEquals(accepted, insert(handler, water(), 500, params.commit));
		assertEquals(1000 + (params.commit ? accepted : 0), fixture.nativeFluid().getAmountAsLong(0));
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void insertPublicCapabilityOperationsPreserveDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		ResourceHandler<FluidResource> handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		int accepted = params.kind == Port.OUTPUT ? 0 : 2000;
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(accepted, handler.insert(water(), 2000, tx));
			if (params.commit) {
				tx.commit();
			}
		}
		assertEquals(params.commit ? accepted : 0, fixture.nativeFluid().getAmountAsLong(0));
		int drained = params.kind == Port.INPUT ? 0 : params.commit ? accepted : 0;
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(drained, handler.extract(water(), 2000, tx));
			if (params.commit) {
				tx.commit();
			}
		}
		assertEquals(params.commit ? accepted - drained : 0, fixture.nativeFluid().getAmountAsLong(0));
	}

	@ParameterizedTest
	@MethodSource("portSides")
	void insertPublicCapabilitySpansTwoTankMembers(PortSide params) {
		Fixture fixture = fixture();
		TankBlockEntity second = new TankBlockEntity(new BlockPos(5, 70, 4), ModBlocks.IRON_TANK.get().defaultBlockState());
		second.setLevel(fixture.level);
		fixture.members.add(second);
		ResourceHandler<FluidResource> handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(params.kind == Port.OUTPUT ? 0 : 20000, handler.insert(water(), 20000, tx));
			tx.commit();
		}
		assertEquals(params.kind == Port.OUTPUT ? 0 : 16000, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(params.kind == Port.OUTPUT ? 0 : 4000, second.getStorageWrapper().getTankFluidHandler().getAmountAsLong(0));
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void extractByAmountRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		insert(fixture.nativeFluid(), water(), 1000);
		int extracted = params.kind == Port.INPUT ? 0 : 250;
		assertEquals(extracted, extract(handler, handler.getResource(0), 250, params.commit));
		assertEquals(1000 - (params.commit ? extracted : 0), fixture.nativeFluid().getAmountAsLong(0));
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void extractByResourceRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		insert(fixture.nativeFluid(), water(), 1000);
		int extracted = params.kind == Port.INPUT ? 0 : 250;
		assertEquals(extracted, extract(handler, water(), 250, params.commit));
		assertEquals(1000 - (params.commit ? extracted : 0), fixture.nativeFluid().getAmountAsLong(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void isValidRespectsPortDirection(Port kind) {
		Fixture fixture = fixture();
		assertEquals(kind != Port.OUTPUT, fixture.port(kind).getExternalFluidHandler(null).isValid(0, water()));
	}

	@ParameterizedTest
	@MethodSource("unavailableControllers")
	void getExternalFluidHandlerRetainedHandleStopsAccessWhenUnavailable(LifecycleParams params) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(params.kind);
		IStorageFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		insert(fixture.nativeFluid(), water(), 1000);
		params.state.makeUnavailable(fixture, port);
		assertUnavailable(handler);
		assertEquals(1000, fixture.nativeFluid().getAmountAsLong(0));
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@MethodSource("restorableControllers")
	void getExternalFluidHandlerRetainedHandleResumesAccessAfterRestoration(LifecycleParams params) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(params.kind);
		IStorageFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		insert(fixture.nativeFluid(), water(), 1000);
		params.state.makeUnavailable(fixture, port);
		params.state.restore(fixture, port);
		assertEquals(1, handler.size());
		assertEquals(16000, handler.getCapacityAsLong(0, water()));
		assertEquals(params.kind == Port.INPUT ? 0 : 1000, handler.getAmountAsLong(0));
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerRetainedHandleFollowsRelinking(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		IStorageFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		ControllerBlockEntity replacement = fixture.controller(fixture.controller.getBlockPos().east());
		fixture.blocks.remove(fixture.controller.getBlockPos());
		port.registerController(replacement);
		assertEquals(1, handler.size());
		assertEquals(16000, handler.getCapacityAsLong(0, water()));
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void insertRejectsDifferentFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		assertEquals(0, insert(handler, water(), 1000));
		assertEquals(1000, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(special, fixture.nativeFluid().getResource(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void extractRejectsDifferentFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		assertEquals(0, extract(handler, water(), 1000));
		assertEquals(1000, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(special, fixture.nativeFluid().getResource(0));
	}

	@ParameterizedTest
	@EnumSource(value = Port.class, names = {"IO", "OUTPUT"})
	void getResourcePreservesFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		assertEquals(1000, handler.getAmountAsLong(0));
		assertEquals(special, handler.getResource(0));
	}

	@ParameterizedTest
	@EnumSource(value = Port.class, names = {"IO", "OUTPUT"})
	void getResourceReturnsADefensiveCopy(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		FluidStack view = handler.getResource(0).toStack((int) handler.getAmountAsLong(0));
		view.setAmount(1);
		assertEquals(1000, handler.getAmountAsLong(0));
		assertEquals(1000, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(special, fixture.nativeFluid().getResource(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void extractPreservesMatchingFluidComponentsWhenAllowed(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		int extracted = kind == Port.INPUT ? 0 : 250;
		assertEquals(extracted, extract(handler, special, 250));
		assertEquals(1000 - extracted, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(special, fixture.nativeFluid().getResource(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void insertPreservesMatchingFluidComponentsWhenAllowed(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidResource special = componentWater();
		insert(fixture.nativeFluid(), special, 1000);
		int accepted = kind == Port.OUTPUT ? 0 : 100;
		assertEquals(accepted, insert(handler, special, 100));
		assertEquals(1000 + accepted, fixture.nativeFluid().getAmountAsLong(0));
		assertEquals(special, fixture.nativeFluid().getResource(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void insertRejectsFluidWhenEmptyTankIsLocked(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		fixture.tank.toggleLock();
		assertEquals(0, insert(handler, componentWater(), 1000));
		assertEquals(0, fixture.nativeFluid().getAmountAsLong(0));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void isValidRejectsFluidWhenEmptyTankIsLocked(Port kind) {
		Fixture fixture = fixture();
		IStorageFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		fixture.tank.toggleLock();
		assertFalse(handler.isValid(0, componentWater()));
	}

	private static Stream<PortSide> portSides() {
		return Arrays.stream(Port.values())
				.flatMap(kind -> Stream.concat(Arrays.stream(Direction.values()), Stream.of((Direction) null)).map(side -> new PortSide(kind, side)));
	}

	private static Stream<TransferParams> transferParams() {
		return portSides().flatMap(port -> Stream.of(false, true).map(commit -> new TransferParams(port.kind, port.side, commit)));
	}

	private static Stream<LifecycleParams> unavailableControllers() {
		return Arrays.stream(Port.values()).flatMap(kind -> Arrays.stream(UnavailableState.values()).map(state -> new LifecycleParams(kind, state)));
	}

	private static Stream<LifecycleParams> restorableControllers() {
		return unavailableControllers().filter(params -> params.state != UnavailableState.PORT_REMOVED);
	}

	private static FluidResource water() {
		return FluidResource.of(Fluids.WATER);
	}

	private static FluidResource componentWater() {
		FluidStack special = new FluidStack(Fluids.WATER, 1000);
		CompoundTag identity = new CompoundTag();
		identity.putString("identity", "special");
		special.set(DataComponents.CUSTOM_DATA, CustomData.of(identity));
		return FluidResource.of(special);
	}

	private static int insert(IStorageFluidHandler handler, FluidResource resource, int amount) {
		return insert(handler, resource, amount, true);
	}

	private static int insert(IStorageFluidHandler handler, FluidResource resource, int amount, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int result = handler.insert(resource, amount, tx, false);
			if (commit) {
				tx.commit();
			}
			return result;
		}
	}

	private static int extract(IStorageFluidHandler handler, FluidResource resource, int amount) {
		return extract(handler, resource, amount, true);
	}

	private static int extract(IStorageFluidHandler handler, FluidResource resource, int amount, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int result = handler.extract(0, resource, amount, tx, false);
			if (commit) {
				tx.commit();
			}
			return result;
		}
	}

	private static void assertUnavailable(IStorageFluidHandler handler) {
		int filled = insert(handler, water(), 1000);
		int drainedByAmount = extract(handler, handler.getResource(0), 1000);
		int drainedByResource = extract(handler, water(), 1000);
		assertEquals(0, handler.size());
		assertEquals(0, handler.getCapacityAsLong(0, water()));
		assertEquals(FluidResource.EMPTY, handler.getResource(0));
		assertEquals(0, handler.getAmountAsLong(0));
		assertFalse(handler.isValid(0, water()));
		assertEquals(0, filled);
		assertEquals(0, drainedByAmount);
		assertEquals(0, drainedByResource);
	}

	private static StorageIOBlockEntity createPort(Port kind) {
		BlockPos pos = new BlockPos(8, 70, 8);
		return switch (kind) {
			case IO -> new StorageIOBlockEntity(pos, ModBlocks.STORAGE_IO.get().defaultBlockState());
			case INPUT -> new StorageInputBlockEntity(pos, ModBlocks.STORAGE_INPUT.get().defaultBlockState());
			case OUTPUT -> new StorageOutputBlockEntity(pos, ModBlocks.STORAGE_OUTPUT.get().defaultBlockState());
		};
	}

	private static Fixture fixture() {
		ServerLevel level = mock(ServerLevel.class);
		ServerChunkCache chunks = mock(ServerChunkCache.class);
		LevelChunk chunk = mock(LevelChunk.class);
		AtomicBoolean loaded = new AtomicBoolean(true);
		Map<BlockPos, BlockEntity> blocks = new HashMap<>();
		when(level.getChunkSource()).thenReturn(chunks);
		when(level.isInValidBounds(any())).thenReturn(true);
		when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		when(chunks.getChunkNow(anyInt(), anyInt())).thenAnswer(invocation -> loaded.get() ? chunk : null);
		when(chunk.getBlockEntity(any(), eq(LevelChunk.EntityCreationType.IMMEDIATE))).thenAnswer(invocation -> blocks.get(invocation.getArgument(0)));
		TankBlockEntity tank = new TankBlockEntity(new BlockPos(4, 70, 4), ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		List<IControllableStorage> members = new ArrayList<>(List.of(tank));
		Fixture fixture = new Fixture(level, loaded, blocks, members, tank, null);
		ControllerBlockEntity controller = fixture.controller(new BlockPos(0, 70, 0));
		return new Fixture(level, loaded, blocks, members, tank, controller);
	}

	private enum Port {
		IO, INPUT, OUTPUT
	}

	private enum UnavailableState {
		EMPTY_MEMBERS, UNLOADED_CONTROLLER_CHUNK, REMOVED_CONTROLLER, UNLINKED_PORT, MISSING_CONTROLLER, UNLOADED_PORT_CHUNK, PORT_REMOVED;

		void makeUnavailable(Fixture fixture, StorageIOBlockEntity port) {
			switch (this) {
				case EMPTY_MEMBERS -> fixture.members.clear();
				case UNLOADED_CONTROLLER_CHUNK -> fixture.loaded.set(false);
				case REMOVED_CONTROLLER -> fixture.controller.setRemoved();
				case UNLINKED_PORT -> port.unregisterController();
				case MISSING_CONTROLLER -> fixture.blocks.remove(fixture.controller.getBlockPos());
				case UNLOADED_PORT_CHUNK -> port.onChunkUnloaded();
				case PORT_REMOVED -> port.setRemoved();
			}
		}

		void restore(Fixture fixture, StorageIOBlockEntity port) {
			switch (this) {
				case EMPTY_MEMBERS -> fixture.members.add(fixture.tank);
				case UNLOADED_CONTROLLER_CHUNK -> fixture.loaded.set(true);
				case REMOVED_CONTROLLER -> fixture.controller.clearRemoved();
				case UNLINKED_PORT -> port.registerController(fixture.controller);
				case MISSING_CONTROLLER -> fixture.blocks.put(fixture.controller.getBlockPos(), fixture.controller);
				case UNLOADED_PORT_CHUNK -> port.onLoad();
				case PORT_REMOVED -> throw new IllegalStateException("Port removal is not restored by these tests");
			}
		}
	}

	private record PortSide(Port kind, Direction side) {
	}
	private record TransferParams(Port kind, Direction side, boolean commit) {
	}
	private record LifecycleParams(Port kind, UnavailableState state) {
	}

	private record Fixture(ServerLevel level, AtomicBoolean loaded, Map<BlockPos, BlockEntity> blocks, List<IControllableStorage> members, TankBlockEntity tank,
			ControllerBlockEntity controller) {
		StorageIOBlockEntity port(Port kind) {
			StorageIOBlockEntity port = createPort(kind);
			port.setLevel(level);
			port.registerController(controller);
			return port;
		}

		TankFluidHandler nativeFluid() {
			return tank.getStorageWrapper().getTankFluidHandler();
		}

		ControllerBlockEntity controller(BlockPos pos) {
			ControllerBlockEntity controller = new ControllerBlockEntity(pos, ModBlocks.CONTROLLER.get().defaultBlockState()) {
				@Override
				protected List<IControllableStorage> getLoadedStorageRepresentatives() {
					return members;
				}
			};
			controller.setLevel(level);
			blocks.put(pos, controller);
			return controller;
		}
	}
}
