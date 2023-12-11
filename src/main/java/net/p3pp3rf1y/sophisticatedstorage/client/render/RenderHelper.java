package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

	static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight, float alpha) {
		renderQuad(consumer, pose, normal, packedOverlay, packedLight, alpha, 0, 0, 1, 1);
	}
	static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight, float alpha, float minU, float minV, float maxU, float maxV) {
		int minX = 0;
		int minY = 0;
		int maxY = 1;
		int maxX = 1;

		addVertex(pose, normal, consumer, maxY, minX, packedOverlay, packedLight, maxU, minV, alpha);
		addVertex(pose, normal, consumer, minY, minX, packedOverlay, packedLight, maxU, maxV, alpha);
		addVertex(pose, normal, consumer, minY, maxX, packedOverlay, packedLight, minU, maxV, alpha);
		addVertex(pose, normal, consumer, maxY, maxX, packedOverlay, packedLight, minU, minV, alpha);
	}

	private static void addVertex(Matrix4f pose, Vector3f normal, VertexConsumer pConsumer, int pY, float pX, int packedOverlay, int packedLight, float u, float v, float alpha) {
		Vector4f pos = new Vector4f(pX, pY, 0, 1.0F);
		pose.transform(pos);
		pConsumer.vertex(pos.x(), pos.y(), pos.z(), 1, 1, 1, alpha, u, v, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
	}
}