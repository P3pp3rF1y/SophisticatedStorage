package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankItemContentsTest {
	private RegistryAccess registries;
	private MockedStatic<RegistryHelper> registryMock;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.clearConfig();
	}

	@BeforeEach
	void mockRegistryAccess() {
		registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		registryMock = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registryMock.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(registries));
	}

	@AfterEach
	void closeRegistryMock() {
		registryMock.close();
	}

	@Test
	void readAndWritePreserveOverCapacityFluidAndSparseInventorySlots() {
		ItemStack original = portableTank();
		CompoundTag canonical = TankItemContents.read(original, registries);
		ItemStack restored = original.copy();
		TankItemContents.clearContents(restored);

		TankItemContents.write(restored, canonical, registries);

		assertPortableContents(restored);
		assertEquals(original.get(ModCoreDataComponents.STORAGE_UUID), restored.get(ModCoreDataComponents.STORAGE_UUID));
		assertEquals(original.get(ModDataComponents.TANK_UPGRADES), restored.get(ModDataComponents.TANK_UPGRADES));
		assertEquals(original.get(ModDataComponents.TANK_CONTAINERS), restored.get(ModDataComponents.TANK_CONTAINERS));
	}

	@Test
	void drainOnAnItemCopyDoesNotChangeTheOriginalFluid() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getTankFluidHandler().drain(1000, FluidAction.EXECUTE);

		assertTrue(ItemStack.isSameItemSameComponents(unchanged, original));
		assertEquals(199000, copy.get(ModCoreDataComponents.FLUID_CONTENTS).getAmount());
		assertPortableContents(original);
	}

	@Test
	void removingAnUpgradeFromAnItemCopyDoesNotChangeTheOriginalUpgrades() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		copy.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(copy.get(ModCoreDataComponents.FLUID_CONTENTS).copy().copyWithAmount(1000)));
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getUpgradeHandler().setStackInSlot(2, ItemStack.EMPTY);

		assertTrue(ItemStack.isSameItemSameComponents(unchanged, original));
		assertFalse(copy.has(ModDataComponents.TANK_UPGRADES));
		assertPortableContents(original);
	}

	@Test
	void removingAContainerFromAnItemCopyDoesNotChangeTheOriginalContainers() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getContainerInventory().setStackInSlot(3, ItemStack.EMPTY);

		assertTrue(ItemStack.isSameItemSameComponents(unchanged, original));
		assertEquals(2, copy.get(ModDataComponents.TANK_CONTAINERS).getSlots());
		assertPortableContents(original);
	}

	@Test
	void clearContentsRemovesOnlyCanonicalContentsAndRetainsPortableMetadata() {
		ItemStack stack = portableTank();
		ItemStack expected = stack.copy();
		expected.remove(ModCoreDataComponents.FLUID_CONTENTS);
		expected.remove(ModDataComponents.TANK_UPGRADES);
		expected.remove(ModDataComponents.TANK_CONTAINERS);

		TankItemContents.clearContents(stack);

		assertFalse(TankItemContents.hasContents(stack));
		assertTrue(TankItemContents.hasPortableData(stack));
		assertTrue(ItemStack.isSameItemSameComponents(expected, stack));
	}

	private static ItemStack portableTank() {
		ItemStack stack = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		FluidStack fluid = new FluidStack(Fluids.WATER, 200000);
		fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct fluid"));
		ItemStack upgrade = new ItemStack(ModItems.PUMP_UPGRADE.get());
		upgrade.set(DataComponents.CUSTOM_NAME, Component.literal("Configured pump"));
		ItemStack input = new ItemStack(Items.BUCKET, 4);
		ItemStack output = new ItemStack(Items.WATER_BUCKET);
		output.set(DataComponents.CUSTOM_NAME, Component.literal("Container result"));
		stack.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(fluid));
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
		stack.set(ModDataComponents.TANK_UPGRADES, ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY, upgrade)));
		stack.set(ModDataComponents.TANK_CONTAINERS, ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, input, ItemStack.EMPTY, output)));
		stack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		stack.set(ModDataComponents.TANK_HEIGHT, 3);
		stack.set(ModDataComponents.PACKED, true);
		stack.set(ModDataComponents.LOCKED, true);
		stack.set(ModDataComponents.LOCK_VISIBLE, false);
		stack.set(ModDataComponents.SHOWS_TIER, false);
		stack.set(ModDataComponents.UPGRADES_VISIBLE, false);
		stack.set(ModDataComponents.TANK_ITEM_MODE, TankBlockItem.Mode.DISPENSE_FLUID);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Portable tank"));
		return stack;
	}

	private static void assertPortableContents(ItemStack stack) {
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
		FluidStack fluid = wrapper.getTankFluidHandler().getFluidInTank(0);
		assertEquals(200000, fluid.getAmount());
		assertEquals(Component.literal("Distinct fluid"), fluid.get(DataComponents.CUSTOM_NAME));
		assertEquals(96000, wrapper.getCapacity());
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).isEmpty());
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(1).isEmpty());
		assertEquals(Component.literal("Configured pump"), wrapper.getUpgradeHandler().getStackInSlot(2).get(DataComponents.CUSTOM_NAME));
		assertTrue(wrapper.getContainerInventory().getStackInSlot(0).isEmpty());
		assertEquals(4, wrapper.getContainerInventory().getStackInSlot(1).getCount());
		assertTrue(wrapper.getContainerInventory().getStackInSlot(2).isEmpty());
		assertEquals(Component.literal("Container result"), wrapper.getContainerInventory().getStackInSlot(3).get(DataComponents.CUSTOM_NAME));
	}
}
