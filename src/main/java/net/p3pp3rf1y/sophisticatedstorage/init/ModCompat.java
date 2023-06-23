package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.compat.quark.QuarkCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.rubidium.RubidiumCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ModCompat {
	private ModCompat() {}

	private static final String RUBIDIUM_MOD_ID = "rubidium";

	private static final Map<String, Supplier<Callable<ICompat>>> compatFactories = new HashMap<>();

	static {
		compatFactories.put(CompatModIds.QUARK, () -> QuarkCompat::new);
		compatFactories.put(RUBIDIUM_MOD_ID, () -> RubidiumCompat::new);
	}

	public static void initCompats() {
		for (Map.Entry<String, Supplier<Callable<ICompat>>> entry : compatFactories.entrySet()) {
			if (ModList.get().isLoaded(entry.getKey())) {
				try {
					entry.getValue().get().call().setup();
				}
				catch (Exception e) {
					SophisticatedStorage.LOGGER.error("Error instantiating compatibility ", e);
				}
			}
		}
	}
}
