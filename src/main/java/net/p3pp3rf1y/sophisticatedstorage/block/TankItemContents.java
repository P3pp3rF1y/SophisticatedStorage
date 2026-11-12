package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

public final class TankItemContents {
	private static final String FLUID_CONTENTS_TAG = "fluidContents";
	private static final String UPGRADES_TAG = "tankUpgrades";
	private static final String CONTAINERS_TAG = "tankContainers";
	private static final String HEIGHT_TAG = "tankHeight";
	private TankItemContents() {
	}
	public static int height(ItemStack stack) {
		return stack.getTag() == null || !stack.getTag().contains(HEIGHT_TAG) ? 1 : stack.getTag().getInt(HEIGHT_TAG);
	}
	public static void height(ItemStack stack, int height) {
		TankTier.validateHeight(height);
		stack.getOrCreateTag().putInt(HEIGHT_TAG, height);
	}
	public static void setUpgradeSlots(ItemStack stack, int slots) {
		stack.getOrCreateTag().putInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, slots);
	}
	public static boolean flag(ItemStack stack, String key, boolean defaultValue) {
		return stack.getTag() == null || !stack.getTag().contains(key) ? defaultValue : stack.getTag().getBoolean(key);
	}
	public static void setFlag(ItemStack stack, String key, boolean value) {
		stack.getOrCreateTag().putBoolean(key, value);
	}
	public static String mode(ItemStack stack) {
		return stack.getTag() == null ? "place_tank" : stack.getTag().getString("tankItemMode");
	}
	public static void mode(ItemStack stack, String mode) {
		stack.getOrCreateTag().putString("tankItemMode", mode);
	}

	public static CompoundTag read(ItemStack stack) {
		CompoundTag itemTag = stack.getTag() == null ? new CompoundTag() : stack.getTag();
		int upgradeSlots = itemTag.contains(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, Tag.TAG_INT)
				? itemTag.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG)
				: ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier().upgradeSlots();
		CompoundTag contents = new CompoundTag();
		CompoundTag fluidData = new CompoundTag();
		if (itemTag.contains(FLUID_CONTENTS_TAG, Tag.TAG_COMPOUND)) {
			FluidStack fluid = FluidStack.loadFluidStackFromNBT(itemTag.getCompound(FLUID_CONTENTS_TAG));
			if (!fluid.isEmpty()) {
				fluidData.put(TankFluidHandler.FLUID_TAG, fluid.writeToNBT(new CompoundTag()));
			}
		}
		contents.put(TankStorageWrapper.FLUID_TAG, fluidData);
		contents.put(UpgradeHandler.UPGRADE_INVENTORY_TAG, readInventory(itemTag, UPGRADES_TAG, upgradeSlots));
		contents.put(TankStorageWrapper.CONTAINERS_TAG, readInventory(itemTag, CONTAINERS_TAG, 4));
		CompoundTag data = new CompoundTag();
		data.put(StorageWrapper.CONTENTS_TAG, contents);
		data.putInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, upgradeSlots);
		data.putString(StorageWrapper.SORT_BY_TAG,
				itemTag.contains(StorageWrapper.SORT_BY_TAG) ? itemTag.getString(StorageWrapper.SORT_BY_TAG) : SortBy.NAME.getSerializedName());
		return data;
	}

	public static void write(ItemStack stack, CompoundTag data) {
		CompoundTag itemTag = stack.getOrCreateTag();
		CompoundTag contents = data.getCompound(StorageWrapper.CONTENTS_TAG);
		FluidStack fluid = FluidStack.loadFluidStackFromNBT(contents.getCompound(TankStorageWrapper.FLUID_TAG).getCompound(TankFluidHandler.FLUID_TAG));
		if (fluid.isEmpty()) {
			itemTag.remove(FLUID_CONTENTS_TAG);
		} else {
			itemTag.put(FLUID_CONTENTS_TAG, fluid.writeToNBT(new CompoundTag()));
		}
		itemTag.putInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, data.getInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG));
		itemTag.putString(StorageWrapper.SORT_BY_TAG, SortBy.fromName(data.getString(StorageWrapper.SORT_BY_TAG)).getSerializedName());
		writeInventory(itemTag, UPGRADES_TAG, contents.getCompound(UpgradeHandler.UPGRADE_INVENTORY_TAG));
		writeInventory(itemTag, CONTAINERS_TAG, contents.getCompound(TankStorageWrapper.CONTAINERS_TAG));
	}

	public static boolean hasContents(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && (!FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_CONTENTS_TAG)).isEmpty() || hasItems(tag, UPGRADES_TAG)
				|| hasItems(tag, CONTAINERS_TAG));
	}
	public static void clearContents(ItemStack stack) {
		if (stack.getTag() != null) {
			stack.getTag().remove(FLUID_CONTENTS_TAG);
			stack.getTag().remove(UPGRADES_TAG);
			stack.getTag().remove(CONTAINERS_TAG);
		}
	}
	public static boolean hasPortableData(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && (tag.contains(HEIGHT_TAG) || tag.contains(StorageWrapper.UUID_TAG) || tag.contains(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG)
				|| hasContents(stack) || tag.contains("locked") || tag.contains("lockVisible") || tag.contains("showsTier") || tag.contains("upgradesVisible")
				|| tag.contains("packed") || LinkedStorageStackData.getEndpoint(stack) != null);
	}

	private static CompoundTag readInventory(CompoundTag itemTag, String key, int slots) {
		ItemStackHandler inventory = new ItemStackHandler(slots);
		CompoundTag stored = itemTag.getCompound(key);
		ItemStackHandler source = new ItemStackHandler();
		if (stored.contains("Size", Tag.TAG_INT)) {
			source.deserializeNBT(stored);
			for (int slot = 0; slot < Math.min(slots, source.getSlots()); slot++) {
				inventory.setStackInSlot(slot, source.getStackInSlot(slot).copy());
			}
		}
		return inventory.serializeNBT();
	}
	private static void writeInventory(CompoundTag itemTag, String key, CompoundTag contents) {
		if (contents.getList("Items", Tag.TAG_COMPOUND).isEmpty()) {
			itemTag.remove(key);
		} else {
			itemTag.put(key, contents.copy());
		}
	}
	private static boolean hasItems(CompoundTag tag, String key) {
		return !tag.getCompound(key).getList("Items", Tag.TAG_COMPOUND).isEmpty();
	}
}
