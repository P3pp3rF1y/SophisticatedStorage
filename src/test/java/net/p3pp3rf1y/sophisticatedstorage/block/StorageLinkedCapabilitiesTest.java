package net.p3pp3rf1y.sophisticatedstorage.block;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.TankPosition;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageLinkedCapabilitiesTest {
	@BeforeAll
	static void bootstrapMinecraft() throws ReflectiveOperationException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		if (!Config.SERVER_SPEC.isLoaded()) {
			CommentedConfig config = CommentedConfig.inMemory();
			Config.SERVER_SPEC.correct(config);
			Constructor<?> constructor = Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(CommentedConfig.class, Path.class,
					ModConfig.class);
			constructor.setAccessible(true);
			Config.SERVER_SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
		}
	}

	@Test
	void canonicalFluidAndEnergyHandlersAreSharedAcrossPlacedEndpoints() {
		IStorageWrapper host = mock(IStorageWrapper.class);
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		IStorageFluidHandler fluid = mock(IStorageFluidHandler.class);
		EnergyHandler energy = mock(EnergyHandler.class);
		when(host.getFluidHandler()).thenReturn(Optional.of(fluid));
		when(host.getEnergyHandler()).thenReturn(Optional.of(energy));
		StorageWrapper first = wrapper();
		StorageWrapper second = wrapper();
		first.setLinkedStorage(host);
		second.setLinkedStorage(host);

		assertSame(fluid, first.getFluidHandler().orElseThrow());
		assertSame(first.getFluidHandler().orElseThrow(), second.getFluidHandler().orElseThrow());
		assertSame(energy, first.getEnergyHandler().orElseThrow());
		assertSame(first.getEnergyHandler().orElseThrow(), second.getEnergyHandler().orElseThrow());
	}

	@Test
	void canonicalHandlerReplacementInvalidatesOnceNotForOrdinaryUpdates() {
		IStorageWrapper host = mock(IStorageWrapper.class);
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		StorageWrapper endpoint = wrapper();
		AtomicInteger invalidations = new AtomicInteger();
		endpoint.setUpgradeCachesInvalidatedHandler(invalidations::incrementAndGet);
		endpoint.setLinkedStorage(host);
		endpoint.onContentsUpdated();
		int afterBinding = invalidations.get();
		when(host.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		when(host.getEnergyHandler()).thenReturn(Optional.of(mock(EnergyHandler.class)));
		endpoint.onContentsUpdated();
		endpoint.onContentsUpdated();
		assertEquals(afterBinding + 1, invalidations.get());
		endpoint.setLinkedStorage(null);
		assertEquals(afterBinding + 2, invalidations.get());
	}

	@Test
	void subscribedGroupNotificationInvalidatesOnlyWhenCanonicalHandlerIdentityChanges() {
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		ServerLevel level = mock(ServerLevel.class);
		block.setLevel(level);
		when(level.players()).thenReturn(List.of());
		LinkedStorageGroupsSavedData savedData = mock(LinkedStorageGroupsSavedData.class);
		LinkedStorageGroupManager manager = mock(LinkedStorageGroupManager.class);
		when(savedData.manager()).thenReturn(manager);
		UUID groupId = UUID.randomUUID();
		LinkedStorageEndpointData endpoint = new LinkedStorageEndpointData(groupId, UUID.randomUUID());
		IStorageWrapper host = mock(IStorageWrapper.class,
				withSettings().extraInterfaces(net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost.class));
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(host.getUpgradeHandler()).thenReturn(mock(net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler.class));
		when(host.getRenderDataHandler()).thenReturn(mock(net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler.class));
		when(host.getRenderDataHandler().getData()).thenReturn(new RenderData());
		when(manager.resolveVirtualHost(endpoint, false))
				.thenReturn(Optional.of((net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost) host));
		AtomicReference<Runnable> groupChange = new AtomicReference<>();
		when(manager.subscribeToGroupChanges(eq(groupId), any())).thenAnswer(invocation -> {
			groupChange.set(invocation.getArgument(1));
			return (Runnable) () -> {
			};
		});
		when(manager.subscribeToRootContentsReplacements(eq(groupId), any())).thenReturn(() -> {
		});
		AtomicInteger invalidations = new AtomicInteger();
		block.getStorageWrapper().setUpgradeCachesInvalidatedHandler(invalidations::incrementAndGet);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			assertTrue(block.bindLinkedStorage(level, endpoint));
			int afterBinding = invalidations.get();
			groupChange.get().run();
			assertEquals(afterBinding, invalidations.get());
			when(host.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
			groupChange.get().run();
			assertEquals(afterBinding + 1, invalidations.get());
			groupChange.get().run();
			assertEquals(afterBinding + 1, invalidations.get());
		}
	}

	@Test
	void unlinkedPackedAndRemovedPlacedStorageNeverResolvesNonItemHandlers() {
		ServerLevel level = mock(ServerLevel.class);
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		block.setLevel(level);
		assertNull(block.getExternalFluidHandler(Direction.UP));
		assertNull(block.getExternalEnergyHandler(null));
		block.setPacked(true);
		assertNull(block.getExternalFluidHandler(null));
		assertNull(block.getExternalEnergyHandler(Direction.DOWN));
		block.setRemoved();
		assertNull(block.getExternalFluidHandler(Direction.UP));
		verify(level, never()).getDataStorage();
	}

	@Test
	void canonicalLayoutProjectsToEveryEndpointWithoutChangingLocalLayout() {
		IStorageWrapper host = mock(IStorageWrapper.class);
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		AtomicInteger columns = new AtomicInteger(4);
		when(host.getColumnsTaken()).thenAnswer(invocation -> columns.get());
		doAnswer(invocation -> {
			columns.set(invocation.getArgument(0));
			return null;
		}).when(host).setColumnsTaken(anyInt(), anyBoolean());
		StorageWrapper first = wrapper();
		StorageWrapper second = wrapper();
		first.setColumnsTaken(1, false);
		first.setLinkedStorage(host);
		second.setLinkedStorage(host);
		assertEquals(4, first.getColumnsTaken());
		assertEquals(4, second.getColumnsTaken());
		first.setColumnsTaken(2, true);
		assertEquals(2, second.getColumnsTaken());
		first.setLinkedStorage(null);
		assertEquals(1, first.getColumnsTaken());
	}

	@Test
	void localPhysicalRenderDoesNotPersistCanonicalTankOrBatteryProjection() {
		StorageWrapper endpoint = wrapper();
		RenderData canonical = new RenderData()
				.withTank(TankPosition.LEFT, new RenderData.TankRenderData((net.neoforged.neoforge.fluids.FluidStackTemplate) null, 0.5f))
				.withBattery(new RenderData.BatteryRenderData(0.7f));
		endpoint.synchronizeLinkedRenderInfo(canonical);
		assertTrue(endpoint.getRenderDataHandler().getData().tanks().isEmpty());
		assertTrue(endpoint.getRenderDataHandler().getData().battery().isEmpty());
		assertFalse(canonical.tanks().isEmpty());
	}

	@Test
	void linkedNeighborChangeDispatchesOnlyAtPrimaryEndpoint() {
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		UUID group = UUID.randomUUID();
		UUID endpoint = UUID.randomUUID();
		CompoundTag saved = ValueIOHelper.collectOutputToTag(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), out -> {
			var data = out.child("linkedStorageEndpoint");
			data.store("groupId", UUIDUtil.CODEC, group);
			data.store("endpointId", UUIDUtil.CODEC, endpoint);
		});
		block.loadSynchronizedData(ValueIOHelper.inputFromCompoundTag(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), saved));
		ServerLevel level = mock(ServerLevel.class);
		block.setLevel(level);
		IStorageWrapper host = mock(IStorageWrapper.class);
		var upgrades = mock(net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler.class);
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(host.getUpgradeHandler()).thenReturn(upgrades);
		INeighborChangeListenerUpgrade listener = mock(INeighborChangeListenerUpgrade.class);
		when(upgrades.getWrappersThatImplement(INeighborChangeListenerUpgrade.class)).thenReturn(List.of(listener));
		block.getStorageWrapper().setLinkedStorage(host);
		LinkedStorageGroupsSavedData data = mock(LinkedStorageGroupsSavedData.class);
		LinkedStorageGroupManager manager = mock(LinkedStorageGroupManager.class);
		when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			block.onNeighborChange(BlockPos.ZERO.above());
			verify(listener, never()).onNeighborChange(any(), any(), any());
			when(manager.isPrimaryEndpoint(group, endpoint)).thenReturn(true);
			block.onNeighborChange(BlockPos.ZERO.above());
			verify(listener).onNeighborChange(level, BlockPos.ZERO, Direction.UP);
		}
	}

	private static StorageWrapper wrapper() {
		return new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState()).getStorageWrapper();
	}
}
