package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankContainerInteractionsTest {
	private MockedStatic<RegistryHelper> registry;

	@BeforeEach
	void mockRegistryAccess() {
		registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
	}

	@AfterEach
	void closeRegistryMock() {
		registry.close();
	}

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModFluids.XP_STILL.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.IRON_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.WATER_BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.POTION.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.GLASS_BOTTLE.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
		Items.EXPERIENCE_BOTTLE.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
		Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.clearConfig();
	}

	@ParameterizedTest
	@MethodSource("containerParams")
	void interactWithCursorStackTransfersOneContainer(ContainerParams params) {
		TankStorageWrapper storage = storage();
		if (!params.intoTank) {
			fill(storage.getTankFluidHandler(), params.fluid);
		}
		AtomicReference<ItemStack> carried = new AtomicReference<>(params.intoTank ? params.filled.copy() : params.empty.copy());

		storage.getContainerHandler().interactWithCursorStack(carried.get(), carried::set);

		assertTrue(ItemStack.isSameItemSameComponents(params.intoTank ? params.empty : params.filled, carried.get()));
		assertTrue(FluidStack.matches(params.intoTank ? params.fluid : FluidStack.EMPTY, storage.getTankFluidHandler().getFluidInTank(0)));
	}

	private static Stream<ContainerParams> containerParams() {
		return Stream.of(false, true)
				.flatMap(intoTank -> Stream.of(
						new ContainerParams(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), new FluidStack(Fluids.WATER, 1000), intoTank),
						new ContainerParams(PotionContents.createItemStack(Items.POTION, Potions.WATER), new ItemStack(Items.GLASS_BOTTLE),
								new FluidStack(Fluids.WATER, 250), intoTank),
						new ContainerParams(new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(Items.GLASS_BOTTLE),
								new FluidStack(ModFluids.XP_STILL.get(), 160), intoTank)));
	}

	private record ContainerParams(ItemStack filled, ItemStack empty, FluidStack fluid, boolean intoTank) {
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void tickContainersWaitsForResultSpace(boolean output) {
		TankStorageWrapper storage = storage();
		ItemStacksResourceHandler inventory = storage.getContainerInventory();
		int slot = output ? TankContainerHandler.OUTPUT_SLOT : TankContainerHandler.INPUT_SLOT;
		int resultSlot = output ? TankContainerHandler.OUTPUT_RESULT_SLOT : TankContainerHandler.INPUT_RESULT_SLOT;
		set(inventory, slot, new ItemStack(output ? Items.GLASS_BOTTLE : Items.EXPERIENCE_BOTTLE, 3));
		set(inventory, resultSlot, new ItemStack(output ? Items.STONE : Items.GLASS_BOTTLE, 64));
		if (output) {
			fill(storage.getTankFluidHandler(), new FluidStack(ModFluids.XP_STILL.get(), 320));
		}

		storage.tickContainers(0);

		assertEquals(output ? 320 : 0, storage.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(3, inventory.getAmountAsLong(slot));
		assertEquals(64, inventory.getAmountAsLong(resultSlot));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void tickContainersResumesAfterResultSpaceBecomesAvailable(boolean output) {
		TankStorageWrapper storage = storage();
		ItemStacksResourceHandler inventory = storage.getContainerInventory();
		int slot = output ? TankContainerHandler.OUTPUT_SLOT : TankContainerHandler.INPUT_SLOT;
		int resultSlot = output ? TankContainerHandler.OUTPUT_RESULT_SLOT : TankContainerHandler.INPUT_RESULT_SLOT;
		set(inventory, slot, new ItemStack(output ? Items.GLASS_BOTTLE : Items.EXPERIENCE_BOTTLE, 3));
		set(inventory, resultSlot, new ItemStack(output ? Items.STONE : Items.GLASS_BOTTLE, 64));
		if (output) {
			fill(storage.getTankFluidHandler(), new FluidStack(ModFluids.XP_STILL.get(), 320));
		}
		storage.tickContainers(0);
		set(inventory, resultSlot, ItemStack.EMPTY);

		storage.tickContainers(1);

		assertEquals(160, storage.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(2, inventory.getAmountAsLong(slot));
		assertEquals(1, inventory.getAmountAsLong(resultSlot));
		assertTrue(inventory.getResource(resultSlot).toStack(1).is(output ? Items.EXPERIENCE_BOTTLE : Items.GLASS_BOTTLE));
	}

	@ParameterizedTest
	@ValueSource(longs = {2, 21})
	void tickContainersProcessesOnlyOneContainerPerCooldown(long nextTick) {
		TankStorageWrapper storage = storage();
		ItemStacksResourceHandler inventory = storage.getContainerInventory();
		set(inventory, 0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		storage.tickContainers(1);

		storage.tickContainers(nextTick);

		int processed = nextTick == 2 ? 1 : 2;
		assertEquals(160 * processed, storage.getTankFluidHandler().getAmountAsLong(0));
		assertEquals(3 - processed, inventory.getAmountAsLong(0));
		assertEquals(processed, inventory.getAmountAsLong(2));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void partitionContentsRetainsContainersOnlyWithTheirOwner(boolean retainContainers) {
		TankStorageWrapper storage = storage();
		set(storage.getContainerInventory(), 0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		set(storage.getContainerInventory(), 3, new ItemStack(Items.GLASS_BOTTLE, 7));
		TankStorageWrapper restored = storage();

		TankItemContents.Contents partition = storage.partitionContents(FluidStack.EMPTY, List.of(), retainContainers);
		restored.applyPartition(partition);

		assertEquals(retainContainers ? 3 : 0, restored.getContainerInventory().getAmountAsLong(0));
		assertEquals(retainContainers ? 7 : 0, restored.getContainerInventory().getAmountAsLong(3));
		assertEquals(retainContainers, !partition.containers().equals(ItemContainerContents.EMPTY));
	}

	@Test
	void getCompatibilityRejectsItemsWithFluidContainers() {
		ItemStack item = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		set(new TankItemStorageWrapper(item).getContainerInventory(), 0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		ServerLevel level = mock(ServerLevel.class);
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, item);

		Compatibility compatibility = adapter.getCompatibility(level, item, descriptor);

		assertEquals(Compatibility.HAS_CONTENTS, compatibility);
	}

	@Test
	void removeContainerItemsReturnsAllSlotsAndClearsTheirContents() {
		TankStorageWrapper storage = storage();
		set(storage.getContainerInventory(), 0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		set(storage.getContainerInventory(), 3, new ItemStack(Items.GLASS_BOTTLE, 7));

		List<ItemStack> removed = storage.removeContainerItems();

		assertEquals(4, removed.size());
		assertEquals(3, removed.getFirst().getCount());
		assertEquals(7, removed.get(3).getCount());
		assertFalse(storage.hasContainerItems());
	}

	private static void set(ItemStacksResourceHandler handler, int slot, ItemStack stack) {
		handler.set(slot, ItemResource.of(stack), stack.getCount());
	}

	private static void fill(TankFluidHandler handler, FluidStack fluid) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(fluid.getAmount(), handler.insert(0, FluidResource.of(fluid), fluid.getAmount(), tx, false));
			tx.commit();
		}
	}

	private static TankStorageWrapper storage() {
		return new TankStorageWrapper(() -> {
		}, () -> {
		}, () -> TankTier.IRON, () -> 1, () -> RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), () -> false) {
			@Override
			protected void onUpgradeRefresh() {
			}
			@Override
			public String getStorageType() {
				return "tank";
			}
			@Override
			public Component getDisplayName() {
				return Component.literal("Tank");
			}
		};
	}
}
