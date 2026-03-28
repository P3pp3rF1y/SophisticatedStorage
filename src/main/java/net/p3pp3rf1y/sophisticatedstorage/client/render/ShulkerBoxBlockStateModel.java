package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;

import java.util.List;

public class ShulkerBoxBlockStateModel implements DynamicBlockStateModel {
	private static final String BLOCK_BREAK_FOLDER = "block/break/";
	public static final Identifier TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getIdentifier(BLOCK_BREAK_FOLDER + "tintable_shulker_box");
	public static final Identifier MAIN_BREAK_TEXTURE = SophisticatedStorage.getIdentifier(BLOCK_BREAK_FOLDER + "shulker_box");

	@Override
	public void collectParts(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, BlockState blockState, RandomSource randomSource, List<BlockStateModelPart> list) {
		//noop - this model is rendered dynamically
	}

	@Override
	public Material.Baked particleMaterial() {
		return new Material.Baked(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MAIN_BREAK_TEXTURE), false);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return new Material.Baked(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WorldHelper.getBlockEntity(level, pos, ShulkerBoxBlockEntity.class)
				.map(be -> be.getStorageWrapper().hasMainColor() ? TINTABLE_BREAK_TEXTURE : MAIN_BREAK_TEXTURE).orElse(MAIN_BREAK_TEXTURE)), false);
	}

	@Override
	public int materialFlags() {
		return 0;
	}

	public static class Unbaked implements CustomUnbakedBlockStateModel {
		public static final MapCodec<ShulkerBoxBlockStateModel.Unbaked> CODEC = MapCodec.unit(ShulkerBoxBlockStateModel.Unbaked::new);
		public static final Identifier ID = SophisticatedStorage.getIdentifier("shulker_box_model_loader");

		@Override
		public BlockStateModel bake(ModelBaker modelBaker) {
			return new ShulkerBoxBlockStateModel();
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
