package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.StorageContentsType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.Config;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class TankStorageWrapper extends StorageWrapper {
	static final String STORAGE_TYPE = "tank";
	static final String FLUID_TAG = "tankFluid";
	static final String CONTAINERS_TAG = "fluidContainers";
	private final Supplier<TankTier> tier;
	private final IntSupplier height;
	private final BooleanSupplier locked;
	private final Runnable synchronize;
	@Nullable
	private TankFluidHandler tankFluidHandler;
	@Nullable
	private ItemStackHandler containerInventory;
	@Nullable
	private TankContainerHandler containerHandler;
	private long containerCooldown;

	protected TankStorageWrapper(Runnable persist, Runnable synchronize, Supplier<TankTier> tier, IntSupplier height, BooleanSupplier locked) {
		super(() -> persist, synchronize, persist);
		this.tier = tier;
		this.height = height;
		this.locked = locked;
		this.synchronize = synchronize;
	}

	public int getCapacity() {
		return getLinkedStorage().map(storage -> ((TankStorageWrapper) storage).getCapacity())
				.orElseGet(() -> tier.get().capacity(height.getAsInt(), getStackMultiplier()));
	}
	public double getStackMultiplier() {
		double multiplier = getBaseStackSizeMultiplier();
		for (StackUpgradeItem.Wrapper wrapper : getUpgradeHandler().getTypeWrappers(StackUpgradeItem.TYPE)) {
			multiplier *= wrapper.getStackSizeMultiplier();
		}
		return multiplier;
	}
	public TankFluidHandler getTankFluidHandler() {
		if (isLinkedStorage()) {
			return ((TankStorageWrapper) getLinkedStorage().orElseThrow()).getTankFluidHandler();
		}
		if (tankFluidHandler == null) {
			TankFluidHandler handler = new TankFluidHandler(this::getCapacity, this::getMaxTransfer, this::getWrappedStorageStack, locked,
					this::shouldVoidFluid, this::persistFluid, synchronize);
			handler.load(getContentsNbt().getCompound(FLUID_TAG));
			tankFluidHandler = handler;
		}
		return tankFluidHandler;
	}
	private int getMaxTransfer() {
		double adjustedMultiplier = 1 + Config.SERVER.pumpUpgrade.stackMultiplierRatio.get() * (getStackMultiplier() - 1);
		return (int) Math.min(Integer.MAX_VALUE,
				Math.max(FluidType.BUCKET_VOLUME, Config.SERVER.pumpUpgrade.maxInputOutput.get() * (double) getFluidTransferScale() * adjustedMultiplier));
	}
	public ItemStackHandler getContainerInventory() {
		if (isLinkedStorage()) {
			return ((TankStorageWrapper) getLinkedStorage().orElseThrow()).getContainerInventory();
		}
		if (containerInventory == null) {
			ItemStackHandler inventory = new ItemStackHandler(4) {
				@Override
				protected void onContentsChanged(int slot) {
					getContentsNbt().put(CONTAINERS_TAG, serializeNBT());
					save();
				}
				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					return getContainerHandler().isItemValid(slot, stack);
				}
			};
			inventory.deserializeNBT(getContentsNbt().getCompound(CONTAINERS_TAG));
			containerInventory = inventory;
		}
		return containerInventory;
	}
	public TankContainerHandler getContainerHandler() {
		if (isLinkedStorage()) {
			return ((TankStorageWrapper) getLinkedStorage().orElseThrow()).getContainerHandler();
		}
		if (containerHandler == null) {
			containerHandler = new TankContainerHandler(getContainerInventory()) {
				@Override
				public FluidStack getContents() {
					return getTankFluidHandler().getFluidInTank(0);
				}
				@Override
				public int getTankCapacity() {
					return getCapacity();
				}
				@Override
				public int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
					return getTankFluidHandler().fill(resource, action, ignoreInOutLimit);
				}
				@Override
				public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
					return getTankFluidHandler().drain(maxDrain, action, ignoreInOutLimit);
				}
			};
		}
		return containerHandler;
	}
	public void tickContainers(long gameTime) {
		if (gameTime >= containerCooldown && getContainerHandler().processContainers()) {
			containerCooldown = gameTime + 20;
		}
	}
	public boolean hasContainerItems() {
		for (int slot = 0; slot < getContainerInventory().getSlots(); slot++) {
			if (!getContainerInventory().getStackInSlot(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}
	public List<ItemStack> removeContainerItems() {
		List<ItemStack> removed = new ArrayList<>();
		for (int slot = 0; slot < getContainerInventory().getSlots(); slot++) {
			removed.add(getContainerInventory().getStackInSlot(slot).copy());
			getContainerInventory().setStackInSlot(slot, ItemStack.EMPTY);
		}
		return removed;
	}
	private void persistFluid() {
		getContentsNbt().put(FLUID_TAG, getTankFluidHandler().save());
		save();
	}
	CompoundTag partitionContents(FluidStack fluid, List<ItemStack> upgrades, boolean retainSettings) {
		CompoundTag data = retainSettings ? saveCanonicalData(new CompoundTag()) : new CompoundTag();
		CompoundTag contents = new CompoundTag();
		if (retainSettings && getContentsNbt().contains(CONTAINERS_TAG)) {
			contents.put(CONTAINERS_TAG, getContentsNbt().getCompound(CONTAINERS_TAG).copy());
		}
		CompoundTag fluidData = new CompoundTag();
		if (!fluid.isEmpty()) {
			fluidData.put(TankFluidHandler.FLUID_TAG, fluid.writeToNBT(new CompoundTag()));
		}
		contents.put(FLUID_TAG, fluidData);
		ItemStackHandler upgradeInventory = new ItemStackHandler(tier.get().upgradeSlots());
		for (int slot = 0; slot < upgrades.size(); slot++) {
			upgradeInventory.setStackInSlot(slot, upgrades.get(slot).copy());
		}
		contents.put(UpgradeHandler.UPGRADE_INVENTORY_TAG, upgradeInventory.serializeNBT());
		data.put(CONTENTS_TAG, contents);
		data.putInt(NUMBER_OF_UPGRADE_SLOTS_TAG, tier.get().upgradeSlots());
		return data;
	}
	void applyPartition(CompoundTag data) {
		loadCanonicalData(data);
		getUpgradeHandler().setRenderUpgradeItems();
		save();
		onUpgradeRefresh();
		synchronize.run();
	}
	private boolean shouldVoidFluid(FluidStack fluid, VoidType type) {
		return getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).stream().anyMatch(wrapper -> wrapper.shouldVoidFluid(fluid, type));
	}
	@Override
	public Optional<IStorageFluidHandler> getFluidHandler() {
		return isLinkedStorage() ? super.getFluidHandler() : Optional.of(getTankFluidHandler());
	}
	@Override
	public double getMinimumStackMultiplierRequired() {
		return (double) getTankFluidHandler().getFluidInTank(0).getAmount() / tier.get().capacity(height.getAsInt(), 1);
	}
	@Override
	public int getFluidTransferScale() {
		return height.getAsInt();
	}
	@Override
	public Set<StorageContentsType> getSupportedContents() {
		return Set.of(StorageContentsType.FLUID);
	}
	@Override
	public Optional<UUID> getContentsUuid() {
		if (isLinkedStorage()) {
			return getLinkedStorage().orElseThrow().getContentsUuid();
		}
		if (contentsUuid == null) {
			contentsUuid = UUID.randomUUID();
			save();
		}
		return Optional.of(contentsUuid);
	}
	@Override
	public int getDefaultNumberOfInventorySlots() {
		return 0;
	}
	@Override
	public int getNumberOfInventorySlots() {
		return 0;
	}
	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return tier.get().upgradeSlots();
	}
	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return false;
	}
	@Override
	protected UpgradeHandler createUpgradeHandler(int slots, CompoundTag contents, Runnable saveHandler, Runnable invalidateCaches) {
		return new TankUpgradeHandler(slots, this, contents, saveHandler, invalidateCaches) {
			@Override
			public void refreshUpgradeWrappers() {
				super.refreshUpgradeWrappers();
				onUpgradeRefresh();
			}
		};
	}
	@Override
	public void onContentsNbtUpdated() {
		tankFluidHandler = null;
		containerInventory = null;
		containerHandler = null;
		super.onContentsNbtUpdated();
	}
	@Override
	void loadCanonicalData(CompoundTag tag) {
		tankFluidHandler = null;
		containerInventory = null;
		containerHandler = null;
		if (!tag.contains(CONTENTS_TAG)) {
			tag.put(CONTENTS_TAG, new CompoundTag());
		}
		if (!tag.contains(NUMBER_OF_UPGRADE_SLOTS_TAG)) {
			tag.putInt(NUMBER_OF_UPGRADE_SLOTS_TAG, tier.get().upgradeSlots());
		}
		super.loadCanonicalData(tag);
	}
	@Override
	void clearCanonicalStorageData() {
		tankFluidHandler = null;
		containerInventory = null;
		containerHandler = null;
		super.clearCanonicalStorageData();
	}
}
