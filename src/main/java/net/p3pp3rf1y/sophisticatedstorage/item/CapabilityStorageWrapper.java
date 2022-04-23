package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

public class CapabilityStorageWrapper {
	private CapabilityStorageWrapper() {}

	public static final Capability<IStorageWrapper> STORAGE_WRAPPER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

	public static Capability<IStorageWrapper> getCapabilityInstance() {
		return STORAGE_WRAPPER_CAPABILITY;
	}

	public static void onRegister(RegisterCapabilitiesEvent event) {
		event.register(IStorageWrapper.class);
	}
}
