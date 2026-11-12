package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageItemEndpointAdapter;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

public final class TankItemLinkedStorageEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
	@Override
	public boolean supports(ItemStack stack) {
		return stack.getCount() == 1 && stack.getItem() instanceof TankBlockItem;
	}

	@Override
	public Identifier factoryId() {
		return TankLinkedStorageHostWrapper.FACTORY_ID;
	}

	@Override
	public Compatibility getCompatibility(ServerLevel level, ItemStack stack, LinkedStorageHostDescriptor descriptor) {
		if (!supports(stack) || !acceptsHostFactory(descriptor.factoryId())) {
			return Compatibility.INCOMPATIBLE;
		}
		return TankItemContents.hasContents(stack) ? Compatibility.HAS_CONTENTS : Compatibility.COMPATIBLE;
	}

	@Override
	public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ItemStack stack) {
		TankTier tier = ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier();
		return new LinkedStorageHostDescriptor(factoryId(), TankLinkedStorageHostWrapper.carrier(tier, stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1),
				stack.getHoverName(), stack.getOrDefault(ModDataComponents.LOCKED, false)));
	}

	@Override
	public ContainerContents copyCanonicalContents(ServerLevel level, ItemStack stack) {
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
		wrapper.getUpgradeHandler();
		return wrapper.copyContentsForLinkedStorage();
	}

	@Override
	public SortBy getSortBy(ServerLevel level, ItemStack stack) {
		return stack.getOrDefault(ModCoreDataComponents.SORT_BY, SortBy.NAME);
	}

	@Override
	public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
		TankItemContents.clearContents(stack);
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpoint);
		stack.set(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT,
				LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
	}

	public static void synchronizePrimaryCarrier(ServerLevel level, ItemStack stack) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			return;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		if (!manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			return;
		}
		manager.resolveVirtualHost(endpoint, false).filter(TankLinkedStorageHostWrapper.class::isInstance).map(TankLinkedStorageHostWrapper.class::cast)
				.ifPresent(host -> {
					TankTier tier = ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier();
					int height = stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
					CompoundTag carrier = TankLinkedStorageHostWrapper.carrier(tier, height, stack.getHoverName(),
							stack.getOrDefault(ModDataComponents.LOCKED, false));
					if (host.getVirtualCarrierSnapshot().orElseThrow().get("renderInfo") instanceof CompoundTag render) {
						carrier.put("renderInfo", render.copy());
					}
					if (!carrier.equals(host.getVirtualCarrierSnapshot().orElseThrow())
							&& host.getTankFluidHandler().getFluidInTank(0).getAmount() <= tier.capacity(height, host.getStackMultiplier())) {
						manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(),
								new LinkedStorageHostDescriptor(TankLinkedStorageHostWrapper.FACTORY_ID, carrier));
						host.changeSize(0, tier.upgradeSlots() - host.getUpgradeHandler().size());
					}
				});
	}
}
