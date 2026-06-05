package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RenderHelper {
	private RenderHelper() {
	}

	private static final Cache<Integer, SpriteData> SPRITE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	public static TextureAtlasSprite getSprite(ResourceLocation blockName, @Nullable Direction direction, RandomSource rand) {
		return getSpriteData(blockName, direction, rand).sprite();
	}

	public static SpriteData getSpriteData(ResourceLocation blockName, @Nullable Direction direction, RandomSource rand) {
		int hash = blockName.hashCode();
		hash = hash * 31 + (direction == null ? 0 : direction.hashCode());

		SpriteData spriteData = SPRITE_CACHE.getIfPresent(hash);
		if (spriteData == null) {
			spriteData = parseSpriteData(blockName, direction, rand);
			SPRITE_CACHE.put(hash, spriteData);
		}
		return spriteData;
	}

	private static SpriteData parseSpriteData(ResourceLocation blockName, @Nullable Direction direction, RandomSource rand) {
		BlockState blockState = getDefaultBlockState(blockName);

		SpriteData spriteData = parseSpriteFromModel(blockState, direction, rand);

		if (spriteData == null) {
			spriteData = new SpriteData(Minecraft.getInstance().getModelManager().getMissingBlockStateModel().particleIcon(), -1, false);
		}

		return spriteData;
	}

	@SuppressWarnings("java:S1874")
	//need to call deprecated getQuads here as well just in case it was overriden by mods instead of the main one
	@Nullable
	private static SpriteData parseSpriteFromModel(BlockState blockState, @Nullable Direction direction, RandomSource rand) {
		SpriteData spriteData = null;

		BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}

		try {
			List<BlockModelPart> parts = blockModel.collectParts(level, BlockPos.ZERO, blockState, rand);

			for (BlockModelPart part : parts) {
				boolean translucent = part.getRenderType(blockState) == RenderType.translucent();
				List<BakedQuad> quads = part.getQuads(direction);
				if (!quads.isEmpty()) {
					BakedQuad quad = quads.getFirst();
					return new SpriteData(quad.sprite(), quad.tintIndex(), translucent);
				}

				for (BakedQuad quad : part.getQuads(null)) {
					if (spriteData == null) {
						spriteData = new SpriteData(quad.sprite(), quad.tintIndex(), translucent);
					}

					if (quad.direction() == direction) {
						return new SpriteData(quad.sprite(), quad.tintIndex(), translucent);
					}
				}
			}
		} catch (Exception e) {
			// NO OP
		}

		if (spriteData == null) {
			try {
				spriteData = new SpriteData(blockModel.particleIcon(level, BlockPos.ZERO, blockState), -1, false);
			} catch (Exception e) {
				// NO OP
			}
		}

		return spriteData;
	}

	public record SpriteData(TextureAtlasSprite sprite, int tintIndex, boolean translucent) {}

	private static BlockState getDefaultBlockState(ResourceLocation blockName) {
		return BuiltInRegistries.BLOCK.getOptional(blockName).map(Block::defaultBlockState).orElse(Blocks.AIR.defaultBlockState());
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

	private static void addVertex(Matrix4f pose, Vector3f normal, VertexConsumer consumer, int pY, float pX, int packedOverlay, int packedLight, float u, float v, float alpha) {
		Vector4f pos = new Vector4f(pX, pY, 0, 1.0F);
		pose.transform(pos);
		int color = ((int) (alpha * 255)) << 24 | 255 << 16 | 255 << 8 | 255;
		consumer.addVertex(pos.x(), pos.y(), pos.z(), color, u, v, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
	}

	public static boolean isSpecialRenderer(ItemStackRenderState renderState) {
		for (ItemStackRenderState.LayerRenderState layer : renderState.layers) {
			if (layer.specialRenderer != null) {
				return true;
			}
		}
		return false;
	}
}
