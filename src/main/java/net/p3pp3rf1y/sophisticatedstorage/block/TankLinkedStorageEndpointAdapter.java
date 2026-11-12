package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public final class TankLinkedStorageEndpointAdapter implements ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> {
	@Override
	public ResourceLocation factoryId() {
		return TankLinkedStorageHostWrapper.FACTORY_ID;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageHostDescriptor descriptor) {
		if (!(endpoint instanceof TankBlockEntity tank) || !tank.isLinkedStorageCandidate() || !acceptsHostFactory(descriptor.factoryId())) {
			return Compatibility.INCOMPATIBLE;
		}
		return tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty()
				&& InventoryHelper.isEmpty(tank.getStorageWrapper().getUpgradeHandler()) && !tank.getStorageWrapper().hasContainerItems()
						? Compatibility.COMPATIBLE
						: Compatibility.HAS_CONTENTS;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		TankBlockEntity tank = requireTank(endpoint);
		return new LinkedStorageHostDescriptor(factoryId(), tank.createTankCarrier());
	}

	@Override
	public CompoundTag copyCanonicalContents(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return requireTank(endpoint).getStorageWrapper().saveCanonicalData(new CompoundTag());
	}

	@Override
	public void bindEndpoint(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageEndpointData data) {
		requireTank(endpoint).bindLinkedStorage(level, data);
	}

	@Override
	public void onEndpointLinked(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		requireTank(endpoint).onLinkedStorageEndpointLinked();
	}

	private static TankBlockEntity requireTank(ILinkedStorageBlockEndpoint endpoint) {
		if (endpoint instanceof TankBlockEntity tank && tank.isLinkedStorageCandidate()) {
			return tank;
		}
		throw new IllegalArgumentException("Unsupported tank endpoint");
	}
}
