package net.p3pp3rf1y.sophisticatedstorage.block;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

public interface INativeControllerFluidStorage {
	Optional<IFluidHandler> getControllerFluidHandler();
}
