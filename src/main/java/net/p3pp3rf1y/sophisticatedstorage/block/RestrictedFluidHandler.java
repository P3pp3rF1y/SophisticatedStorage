package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;

record RestrictedFluidHandler(IStorageFluidHandler handler, boolean allowFill, boolean allowDrain) implements IStorageFluidHandler {
	@Override
	public int size() {
		return handler.size();
	}

	@Override
	public FluidResource getResource(int tank) {
		return allowDrain ? handler.getResource(tank) : FluidResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int tank) {
		return allowDrain ? handler.getAmountAsLong(tank) : 0;
	}

	@Override
	public long getCapacityAsLong(int tank, FluidResource resource) {
		return handler.getCapacityAsLong(tank, resource);
	}

	@Override
	public boolean isValid(int tank, FluidResource resource) {
		return allowFill && handler.isValid(tank, resource);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext tx) {
		return insert(tank, resource, amount, tx, true);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		return allowFill ? handler.insert(tank, resource, amount, tx, ignoreInOutLimit) : 0;
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext tx) {
		return insert(resource, amount, tx, true);
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		return allowFill ? handler.insert(resource, amount, tx, ignoreInOutLimit) : 0;
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext tx) {
		return extract(tank, resource, amount, tx, true);
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		return allowDrain ? handler.extract(tank, resource, amount, tx, ignoreInOutLimit) : 0;
	}

	@Override
	public int extract(FluidResource resource, int amount, TransactionContext tx) {
		return extract(resource, amount, tx, true);
	}

	public int extract(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		if (!allowDrain) {
			return 0;
		}
		if (ignoreInOutLimit) {
			return handler.extract(resource, amount, tx);
		}
		int extracted = 0;
		for (int tank = 0; tank < handler.size(); tank++) {
			extracted += handler.extract(tank, resource, amount - extracted, tx, false);
			if (extracted == amount) {
				return extracted;
			}
		}
		return extracted;
	}
}
