package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

import javax.annotation.Nullable;

import java.util.List;

public class TankItemStorageWrapper extends TankStorageWrapper {
	private final ItemStack stack;

	public TankItemStorageWrapper(ItemStack stack) {
		super(() -> {
		}, () -> {
		}, () -> ((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier(), () -> TankItemContents.height(stack),
				() -> TankItemContents.flag(stack, "locked", false));
		this.stack = stack;
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
		if (endpoint == null) {
			loadCanonicalData(TankItemContents.read(stack));
			contentsUuid = net.p3pp3rf1y.sophisticatedcore.util.NBTHelper.getUniqueId(stack, UUID_TAG).orElse(null);
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
		return new ItemFluidHandler(stack, new ControllerFluidHandler(() -> {
			if (stack.getCount() != 1) {
				return List.of();
			}
			TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
			return LinkedStorageStackData.getEndpoint(stack) != null && !wrapper.isLinkedStorage() ? List.of() : List.of(wrapper.getTankFluidHandler());
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
		TankItemContents.write(stack, saveCanonicalData(new CompoundTag()));
		if (contentsUuid != null) {
			stack.getOrCreateTag().putUUID(UUID_TAG, contentsUuid);
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
