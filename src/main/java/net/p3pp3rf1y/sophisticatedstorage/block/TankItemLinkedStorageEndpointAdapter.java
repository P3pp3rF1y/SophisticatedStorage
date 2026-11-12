package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

public final class TankItemLinkedStorageEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
	@Override
	public boolean supports(ItemStack stack) {
		return stack.getCount() == 1 && stack.getItem() instanceof TankBlockItem;
	}
	@Override
	public ResourceLocation factoryId() {
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
		return new LinkedStorageHostDescriptor(factoryId(), TankLinkedStorageHostWrapper.carrier(tier, height(stack), stack.getHoverName(), locked(stack)));
	}
	@Override
	public CompoundTag copyCanonicalContents(ServerLevel level, ItemStack stack) {
		CompoundTag data = TankItemContents.read(stack);
		data.putInt(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier().upgradeSlots());
		return data;
	}
	@Override
	public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
		TankItemContents.clearContents(stack);
		LinkedStorageStackData.setEndpoint(stack, endpoint);
		LinkedStorageStackData.setPrimaryEndpoint(stack,
				LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()));
	}
	public static void synchronizePrimaryCarrier(ServerLevel level, ItemStack stack) {
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
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
					int height = height(stack);
					CompoundTag carrier = TankLinkedStorageHostWrapper.carrier(tier, height, stack.getHoverName(), locked(stack));
					carrier.put(StorageWrapper.RENDER_INFO_TAG, host.getRenderInfoNbt());
					if (!carrier.equals(host.getVirtualCarrierSnapshot().orElseThrow())
							&& host.getTankFluidHandler().getFluidInTank(0).getAmount() <= tier.capacity(height, host.getStackMultiplier())) {
						manager.updatePrimaryHostDescriptor(endpoint.groupId(), endpoint.endpointId(),
								new LinkedStorageHostDescriptor(TankLinkedStorageHostWrapper.FACTORY_ID, carrier));
						host.changeSize(0, tier.upgradeSlots() - host.getUpgradeHandler().getSlots());
					}
				});
	}
	private static int height(ItemStack stack) {
		return TankItemContents.height(stack);
	}
	private static boolean locked(ItemStack stack) {
		return TankItemContents.flag(stack, "locked", false);
	}
}
