package net.p3pp3rf1y.sophisticatedstorage.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModParticles;

import java.util.Objects;

public class CustomTintTerrainParticleData extends ParticleType<CustomTintTerrainParticleData> implements ParticleOptions {
	private final BlockState state;

	public CustomTintTerrainParticleData() {
		this(Blocks.AIR.defaultBlockState());
	}

	public CustomTintTerrainParticleData(BlockState state) {
		super(false, DESERIALIZER);
		this.state = state;
	}

	@Override
	public CustomTintTerrainParticleData getType() {
		return ModParticles.TERRAIN_PARTICLE.get();
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buffer) {
		buffer.writeVarInt(GameData.getBlockStateIDMap().getId(state));
	}

	@Override
	public String writeToString() {
		return ForgeRegistries.PARTICLE_TYPES.getKey(getType()) + " " + BlockStateParser.serialize(state);
	}

	public static final Deserializer<CustomTintTerrainParticleData> DESERIALIZER = new Deserializer<>() {
		@Override
		public CustomTintTerrainParticleData fromCommand(ParticleType<CustomTintTerrainParticleData> particleType, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			return new CustomTintTerrainParticleData(Objects.requireNonNull((new BlockStateParser(reader, false)).parse(false).getState()));
		}

		@Override
		public CustomTintTerrainParticleData fromNetwork(ParticleType<CustomTintTerrainParticleData> particleType, FriendlyByteBuf buffer) {
			return new CustomTintTerrainParticleData(Objects.requireNonNull(GameData.getBlockStateIDMap().byId(buffer.readVarInt())));
		}
	};

	private final Codec<CustomTintTerrainParticleData> codec = BlockState.CODEC.xmap(CustomTintTerrainParticleData::new, data -> data.state);

	@Override
	public Codec<CustomTintTerrainParticleData> codec() {
		return codec;
	}
}
