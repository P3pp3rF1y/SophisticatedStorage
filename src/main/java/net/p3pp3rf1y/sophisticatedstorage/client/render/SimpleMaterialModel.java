package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.*;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleMaterialModel extends AbstractUnbakedModel {
	static final ModelProperty<ResourceLocation> MATERIAL = new ModelProperty<>();
	private static final ModelProperty<Boolean> OVERLAY_HIDDEN = new ModelProperty<>();
	static final ModelProperty<Boolean> OVERLAY_ONLY = new ModelProperty<>();
	static final ModelProperty<Boolean> OVERLAY_EXPANDED = new ModelProperty<>();
	private static final float HIDDEN_OVERLAY_OFFSET = 0.01f;

	private final UnbakedModel baseModel;
	@Nullable
	private final UnbakedModel overlayModel;

	private SimpleMaterialModel(UnbakedModel baseModel, @Nullable UnbakedModel overlayModel, StandardModelParameters parameters) {
		super(parameters);
		this.baseModel = baseModel;
		this.overlayModel = overlayModel;
	}

	@Override
	public BakedModel bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, boolean useAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap contextMap) {
		BakedModel base = UnbakedModel.bakeWithTopModelValues(baseModel, baker, modelState);
		BakedModel overlay = overlayModel == null ? null : UnbakedModel.bakeWithTopModelValues(overlayModel, baker, modelState);
		return new Baked(base, overlay);
	}

	@Override
	public void resolveDependencies(ResolvableModel.Resolver resolver) {
		super.resolveDependencies(resolver);
		baseModel.resolveDependencies(resolver);
		if (overlayModel != null) {
			overlayModel.resolveDependencies(resolver);
		}
	}

	@Override
	public TextureSlots.Data getTextureSlots() {
		Map<String, TextureSlots.SlotContents> textures = new HashMap<>(super.getTextureSlots().values());
		addAllTextureSlots(baseModel, textures);
		if (overlayModel != null) {
			addAllTextureSlots(overlayModel, textures);
		}
		return new TextureSlots.Data(textures);
	}

	private void addAllTextureSlots(UnbakedModel unbakedModel, Map<String, TextureSlots.SlotContents> textures) {
		UnbakedModel model = unbakedModel;
		while (model != null) {
			model.getTextureSlots().values().forEach(textures::putIfAbsent);
			model = model.getParent();
		}
	}

	public static class Baked implements IDynamicBakedModel {
		private final BakedModel base;
		@Nullable
		private final BakedModel overlay;

		private Baked(BakedModel base, @Nullable BakedModel overlay) {
			this.base = base;
			this.overlay = overlay;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
			ResourceLocation material = getMaterial(data);
			if (isOverlayOnly(data)) {
				return overlay != null && material != null && shouldRenderOverlay(renderType) ? getOverlayQuads(state, side, rand, isOverlayExpanded(data)) : List.of();
			}

			if (material == null) {
				return base.getQuads(state, side, rand, data, renderType);
			}

			List<BakedQuad> quads = new ArrayList<>();
			for (BakedQuad quad : getBaseQuadsForMaterial(state, side, rand)) {
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(material, quad.getDirection(), rand);
				if (shouldRenderMaterial(spriteData, renderType)) {
					quads.add(respriteQuad(quad, spriteData.sprite()));
				}
			}

			if (overlay != null && !isOverlayHidden(data) && shouldRenderOverlay(renderType)) {
				quads.addAll(overlay.getQuads(state, side, rand, ModelData.EMPTY, null));
			}
			return quads;
		}

		private boolean shouldRenderOverlay(@Nullable RenderType renderType) {
			return renderType == null || renderType == RenderType.translucent();
		}

		private List<BakedQuad> getBaseQuadsForMaterial(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			List<BakedQuad> quads = new ArrayList<>(base.getQuads(state, side, rand, ModelData.EMPTY, null));
			if (state == null && side == null) {
				for (Direction direction : Direction.values()) {
					quads.addAll(base.getQuads(null, direction, rand, ModelData.EMPTY, null));
				}
			}
			return quads;
		}

		@Nullable
		private ResourceLocation getMaterial(ModelData data) {
			return data.get(MATERIAL);
		}

		private boolean isOverlayHidden(ModelData data) {
			return Boolean.TRUE.equals(data.get(OVERLAY_HIDDEN));
		}

		private boolean isOverlayOnly(ModelData data) {
			return Boolean.TRUE.equals(data.get(OVERLAY_ONLY));
		}

		private boolean isOverlayExpanded(ModelData data) {
			return Boolean.TRUE.equals(data.get(OVERLAY_EXPANDED));
		}

		private List<BakedQuad> getOverlayQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, boolean expanded) {
			if (overlay == null) {
				return List.of();
			}

			List<BakedQuad> overlayQuads = overlay.getQuads(state, side, rand, ModelData.EMPTY, null);
			if (!expanded) {
				return overlayQuads;
			}

			List<BakedQuad> expandedQuads = new ArrayList<>(overlayQuads.size());
			for (BakedQuad quad : overlayQuads) {
				expandedQuads.add(offsetQuad(quad, HIDDEN_OVERLAY_OFFSET));
			}
			return expandedQuads;
		}

		private boolean shouldRenderMaterial(RenderHelper.SpriteData spriteData, @Nullable RenderType renderType) {
			if (renderType == null) {
				return true;
			}

			return (renderType == RenderType.translucent()) == spriteData.translucent();
		}

		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
			ModelData.Builder builder = ModelData.builder();
			WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).ifPresent(simpleMaterialHolder -> {
				simpleMaterialHolder.getMaterial().ifPresent(material -> builder.with(MATERIAL, material));
				if (simpleMaterialHolder.isOverlayHidden()) {
					builder.with(OVERLAY_HIDDEN, true);
				}
			});
			return builder.build();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return base.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return base.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return base.usesBlockLight();
		}

		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return base.getParticleIcon();
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			ResourceLocation material = getMaterial(data);
			return material == null ? base.getParticleIcon(data) : RenderHelper.getSprite(material, null, RandomSource.create(0));
		}

		@SuppressWarnings({"java:S1874", "deprecation"})
		@Override
		public ItemTransforms getTransforms() {
			return base.getTransforms();
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
			if (isOverlayOnly(data)) {
				return ChunkRenderTypeSet.of(RenderType.translucent());
			}

			return getMaterial(data) == null ? base.getRenderTypes(state, rand, data) : ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent());
		}

		@SuppressWarnings("deprecation")
		@Override
		public List<BakedModel> getRenderPasses(ItemStack itemStack) {
			return SimpleMaterialBlockItem.getMaterial(itemStack)
					.<List<BakedModel>>map(material -> List.of(new ResolvedModel(this, material)))
					.orElseGet(() -> base.getRenderPasses(itemStack));
		}

		private static BakedQuad respriteQuad(BakedQuad quad, TextureAtlasSprite newSprite) {
			TextureAtlasSprite oldSprite = quad.getSprite();
			int[] vertices = quad.getVertices();
			QuadBakingVertexConsumer quadBuilder = new QuadBakingVertexConsumer();
			quadBuilder.setSprite(newSprite);
			quadBuilder.setDirection(quad.getDirection());
			quadBuilder.setTintIndex(-1);
			quadBuilder.setShade(quad.isShade());
			quadBuilder.setHasAmbientOcclusion(quad.hasAmbientOcclusion());
			Direction normal = quad.getDirection();

			for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
				int baseIndex = vertexIndex * IQuadTransformer.STRIDE;
				float x = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION]);
				float y = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 1]);
				float z = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 2]);
				float u = remapU(oldSprite, newSprite, Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.UV0]));
				float v = remapV(oldSprite, newSprite, Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.UV0 + 1]));
				int packedLight = vertices[baseIndex + IQuadTransformer.UV2];

				quadBuilder.addVertex(x, y, z)
						.setColor(255, 255, 255, 255)
						.setUv(u, v)
						.setUv2(packedLight & 0xFFFF, (packedLight >>> 16) & 0xFFFF)
						.setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());

				if (IQuadTransformer.UV1 >= 0) {
					int packedOverlay = vertices[baseIndex + IQuadTransformer.UV1];
					quadBuilder.setUv1(packedOverlay & 0xFFFF, (packedOverlay >>> 16) & 0xFFFF);
				}
			}

			return quadBuilder.bakeQuad();
		}

		private static BakedQuad offsetQuad(BakedQuad quad, float offset) {
			int[] vertices = quad.getVertices().clone();
			Direction normal = quad.getDirection();
			float xOffset = normal.getStepX() * offset;
			float yOffset = normal.getStepY() * offset;
			float zOffset = normal.getStepZ() * offset;

			for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
				int baseIndex = vertexIndex * IQuadTransformer.STRIDE;
				vertices[baseIndex + IQuadTransformer.POSITION] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION]) + xOffset);
				vertices[baseIndex + IQuadTransformer.POSITION + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 1]) + yOffset);
				vertices[baseIndex + IQuadTransformer.POSITION + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 2]) + zOffset);
			}

			return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.getLightEmission(), quad.hasAmbientOcclusion());
		}

		private static float remapU(TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, float u) {
			float denominator = oldSprite.getU1() - oldSprite.getU0();
			if (denominator == 0) {
				return newSprite.getU0();
			}
			float ratio = (u - oldSprite.getU0()) / denominator;
			return newSprite.getU0() + ratio * (newSprite.getU1() - newSprite.getU0());
		}

		private static float remapV(TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, float v) {
			float denominator = oldSprite.getV1() - oldSprite.getV0();
			if (denominator == 0) {
				return newSprite.getV0();
			}
			float ratio = (v - oldSprite.getV0()) / denominator;
			return newSprite.getV0() + ratio * (newSprite.getV1() - newSprite.getV0());
		}
	}

	private static class ResolvedModel implements BakedModel {
		private final Baked originalModel;
		private final ResourceLocation material;

		private ResolvedModel(Baked originalModel, ResourceLocation material) {
			this.originalModel = originalModel;
			this.material = material;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			return originalModel.getQuads(state, side, rand, getMaterialModelData(), null);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
			return originalModel.getQuads(state, side, rand, getMaterialModelData(), renderType);
		}

		private ModelData getMaterialModelData() {
			return ModelData.builder().with(MATERIAL, material).build();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return originalModel.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return originalModel.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return originalModel.usesBlockLight();
		}

		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return originalModel.getParticleIcon(getMaterialModelData());
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			return originalModel.getParticleIcon(getMaterialModelData());
		}

		@SuppressWarnings({"java:S1874", "deprecation"})
		@Override
		public ItemTransforms getTransforms() {
			return originalModel.getTransforms();
		}

		@Override
		public void applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
			originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
		}

		@Override
		public RenderType getRenderType(ItemStack itemStack) {
			return RenderTypeHelper.getEntityRenderType(RenderType.translucent());
		}
	}

	@SuppressWarnings("java:S6548")
	public static final class Loader implements UnbakedModelLoader<SimpleMaterialModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public SimpleMaterialModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
			if (!jsonObject.has("base")) {
				throw new JsonParseException("Simple material model requires a base model");
			}

			UnbakedModel baseModel = deserializationContext.deserialize(jsonObject.get("base"), UnbakedModel.class);
			UnbakedModel overlayModel = jsonObject.has("overlay") ? deserializationContext.deserialize(jsonObject.get("overlay"), UnbakedModel.class) : null;
			StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, deserializationContext);
			return new SimpleMaterialModel(baseModel, overlayModel, parameters);
		}
	}
}
