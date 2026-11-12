package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankLinkedStorageTest {
	private ServerLevel level;

	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@org.junit.jupiter.api.BeforeEach
	void mockLevel() {
		level = mock(ServerLevel.class);
	}

	@AfterEach
	void clearClientSnapshots() {
		ClientLinkedStorageContents.clear();
	}

	@Test
	void createUsesTheNestedClientCarrierAndCanonicalFluidForItemRendering() {
		HostFixture fixture = sharedHost();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageStackData.setEndpoint(tank, new LinkedStorageEndpointData(fixture.id, UUID.randomUUID()));
		LinkedStorageStackData.setPrimaryEndpoint(tank, true);
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, TankLinkedStorageHostWrapper.FACTORY_ID.toString());
		descriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG, fixture.host.getVirtualCarrierSnapshot().orElseThrow());
		ClientLinkedStorageContents.updateHostDescriptor(fixture.id, descriptor);
		ClientLinkedStorageContents.updateContents(fixture.id, 1, fixture.contents.getContents(), Component.literal("Shared"), 0, 2, 0);

		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(tank);

		assertEquals(96000, wrapper.getCapacity());
		assertEquals(50000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertSame(Fluids.WATER, wrapper.getTankFluidHandler().getFluidInTank(0).getFluid());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "sophisticatedstorage:storage"})
	void createKeepsClientContentsUnresolvedWithoutATankHostDescriptor(String factoryId) {
		HostFixture fixture = sharedHost();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageStackData.setEndpoint(tank, new LinkedStorageEndpointData(fixture.id, UUID.randomUUID()));
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, factoryId);
		ClientLinkedStorageContents.updateHostDescriptor(fixture.id, descriptor);
		ClientLinkedStorageContents.updateContents(fixture.id, 1, fixture.contents.getContents(), Component.literal("Shared"), 0, 2, 0);

		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(tank);

		assertEquals(TankTier.IRON.capacity(1, 1), wrapper.getCapacity());
		assertTrue(wrapper.getTankFluidHandler().getFluidInTank(0).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"sophisticatedstorage:storage", "sophisticatedbackpacks:backpack", "minecraft:chest"})
	void getCompatibilityRejectsOtherHostFamilies(String factoryId) {
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get());
		LinkedStorageHostDescriptor descriptor = new LinkedStorageHostDescriptor(new ResourceLocation(factoryId), new CompoundTag());

		Compatibility compatibility = adapter.getCompatibility(level, tank, descriptor);

		assertEquals(Compatibility.INCOMPATIBLE, compatibility);
	}

	@Test
	void supportsExcludesTankItemsFromTheGenericStorageAdapter() {
		ItemStack tank = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get());

		boolean supported = new StorageItemLinkedStorageEndpointAdapter().supports(tank);

		assertFalse(supported);
	}

	@Test
	void supportsRejectsOrdinaryStorageItems() {
		ItemStack chest = new ItemStack(ModBlocks.CHEST_ITEM.get());

		boolean supported = new TankItemLinkedStorageEndpointAdapter().supports(chest);

		assertFalse(supported);
	}

	@Test
	void supportsRejectsStackedTankItems() {
		ItemStack tanks = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get(), 64);

		boolean supported = new TankItemLinkedStorageEndpointAdapter().supports(tanks);

		assertFalse(supported);
	}

	@Test
	void createHostDescriptorRetainsThePortableHeightAndTankFactory() {
		ItemStack tank = new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get());
		TankItemContents.height(tank, 5);

		LinkedStorageHostDescriptor descriptor = new TankItemLinkedStorageEndpointAdapter().createHostDescriptor(level, tank);

		assertEquals(TankLinkedStorageHostWrapper.FACTORY_ID, descriptor.factoryId());
		assertEquals(5, descriptor.virtualCarrier().getInt("height"));
	}

	@Test
	void getCompatibilityAcceptsAnEmptyTankOfAnotherTier() {
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, new ItemStack(ModBlocks.NETHERITE_TANK_ITEM.get()));
		ItemStack tank = new ItemStack(ModBlocks.TANK_ITEM.get());

		Compatibility compatibility = adapter.getCompatibility(level, tank, descriptor);

		assertEquals(Compatibility.COMPATIBLE, compatibility);
	}

	@ParameterizedTest
	@EnumSource(ContentsKind.class)
	void getCompatibilityRejectsPopulatedItems(ContentsKind kind) {
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, tank);
		populate(new TankItemStorageWrapper(tank), kind);

		Compatibility compatibility = adapter.getCompatibility(level, tank, descriptor);

		assertEquals(Compatibility.HAS_CONTENTS, compatibility);
	}

	@ParameterizedTest
	@EnumSource(ContentsKind.class)
	void getCompatibilityAcceptsItemsAfterTheirLastContentsAreRemoved(ContentsKind kind) {
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, tank);
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(tank);
		populate(wrapper, kind);
		switch (kind) {
			case FLUID -> wrapper.getTankFluidHandler().drain(1234, FluidAction.EXECUTE);
			case UPGRADE -> wrapper.getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
			case CONTAINER -> wrapper.getContainerInventory().setStackInSlot(0, ItemStack.EMPTY);
		}

		Compatibility compatibility = adapter.getCompatibility(level, tank, descriptor);

		assertEquals(Compatibility.COMPATIBLE, compatibility);
	}

	@Test
	void copyCanonicalContentsDoesNotAliasTheItemFluid() {
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		populate(new TankItemStorageWrapper(tank), ContentsKind.FLUID);
		CompoundTag canonical = new TankItemLinkedStorageEndpointAdapter().copyCanonicalContents(level, tank);

		canonical.getCompound(StorageWrapper.CONTENTS_TAG).remove("tankFluid");

		assertEquals(1234, new TankItemStorageWrapper(tank).getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@Test
	void createReloadsPersistedItemUpgrades() {
		ItemStack tank = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		populate(new TankItemStorageWrapper(tank), ContentsKind.UPGRADE);

		TankItemStorageWrapper restored = new TankItemStorageWrapper(tank);

		assertTrue(restored.getUpgradeHandler().getStackInSlot(0).is(ModItems.PUMP_UPGRADE.get()));
	}

	@Test
	void createReloadsCanonicalFluidAndGroupIdentity() {
		HostFixture fixture = sharedHost();

		TankLinkedStorageHostWrapper restored = TankLinkedStorageHostWrapper.create(fixture.contents, fixture.host.getVirtualCarrierSnapshot().orElseThrow());

		assertEquals(50000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(Optional.of(fixture.id), restored.getContentsUuid());
	}

	@Test
	void onVirtualCarrierChangedUpdatesCapacityWithoutLosingCanonicalFluid() {
		HostFixture fixture = sharedHost();
		CompoundTag carrier = TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true);

		fixture.host.onVirtualCarrierChanged(carrier);

		assertEquals(128000, fixture.host.getCapacity());
		assertEquals(50000, fixture.host.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(Optional.of(fixture.id), fixture.host.getContentsUuid());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillOnALockedHostAcceptsOnlyTheExistingFluid(boolean matching) {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		FluidStack incoming = new FluidStack(matching ? Fluids.WATER : Fluids.LAVA, 1000);
		clearInvocations(fixture.contents);

		int filled = fixture.host.getTankFluidHandler().fill(incoming, FluidAction.EXECUTE);

		assertEquals(matching ? 1000 : 0, filled);
		assertEquals(matching ? 51000 : 50000, fixture.host.getTankFluidHandler().getFluidInTank(0).getAmount());
		if (matching) {
			verify(fixture.contents, atLeastOnce()).markChanged();
		}
	}

	@Test
	void drainOnALockedHostRemovesAllMatchingFluid() {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		fixture.host.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);

		FluidStack drained = fixture.host.getTankFluidHandler().drain(51000, FluidAction.EXECUTE);

		assertEquals(51000, drained.getAmount());
		assertTrue(fixture.host.getTankFluidHandler().getFluidInTank(0).isEmpty());
	}

	@Test
	void fillRejectsAnEmptyLockedHostAfterDraining() {
		HostFixture fixture = sharedHost();
		fixture.host.onVirtualCarrierChanged(TankLinkedStorageHostWrapper.carrier(TankTier.GOLD, 2, Component.literal("Resized"), true));
		fixture.host.getTankFluidHandler().drain(50000, FluidAction.EXECUTE);

		int filled = fixture.host.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertTrue(fixture.host.getTankFluidHandler().getFluidInTank(0).isEmpty());
	}

	private static void populate(TankItemStorageWrapper wrapper, ContentsKind kind) {
		switch (kind) {
			case FLUID -> wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);
			case UPGRADE -> wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			case CONTAINER -> wrapper.getContainerInventory().setStackInSlot(0, new ItemStack(Items.BUCKET));
		}
	}

	private static HostFixture sharedHost() {
		ILinkedStorageContents contents = mock(ILinkedStorageContents.class);
		UUID id = UUID.randomUUID();
		when(contents.getContents()).thenReturn(new CompoundTag());
		when(contents.groupId()).thenReturn(id);
		TankLinkedStorageHostWrapper host = TankLinkedStorageHostWrapper.create(contents,
				TankLinkedStorageHostWrapper.carrier(TankTier.IRON, 3, Component.literal("Shared"), false));
		host.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		host.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 50000), FluidAction.EXECUTE);
		return new HostFixture(contents, id, host);
	}

	private enum ContentsKind {
		FLUID, UPGRADE, CONTAINER
	}

	private record HostFixture(ILinkedStorageContents contents, UUID id, TankLinkedStorageHostWrapper host) {
	}
}
