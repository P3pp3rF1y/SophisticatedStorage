package net.p3pp3rf1y.sophisticatedstorage.block;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageLinkedCapabilitiesTest {
	@Test
	void reloadFromStorageContentsKeepsStorageCategoryEditsInTheSettingsCompound() {
		StorageWrapper wrapper = wrapper();
		SettingsHandler handler = wrapper.getSettingsHandler();
		MemorySettingsCategory memory = handler.getTypeCategory(MemorySettingsCategory.class);
		ItemDisplaySettingsCategory display = handler.getTypeCategory(ItemDisplaySettingsCategory.class);
		CompoundTag root = new CompoundTag();
		CompoundTag settings = new CompoundTag();
		settings.putString("preserved", "current-settings");
		root.put("settings", settings);

		try (MockedStatic<RegistryHelper> registryHelper = Mockito.mockStatic(RegistryHelper.class, Mockito.CALLS_REAL_METHODS)) {
			registryHelper.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
			handler.reloadFromStorageContents(root);
			memory.setFilter(0, new ItemStack(Items.DIAMOND));
			display.selectSlot(0);
		}

		assertEquals("current-settings", handler.getNbt().getString("preserved"));
		assertTrue(memory.getSlotFilterStack(0, false).orElseThrow().is(Items.DIAMOND));
		assertTrue(display.getSlots().contains(0));
		assertFalse(handler.getNbt().contains("settings"));
		assertSame(memory, handler.getTypeCategory(MemorySettingsCategory.class));
	}
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
	void changeSizeRejectsBackpackNativeHosts() {
		IStorageWrapper backpackHost = host();
		StorageWrapper endpoint = wrapper();
		endpoint.setLinkedStorage(backpackHost);

		assertThrows(IllegalStateException.class, () -> endpoint.changeSize(9, 1));
	}

	@Test
	void changeSizeDelegatesStorageNativeResizing() {
		StorageWrapper endpoint = wrapper();
		StorageWrapper storageHost = mock(StorageWrapper.class);
		when(storageHost.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(storageHost.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		when(storageHost.getFluidHandler()).thenReturn(Optional.empty());
		when(storageHost.getEnergyStorage()).thenReturn(Optional.empty());
		endpoint.setLinkedStorage(storageHost);

		endpoint.changeSize(9, 1);

		verify(storageHost).changeSize(9, 1);
	}

	@Test
	void onNeighborChangeDispatchesOnlyPrimaryLinkedEndpoint() {
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		UUID group = UUID.randomUUID();
		UUID endpoint = UUID.randomUUID();
		CompoundTag saved = new CompoundTag();
		CompoundTag binding = new CompoundTag();
		binding.putUUID("groupId", group);
		binding.putUUID("endpointId", endpoint);
		saved.put("linkedStorageEndpoint", binding);
		block.loadAdditional(saved, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		ServerLevel level = mock(ServerLevel.class);
		block.setLevel(level);
		IStorageWrapper host = host();
		INeighborChangeListenerUpgrade listener = mock(INeighborChangeListenerUpgrade.class);
		when(host.getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class)).thenReturn(List.of(listener));
		block.getStorageWrapper().setLinkedStorage(host);
		LinkedStorageGroupsSavedData data = mock(LinkedStorageGroupsSavedData.class);
		LinkedStorageGroupManager manager = mock(LinkedStorageGroupManager.class);
		when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);

			block.onNeighborChange(BlockPos.ZERO.above());
			boolean ignoredSecondary = mockingDetails(listener).getInvocations().isEmpty();
			when(manager.isPrimaryEndpoint(group, endpoint)).thenReturn(true);
			block.onNeighborChange(BlockPos.ZERO.above());

			assertTrue(ignoredSecondary);
			verify(listener).onNeighborChange(level, BlockPos.ZERO, Direction.UP);
		}
	}

	@Test
	void onNeighborChangeDispatchesOrdinaryWrapperListenersWithoutConsultingLinkedGroups() {
		BarrelBlockEntity ordinary = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		ServerLevel level = mock(ServerLevel.class);
		ordinary.setLevel(level);
		StorageWrapper nativeWrapper = ordinary.getStorageWrapper();
		ItemStack hopper = new ItemStack(ModItems.HOPPER_UPGRADE.get());
		hopper.set(ModDataComponents.PUSH_DIRECTIONS, Set.of(Direction.UP));
		hopper.set(ModDataComponents.PULL_DIRECTIONS, Set.of());
		nativeWrapper.getUpgradeHandler().setStackInSlot(0, hopper);
		when(level.getBlockState(BlockPos.ZERO)).thenReturn(Blocks.BARREL.defaultBlockState());
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = mockStatic(LinkedStorageGroupsSavedData.class)) {
			clearInvocations(level);

			ordinary.onNeighborChange(BlockPos.ZERO.above());

			assertNull(ordinary.getLinkedStorageEndpointData());
			assertTrue(nativeWrapper.getLinkedStorageEndpoint().isEmpty());
			assertEquals(1, nativeWrapper.getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class).size());
			verify(level).getBlockState(BlockPos.ZERO);
			groups.verifyNoInteractions();
		}
	}

	@Test
	void getColumnsTakenFollowsCanonicalLayoutChangesAcrossEndpointsWithoutOverwritingLocalLayout() {
		IStorageWrapper host = host();
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

		int initialFirst = first.getColumnsTaken();
		int initialSecond = second.getColumnsTaken();
		first.setColumnsTaken(2, true);
		int updatedFirst = first.getColumnsTaken();
		int updatedSecond = second.getColumnsTaken();
		columns.set(0);
		int clearedFirst = first.getColumnsTaken();
		int clearedSecond = second.getColumnsTaken();
		first.setLinkedStorage(null);

		assertEquals(4, initialFirst);
		assertEquals(4, initialSecond);
		assertEquals(2, updatedFirst);
		assertEquals(2, updatedSecond);
		verify(host).setColumnsTaken(2, true);
		assertEquals(0, clearedFirst);
		assertEquals(0, clearedSecond);
		assertEquals(1, first.getColumnsTaken());
	}

	@Test
	void getFluidHandlerAndEnergyStorageShareCanonicalNonItemHandlersAcrossEndpoints() {
		IStorageWrapper host = host();
		IStorageFluidHandler fluid = mock(IStorageFluidHandler.class);
		IEnergyStorage energy = mock(IEnergyStorage.class);
		when(host.getFluidHandler()).thenReturn(Optional.of(fluid));
		when(host.getEnergyStorage()).thenReturn(Optional.of(energy));
		StorageWrapper first = wrapper();
		StorageWrapper second = wrapper();

		first.setLinkedStorage(host);
		second.setLinkedStorage(host);

		assertSame(fluid, first.getFluidHandler().orElseThrow());
		assertSame(first.getFluidHandler().orElseThrow(), second.getFluidHandler().orElseThrow());
		assertSame(energy, first.getEnergyStorage().orElseThrow());
		assertSame(first.getEnergyStorage().orElseThrow(), second.getEnergyStorage().orElseThrow());
	}

	@Test
	void onContentsNbtUpdatedInvalidatesOnceForReplacementButNotResourceOrRenderChanges() {
		IStorageWrapper host = host();
		StorageWrapper endpoint = wrapper();
		AtomicInteger invalidations = new AtomicInteger();
		endpoint.setUpgradeCachesInvalidatedHandler(invalidations::incrementAndGet);

		endpoint.setLinkedStorage(host);
		endpoint.setLinkedStorage(host);
		endpoint.onContentsNbtUpdated();
		int afterBinding = invalidations.get();
		when(host.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		when(host.getEnergyStorage()).thenReturn(Optional.of(mock(IEnergyStorage.class)));
		endpoint.onContentsNbtUpdated();
		endpoint.onContentsNbtUpdated();
		int afterReplacement = invalidations.get();
		endpoint.setLinkedStorage(null);
		endpoint.setLinkedStorage(null);

		assertEquals(1, afterBinding);
		assertEquals(2, afterReplacement);
		assertEquals(3, invalidations.get());
		assertTrue(endpoint.getFluidHandler().isEmpty());
		assertTrue(endpoint.getEnergyStorage().isEmpty());
	}

	@Test
	void getFluidHandlerAndEnergyStorageRemainAbsentForStorageMainAndUnlinkedEndpoints() {
		StorageWrapper endpoint = wrapper();
		boolean unlinkedFluidAbsent = endpoint.getFluidHandler().isEmpty();
		boolean unlinkedEnergyAbsent = endpoint.getEnergyStorage().isEmpty();

		endpoint.setLinkedStorage(host());

		assertTrue(unlinkedFluidAbsent);
		assertTrue(unlinkedEnergyAbsent);
		assertTrue(endpoint.getFluidHandler().isEmpty());
		assertTrue(endpoint.getEnergyStorage().isEmpty());
	}

	@Test
	void onContentsNbtUpdatedInvalidatesPlacedCapabilityListenersOncePerHandlerReplacement() {
		ServerLevel level = mock(ServerLevel.class);
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		block.setLevel(level);
		IStorageWrapper host = host();
		StorageWrapper endpoint = block.getStorageWrapper();

		endpoint.setLinkedStorage(host);
		endpoint.onContentsNbtUpdated();
		long afterBinding = mockingDetails(level).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("invalidateCapabilities")).count();
		when(host.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		when(host.getEnergyStorage()).thenReturn(Optional.of(mock(IEnergyStorage.class)));
		endpoint.onContentsNbtUpdated();
		endpoint.onContentsNbtUpdated();
		long afterReplacement = mockingDetails(level).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("invalidateCapabilities")).count();
		endpoint.setLinkedStorage(null);
		endpoint.setLinkedStorage(null);

		assertEquals(afterBinding + 1, afterReplacement);
		verify(level, times(3)).invalidateCapabilities(BlockPos.ZERO);
	}

	@Test
	void getExternalHandlersRejectUnlinkedPackedAndRemovedEndpointsWithoutResolvingHost() {
		ServerLevel level = mock(ServerLevel.class);
		BarrelBlockEntity block = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
		block.setLevel(level);

		Object unlinkedFluid = block.getExternalFluidHandler(Direction.UP);
		Object unlinkedEnergy = block.getExternalEnergyStorage(null);
		block.setPacked(true);
		Object packedFluid = block.getExternalFluidHandler(null);
		Object packedEnergy = block.getExternalEnergyStorage(Direction.DOWN);
		block.setRemoved();

		assertNull(unlinkedFluid);
		assertNull(unlinkedEnergy);
		assertNull(packedFluid);
		assertNull(packedEnergy);
		assertNull(block.getExternalFluidHandler(Direction.UP));
		assertNull(block.getExternalEnergyStorage(null));
		verify(level, never()).getDataStorage();
	}

	@Test
	void synchronizeLinkedRenderInfoDoesNotPersistTankOrBatteryProjection() {
		StorageWrapper endpoint = wrapper();
		CompoundTag canonical = new CompoundTag();
		canonical.put("tanks", new CompoundTag());
		canonical.put("battery", new CompoundTag());

		endpoint.synchronizeLinkedRenderInfo(canonical);

		assertFalse(endpoint.getRenderInfoNbt().contains("tanks"));
		assertFalse(endpoint.getRenderInfoNbt().contains("battery"));
		assertTrue(canonical.contains("tanks"));
		assertTrue(canonical.contains("battery"));
	}

	private static IStorageWrapper host() {
		IStorageWrapper host = mock(IStorageWrapper.class);
		when(host.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(host.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		when(host.getFluidHandler()).thenReturn(Optional.empty());
		when(host.getEnergyStorage()).thenReturn(Optional.empty());
		return host;
	}

	private static StorageWrapper wrapper() {
		return new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState()).getStorageWrapper();
	}
}
