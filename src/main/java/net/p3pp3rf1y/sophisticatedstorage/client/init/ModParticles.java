package net.p3pp3rf1y.sophisticatedstorage.client.init;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticle;

public class ModParticles {
	private ModParticles() {}

	@SuppressWarnings("unused") // need this to register the event correctly
	public static void registerProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(net.p3pp3rf1y.sophisticatedstorage.init.ModParticles.TERRAIN_PARTICLE.get(), spriteSet -> new CustomTintTerrainParticle.Factory());
	}
}
