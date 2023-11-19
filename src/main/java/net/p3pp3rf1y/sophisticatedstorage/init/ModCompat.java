package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.compat.chipped.ChippedCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.quark.QuarkCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.rubidium.RubidiumCompat;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ModCompat {
	private ModCompat() {}

	private static final String RUBIDIUM_MOD_ID = "rubidium";

	private static final Map<CompatInfo, Supplier<Callable<ICompat>>> compatFactories = new HashMap<>();

	private static final Map<CompatInfo, ICompat> loadedCompats = new HashMap<>();

	static {
		compatFactories.put(new CompatInfo(CompatModIds.QUARK, null), () -> QuarkCompat::new);
		compatFactories.put(new CompatInfo(RUBIDIUM_MOD_ID, fromSpec("[0.6.5]")), () -> RubidiumCompat::new);
		compatFactories.put(new CompatInfo(CompatModIds.CHIPPED, null), () -> ChippedCompat::new);
	}

	public static void compatsSetup() {
		loadedCompats.values().forEach(ICompat::setup);
	}

	@Nullable
	private static VersionRange fromSpec(String spec) {
		try {
			return VersionRange.createFromVersionSpec(spec);
		}
		catch (InvalidVersionSpecificationException e) {
			return null;
		}
	}

	public static void initCompats() {
		for (Map.Entry<CompatInfo, Supplier<Callable<ICompat>>> entry : compatFactories.entrySet()) {
			if (entry.getKey().isLoaded()) {
				try {
					loadedCompats.put(entry.getKey(), entry.getValue().get().call());
				}
				catch (Exception e) {
					SophisticatedStorage.LOGGER.error("Error instantiating compatibility ", e);
				}
			}
		}

		loadedCompats.values().forEach(ICompat::init);
	}

	record CompatInfo(String modId, @Nullable VersionRange supportedVersionRange) {
		public boolean isLoaded() {
			return ModList.get().getModContainerById(modId())
					.map(container -> supportedVersionRange() == null || supportedVersionRange().containsVersion(container.getModInfo().getVersion()))
					.orElse(false);
		}
	}
}
