package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
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
	private final BiPredicate<FluidStack, VoidType> shouldVoid;
	private final Runnable persist;
	private final Runnable synchronize;
	private FluidStack contents = FluidStack.EMPTY;
	private FluidStack synchronizedContents = FluidStack.EMPTY;
	private int synchronizedLevel = -1;

	public TankFluidHandler(IntSupplier capacity, Supplier<ItemStack> container, BooleanSupplier locked, BiPredicate<FluidStack, VoidType> shouldVoid,
			Runnable persist, Runnable synchronize) {
		this(capacity, () -> Integer.MAX_VALUE, container, locked, shouldVoid, persist, synchronize);
	}

	public TankFluidHandler(IntSupplier capacity, IntSupplier maxTransfer, Supplier<ItemStack> container, BooleanSupplier locked,
			BiPredicate<FluidStack, VoidType> shouldVoid, Runnable persist, Runnable synchronize) {
		this.capacity = capacity;
		this.maxTransfer = maxTransfer;
		this.container = container;
		this.shouldVoid = shouldVoid;
		this.persist = persist;
		this.synchronize = synchronize;
		lockedHandler = new ContentsFilteredFluidHandler(this, locked);
	}

	@Override
	public ItemStack getContainer() {
		return container.get();
	}
	@Override
	public int getTanks() {
		return 1;
	}
	@Override
	public FluidStack getFluidInTank(int tank) {
		return contents.copy();
	}
	@Override
	public int getTankCapacity(int tank) {
		return capacity.getAsInt();
	}
	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return !stack.isEmpty() && lockedHandler.matchesContents(stack) && (contents.isEmpty() || contents.isFluidEqual(stack));
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return fill(resource, action, true);
	}
	@Override
	public int fill(FluidStack resource, FluidAction action, boolean ignoreInOutLimit) {
		if (!isFluidValid(0, resource)) {
			return 0;
		}
		int requested = ignoreInOutLimit ? resource.getAmount() : Math.min(resource.getAmount(), maxTransfer.getAsInt());
		if (shouldVoid.test(resource, VoidType.ALWAYS)) {
			return requested;
		}
		int filled = Math.min(requested, Math.max(0, capacity.getAsInt() - contents.getAmount()));
		if (action.execute() && filled > 0) {
			contents = new FluidStack(resource, contents.getAmount() + filled);
			onContentsChanged();
		}
		return shouldVoid.test(resource, VoidType.STORAGE_OVERFLOW) || shouldVoid.test(resource, VoidType.SLOT_OVERFLOW) ? requested : filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return drain(resource, action, true);
	}
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action, boolean ignoreInOutLimit) {
		return !resource.isEmpty() && contents.isFluidEqual(resource) ? drain(resource.getAmount(), action, ignoreInOutLimit) : FluidStack.EMPTY;
	}
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return drain(maxDrain, action, true);
	}
	@Override
	public FluidStack drain(int maxDrain, FluidAction action, boolean ignoreInOutLimit) {
		if (maxDrain <= 0 || contents.isEmpty()) {
			return FluidStack.EMPTY;
		}
		int requested = ignoreInOutLimit ? maxDrain : Math.min(maxDrain, maxTransfer.getAsInt());
		FluidStack drained = new FluidStack(contents, Math.min(requested, contents.getAmount()));
		if (action.execute()) {
			contents = new FluidStack(contents, contents.getAmount() - drained.getAmount());
			onContentsChanged();
		}
		return drained;
	}
	@Override
	public FluidStack drain(TagKey<Fluid> resourceTag, int maxDrain, FluidAction action, boolean ignoreInOutLimit) {
		return contents.getFluid().is(resourceTag) ? drain(maxDrain, action, ignoreInOutLimit) : FluidStack.EMPTY;
	}
	@Override
	public int fill(TagKey<Fluid> fluidTag, int maxFill, Fluid fallbackFluid, FluidAction action, boolean ignoreInOutLimit) {
		return fill(contents.getFluid().is(fluidTag) ? new FluidStack(contents, maxFill) : new FluidStack(fallbackFluid, maxFill), action, ignoreInOutLimit);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (!contents.isEmpty()) {
			tag.put(FLUID_TAG, contents.writeToNBT(new CompoundTag()));
		}
		return tag;
	}
	public void load(CompoundTag tag) {
		contents = FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_TAG));
		synchronizedContents = FluidStack.EMPTY;
		synchronizedLevel = -1;
	}
	private void onContentsChanged() {
		persist.run();
		int level = (int) ((long) contents.getAmount() * 256 / Math.max(1, capacity.getAsInt()));
		if (!contents.isFluidEqual(synchronizedContents) || level != synchronizedLevel) {
			synchronizedContents = contents.copy();
			synchronizedLevel = level;
			synchronize.run();
		}
	}
}
