package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

import javax.annotation.Nullable;

public class TankItemStorageWrapper extends TankStorageWrapper {
	private final ItemStack stack;
	private boolean loading = true;

	public TankItemStorageWrapper(ItemStack stack) {
		super(() -> {
		}, () -> {
		}, () -> ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier(), () -> stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1),
				() -> RegistryHelper.getRegistryAccess().orElseThrow(), () -> stack.getOrDefault(ModDataComponents.LOCKED, false));
		this.stack = stack;
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			loadPartition(TankItemContents.read(stack));
			contentsUuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		} else if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				LinkedStorageGroupsSavedData.get(server.overworld()).manager().resolveVirtualHost(endpoint, false).filter(TankStorageWrapper.class::isInstance)
						.map(TankStorageWrapper.class::cast).ifPresent(this::setLinkedStorage);
			}
		} else {
			ClientLinkedStorageContents.getContents(endpoint.groupId())
					.ifPresent(contents -> ClientLinkedStorageContents.getHostDescriptor(endpoint.groupId())
							.filter(descriptor -> TankLinkedStorageHostWrapper.FACTORY_ID.toString()
									.equals(descriptor.getStringOr(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "")))
							.map(descriptor -> descriptor.getCompoundOrEmpty(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG))
							.ifPresent(carrier -> setLinkedStorage(TankLinkedStorageHostWrapper.create(contents, carrier))));
		}
		loading = false;
	}

	@Nullable
	public static ResourceHandler<FluidResource> getCapability(ItemStack stack) {
		if (RegistryHelper.getRegistryAccess().isEmpty()) {
			return null;
		}
		return new ControllerFluidHandler(() -> {
			if (stack.getCount() != 1) {
				return java.util.List.of();
			}
			TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
			return stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) && wrapper.getLinkedStorage().isEmpty()
					? java.util.List.of()
					: java.util.List.of(wrapper.getTankFluidHandler());
		});
	}

	@Override
	protected UpgradeHandler createUpgradeHandler(int slots, ContainerContents contents, Runnable saveHandler, Runnable invalidateCaches) {
		return super.createUpgradeHandler(slots, contents, this::save, invalidateCaches);
	}

	@Override
	protected void save() {
		if (stack == null || loading || getLinkedStorage().isPresent()) {
			return;
		}
		TankItemContents.write(stack, partitionContents(getTankFluidHandler().getFluidInTank(0),
				java.util.stream.IntStream.range(0, getUpgradeHandler().size()).mapToObj(slot -> getUpgradeHandler().getStackInSlot(slot)).toList(), true));
		if (contentsUuid != null) {
			stack.set(ModCoreDataComponents.STORAGE_UUID, contentsUuid);
		}
	}

	@Override
	public ItemStack getWrappedStorageStack() {
		return stack;
	}

	@Override
	protected void onUpgradeRefresh() {
	}

	@Override
	public String getStorageType() {
		return STORAGE_TYPE;
	}

	@Override
	public Component getDisplayName() {
		return stack.getHoverName();
	}
}
