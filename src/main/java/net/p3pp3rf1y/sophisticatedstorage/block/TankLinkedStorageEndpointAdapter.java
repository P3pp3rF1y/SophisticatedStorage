package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageBlockEndpoint;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;

public final class TankLinkedStorageEndpointAdapter implements ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> {
	@Override
	public Identifier factoryId() {
		return TankLinkedStorageHostWrapper.FACTORY_ID;
	}
	@Override
	public Compatibility getCompatibility(ServerLevel level, ILinkedStorageBlockEndpoint endpoint, LinkedStorageHostDescriptor descriptor) {
		if (!(endpoint instanceof TankBlockEntity tank) || !tank.isLinkedStorageCandidate() || !acceptsHostFactory(descriptor.factoryId())) {
			return Compatibility.INCOMPATIBLE;
		}
		return tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty()
				&& ResourceHandlerUtil.isEmpty(tank.getStorageWrapper().getUpgradeHandler()) && !tank.getStorageWrapper().hasContainerItems()
						? Compatibility.COMPATIBLE
						: Compatibility.HAS_CONTENTS;
	}
	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return new LinkedStorageHostDescriptor(factoryId(), requireTank(endpoint).createTankCarrier());
	}
	@Override
	public ContainerContents copyCanonicalContents(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return requireTank(endpoint).getStorageWrapper().copyContentsForLinkedStorage();
	}
	@Override
	public SortBy getSortBy(ServerLevel level, ILinkedStorageBlockEndpoint endpoint) {
		return requireTank(endpoint).getStorageWrapper().getSortBy();
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
