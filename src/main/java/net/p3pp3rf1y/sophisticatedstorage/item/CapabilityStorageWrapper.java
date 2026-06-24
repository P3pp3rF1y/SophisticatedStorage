package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class CapabilityStorageWrapper {
	private CapabilityStorageWrapper() {
	}

	public static final Capability<StackStorageWrapper> STORAGE_WRAPPER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static Capability<StackStorageWrapper> getCapabilityInstance() {
		return STORAGE_WRAPPER_CAPABILITY;
	}

	public static void onRegister(RegisterCapabilitiesEvent event) {
		event.register(StackStorageWrapper.class);
	}
}
