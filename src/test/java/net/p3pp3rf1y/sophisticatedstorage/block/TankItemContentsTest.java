package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TankItemContentsTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@Test
	void readAndWritePreserveOverCapacityFluidAndSparseInventorySlots() {
		ItemStack original = portableTank();
		CompoundTag canonical = TankItemContents.read(original);
		ItemStack restored = original.copy();
		TankItemContents.clearContents(restored);

		TankItemContents.write(restored, canonical);

		assertPortableContents(restored);
		assertEquals(original.getTag().getUUID(StorageWrapper.UUID_TAG), restored.getTag().getUUID(StorageWrapper.UUID_TAG));
		assertEquals(original.getTag().getCompound("tankUpgrades"), restored.getTag().getCompound("tankUpgrades"));
		assertEquals(original.getTag().getCompound("tankContainers"), restored.getTag().getCompound("tankContainers"));
	}

	@Test
	void drainOnAnItemCopyDoesNotChangeTheOriginalFluid() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getTankFluidHandler().drain(1000, FluidAction.EXECUTE);

		assertTrue(ItemStack.isSameItemSameTags(unchanged, original));
		assertEquals(199000, FluidStack.loadFluidStackFromNBT(copy.getTag().getCompound("fluidContents")).getAmount());
		assertPortableContents(original);
	}

	@Test
	void removingAnUpgradeFromAnItemCopyDoesNotChangeTheOriginalUpgrades() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		copy.getTag().put("fluidContents", new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000).writeToNBT(new CompoundTag()));
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getUpgradeHandler().setStackInSlot(2, ItemStack.EMPTY);

		assertTrue(ItemStack.isSameItemSameTags(unchanged, original));
		assertFalse(copy.getTag().contains("tankUpgrades"));
		assertPortableContents(original);
	}

	@Test
	void removingAContainerFromAnItemCopyDoesNotChangeTheOriginalContainers() {
		ItemStack original = portableTank();
		ItemStack unchanged = original.copy();
		ItemStack copy = original.copy();
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(copy);

		wrapper.getContainerInventory().setStackInSlot(3, ItemStack.EMPTY);

		assertTrue(ItemStack.isSameItemSameTags(unchanged, original));
		assertEquals(1, copy.getTag().getCompound("tankContainers").getList("Items", 10).size());
		assertEquals(4, copy.getTag().getCompound("tankContainers").getInt("Size"));
		assertEquals(4, wrapper.getContainerInventory().getStackInSlot(1).getCount());
		assertTrue(wrapper.getContainerInventory().getStackInSlot(3).isEmpty());
		assertPortableContents(original);
	}

	@Test
	void clearContentsRemovesOnlyCanonicalContentsAndRetainsPortableMetadata() {
		ItemStack stack = portableTank();
		ItemStack expected = stack.copy();
		expected.getTag().remove("fluidContents");
		expected.getTag().remove("tankUpgrades");
		expected.getTag().remove("tankContainers");

		TankItemContents.clearContents(stack);

		assertFalse(TankItemContents.hasContents(stack));
		assertTrue(TankItemContents.hasPortableData(stack));
		assertTrue(ItemStack.isSameItemSameTags(expected, stack));
	}

	private static ItemStack portableTank() {
		ItemStack stack = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		FluidStack fluid = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 200000);
		CompoundTag fluidIdentity = new CompoundTag();
		fluidIdentity.putString("identity", "Distinct fluid");
		fluid.setTag(fluidIdentity);
		ItemStack upgrade = new ItemStack(ModItems.PUMP_UPGRADE.get());
		upgrade.setHoverName(Component.literal("Configured pump"));
		ItemStack input = new ItemStack(Items.BUCKET, 4);
		ItemStack output = new ItemStack(Items.WATER_BUCKET);
		output.setHoverName(Component.literal("Container result"));
		ItemStackHandler upgrades = new ItemStackHandler(3);
		upgrades.setStackInSlot(2, upgrade);
		ItemStackHandler containers = new ItemStackHandler(4);
		containers.setStackInSlot(1, input);
		containers.setStackInSlot(3, output);
		stack.getOrCreateTag().put("fluidContents", fluid.writeToNBT(new CompoundTag()));
		TankItemContents.setUpgradeSlots(stack, 3);
		stack.getTag().put("tankUpgrades", upgrades.serializeNBT());
		stack.getTag().put("tankContainers", containers.serializeNBT());
		stack.getTag().putUUID(StorageWrapper.UUID_TAG, java.util.UUID.randomUUID());
		TankItemContents.height(stack, 3);
		TankItemContents.setFlag(stack, "packed", true);
		TankItemContents.setFlag(stack, "locked", true);
		TankItemContents.setFlag(stack, "lockVisible", false);
		TankItemContents.setFlag(stack, "showsTier", false);
		TankItemContents.setFlag(stack, "upgradesVisible", false);
		TankItemContents.mode(stack, "dispense_fluid");
		stack.setHoverName(Component.literal("Portable tank"));
		return stack;
	}

	private static void assertPortableContents(ItemStack stack) {
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
		FluidStack fluid = wrapper.getTankFluidHandler().getFluidInTank(0);
		assertEquals(200000, fluid.getAmount());
		assertEquals("Distinct fluid", fluid.getTag().getString("identity"));
		assertEquals(96000, wrapper.getCapacity());
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).isEmpty());
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(1).isEmpty());
		assertEquals(Component.literal("Configured pump"), wrapper.getUpgradeHandler().getStackInSlot(2).getHoverName());
		assertTrue(wrapper.getContainerInventory().getStackInSlot(0).isEmpty());
		assertEquals(4, wrapper.getContainerInventory().getStackInSlot(1).getCount());
		assertTrue(wrapper.getContainerInventory().getStackInSlot(2).isEmpty());
		assertEquals(Component.literal("Container result"), wrapper.getContainerInventory().getStackInSlot(3).getHoverName());
	}
}
