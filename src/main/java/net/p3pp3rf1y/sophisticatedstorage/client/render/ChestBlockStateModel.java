package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestBlockStateModel implements DynamicBlockStateModel {
	private static final String BREAK_TEXTURE_FOLDER = "block/break/";
	private static final Map<String, Identifier> WOOD_BREAK_TEXTURES = new HashMap<>();
	public static final Identifier TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getIdentifier(BREAK_TEXTURE_FOLDER + "tintable_chest");

	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> WOOD_BREAK_TEXTURES.put(woodType.name(), SophisticatedStorage.getIdentifier(BREAK_TEXTURE_FOLDER + woodType.name() + "_chest")));
	}

	@Override
	public void collectParts(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, BlockState blockState, RandomSource randomSource, List<BlockStateModelPart> list) {
		//noop - this model is rendered dynamically
	}

	@Override
	public Material.Baked particleMaterial() {
		return new Material.Baked(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(TINTABLE_BREAK_TEXTURE), false);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return new Material.Baked(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class)
				.map(be -> {
					boolean hasMainColor = be.getStorageWrapper().hasMainColor();
					String woodName = be.getWoodType().map(WoodType::name).orElse("");

					Identifier texture = hasMainColor ? TINTABLE_BREAK_TEXTURE : WOOD_BREAK_TEXTURES.getOrDefault(woodName, TINTABLE_BREAK_TEXTURE);
					return texture;
				}).orElse(TINTABLE_BREAK_TEXTURE)), false);
	}

	@Override
	public int materialFlags() {
		return 0;
	}

	public static class Unbaked implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);
		public static final Identifier ID = SophisticatedStorage.getIdentifier("chest_model_loader");

		@Override
		public BlockStateModel bake(ModelBaker modelBaker) {
			return new ChestBlockStateModel();
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			//noop
		}

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}
	}
}
