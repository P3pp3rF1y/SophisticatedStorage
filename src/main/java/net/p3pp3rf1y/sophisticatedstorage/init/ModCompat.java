package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.compat.quark.QuarkCompat;
import net.p3pp3rf1y.sophisticatedstorage.compat.rubidium.RubidiumCompat;
import org.apache.maven.artifact.versioning.ArtifactVersion;
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

	static {
		compatFactories.put(new CompatInfo(CompatModIds.QUARK, null), () -> QuarkCompat::new);
		compatFactories.put(new CompatInfo(RUBIDIUM_MOD_ID, fromSpec("[0.6.5]")), () -> RubidiumCompat::new);
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
					entry.getValue().get().call().setup();
				}
				catch (Exception e) {
					SophisticatedStorage.LOGGER.error("Error instantiating compatibility ", e);
				}
			}
		}
	}

	record CompatInfo(String modId, @Nullable VersionRange supportedVersionRange){
		public boolean isLoaded() {
			return ModList.get().getModContainerById(modId())
					.map(container -> supportedVersionRange() == null || supportedVersionRange().containsVersion(container.getModInfo().getVersion()))
					.orElse(false);
		}
	}
}
