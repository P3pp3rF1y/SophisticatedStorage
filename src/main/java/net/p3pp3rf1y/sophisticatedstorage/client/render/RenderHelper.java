package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RenderHelper {
	private RenderHelper() {}

	private static final Cache<Integer, TextureAtlasSprite> SPRITE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	public static TextureAtlasSprite getSprite(ResourceLocation blockName, @Nullable Direction direction, RandomSource rand) {

		int hash = blockName.hashCode();
		hash = hash * 31 + (direction == null ? 0 : direction.hashCode());

		TextureAtlasSprite sprite = SPRITE_CACHE.getIfPresent(hash);
		if (sprite == null) {
			sprite = parseSprite(blockName, direction, rand);
			SPRITE_CACHE.put(hash, sprite);
		}
		return sprite;
	}

	private static TextureAtlasSprite parseSprite(ResourceLocation blockName, @Nullable Direction direction, RandomSource rand) {
		BlockState blockState = getDefaultBlockState(blockName);

		TextureAtlasSprite sprite = parseSpriteFromModel(blockState, direction, rand);

		if (sprite == null) {
			sprite = Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon(ModelData.EMPTY);
		}

		return sprite;
	}

	@SuppressWarnings("java:S1874") //need to call deprecated getQuads here as well just in case it was overriden by mods instead of the main one
	@Nullable
	private static TextureAtlasSprite parseSpriteFromModel(BlockState blockState, @Nullable Direction direction, RandomSource rand) {
		TextureAtlasSprite sprite = null;

		BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
		try {
			for (RenderType layer : blockModel.getRenderTypes(blockState, rand, ModelData.EMPTY)) {
				List<BakedQuad> culledQuads = blockModel.getQuads(blockState, direction, rand, ModelData.EMPTY, layer);
				if (!culledQuads.isEmpty()) {
					return culledQuads.get(0).getSprite();
				}

				//noinspection deprecation
				for (BakedQuad bakedQuad : blockModel.getQuads(blockState, null, rand)) {
					if (sprite == null) {
						sprite = bakedQuad.getSprite();
					}

					if (bakedQuad.getDirection() == direction) {
						return bakedQuad.getSprite();
					}
				}
			}
		}
		catch (Exception e) {
			// NO OP
		}

		if (sprite == null) {
			try {
				sprite = blockModel.getParticleIcon(ModelData.EMPTY);
			}
			catch (Exception e) {
				// NO OP
			}
		}

		return sprite;
	}

	private static BlockState getDefaultBlockState(ResourceLocation blockName) {
		Block block = ForgeRegistries.BLOCKS.getValue(blockName);
		return block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
	}

}