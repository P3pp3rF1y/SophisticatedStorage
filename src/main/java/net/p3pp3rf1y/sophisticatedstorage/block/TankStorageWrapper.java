package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.StorageContentsType;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import org.jspecify.annotations.Nullable;

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
	private final Supplier<TankTier> tier;
	private final IntSupplier height;
	private final Supplier<RegistryAccess> registries;
	private final BooleanSupplier locked;
	private final Runnable synchronize;
	@Nullable
	private TankFluidHandler tankFluidHandler;
	@Nullable
	private ItemStacksResourceHandler containerInventory;
	@Nullable
	private TankContainerHandler containerHandler;
	private long containerCooldown;

	protected TankStorageWrapper(Runnable persist, Runnable synchronize, Supplier<TankTier> tier, IntSupplier height, Supplier<RegistryAccess> registries,
			BooleanSupplier locked) {
		super(() -> persist, synchronize, persist);
		this.tier = tier;
		this.height = height;
		this.registries = registries;
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
		if (getLinkedStorage().isPresent()) {
			return ((TankStorageWrapper) getLinkedStorage().orElseThrow()).getTankFluidHandler();
		}
		if (tankFluidHandler == null) {
			TankFluidHandler handler = new TankFluidHandler(this::getCapacity, this::getMaxTransfer, this::getWrappedStorageStack, locked,
					this::shouldVoidFluid, this::persistFluid, synchronize);
			CompoundTag tag = new CompoundTag();
			FluidStack fluid = getContents().tank().fluid();
			if (!fluid.isEmpty()) {
				tag.put(TankFluidHandler.FLUID_TAG,
						FluidStack.CODEC.encodeStart(registries.get().createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow());
			}
			handler.load(registries.get(), tag);
			tankFluidHandler = handler;
		}
		return tankFluidHandler;
	}

	private int getMaxTransfer() {
		double adjustedMultiplier = 1 + Config.SERVER.pumpUpgrade.stackMultiplierRatio.get() * (getStackMultiplier() - 1);
		return (int) Math.min(Integer.MAX_VALUE,
				Math.max(FluidType.BUCKET_VOLUME, Config.SERVER.pumpUpgrade.maxInputOutput.get() * (double) getFluidTransferScale() * adjustedMultiplier));
	}

	public ItemStacksResourceHandler getContainerInventory() {
		if (getLinkedStorage().isPresent()) {
			return ((TankStorageWrapper) getLinkedStorage().orElseThrow()).getContainerInventory();
		}
		if (containerInventory == null) {
			boolean[] loading = {true};
			ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(4) {
				@Override
				protected void onContentsChanged(int slot, ItemStack previousContents) {
					if (!loading[0]) {
						getContents().tank().setContainers(ItemContainerContents.fromItems(containerStacks(this)));
						save();
					}
				}
				@Override
				public boolean isValid(int slot, ItemResource resource) {
					return loading[0] || getContainerHandler().isItemValid(slot, resource.toStack(1));
				}
			};
			containerInventory = inventory;
			ItemContainerContents containers = getContents().tank().containers();
			for (int slot = 0; slot < Math.min(4, containers.getSlots()); slot++) {
				ItemStack stack = containers.getStackInSlot(slot);
				inventory.set(slot, ItemResource.of(stack), stack.getCount());
			}
			loading[0] = false;
		}
		return containerInventory;
	}

	private static List<ItemStack> containerStacks(ItemStacksResourceHandler inventory) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot = 0; slot < inventory.size(); slot++) {
			stacks.add(inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot)));
		}
		return stacks;
	}

	public TankContainerHandler getContainerHandler() {
		if (getLinkedStorage().isPresent()) {
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
				public int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
					return getTankFluidHandler().insert(0, resource, amount, tx, ignoreInOutLimit);
				}
				@Override
				public int extract(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
					return getTankFluidHandler().extract(0, resource, amount, tx, ignoreInOutLimit);
				}
				@Override
				protected ItemStack getStackInSlot(int slot) {
					ItemStacksResourceHandler inventory = getContainerInventory();
					return inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
				}
				@Override
				protected void setStackInSlot(int slot, ItemStack stack) {
					getContainerInventory().set(slot, ItemResource.of(stack), stack.getCount());
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
		return containerStacks(getContainerInventory()).stream().anyMatch(stack -> !stack.isEmpty());
	}
	public List<ItemStack> removeContainerItems() {
		List<ItemStack> removed = containerStacks(getContainerInventory());
		for (int slot = 0; slot < getContainerInventory().size(); slot++) {
			getContainerInventory().set(slot, ItemResource.EMPTY, 0);
		}
		return removed;
	}
	private void persistFluid() {
		getContents().tank().setFluid(getTankFluidHandler().getFluidInTank(0));
		save();
	}

	TankItemContents.Contents partitionContents(FluidStack fluid, List<ItemStack> upgrades, boolean retainSettings) {
		List<ItemStack> upgradeStacks = new ArrayList<>();
		for (int slot = 0; slot < Math.max(tier.get().upgradeSlots(), upgrades.size()); slot++) {
			upgradeStacks.add(slot < upgrades.size() ? upgrades.get(slot).copy() : ItemStack.EMPTY);
		}
		ItemContainerContents containers = retainSettings
				? containerInventory == null ? getContents().tank().containers() : ItemContainerContents.fromItems(containerStacks(containerInventory))
				: ItemContainerContents.EMPTY;
		return new TankItemContents.Contents(fluid.copy(), ItemContainerContents.fromItems(upgradeStacks), containers, upgradeStacks.size(),
				retainSettings ? getSortBy() : net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy.NAME);
	}
	void applyPartition(TankItemContents.Contents partition) {
		loadPartition(partition);
		save();
		onUpgradeRefresh();
		synchronize.run();
	}
	protected void loadPartition(TankItemContents.Contents partition) {
		numberOfUpgradeSlots = partition.upgradeSlots();
		ContainerContents contents = getContents().copy();
		contents.tank().setFluid(partition.fluid());
		contents.tank().setContainers(partition.containers());
		contents.upgrades().setStacks(NonNullList.withSize(partition.upgradeSlots(), ItemStack.EMPTY));
		for (int slot = 0; slot < Math.min(partition.upgrades().getSlots(), partition.upgradeSlots()); slot++) {
			contents.upgrades().stacks().set(slot, partition.upgrades().getStackInSlot(slot));
		}
		replaceContents(contents);
		setSortBy(partition.sortBy());
		getUpgradeHandler().setRenderUpgradeItems();
	}
	@Override
	void clearCanonicalStorageData() {
		applyPartition(new TankItemContents.Contents(FluidStack.EMPTY, ItemContainerContents.EMPTY, ItemContainerContents.EMPTY, tier.get().upgradeSlots(),
				net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy.NAME));
	}
	private boolean shouldVoidFluid(FluidResource fluid, VoidType type) {
		return getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).stream().anyMatch(wrapper -> wrapper.shouldVoidFluid(fluid, type));
	}
	@Override
	public Optional<IStorageFluidHandler> getFluidHandler() {
		return getLinkedStorage().isPresent() ? super.getFluidHandler() : Optional.of(getTankFluidHandler());
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
		if (getLinkedStorage().isPresent()) {
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
	protected boolean isAllowedInStorage(ItemResource resource) {
		return false;
	}
	@Override
	protected UpgradeHandler createUpgradeHandler(int slots, ContainerContents contents, Runnable saveHandler, Runnable invalidateCaches) {
		return new TankUpgradeHandler(slots, this, contents, saveHandler, invalidateCaches) {
			@Override
			public void refreshUpgradeWrappers() {
				super.refreshUpgradeWrappers();
				onUpgradeRefresh();
			}
		};
	}
	@Override
	public void onContentsUpdated() {
		tankFluidHandler = null;
		containerInventory = null;
		containerHandler = null;
		super.onContentsUpdated();
	}
}
