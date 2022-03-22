package net.p3pp3rf1y.sophisticatedstorage.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;

import javax.annotation.Nullable;

public class CustomTintTerrainParticle extends TerrainParticle {
	public CustomTintTerrainParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state, BlockPos pos) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed, state);

		if (state.getBlock() instanceof BarrelBlock) {
			int i = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 1000);
			rCol *= (i >> 16 & 255) / 255.0F;
			gCol *= (i >> 8 & 255) / 255.0F;
			bCol *= (i & 255) / 255.0F;
		}
	}

	public static class Factory implements ParticleProvider<CustomTintTerrainParticleData> {
		@Nullable
		@Override
		public Particle createParticle(CustomTintTerrainParticleData type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = level.getBlockState(pos);
			CustomTintTerrainParticle particle = new CustomTintTerrainParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, state, pos);
			particle.updateSprite(state, pos);
			return particle;
		}
	}
}
