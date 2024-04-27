package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;

import java.util.function.Supplier;

public class ModParticles {
	private ModParticles() {
	}

	private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, SophisticatedStorage.MOD_ID);

	public static final Supplier<CustomTintTerrainParticleData> TERRAIN_PARTICLE = PARTICLES.register("terrain_particle", CustomTintTerrainParticleData::new);

	public static void registerParticles(IEventBus modBus) {
		PARTICLES.register(modBus);
	}

}
