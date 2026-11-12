package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

record RestrictedFluidHandler(IFluidHandler handler, boolean allowFill, boolean allowDrain) implements IFluidHandler {
	@Override
	public int getTanks() {
		return handler.getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return allowDrain ? handler.getFluidInTank(tank) : FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return handler.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return allowFill && handler.isFluidValid(tank, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return allowFill ? handler.fill(resource, action) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return allowDrain ? handler.drain(resource, action) : FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return allowDrain ? handler.drain(maxDrain, action) : FluidStack.EMPTY;
	}
}
