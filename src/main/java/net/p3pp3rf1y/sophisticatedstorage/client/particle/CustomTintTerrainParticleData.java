package net.p3pp3rf1y.sophisticatedstorage.client.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModParticles;

public class CustomTintTerrainParticleData extends ParticleType<CustomTintTerrainParticleData> implements ParticleOptions {
	private final BlockState state;
	private final BlockPos pos;

	public CustomTintTerrainParticleData() {
		this(Blocks.AIR.defaultBlockState(), BlockPos.ZERO);
	}

	public CustomTintTerrainParticleData(BlockState state, BlockPos pos) {
		super(false);
		this.state = state;
		this.pos = pos;
	}

	@Override
	public CustomTintTerrainParticleData getType() {
		return ModParticles.TERRAIN_PARTICLE.get();
	}

	private final MapCodec<CustomTintTerrainParticleData> codec = RecordCodecBuilder.mapCodec(
			particleDataInstance -> particleDataInstance.group(
					BlockState.CODEC.fieldOf("state").forGetter(data -> data.state),
					BlockPos.CODEC.fieldOf("pos").forGetter(data -> data.pos)
			).apply(particleDataInstance, CustomTintTerrainParticleData::new));
	private final StreamCodec<RegistryFriendlyByteBuf, CustomTintTerrainParticleData> streamCodec = StreamCodec.composite(
			StreamCodecHelper.BLOCKSTATE,
			CustomTintTerrainParticleData::getState,
			BlockPos.STREAM_CODEC,
			CustomTintTerrainParticleData::getPos,
			CustomTintTerrainParticleData::new);

	@Override
	public MapCodec<CustomTintTerrainParticleData> codec() {
		return codec;
	}

	@Override
	public StreamCodec<? super RegistryFriendlyByteBuf, CustomTintTerrainParticleData> streamCodec() {
		return streamCodec;
	}

	public BlockState getState() {
		return state;
	}

	public BlockPos getPos() {
		return pos;
	}
}
