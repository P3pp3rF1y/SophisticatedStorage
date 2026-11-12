package net.p3pp3rf1y.sophisticatedstorage.block;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;

import java.util.Optional;

public interface INativeControllerFluidStorage {
	Optional<IStorageFluidHandler> getControllerFluidHandler();
}
