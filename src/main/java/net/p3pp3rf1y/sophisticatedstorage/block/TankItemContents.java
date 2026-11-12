package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

public final class TankItemContents {

	private TankItemContents() {
	}

	public static CompoundTag read(ItemStack stack, HolderLookup.Provider registries) {
		int upgradeSlots = stack.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS,
				((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier().upgradeSlots());
		CompoundTag contents = new CompoundTag();
		FluidStack fluid = stack.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY).copy();
		CompoundTag fluidData = new CompoundTag();
		if (!fluid.isEmpty()) {
			fluidData.put(TankFluidHandler.FLUID_TAG, fluid.save(registries));
		}
		contents.put(TankStorageWrapper.FLUID_TAG, fluidData);
		contents.put(UpgradeHandler.UPGRADE_INVENTORY_TAG,
				serializeInventory(stack.getOrDefault(ModDataComponents.TANK_UPGRADES, ItemContainerContents.EMPTY), upgradeSlots, registries));
		contents.put(TankStorageWrapper.CONTAINERS_TAG,
				serializeInventory(stack.getOrDefault(ModDataComponents.TANK_CONTAINERS, ItemContainerContents.EMPTY), 4, registries));
		CompoundTag data = new CompoundTag();
		data.put(StorageWrapper.CONTENTS_TAG, contents);
		data.putInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, upgradeSlots);
		data.putString(StorageWrapper.SORT_BY_TAG, stack.getOrDefault(ModCoreDataComponents.SORT_BY, SortBy.NAME).getSerializedName());
		return data;
	}

	public static void write(ItemStack stack, CompoundTag data, HolderLookup.Provider registries) {
		CompoundTag contents = data.getCompound(StorageWrapper.CONTENTS_TAG);
		FluidStack fluid = FluidStack.parseOptional(registries, contents.getCompound(TankStorageWrapper.FLUID_TAG).getCompound(TankFluidHandler.FLUID_TAG));
		if (fluid.isEmpty()) {
			stack.remove(ModCoreDataComponents.FLUID_CONTENTS);
		} else {
			stack.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(fluid));
		}
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, data.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG));
		stack.set(ModCoreDataComponents.SORT_BY, SortBy.fromName(data.getString(StorageWrapper.SORT_BY_TAG)));
		ItemContainerContents upgrades = deserializeInventory(contents.getCompound(UpgradeHandler.UPGRADE_INVENTORY_TAG), registries);
		if (upgrades.equals(ItemContainerContents.EMPTY)) {
			stack.remove(ModDataComponents.TANK_UPGRADES);
		} else {
			stack.set(ModDataComponents.TANK_UPGRADES, upgrades);
		}
		ItemContainerContents containers = deserializeInventory(contents.getCompound(TankStorageWrapper.CONTAINERS_TAG), registries);
		if (containers.equals(ItemContainerContents.EMPTY)) {
			stack.remove(ModDataComponents.TANK_CONTAINERS);
		} else {
			stack.set(ModDataComponents.TANK_CONTAINERS, containers);
		}
	}

	public static boolean hasContents(ItemStack stack) {
		return !stack.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY).isEmpty()
				|| stack.getOrDefault(ModDataComponents.TANK_UPGRADES, ItemContainerContents.EMPTY).nonEmptyStream().findAny().isPresent()
				|| stack.getOrDefault(ModDataComponents.TANK_CONTAINERS, ItemContainerContents.EMPTY).nonEmptyStream().findAny().isPresent();
	}

	public static void clearContents(ItemStack stack) {
		stack.remove(ModCoreDataComponents.FLUID_CONTENTS);
		stack.remove(ModDataComponents.TANK_UPGRADES);
		stack.remove(ModDataComponents.TANK_CONTAINERS);
	}

	public static boolean hasPortableData(ItemStack stack) {
		return stack.has(ModDataComponents.TANK_HEIGHT) || stack.has(ModCoreDataComponents.STORAGE_UUID)
				|| stack.has(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS) || hasContents(stack) || stack.has(ModDataComponents.LOCKED)
				|| stack.has(ModDataComponents.LOCK_VISIBLE) || stack.has(ModDataComponents.SHOWS_TIER) || stack.has(ModDataComponents.UPGRADES_VISIBLE)
				|| stack.has(ModDataComponents.PACKED) || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
	}

	private static CompoundTag serializeInventory(ItemContainerContents contents, int slots, HolderLookup.Provider registries) {
		ItemStackHandler inventory = new ItemStackHandler(slots);
		for (int slot = 0; slot < Math.min(slots, contents.getSlots()); slot++) {
			inventory.setStackInSlot(slot, contents.getStackInSlot(slot));
		}
		return inventory.serializeNBT(registries);
	}

	private static ItemContainerContents deserializeInventory(CompoundTag data, HolderLookup.Provider registries) {
		ItemStackHandler inventory = new ItemStackHandler();
		inventory.deserializeNBT(registries, data);
		NonNullList<ItemStack> items = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			items.set(slot, inventory.getStackInSlot(slot));
		}
		return ItemContainerContents.fromItems(items);
	}
}
