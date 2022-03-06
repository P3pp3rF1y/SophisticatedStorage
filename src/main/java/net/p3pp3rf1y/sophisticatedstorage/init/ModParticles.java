package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticle;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;

public class ModParticles {
	private ModParticles() {}

	private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SophisticatedStorage.MOD_ID);

	public static final RegistryObject<CustomTintTerrainParticleData> TERRAIN_PARTICLE = PARTICLES.register("terrain_particle", CustomTintTerrainParticleData::new);

	public static void registerParticles(IEventBus modBus) {
		PARTICLES.register(modBus);
	}

	@SuppressWarnings("unused") // need this to register the event correctly
	public static void registerFactories(ParticleFactoryRegisterEvent event) {
		Minecraft.getInstance().particleEngine.register(TERRAIN_PARTICLE.get(), CustomTintTerrainParticle.Factory::new);
	}
}
