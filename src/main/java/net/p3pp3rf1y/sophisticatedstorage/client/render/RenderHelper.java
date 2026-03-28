package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RenderHelper {
	public static final CameraRenderState ZERO_POS_CAMERA_RENDER_STATE = new CameraRenderState();

	private RenderHelper() {
	}

	private static final Cache<Integer, TextureAtlasSprite> SPRITE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	public static TextureAtlasSprite getSprite(Identifier blockName, @Nullable Direction direction, RandomSource rand) {

		int hash = blockName.hashCode();
		hash = hash * 31 + (direction == null ? 0 : direction.hashCode());

		TextureAtlasSprite sprite = SPRITE_CACHE.getIfPresent(hash);
		if (sprite == null) {
			sprite = parseSprite(blockName, direction, rand);
			SPRITE_CACHE.put(hash, sprite);
		}
		return sprite;
	}

	private static TextureAtlasSprite parseSprite(Identifier blockName, @Nullable Direction direction, RandomSource rand) {
		BlockState blockState = getDefaultBlockState(blockName);

		TextureAtlasSprite sprite = parseSpriteFromModel(blockState, direction, rand);

		if (sprite == null) {
			sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(Blocks.AIR.defaultBlockState()).sprite();
		}

		return sprite;
	}

	@SuppressWarnings("java:S1874")
	//need to call deprecated getQuads here as well just in case it was overriden by mods instead of the main one
	@Nullable
	private static TextureAtlasSprite parseSpriteFromModel(BlockState blockState, @Nullable Direction direction, RandomSource rand) {
		TextureAtlasSprite sprite = null;

		BlockStateModel blockModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}

		try {
			List<BlockStateModelPart> parts = new ArrayList<>();
			blockModel.collectParts(level, BlockPos.ZERO, blockState, rand, parts);

			for (BlockStateModelPart part : parts) {
				List<BakedQuad> quads = part.getQuads(direction);
				if (!quads.isEmpty()) {
					return quads.getFirst().materialInfo().sprite();
				}

				for (BakedQuad quad : part.getQuads(null)) {
					if (sprite == null) {
						sprite = quad.materialInfo().sprite();
					}

					if (quad.direction() == direction) {
						return quad.materialInfo().sprite();
					}
				}
			}
		} catch (Exception e) {
			// NO OP
		}

		if (sprite == null) {
			try {
				sprite = blockModel.particleMaterial(level, BlockPos.ZERO, blockState).sprite();
			} catch (Exception e) {
				// NO OP
			}
		}

		return sprite;
	}

	private static BlockState getDefaultBlockState(Identifier blockName) {
		return BuiltInRegistries.BLOCK.getOptional(blockName).map(Block::defaultBlockState).orElse(Blocks.AIR.defaultBlockState());
	}

	static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight, float alpha, TextureAtlasSprite sprite) {
		renderQuad(consumer, pose, normal, packedOverlay, packedLight, alpha, 0, 0, 1, 1, sprite);
	}

	static void renderQuad(VertexConsumer consumer, Matrix4f pose, Vector3f normal, int packedOverlay, int packedLight, float alpha, float minU, float minV, float maxU, float maxV, TextureAtlasSprite sprite) {
		VertexConsumer spriteConsumer = sprite.wrap(consumer);
		int minX = 0;
		int minY = 0;
		int maxY = 1;
		int maxX = 1;

		addVertex(pose, normal, spriteConsumer, maxY, minX, packedOverlay, packedLight, maxU, minV, alpha);
		addVertex(pose, normal, spriteConsumer, minY, minX, packedOverlay, packedLight, maxU, maxV, alpha);
		addVertex(pose, normal, spriteConsumer, minY, maxX, packedOverlay, packedLight, minU, maxV, alpha);
		addVertex(pose, normal, spriteConsumer, maxY, maxX, packedOverlay, packedLight, minU, minV, alpha);
	}

	private static void addVertex(Matrix4f pose, Vector3f normal, VertexConsumer consumer, int pY, float pX, int packedOverlay, int packedLight, float u, float v, float alpha) {
		Vector4f pos = new Vector4f(pX, pY, 0, 1.0F);
		pose.transform(pos);
		int color = ((int) (alpha * 255)) << 24 | 255 << 16 | 255 << 8 | 255;
		consumer.addVertex(pos.x(), pos.y(), pos.z(), color, u, v, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
	}

	public static boolean isSpecialRenderer(ItemStackRenderState renderState) {
		return false;
	}
}
