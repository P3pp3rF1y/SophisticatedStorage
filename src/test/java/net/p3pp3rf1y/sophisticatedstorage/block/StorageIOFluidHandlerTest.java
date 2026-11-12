package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageIOFluidHandlerTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@ParameterizedTest
	@MethodSource("portSides")
	void getExternalFluidHandlerExposesNetworkOnEverySideIncludingUnsidedQueries(PortSide params) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(params.kind);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);

		IFluidHandler handler = port.getExternalFluidHandler(params.side);

		assertNotNull(handler);
		assertEquals(1, handler.getTanks());
		assertEquals(16000, handler.getTankCapacity(0));
		assertEquals(params.kind == Port.INPUT ? 0 : 1000, handler.getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerReturnsNullWithoutLevelOrController(Port kind) {
		StorageIOBlockEntity port = createPort(kind);

		IFluidHandler handler = port.getExternalFluidHandler(null);

		assertNull(handler);
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerReturnsNullAfterUnlinking(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		port.unregisterController();

		IFluidHandler handler = port.getExternalFluidHandler(null);

		assertNull(handler);
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerAllowsClientDiscoveryWithoutAccessingTheController(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		when(fixture.level.isClientSide()).thenReturn(true);
		clearInvocations(fixture.level);

		IFluidHandler handler = port.getExternalFluidHandler(null);

		assertNotNull(handler);
		assertUnavailable(handler);
		assertEquals(1000, fixture.nativeFluid().getFluidInTank(0).getAmount());
		verify(fixture.level, never()).getChunkSource();
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void fillRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		int accepted = params.kind == Port.OUTPUT ? 0 : 500;

		int transferred = handler.fill(new FluidStack(Fluids.WATER, 500), params.action);

		assertEquals(accepted, transferred);
		assertEquals(1000 + (params.action == EXECUTE ? accepted : 0), fixture.nativeFluid().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void drainByAmountRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		int extracted = params.kind == Port.INPUT ? 0 : 250;

		FluidStack drained = handler.drain(250, params.action);

		assertEquals(extracted, drained.getAmount());
		assertEquals(1000 - (params.action == EXECUTE ? extracted : 0), fixture.nativeFluid().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@MethodSource("transferParams")
	void drainByResourceRespectsPortDirectionAndSimulation(TransferParams params) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(params.kind).getExternalFluidHandler(params.side);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		int extracted = params.kind == Port.INPUT ? 0 : 250;

		FluidStack drained = handler.drain(new FluidStack(Fluids.WATER, 250), params.action);

		assertEquals(extracted, drained.getAmount());
		assertEquals(1000 - (params.action == EXECUTE ? extracted : 0), fixture.nativeFluid().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void isFluidValidRespectsPortDirection(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);

		boolean valid = handler.isFluidValid(0, new FluidStack(Fluids.WATER, 100));

		assertEquals(kind != Port.OUTPUT, valid);
	}

	@ParameterizedTest
	@MethodSource("unavailableControllers")
	void getExternalFluidHandlerRetainedHandleStopsAccessWhenUnavailable(LifecycleParams params) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(params.kind);
		IFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);

		params.state.makeUnavailable(fixture, port);

		assertUnavailable(handler);
		assertEquals(1000, fixture.nativeFluid().getFluidInTank(0).getAmount());
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@MethodSource("restorableControllers")
	void getExternalFluidHandlerRetainedHandleResumesAccessAfterRestoration(LifecycleParams params) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(params.kind);
		IFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		fixture.nativeFluid().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		params.state.makeUnavailable(fixture, port);

		params.state.restore(fixture, port);

		assertEquals(1, handler.getTanks());
		assertEquals(16000, handler.getTankCapacity(0));
		assertEquals(params.kind == Port.INPUT ? 0 : 1000, handler.getFluidInTank(0).getAmount());
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void getExternalFluidHandlerRetainedHandleFollowsRelinking(Port kind) {
		Fixture fixture = fixture();
		StorageIOBlockEntity port = fixture.port(kind);
		IFluidHandler handler = port.getExternalFluidHandler(Direction.NORTH);
		ControllerBlockEntity replacement = fixture.controller(fixture.controller.getBlockPos().east());
		fixture.blocks.remove(fixture.controller.getBlockPos());

		port.registerController(replacement);

		assertEquals(1, handler.getTanks());
		assertEquals(16000, handler.getTankCapacity(0));
		verify(fixture.level, never()).getBlockEntity(any());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void fillRejectsDifferentFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);

		int accepted = handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);

		assertEquals(0, accepted);
		assertEquals(1000, fixture.nativeFluid().getFluidInTank(0).getAmount());
		assertTrue(special.isFluidEqual(fixture.nativeFluid().getFluidInTank(0)));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void drainRejectsDifferentFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);

		FluidStack drained = handler.drain(new FluidStack(Fluids.WATER, 1000), EXECUTE);

		assertTrue(drained.isEmpty());
		assertEquals(1000, fixture.nativeFluid().getFluidInTank(0).getAmount());
		assertTrue(special.isFluidEqual(fixture.nativeFluid().getFluidInTank(0)));
	}

	@ParameterizedTest
	@EnumSource(value = Port.class, names = {"IO", "OUTPUT"})
	void getFluidInTankPreservesFluidComponents(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);

		FluidStack view = handler.getFluidInTank(0);

		assertEquals(1000, view.getAmount());
		assertTrue(special.isFluidEqual(view));
	}

	@ParameterizedTest
	@EnumSource(value = Port.class, names = {"IO", "OUTPUT"})
	void getFluidInTankReturnsADefensiveCopy(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);
		FluidStack view = handler.getFluidInTank(0);

		view.setAmount(1);

		assertEquals(1000, handler.getFluidInTank(0).getAmount());
		assertEquals(1000, fixture.nativeFluid().getFluidInTank(0).getAmount());
		assertTrue(special.isFluidEqual(fixture.nativeFluid().getFluidInTank(0)));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void drainPreservesMatchingFluidComponentsWhenAllowed(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);
		int extracted = kind == Port.INPUT ? 0 : 250;

		FluidStack drained = handler.drain(new FluidStack(special, 250), EXECUTE);

		assertEquals(extracted, drained.getAmount());
		assertEquals(1000 - extracted, fixture.nativeFluid().getFluidInTank(0).getAmount());
		if (extracted > 0) {
			assertTrue(special.isFluidEqual(drained));
		}
		assertTrue(special.isFluidEqual(fixture.nativeFluid().getFluidInTank(0)));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void fillPreservesMatchingFluidComponentsWhenAllowed(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		FluidStack special = componentWater();
		fixture.nativeFluid().fill(special, EXECUTE);
		int expected = kind == Port.OUTPUT ? 0 : 100;

		int accepted = handler.fill(new FluidStack(special, 100), EXECUTE);

		assertEquals(expected, accepted);
		assertEquals(1000 + expected, fixture.nativeFluid().getFluidInTank(0).getAmount());
		assertTrue(special.isFluidEqual(fixture.nativeFluid().getFluidInTank(0)));
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void fillRejectsFluidWhenEmptyTankIsLocked(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		fixture.tank.toggleLock();

		int accepted = handler.fill(componentWater(), EXECUTE);

		assertEquals(0, accepted);
		assertTrue(fixture.nativeFluid().getFluidInTank(0).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(Port.class)
	void isFluidValidRejectsFluidWhenEmptyTankIsLocked(Port kind) {
		Fixture fixture = fixture();
		IFluidHandler handler = fixture.port(kind).getExternalFluidHandler(null);
		fixture.tank.toggleLock();

		boolean valid = handler.isFluidValid(0, componentWater());

		assertFalse(valid);
	}

	private static Stream<PortSide> portSides() {
		return Arrays.stream(Port.values())
				.flatMap(kind -> Stream.concat(Arrays.stream(Direction.values()), Stream.of((Direction) null)).map(side -> new PortSide(kind, side)));
	}

	private static Stream<TransferParams> transferParams() {
		return portSides().flatMap(port -> Arrays.stream(FluidAction.values()).map(action -> new TransferParams(port.kind, port.side, action)));
	}

	private static Stream<LifecycleParams> unavailableControllers() {
		return Arrays.stream(Port.values()).flatMap(kind -> Arrays.stream(UnavailableState.values()).map(state -> new LifecycleParams(kind, state)));
	}

	private static Stream<LifecycleParams> restorableControllers() {
		return unavailableControllers().filter(params -> params.state != UnavailableState.PORT_REMOVED);
	}

	private static FluidStack componentWater() {
		FluidStack special = new FluidStack(Fluids.WATER, 1000);
		CompoundTag identity = new CompoundTag();
		identity.putString("identity", "special");
		special.setTag(identity);
		return special;
	}

	private static void assertUnavailable(IFluidHandler handler) {
		int filled = handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
		FluidStack drainedByAmount = handler.drain(1000, EXECUTE);
		FluidStack drainedByResource = handler.drain(new FluidStack(Fluids.WATER, 1000), EXECUTE);

		assertEquals(0, handler.getTanks());
		assertEquals(0, handler.getTankCapacity(0));
		assertTrue(handler.getFluidInTank(0).isEmpty());
		assertFalse(handler.isFluidValid(0, new FluidStack(Fluids.WATER, 1000)));
		assertEquals(0, filled);
		assertTrue(drainedByAmount.isEmpty());
		assertTrue(drainedByResource.isEmpty());
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
		when(level.isInWorldBounds(any())).thenReturn(true);
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

	private record TransferParams(Port kind, Direction side, FluidAction action) {
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
