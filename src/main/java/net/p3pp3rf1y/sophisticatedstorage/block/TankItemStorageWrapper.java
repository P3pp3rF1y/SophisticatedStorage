package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

import javax.annotation.Nullable;

import java.util.List;

public class TankItemStorageWrapper extends TankStorageWrapper {
	private final ItemStack stack;

	public TankItemStorageWrapper(ItemStack stack) {
		super(() -> {
		}, () -> {
		}, () -> ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier(), () -> stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1),
				() -> RegistryHelper.getRegistryAccess().orElseThrow(), () -> stack.getOrDefault(ModDataComponents.LOCKED, false));
		this.stack = stack;
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			loadCanonicalData(TankItemContents.read(stack, RegistryHelper.getRegistryAccess().orElseThrow()));
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
									.equals(descriptor.getString(LinkedStorageHostDescriptor.FACTORY_ID_TAG)))
							.map(descriptor -> descriptor.getCompound(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG))
							.ifPresent(carrier -> setLinkedStorage(TankLinkedStorageHostWrapper.create(contents, carrier))));
		}
	}

	@Nullable
	public static IFluidHandlerItem getCapability(ItemStack stack) {
		if (RegistryHelper.getRegistryAccess().isEmpty()) {
			return null;
		}
		return new ItemFluidHandler(stack, new ControllerFluidHandler(() -> {
			if (stack.getCount() != 1) {
				return List.of();
			}
			TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
			return stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) && !wrapper.isLinkedStorage() ? List.of() : List.of(wrapper.getTankFluidHandler());
		}));
	}

	private record ItemFluidHandler(ItemStack stack, IFluidHandler delegate) implements IFluidHandlerItem {
		@Override
		public ItemStack getContainer() {
			return stack;
		}
		@Override
		public int getTanks() {
			return delegate.getTanks();
		}
		@Override
		public FluidStack getFluidInTank(int tank) {
			return delegate.getFluidInTank(tank);
		}
		@Override
		public int getTankCapacity(int tank) {
			return delegate.getTankCapacity(tank);
		}
		@Override
		public boolean isFluidValid(int tank, FluidStack fluid) {
			return delegate.isFluidValid(tank, fluid);
		}
		@Override
		public int fill(FluidStack fluid, FluidAction action) {
			return delegate.fill(fluid, action);
		}
		@Override
		public FluidStack drain(FluidStack fluid, FluidAction action) {
			return delegate.drain(fluid, action);
		}
		@Override
		public FluidStack drain(int amount, FluidAction action) {
			return delegate.drain(amount, action);
		}
	}

	@Override
	protected void save() {
		if (stack == null || isLinkedStorage()) {
			return;
		}
		TankItemContents.write(stack, saveCanonicalData(new CompoundTag()), RegistryHelper.getRegistryAccess().orElseThrow());
		if (contentsUuid != null) {
			stack.set(ModCoreDataComponents.STORAGE_UUID, contentsUuid);
		}
	}

	@Override
	protected UpgradeHandler createUpgradeHandler(int slots, CompoundTag contents, Runnable saveHandler, Runnable invalidateCaches) {
		return super.createUpgradeHandler(slots, contents, this::save, invalidateCaches);
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
