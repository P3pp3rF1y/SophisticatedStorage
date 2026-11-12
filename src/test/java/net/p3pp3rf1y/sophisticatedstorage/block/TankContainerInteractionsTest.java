package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankContainerInteractionsTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@ParameterizedTest
	@MethodSource("containerParams")
	void interactWithCursorStackTransfersOneContainer(ContainerParams params) {
		TankStorageWrapper storage = storage();
		if (!params.intoTank) {
			storage.getTankFluidHandler().fill(params.fluid, FluidAction.EXECUTE);
		}
		AtomicReference<ItemStack> carried = new AtomicReference<>(params.intoTank ? params.filled.copy() : params.empty.copy());

		storage.getContainerHandler().interactWithCursorStack(carried.get(), carried::set);

		assertTrue(ItemStack.isSameItemSameTags(params.intoTank ? params.empty : params.filled, carried.get()));
		assertTrue((params.intoTank ? params.fluid : FluidStack.EMPTY).isFluidStackIdentical(storage.getTankFluidHandler().getFluidInTank(0)));
	}

	private static Stream<ContainerParams> containerParams() {
		return Stream.of(false, true)
				.flatMap(intoTank -> Stream.of(
						new ContainerParams(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), new FluidStack(Fluids.WATER, 1000), intoTank),
						new ContainerParams(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), new ItemStack(Items.GLASS_BOTTLE),
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
		IItemHandlerModifiable inventory = storage.getContainerInventory();
		int slot = output ? TankContainerHandler.OUTPUT_SLOT : TankContainerHandler.INPUT_SLOT;
		int resultSlot = output ? TankContainerHandler.OUTPUT_RESULT_SLOT : TankContainerHandler.INPUT_RESULT_SLOT;
		inventory.setStackInSlot(slot, new ItemStack(output ? Items.GLASS_BOTTLE : Items.EXPERIENCE_BOTTLE, 3));
		inventory.setStackInSlot(resultSlot, new ItemStack(output ? Items.STONE : Items.GLASS_BOTTLE, 64));
		if (output) {
			storage.getTankFluidHandler().fill(new FluidStack(ModFluids.XP_STILL.get(), 320), FluidAction.EXECUTE);
		}

		storage.tickContainers(0);

		assertEquals(output ? 320 : 0, storage.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(3, inventory.getStackInSlot(slot).getCount());
		assertEquals(64, inventory.getStackInSlot(resultSlot).getCount());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void tickContainersResumesAfterResultSpaceBecomesAvailable(boolean output) {
		TankStorageWrapper storage = storage();
		IItemHandlerModifiable inventory = storage.getContainerInventory();
		int slot = output ? TankContainerHandler.OUTPUT_SLOT : TankContainerHandler.INPUT_SLOT;
		int resultSlot = output ? TankContainerHandler.OUTPUT_RESULT_SLOT : TankContainerHandler.INPUT_RESULT_SLOT;
		inventory.setStackInSlot(slot, new ItemStack(output ? Items.GLASS_BOTTLE : Items.EXPERIENCE_BOTTLE, 3));
		inventory.setStackInSlot(resultSlot, new ItemStack(output ? Items.STONE : Items.GLASS_BOTTLE, 64));
		if (output) {
			storage.getTankFluidHandler().fill(new FluidStack(ModFluids.XP_STILL.get(), 320), FluidAction.EXECUTE);
		}
		storage.tickContainers(0);
		inventory.setStackInSlot(resultSlot, ItemStack.EMPTY);

		storage.tickContainers(1);

		assertEquals(160, storage.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(2, inventory.getStackInSlot(slot).getCount());
		assertEquals(1, inventory.getStackInSlot(resultSlot).getCount());
		assertTrue(inventory.getStackInSlot(resultSlot).is(output ? Items.EXPERIENCE_BOTTLE : Items.GLASS_BOTTLE));
	}

	@ParameterizedTest
	@ValueSource(longs = {2, 21})
	void tickContainersProcessesOnlyOneContainerPerCooldown(long nextTick) {
		TankStorageWrapper storage = storage();
		IItemHandlerModifiable inventory = storage.getContainerInventory();
		inventory.setStackInSlot(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		storage.tickContainers(1);

		storage.tickContainers(nextTick);

		int processed = nextTick == 2 ? 1 : 2;
		assertEquals(160 * processed, storage.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(3 - processed, inventory.getStackInSlot(0).getCount());
		assertEquals(processed, inventory.getStackInSlot(2).getCount());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void partitionContentsRetainsContainersOnlyWithTheirOwner(boolean retainContainers) {
		TankStorageWrapper storage = storage();
		storage.getContainerInventory().setStackInSlot(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		storage.getContainerInventory().setStackInSlot(3, new ItemStack(Items.GLASS_BOTTLE, 7));
		TankStorageWrapper restored = storage();

		CompoundTag partition = storage.partitionContents(FluidStack.EMPTY, List.of(), retainContainers);
		restored.loadCanonicalData(partition);

		assertEquals(retainContainers ? 3 : 0, restored.getContainerInventory().getStackInSlot(0).getCount());
		assertEquals(retainContainers ? 7 : 0, restored.getContainerInventory().getStackInSlot(3).getCount());
		assertEquals(retainContainers, partition.getCompound(StorageWrapper.CONTENTS_TAG).contains(TankStorageWrapper.CONTAINERS_TAG));
	}

	@Test
	void getCompatibilityRejectsItemsWithFluidContainers() {
		ItemStack item = new ItemStack(ModBlocks.IRON_TANK_ITEM.get());
		new TankItemStorageWrapper(item).getContainerInventory().setStackInSlot(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		TankItemLinkedStorageEndpointAdapter adapter = new TankItemLinkedStorageEndpointAdapter();
		ServerLevel level = mock(ServerLevel.class);
		LinkedStorageHostDescriptor descriptor = adapter.createHostDescriptor(level, item);

		Compatibility compatibility = adapter.getCompatibility(level, item, descriptor);

		assertEquals(Compatibility.HAS_CONTENTS, compatibility);
	}

	@Test
	void removeContainerItemsReturnsAllSlotsAndClearsTheirContents() {
		TankStorageWrapper storage = storage();
		storage.getContainerInventory().setStackInSlot(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		storage.getContainerInventory().setStackInSlot(3, new ItemStack(Items.GLASS_BOTTLE, 7));

		List<ItemStack> removed = storage.removeContainerItems();

		assertEquals(4, removed.size());
		assertEquals(3, removed.get(0).getCount());
		assertEquals(7, removed.get(3).getCount());
		assertFalse(storage.hasContainerItems());
	}

	private static TankStorageWrapper storage() {
		return new TankStorageWrapper(() -> {
		}, () -> {
		}, () -> TankTier.IRON, () -> 1, () -> false) {
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
