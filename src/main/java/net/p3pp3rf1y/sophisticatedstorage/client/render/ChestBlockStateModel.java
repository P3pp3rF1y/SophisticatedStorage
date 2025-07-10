package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
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
	private static final String BLOCK_BREAK_FOLDER = "block/break/";
	private static final Map<String, ResourceLocation> WOOD_BREAK_TEXTURES = new HashMap<>();
	public static final ResourceLocation TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "tintable_chest");

	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> WOOD_BREAK_TEXTURES.put(woodType.name(), SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + woodType.name() + "_chest")));
	}

	@Override
	public void collectParts(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, BlockState blockState, RandomSource randomSource, List<BlockModelPart> list) {
		//noop - this model is rendered dynamically
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TINTABLE_BREAK_TEXTURE);
	}

	@Override
	public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class)
				.map(be -> {
					boolean hasMainColor = be.getStorageWrapper().hasMainColor();
					String woodName = be.getWoodType().map(WoodType::name).orElse("");

					ResourceLocation texture = hasMainColor ? TINTABLE_BREAK_TEXTURE : WOOD_BREAK_TEXTURES.getOrDefault(woodName, TINTABLE_BREAK_TEXTURE);
					return texture;
				}).orElse(TINTABLE_BREAK_TEXTURE));
	}

	public static class Unbaked implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);
		public static final ResourceLocation ID = SophisticatedStorage.getRL("chest_model_loader");

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
