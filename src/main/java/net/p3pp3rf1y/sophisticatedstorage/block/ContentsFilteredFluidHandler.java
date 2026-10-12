package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.BooleanSupplier;

public record ContentsFilteredFluidHandler(IFluidHandler fluidHandler, BooleanSupplier isLocked) implements IFluidHandler {
	@Override
	public int getTanks() {
		return fluidHandler.getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return fluidHandler.getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return fluidHandler.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return matchesContents(stack) && fluidHandler.isFluidValid(tank, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return matchesContents(resource) ? fluidHandler.fill(resource, action) : 0;
	}

	private boolean matchesContents(FluidStack resource) {
		if (!isLocked.getAsBoolean()) {
			return true;
		}
		if (!resource.isEmpty()) {
			for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
				FluidStack contents = fluidHandler.getFluidInTank(tank);
				if (!contents.isEmpty() && contents.isFluidEqual(resource)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return fluidHandler.drain(resource, action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return fluidHandler.drain(maxDrain, action);
	}
}
