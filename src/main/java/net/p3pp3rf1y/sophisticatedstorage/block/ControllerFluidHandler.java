package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ControllerFluidHandler implements IStorageFluidHandler {
	private final Supplier<List<IStorageFluidHandler>> delegates;

	public ControllerFluidHandler(Supplier<List<IStorageFluidHandler>> delegates) {
		this.delegates = delegates;
	}

	@Override
	public int size() {
		return delegates.get().stream().mapToInt(IStorageFluidHandler::size).sum();
	}

	@Override
	public FluidResource getResource(int tank) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.getResource(tank);
			}
			tank -= handler.size();
		}
		return FluidResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int tank) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.getAmountAsLong(tank);
			}
			tank -= handler.size();
		}
		return 0;
	}

	@Override
	public long getCapacityAsLong(int tank, FluidResource resource) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.getCapacityAsLong(tank, resource);
			}
			tank -= handler.size();
		}
		return 0;
	}

	@Override
	public boolean isValid(int tank, FluidResource resource) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.isValid(tank, resource);
			}
			tank -= handler.size();
		}
		return false;
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext tx) {
		return insert(tank, resource, amount, tx, true);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.insert(tank, resource, amount, tx, ignoreInOutLimit);
			}
			tank -= handler.size();
		}
		return 0;
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext tx) {
		return insert(resource, amount, tx, true);
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}
		List<IStorageFluidHandler> handlers = delegates.get();
		int filled = insertMatchingHandlers(handlers, resource, amount, tx, ignoreInOutLimit);
		if (filled < amount) {
			filled += insertEmptyHandlers(handlers, resource, amount - filled, tx, ignoreInOutLimit);
		}
		return filled;
	}

	private static int insertMatchingHandlers(List<IStorageFluidHandler> handlers, FluidResource resource, int amount, TransactionContext tx,
			boolean ignoreInOutLimit) {
		return insertHandlers(handlers, resource, amount, tx, ignoreInOutLimit, handler -> hasMatchingFluid(handler, resource));
	}

	private static int insertEmptyHandlers(List<IStorageFluidHandler> handlers, FluidResource resource, int amount, TransactionContext tx,
			boolean ignoreInOutLimit) {
		return insertHandlers(handlers, resource, amount, tx, ignoreInOutLimit, handler -> !hasMatchingFluid(handler, resource) && hasEmptyTank(handler));
	}

	private static int insertHandlers(List<IStorageFluidHandler> handlers, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit,
			Predicate<IStorageFluidHandler> eligible) {
		int filled = 0;
		for (IStorageFluidHandler handler : handlers) {
			if (eligible.test(handler)) {
				filled += handler.insert(resource, amount - filled, tx, ignoreInOutLimit);
				if (filled == amount) {
					return filled;
				}
			}
		}
		return filled;
	}

	private static boolean hasMatchingFluid(IStorageFluidHandler handler, FluidResource resource) {
		for (int tank = 0; tank < handler.size(); tank++) {
			if (handler.getAmountAsLong(tank) > 0 && handler.getResource(tank).equals(resource)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasEmptyTank(IStorageFluidHandler handler) {
		for (int tank = 0; tank < handler.size(); tank++) {
			if (handler.getAmountAsLong(tank) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext tx) {
		return extract(tank, resource, amount, tx, true);
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		for (IStorageFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.size()) {
				return handler.extract(tank, resource, amount, tx, ignoreInOutLimit);
			}
			tank -= handler.size();
		}
		return 0;
	}

	@Override
	public int extract(FluidResource resource, int amount, TransactionContext tx) {
		return extract(resource, amount, tx, true);
	}

	public int extract(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}
		int drained = 0;
		for (IStorageFluidHandler handler : delegates.get()) {
			for (int tank = 0; tank < handler.size(); tank++) {
				drained += handler.extract(tank, resource, amount - drained, tx, ignoreInOutLimit);
				if (drained == amount) {
					return drained;
				}
			}
		}
		return drained;
	}
}
