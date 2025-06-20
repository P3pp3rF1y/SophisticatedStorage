package net.p3pp3rf1y.sophisticatedstorage.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedstorage.compat.chipped.ChippedCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.quark.QuarkCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.sawmill.SawmillCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.sb.SBCompat;

public class ModCompat {
	private static final String SB_MOD_ID = "sophisticatedbackpacks";
	public static final String SAWMILL_MOD_ID = "sawmill";
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), () -> modBus -> new QuarkCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CHIPPED, null), () -> modBus -> new ChippedCompat());
		CompatRegistry.registerCompat(new CompatInfo(SB_MOD_ID, null), () -> modBus -> new SBCompat());
		CompatRegistry.registerCompat(new CompatInfo(SAWMILL_MOD_ID, null), () -> modBus -> new SawmillCompat());
	}
}
