package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;

import java.util.function.BooleanSupplier;

/** Only accepts fluids already present when this endpoint is locked; extraction stays available. */
public record ContentsFilteredFluidHandler(IStorageFluidHandler fluidHandler, BooleanSupplier isLocked) implements IStorageFluidHandler {
	@Override
	public int size() {
		return fluidHandler.size();
	}

	@Override
	public FluidResource getResource(int index) {
		return fluidHandler.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return fluidHandler.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource) {
		return matchesContents(resource) ? fluidHandler.getCapacityAsLong(index, resource) : 0;
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		return matchesContents(resource) && fluidHandler.isValid(index, resource);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		return matchesContents(resource) ? fluidHandler.insert(index, resource, amount, tx, ignoreInOutLimit) : 0;
	}

	private boolean matchesContents(FluidResource resource) {
		if (!isLocked.getAsBoolean()) {
			return true;
		}
		if (!resource.isEmpty()) {
			for (int index = 0; index < fluidHandler.size(); index++) {
				if (fluidHandler.getAmountAsLong(index) > 0 && fluidHandler.getResource(index).equals(resource)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		return fluidHandler.extract(index, resource, amount, tx, ignoreInOutLimit);
	}
}
