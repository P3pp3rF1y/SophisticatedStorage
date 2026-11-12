package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ControllerFluidHandler implements IFluidHandler {
	private final Supplier<List<IFluidHandler>> delegates;

	public ControllerFluidHandler(Supplier<List<IFluidHandler>> delegates) {
		this.delegates = delegates;
	}

	@Override
	public int getTanks() {
		return delegates.get().stream().mapToInt(IFluidHandler::getTanks).sum();
	}
	@Override
	public FluidStack getFluidInTank(int tank) {
		for (IFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.getTanks()) {
				return handler.getFluidInTank(tank).copy();
			}
			tank -= handler.getTanks();
		}
		return FluidStack.EMPTY;
	}
	@Override
	public int getTankCapacity(int tank) {
		for (IFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.getTanks()) {
				return handler.getTankCapacity(tank);
			}
			tank -= handler.getTanks();
		}
		return 0;
	}
	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		for (IFluidHandler handler : delegates.get()) {
			if (tank >= 0 && tank < handler.getTanks()) {
				return handler.isFluidValid(tank, stack);
			}
			tank -= handler.getTanks();
		}
		return false;
	}
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return 0;
		}
		List<IFluidHandler> handlers = delegates.get();
		int filled = fillMatchingHandlers(handlers, resource, action);
		if (filled < resource.getAmount()) {
			filled += fillEmptyHandlers(handlers, new FluidStack(resource, resource.getAmount() - filled), action);
		}
		return filled;
	}

	private static int fillMatchingHandlers(List<IFluidHandler> handlers, FluidStack resource, FluidAction action) {
		return fillHandlers(handlers, resource, action, handler -> hasMatchingFluid(handler, resource));
	}

	private static int fillEmptyHandlers(List<IFluidHandler> handlers, FluidStack resource, FluidAction action) {
		return fillHandlers(handlers, resource, action, handler -> !hasMatchingFluid(handler, resource) && hasEmptyTank(handler));
	}

	private static int fillHandlers(List<IFluidHandler> handlers, FluidStack resource, FluidAction action, Predicate<IFluidHandler> eligible) {
		int filled = 0;
		for (IFluidHandler handler : handlers) {
			if (eligible.test(handler)) {
				filled += handler.fill(new FluidStack(resource, resource.getAmount() - filled), action);
				if (filled == resource.getAmount()) {
					return filled;
				}
			}
		}
		return filled;
	}

	private static boolean hasMatchingFluid(IFluidHandler handler, FluidStack resource) {
		for (int tank = 0; tank < handler.getTanks(); tank++) {
			FluidStack current = handler.getFluidInTank(tank);
			if (!current.isEmpty() && current.isFluidEqual(resource)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasEmptyTank(IFluidHandler handler) {
		for (int tank = 0; tank < handler.getTanks(); tank++) {
			if (handler.getFluidInTank(tank).isEmpty()) {
				return true;
			}
		}
		return false;
	}
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return FluidStack.EMPTY;
		}
		int drained = 0;
		for (IFluidHandler handler : delegates.get()) {
			drained += handler.drain(new FluidStack(resource, resource.getAmount() - drained), action).getAmount();
			if (drained == resource.getAmount()) {
				break;
			}
		}
		return new FluidStack(resource, drained);
	}
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0) {
			return FluidStack.EMPTY;
		}
		List<IFluidHandler> handlers = delegates.get();
		FluidStack result = FluidStack.EMPTY;
		for (IFluidHandler handler : handlers) {
			FluidStack drained = result.isEmpty()
					? handler.drain(maxDrain, action)
					: handler.drain(new FluidStack(result, maxDrain - result.getAmount()), action);
			if (!drained.isEmpty()) {
				result = new FluidStack(drained, result.getAmount() + drained.getAmount());
				if (result.getAmount() == maxDrain) {
					break;
				}
			}
		}
		return result;
	}
}
