package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;

import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class TankFluidHandler implements IStorageFluidHandler {
	static final String FLUID_TAG = "fluid";
	private final IntSupplier capacity;
	private final IntSupplier maxTransfer;
	private final Supplier<ItemStack> container;
	private final ContentsFilteredFluidHandler lockedHandler;
	private final BiPredicate<FluidResource, VoidType> shouldVoid;
	private final Runnable persist;
	private final Runnable synchronize;
	private final SnapshotJournal<FluidStack> journal = new SnapshotJournal<>() {
		@Override
		protected FluidStack createSnapshot() {
			return contents.copy();
		}

		@Override
		protected void revertToSnapshot(FluidStack snapshot) {
			contents = snapshot;
		}

		@Override
		protected void onRootCommit(FluidStack originalState) {
			onContentsChanged();
		}
	};
	private FluidStack contents = FluidStack.EMPTY;
	private FluidStack synchronizedContents = FluidStack.EMPTY;
	private int synchronizedLevel = -1;

	public TankFluidHandler(IntSupplier capacity, Supplier<ItemStack> container, BooleanSupplier locked, BiPredicate<FluidResource, VoidType> shouldVoid,
			Runnable persist, Runnable synchronize) {
		this(capacity, () -> Integer.MAX_VALUE, container, locked, shouldVoid, persist, synchronize);
	}

	public TankFluidHandler(IntSupplier capacity, IntSupplier maxTransfer, Supplier<ItemStack> container, BooleanSupplier locked,
			BiPredicate<FluidResource, VoidType> shouldVoid, Runnable persist, Runnable synchronize) {
		this.capacity = capacity;
		this.maxTransfer = maxTransfer;
		this.container = container;
		this.shouldVoid = shouldVoid;
		this.persist = persist;
		this.synchronize = synchronize;
		lockedHandler = new ContentsFilteredFluidHandler(this, locked);
	}

	public ItemStack getContainer() {
		return container.get();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public FluidResource getResource(int index) {
		return FluidResource.of(contents);
	}

	@Override
	public long getAmountAsLong(int index) {
		return contents.getAmount();
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource) {
		return capacity.getAsInt();
	}

	public FluidStack getFluidInTank(int tank) {
		return contents.copy();
	}

	public int getTankCapacity(int tank) {
		return capacity.getAsInt();
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		return index == 0 && !resource.isEmpty() && lockedHandler.matchesContents(resource) && (contents.isEmpty() || resource.matches(contents));
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
		return insert(index, resource, amount, tx, true);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		if (!isValid(index, resource) || amount <= 0) {
			return 0;
		}
		int requested = ignoreInOutLimit ? amount : Math.min(amount, maxTransfer.getAsInt());
		if (shouldVoid.test(resource, VoidType.ALWAYS)) {
			return requested;
		}
		int filled = Math.min(requested, Math.max(0, capacity.getAsInt() - contents.getAmount()));
		if (filled > 0) {
			journal.updateSnapshots(tx);
			contents = contents.isEmpty() ? resource.toStack(filled) : contents.copyWithAmount(contents.getAmount() + filled);
		}
		return shouldVoid.test(resource, VoidType.STORAGE_OVERFLOW) || shouldVoid.test(resource, VoidType.SLOT_OVERFLOW) ? requested : filled;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
		return extract(index, resource, amount, tx, true);
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		if (index != 0 || amount <= 0 || contents.isEmpty() || !resource.matches(contents)) {
			return 0;
		}
		int requested = ignoreInOutLimit ? amount : Math.min(amount, maxTransfer.getAsInt());
		int drained = Math.min(requested, contents.getAmount());
		if (drained > 0) {
			journal.updateSnapshots(tx);
			contents = contents.copyWithAmount(contents.getAmount() - drained);
		}
		return drained;
	}

	public CompoundTag save(RegistryAccess registries) {
		CompoundTag tag = new CompoundTag();
		if (!contents.isEmpty()) {
			tag.put(FLUID_TAG, FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), contents).getOrThrow());
		}
		return tag;
	}

	public void load(RegistryAccess registries, CompoundTag tag) {
		contents = tag.get(FLUID_TAG) == null
				? FluidStack.EMPTY
				: FluidStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(FLUID_TAG)).result().orElse(FluidStack.EMPTY);
		synchronizedContents = FluidStack.EMPTY;
		synchronizedLevel = -1;
	}

	private void onContentsChanged() {
		persist.run();
		int level = (int) ((long) contents.getAmount() * 256 / Math.max(1, capacity.getAsInt()));
		if (!FluidStack.isSameFluidSameComponents(contents, synchronizedContents) || level != synchronizedLevel) {
			synchronizedContents = contents.copy();
			synchronizedLevel = level;
			synchronize.run();
		}
	}
}
