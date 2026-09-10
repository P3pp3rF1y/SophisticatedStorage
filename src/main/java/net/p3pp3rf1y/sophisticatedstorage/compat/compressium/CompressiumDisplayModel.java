package net.p3pp3rf1y.sophisticatedstorage.compat.compressium;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CompressiumDisplayModel extends BakedModelWrapper<BakedModel> {
	private static final ResourceLocation MODEL_LOCATION = new ResourceLocation("sophisticatedstorage", "compressium_display");
	private static final FaceBakery FACE_BAKERY = new FaceBakery();
	private static final Cache<ModelKey, BakedModel> MODELS = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();
	private final Map<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
	private final ItemTransforms transforms;

	private CompressiumDisplayModel(BakedModel original, AlphaMask mask) {
		super(original);
		for (Direction direction : Direction.values()) {
			quads.put(direction, bakeFaceQuads(original.getParticleIcon(), mask, direction));
		}
		ItemTransforms source = original.getTransforms();
		ItemTransform fixed = source.getTransform(ItemTransforms.TransformType.FIXED);
		Vector3f scale = fixed.scale.copy();
		scale.mul(0.5f);
		Vector3f translation = fixed.translation.copy();
		translation.mul(0.5f);
		transforms = new ItemTransforms(source.thirdPersonLeftHand, source.thirdPersonRightHand, source.firstPersonLeftHand,
				source.firstPersonRightHand, source.head, source.gui, source.ground, new ItemTransform(fixed.rotation, translation, scale), source.moddedTransforms);
	}

	public static BakedModel wrap(ItemStack stack, BakedModel model) {
		if (!ModList.get().isLoaded("compressium") || !(stack.getItem() instanceof BlockItem) || model instanceof CompressiumDisplayModel) {
			return model;
		}
		ResourceLocation name = stack.getItem().getRegistryName();
		if (name == null || !"compressium".equals(name.getNamespace())) {
			return model;
		}
		ModelKey key = new ModelKey(model, name);
		BakedModel cached = MODELS.getIfPresent(key);
		if (cached != null) {
			return cached;
		}
		BakedModel replacement = loadModel(name, model);
		MODELS.put(key, replacement);
		return replacement;
	}

	private static BakedModel loadModel(ResourceLocation name, BakedModel original) {
		String path = name.getPath();
		int separator = path.lastIndexOf('_');
		if (separator < 0) {
			return original;
		}
		try {
			int tier = Integer.parseInt(path.substring(separator + 1));
			if (tier < 1 || tier > 9) {
				return original;
			}
			ResourceLocation texture = new ResourceLocation("compressium", "textures/block/layer_" + tier + ".png");
			try (Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture);
					NativeImage image = NativeImage.read(resource.getInputStream())) {
				int[] alpha = new int[image.getWidth() * image.getHeight()];
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						alpha[y * image.getWidth() + x] = NativeImage.getA(image.getPixelRGBA(x, y));
					}
				}
				return new CompressiumDisplayModel(original, new AlphaMask(image.getWidth(), image.getHeight(), alpha));
			}
		} catch (IOException | NumberFormatException e) {
			return original;
		}
	}

	public static void clearCache() {
		MODELS.invalidateAll();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random) {
		return side == null ? List.of() : quads.get(side);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random, IModelData data) {
		return getQuads(state, side, random);
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public ItemTransforms getTransforms() {
		return transforms;
	}

	@Override
	public boolean doesHandlePerspectives() {
		return true;
	}

	@Override
	public BakedModel handlePerspective(ItemTransforms.TransformType type, PoseStack poseStack) {
		return type == ItemTransforms.TransformType.FIXED ? ForgeHooksClient.handlePerspective(this, type, poseStack) : originalModel.handlePerspective(type, poseStack);
	}

	private static List<BakedQuad> bakeFaceQuads(TextureAtlasSprite baseSprite, AlphaMask alphaMask, Direction direction) {
		List<BakedQuad> quads = new ArrayList<>(alphaMask.width() * alphaMask.height());
		float step = 16.0F / alphaMask.width();
		float verticalStep = 16.0F / alphaMask.height();
		for (int y = 0; y < alphaMask.height(); y++) {
			for (int x = 0; x < alphaMask.width(); x++) {
				int alpha = alphaMask.alpha(x, y);
				float x0 = x * step;
				float x1 = (x + 1) * step;
				float y0 = 16.0F - (y + 1) * verticalStep;
				float y1 = 16.0F - y * verticalStep;
				float u0 = x * step;
				float u1 = (x + 1) * step;
				float v0 = y * verticalStep;
				float v1 = (y + 1) * verticalStep;
				int multiplier = Math.max(0, 255 - alpha);
				quads.add(withVertexColor(bakeFaceQuad(baseSprite, direction, x0, y0, x1, y1, u0, v0, u1, v1), grayVertexColor(multiplier)));
			}
		}
		return quads;
	}

	private static BakedQuad bakeFaceQuad(TextureAtlasSprite sprite, Direction direction, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
		Vector3f from;
		Vector3f to;
		if (direction == Direction.UP || direction == Direction.DOWN) {
			from = new Vector3f(x0, 0.0F, y0);
			to = new Vector3f(x1, 16.0F, y1);
		} else if (direction == Direction.EAST || direction == Direction.WEST) {
			from = new Vector3f(0.0F, y0, x0);
			to = new Vector3f(16.0F, y1, x1);
		} else {
			from = new Vector3f(x0, y0, 0.0F);
			to = new Vector3f(x1, y1, 16.0F);
		}
		return FACE_BAKERY.bakeQuad(from, to,
				new BlockElementFace(null, -1, "", new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0)),
				sprite, direction, BlockModelRotation.X0_Y0, null, true, MODEL_LOCATION);
	}

	private static BakedQuad withVertexColor(BakedQuad quad, int color) {
		int[] vertices = quad.getVertices().clone();
		for (int vertex = 0; vertex < 4; vertex++) {
			vertices[vertex * 8 + 3] = color;
		}
		return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
	}

	private static int grayVertexColor(int value) {
		int clamped = Math.max(0, Math.min(255, value));
		return 0xFF000000 | (clamped << 16) | (clamped << 8) | clamped;
	}

	private record ModelKey(BakedModel original, ResourceLocation item) {}

	private record AlphaMask(int width, int height, int[] alpha) {
		int alpha(int x, int y) {
			return alpha[y * width + x];
		}
	}
}
