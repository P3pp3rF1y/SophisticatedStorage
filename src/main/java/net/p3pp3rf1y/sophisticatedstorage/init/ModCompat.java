package net.p3pp3rf1y.sophisticatedstorage.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedstorage.compat.chipped.ChippedCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.quark.QuarkCompat;

public class ModCompat {
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), () -> modBus -> new QuarkCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CHIPPED, null), () -> modBus -> new ChippedCompat());
	}
}
