package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankLinkedStorageTest {
	private MockedStatic<RegistryHelper> registry;
	private ServerLevel level;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.NETHERITE_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.IRON_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.CHEST_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.PUMP_UPGRADE.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.STACK_UPGRADE_TIER_1.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@BeforeEach
	void mockRegistryAccess() {
		RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(registries));
		level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries);
	}

	@AfterEach
	void closeRegistryMock() {
		ClientLinkedStorageContents.clear();
		registry.close();
	}

	@Test
	void createUsesTheNestedClientCarrierAndCanonicalFluidForItemRendering() {
		HostFixture fixture = sharedHost();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		tank.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(fixture.id, UUID.randomUUID()));
		tank.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT, true);
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, TankLinkedStorageHostWrapper.FACTORY_ID.toString());
		descriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG, fixture.host.getVirtualCarrierSnapshot().orElseThrow());
		ClientLinkedStorageContents.updateHostDescriptor(fixture.id, descriptor);
		ClientLinkedStorageContents.updateContents(fixture.id, 1, fixture.contents.contents(), Component.literal("Shared"), 0, 2, 0);

		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(tank);

		assertEquals(96000, wrapper.getCapacity());
		assertEquals(50000, wrapper.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(FluidResource.of(Fluids.WATER), wrapper.getTankFluidHandler().getResource(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "sophisticatedstorage:storage"})
	void createKeepsClientContentsUnresolvedWithoutATankHostDescriptor(String factoryId) {
		HostFixture fixture = sharedHost();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		tank.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(fixture.id, UUID.randomUUID()));
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, factoryId);
		ClientLinkedStorageContents.updateHostDescriptor(fixture.id, descriptor);
		ClientLinkedStorageContents.updateContents(fixture.id, 1, fixture.contents.contents(), Component.literal("Shared"), 0, 2, 0);

		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(tank);

		assertEquals(TankTier.IRON.capacity(1, 1), wrapper.getCapacity());
		assertEquals(0, wrapper.getTankFluidHandler().getAmountAsLong(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {"sophisticatedstorage:storage", "sophisticatedbackpacks:backpack", "minecraft:chest"})
	void getCompatibilityRejectsOtherHostFamilies(String factoryId) {
		var adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get());
		var descriptor = new LinkedStorageHostDescriptor(Identifier.parse(factoryId), new CompoundTag());

		assertEquals(Compatibility.INCOMPATIBLE, adapter.getCompatibility(level, tank, descriptor));
	}

	@Test
	void supportsExcludesTankItemsFromTheGenericStorageAdapter() {
		assertFalse(new StorageItemLinkedStorageEndpointAdapter().supports(new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get())));
	}

	@Test
	void supportsRejectsOrdinaryStorageItems() {
		assertFalse(new TankItemLinkedStorageEndpointAdapter().supports(new ItemStack(ModBlocks.CHEST_ITEM.get())));
	}

	@Test
	void supportsRejectsStackedTankItems() {
		assertFalse(new TankItemLinkedStorageEndpointAdapter().supports(new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get(), 64)));
	}

	@Test
	void createHostDescriptorRetainsThePortableHeightAndTankFactory() {
		ItemStack tank = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get());
		tank.set(ModDataComponents.TANK_HEIGHT, 5);

		LinkedStorageHostDescriptor descriptor = new TankItemLinkedStorageEndpointAdapter().createHostDescriptor(level, tank);

		assertEquals(TankLinkedStorageHostWrapper.FACTORY_ID, descriptor.factoryId());
		assertEquals(5, descriptor.virtualCarrier().getIntOr("height", 0));
	}

	@Test
	void getCompatibilityAcceptsAnEmptyTankOfAnotherTier() {
		var adapter = new TankItemLinkedStorageEndpointAdapter();
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get()));

		assertEquals(Compatibility.COMPATIBLE, adapter.getCompatibility(level, new ItemStack(ModBlocks.TANK_ITEM.get()), descriptor));
	}

	@ParameterizedTest
	@EnumSource(ContentsKind.class)
	void getCompatibilityRejectsPopulatedItems(ContentsKind kind) {
		var adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, tank);
		populate(new TankItemStorageWrapper(tank), kind);

		assertEquals(Compatibility.HAS_CONTENTS, adapter.getCompatibility(level, tank, descriptor));
	}

	@ParameterizedTest
	@EnumSource(ContentsKind.class)
	void getCompatibilityAcceptsItemsAfterTheirLastContentsAreRemoved(ContentsKind kind) {
		var adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, tank);
		var wrapper = new TankItemStorageWrapper(tank);
		populate(wrapper, kind);
		switch (kind) {
			case FLUID -> extract(wrapper.getTankFluidHandler(), 1234);
			case UPGRADE -> wrapper.getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
			case CONTAINER -> wrapper.getContainerInventory().set(0, ItemResource.EMPTY, 0);
		}

		assertEquals(Compatibility.COMPATIBLE, adapter.getCompatibility(level, tank, descriptor));
	}

	@Test
	void copyCanonicalContentsDoesNotAliasTheItemFluid() {
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		populate(new TankItemStorageWrapper(tank), ContentsKind.FLUID);
		ContainerContents canonical = new TankItemLinkedStorageEndpointAdapter().copyCanonicalContents(level, tank);

		canonical.tank().setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);

		assertEquals(1234, new TankItemStorageWrapper(tank).getTankFluidHandler().getAmountAsLong(0));
	}

	@Test
	void createReloadsPersistedItemUpgrades() {
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		populate(new TankItemStorageWrapper(tank), ContentsKind.UPGRADE);

		var restored = new TankItemStorageWrapper(tank);

		assertTrue(restored.getUpgradeHandler().getStackInSlot(0).is(ModItems.PUMP_UPGRADE));
	}

	@Test
	void createReloadsCanonicalFluidAndGroupIdentity() {
		HostFixture fixture = sharedHost();

		TankLinkedStorageHostWrapper restored = TankLinkedStorageHostWrapper.create(fixture.contents, fixture.host.getVirtualCarrierSnapshot().orElseThrow());

		assertEquals(50000, restored.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(Optional.of(fixture.id), restored.getContentsUuid());
	}

	@Test
	void onVirtualCarrierChangedUpdatesCapacityWithoutLosingCanonicalFluid() {
		HostFixture fixture = sharedHost();
		CompoundTag carrier = TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true);

		fixture.host.onVirtualCarrierChanged(carrier);

		assertEquals(128000, fixture.host.getCapacity());
		assertEquals(50000, fixture.host.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(Optional.of(fixture.id), fixture.host.getContentsUuid());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillOnALockedHostAcceptsOnlyTheExistingFluid(boolean matching) {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		clearInvocations(fixture.contents);

		int filled = insert(fixture.host.getTankFluidHandler(), FluidResource.of(matching ? Fluids.WATER : Fluids.LAVA), 1000);

		assertEquals(matching ? 1000 : 0, filled);
		assertEquals(matching ? 51000 : 50000, fixture.host.getTankFluidHandler().getAmountAsLong(0));
		if (matching) {
			verify(fixture.contents, atLeastOnce()).markChanged();
		}
	}

	@Test
	void drainOnALockedHostRemovesAllMatchingFluid() {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		insert(fixture.host.getTankFluidHandler(), FluidResource.of(Fluids.WATER), 1000);

		assertEquals(51000, extract(fixture.host.getTankFluidHandler(), 51000));
		assertTrue(fixture.host.getTankFluidHandler().getFluidInTank(0).isEmpty());
	}

	@Test
	void fillRejectsAnEmptyLockedHostAfterDraining() {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		extract(fixture.host.getTankFluidHandler(), 50000);

		assertEquals(0, insert(fixture.host.getTankFluidHandler(), FluidResource.of(Fluids.WATER), 1));
		assertTrue(fixture.host.getTankFluidHandler().getFluidInTank(0).isEmpty());
	}

	private static int insert(TankFluidHandler handler, FluidResource resource, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			int result = handler.insert(0, resource, amount, tx, true);
			tx.commit();
			return result;
		}
	}

	private static int extract(TankFluidHandler handler, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			int result = handler.extract(0, FluidResource.of(Fluids.WATER), amount, tx, true);
			tx.commit();
			return result;
		}
	}

	private static void populate(TankItemStorageWrapper wrapper, ContentsKind kind) {
		switch (kind) {
			case FLUID -> insert(wrapper.getTankFluidHandler(), FluidResource.of(Fluids.WATER), 1234);
			case UPGRADE -> {
				wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
				assertTrue(wrapper.getWrappedStorageStack().has(ModDataComponents.TANK_UPGRADES));
			}
			case CONTAINER -> wrapper.getContainerInventory().set(0, ItemResource.of(new ItemStack(Items.BUCKET)), 1);
		}
	}

	private static HostFixture sharedHost() {
		var contents = mock(ILinkedStorageContents.class);
		UUID id = UUID.randomUUID();
		when(contents.contents()).thenReturn(new ContainerContents());
		when(contents.groupId()).thenReturn(id);
		TankLinkedStorageHostWrapper host = TankLinkedStorageHostWrapper.create(contents,
				TankLinkedStorageHostWrapper.carrier(TankTier.IRON, 3, Component.literal("Shared"), false));
		host.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		insert(host.getTankFluidHandler(), FluidResource.of(Fluids.WATER), 50000);
		return new HostFixture(contents, id, host);
	}

	private enum ContentsKind {
		FLUID, UPGRADE, CONTAINER
	}

	private record HostFixture(ILinkedStorageContents contents, UUID id, TankLinkedStorageHostWrapper host) {
	}
}
