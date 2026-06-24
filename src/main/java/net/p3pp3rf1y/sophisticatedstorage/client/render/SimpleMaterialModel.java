package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SimpleMaterialModel extends AbstractUnbakedModel {
	private static final float HIDDEN_OVERLAY_OFFSET = 0.01f;
	private static final Vector3f DEFAULT_ROTATION = new Vector3f(0, 0, 0);
	private static final ItemTransforms ITEM_TRANSFORMS = createItemTransforms();

	@SuppressWarnings("java:S4738")
	private static ItemTransforms createItemTransforms() {
		return new ItemTransforms(
				new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 14.25f / 16f, 0), new Vector3f(1, 1, 1), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3 / 16f, 0), new Vector3f(0.25f, 0.25f, 0.25f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f), DEFAULT_ROTATION), ImmutableMap.of());
	}

	private final UnbakedModel baseModel;
	@Nullable
	private final UnbakedModel overlayModel;

	private SimpleMaterialModel(UnbakedModel baseModel, @Nullable UnbakedModel overlayModel, StandardModelParameters parameters) {
		super(parameters);
		this.baseModel = baseModel;
		this.overlayModel = overlayModel;
	}

	@Override
	public void resolveDependencies(ResolvableModel.Resolver resolver) {
		super.resolveDependencies(resolver);
		resolveInlineModelDependencies(baseModel, resolver);
		if (overlayModel != null) {
			resolveInlineModelDependencies(overlayModel, resolver);
		}
	}

	private static void resolveInlineModelDependencies(UnbakedModel model, ResolvableModel.Resolver resolver) {
		if (model.parent() != null) {
			resolver.markDependency(model.parent());
		}
		model.resolveDependencies(resolver);
	}

	@Override
	public TextureSlots.Data textureSlots() {
		Map<String, TextureSlots.SlotContents> textures = new HashMap<>(super.textureSlots().values());
		addAllTextureSlots(baseModel, textures);
		if (overlayModel != null) {
			addAllTextureSlots(overlayModel, textures);
		}
		return new TextureSlots.Data(textures);
	}

	private static void addAllTextureSlots(UnbakedModel unbakedModel, Map<String, TextureSlots.SlotContents> textures) {
		unbakedModel.textureSlots().values().forEach(textures::putIfAbsent);
	}

	public SimpleMaterialBlockStateModel bakeBlockStateModel(ModelBaker baker, ResolvedModel resolvedModel, ModelState modelState) {
		ResolvedModel resolvedBase = baker.resolveInlineModel(baseModel, () -> resolvedModel.debugName() + "_base");
		QuadCollection baseQuads = bakeModel(resolvedBase, baker, modelState);
		TextureAtlasSprite particleIcon = resolvedBase.resolveParticleSprite(resolvedBase.getTopTextureSlots(), baker);
		boolean ambientOcclusion = resolvedBase.getTopAmbientOcclusion();

		QuadCollection overlayQuads = QuadCollection.EMPTY;
		if (overlayModel != null) {
			ResolvedModel resolvedOverlay = baker.resolveInlineModel(overlayModel, () -> resolvedModel.debugName() + "_overlay");
			overlayQuads = bakeModel(resolvedOverlay, baker, modelState);
		}

		return new SimpleMaterialBlockStateModel(baseQuads, overlayQuads, particleIcon, ambientOcclusion);
	}

	private static QuadCollection bakeModel(ResolvedModel resolvedModel, ModelBaker baker, ModelState modelState) {
		return resolvedModel.bakeTopGeometry(resolvedModel.getTopTextureSlots(), baker, modelState);
	}

	public static class SimpleMaterialBlockStateModel implements DynamicBlockStateModel {
		private final QuadCollection baseQuads;
		private final QuadCollection overlayQuads;
		private final TextureAtlasSprite particleIcon;
		private final boolean ambientOcclusion;

		private SimpleMaterialBlockStateModel(QuadCollection baseQuads, QuadCollection overlayQuads, TextureAtlasSprite particleIcon,
				boolean ambientOcclusion) {
			this.baseQuads = baseQuads;
			this.overlayQuads = overlayQuads;
			this.particleIcon = particleIcon;
			this.ambientOcclusion = ambientOcclusion;
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, List<BlockModelPart> parts) {
			Optional<ISimpleMaterialHolder> simpleMaterialHolder = WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class);
			ResourceLocation material = simpleMaterialHolder.flatMap(ISimpleMaterialHolder::getMaterial).orElse(null);
			if (material == null) {
				parts.add(new SimpleModelWrapper(baseQuads, ambientOcclusion, particleIcon, ChunkSectionLayer.CUTOUT));
				return;
			}

			addMaterialParts(parts, material, simpleMaterialHolder.map(ISimpleMaterialHolder::isOverlayHidden).orElse(false), false, rand);
		}

		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> output) {
			output.add(new SimpleModelWrapper(baseQuads, ambientOcclusion, particleIcon, ChunkSectionLayer.CUTOUT));
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			return particleIcon;
		}

		@Override
		public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			return WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).flatMap(ISimpleMaterialHolder::getMaterial)
					.map(material -> RenderHelper.getSprite(material, null, RandomSource.create(0))).orElse(particleIcon);
		}

		public List<BakedQuad> getItemQuads(ResourceLocation material, RandomSource rand) {
			List<BlockModelPart> parts = new ArrayList<>();
			addMaterialParts(parts, material, false, false, rand);
			List<BakedQuad> quads = new ArrayList<>();
			for (BlockModelPart part : parts) {
				for (Direction direction : Direction.values()) {
					quads.addAll(part.getQuads(direction));
				}
				quads.addAll(part.getQuads(null));
			}
			return quads;
		}

		public List<BakedQuad> getOverlayOnlyQuads(ResourceLocation material, boolean expanded) {
			if (overlayQuads.getAll().isEmpty()) {
				return List.of();
			}

			List<BakedQuad> quads = new ArrayList<>();
			addOverlayQuads(overlayQuads, quads, expanded);
			return quads;
		}

		private void addMaterialParts(List<BlockModelPart> parts, ResourceLocation material, boolean overlayHidden, boolean overlayExpanded,
				RandomSource rand) {
			QuadCollection.Builder cutoutBuilder = new QuadCollection.Builder();
			QuadCollection.Builder translucentBuilder = new QuadCollection.Builder();

			addMaterialQuads(cutoutBuilder, translucentBuilder, material, rand);
			if (!overlayHidden && !overlayQuads.getAll().isEmpty()) {
				addOverlayQuads(overlayQuads, translucentBuilder, overlayExpanded);
			}

			QuadCollection cutoutQuads = cutoutBuilder.build();
			if (!cutoutQuads.getAll().isEmpty()) {
				parts.add(new SimpleModelWrapper(cutoutQuads, ambientOcclusion, particleIcon, ChunkSectionLayer.CUTOUT));
			}

			QuadCollection translucentQuads = translucentBuilder.build();
			if (!translucentQuads.getAll().isEmpty()) {
				parts.add(new SimpleModelWrapper(translucentQuads, ambientOcclusion, particleIcon, ChunkSectionLayer.TRANSLUCENT));
			}
		}

		private void addMaterialQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder, ResourceLocation material,
				RandomSource rand) {
			for (Direction direction : Direction.values()) {
				for (BakedQuad quad : baseQuads.getQuads(direction)) {
					RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(material, direction, rand);
					(spriteData.translucent() ? translucentBuilder : cutoutBuilder).addCulledFace(direction, respriteQuad(quad, spriteData.sprite()));
				}
				rand.setSeed(42L);
			}
			for (BakedQuad quad : baseQuads.getQuads(null)) {
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(material, quad.direction(), rand);
				(spriteData.translucent() ? translucentBuilder : cutoutBuilder).addUnculledFace(respriteQuad(quad, spriteData.sprite()));
			}
		}

		private static void addOverlayQuads(QuadCollection source, QuadCollection.Builder builder, boolean expanded) {
			for (Direction direction : Direction.values()) {
				for (BakedQuad quad : source.getQuads(direction)) {
					builder.addCulledFace(direction, expanded ? offsetQuad(quad, HIDDEN_OVERLAY_OFFSET) : quad);
				}
			}
			for (BakedQuad quad : source.getQuads(null)) {
				builder.addUnculledFace(expanded ? offsetQuad(quad, HIDDEN_OVERLAY_OFFSET) : quad);
			}
		}

		private static void addOverlayQuads(QuadCollection source, List<BakedQuad> quads, boolean expanded) {
			for (Direction direction : Direction.values()) {
				for (BakedQuad quad : source.getQuads(direction)) {
					quads.add(expanded ? offsetQuad(quad, HIDDEN_OVERLAY_OFFSET) : quad);
				}
			}
			for (BakedQuad quad : source.getQuads(null)) {
				quads.add(expanded ? offsetQuad(quad, HIDDEN_OVERLAY_OFFSET) : quad);
			}
		}
	}

	private static BakedQuad respriteQuad(BakedQuad quad, TextureAtlasSprite newSprite) {
		TextureAtlasSprite oldSprite = quad.sprite();
		int[] vertices = quad.vertices();
		QuadBakingVertexConsumer quadBuilder = new QuadBakingVertexConsumer();
		quadBuilder.setSprite(newSprite);
		quadBuilder.setDirection(quad.direction());
		quadBuilder.setTintIndex(-1);
		quadBuilder.setShade(quad.shade());
		quadBuilder.setHasAmbientOcclusion(quad.hasAmbientOcclusion());
		Direction normal = quad.direction();

		for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
			int baseIndex = vertexIndex * IQuadTransformer.STRIDE;
			float x = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION]);
			float y = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 1]);
			float z = Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 2]);
			float u = remapU(oldSprite, newSprite, Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.UV0]));
			float v = remapV(oldSprite, newSprite, Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.UV0 + 1]));
			int packedLight = vertices[baseIndex + IQuadTransformer.UV2];

			quadBuilder.addVertex(x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setUv2(packedLight & 0xFFFF, (packedLight >>> 16) & 0xFFFF)
					.setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());

			if (IQuadTransformer.UV1 >= 0) {
				int packedOverlay = vertices[baseIndex + IQuadTransformer.UV1];
				quadBuilder.setUv1(packedOverlay & 0xFFFF, (packedOverlay >>> 16) & 0xFFFF);
			}
		}

		return quadBuilder.bakeQuad();
	}

	private static BakedQuad offsetQuad(BakedQuad quad, float offset) {
		int[] vertices = quad.vertices().clone();
		Direction normal = quad.direction();
		float xOffset = normal.getStepX() * offset;
		float yOffset = normal.getStepY() * offset;
		float zOffset = normal.getStepZ() * offset;

		for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
			int baseIndex = vertexIndex * IQuadTransformer.STRIDE;
			vertices[baseIndex + IQuadTransformer.POSITION] = Float
					.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION]) + xOffset);
			vertices[baseIndex + IQuadTransformer.POSITION + 1] = Float
					.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 1]) + yOffset);
			vertices[baseIndex + IQuadTransformer.POSITION + 2] = Float
					.floatToRawIntBits(Float.intBitsToFloat(vertices[baseIndex + IQuadTransformer.POSITION + 2]) + zOffset);
		}

		return new BakedQuad(vertices, quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission(), quad.hasAmbientOcclusion());
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

	public record SimpleMaterialItemModel(SimpleMaterialBlockStateModel model, Supplier<Vector3f[]> extents,
			ModelRenderProperties properties) implements ItemModel {
		private SimpleMaterialItemModel(SimpleMaterialBlockStateModel model, ModelRenderProperties properties) {
			this(model, Suppliers.memoize(() -> BlockModelWrapper.computeExtents(model.baseQuads.getAll())), properties);
		}

		@Override
		public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
				@Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed) {
			state.appendModelIdentityElement(this);
			SimpleMaterialBlockItem.getMaterial(stack).ifPresentOrElse(material -> {
				state.appendModelIdentityElement(material);
				ItemStackRenderState.LayerRenderState layerState = state.newLayer();
				layerState.setExtents(extents);
				layerState.setUsesBlockLight(true);
				layerState.setRenderType(Sheets.translucentItemSheet());
				layerState.setParticleIcon(RenderHelper.getSprite(material, null, RandomSource.create(0)));
				properties.applyToLayer(layerState, itemDisplayContext);
				layerState.setTransform(ITEM_TRANSFORMS.getTransform(itemDisplayContext));
				layerState.prepareQuadList().addAll(model.getItemQuads(material, clientLevel != null ? clientLevel.random : RandomSource.create(42L)));
			}, () -> {
				ItemStackRenderState.LayerRenderState layerState = state.newLayer();
				layerState.setExtents(extents);
				layerState.setUsesBlockLight(true);
				layerState.setRenderType(Sheets.translucentItemSheet());
				layerState.setParticleIcon(model.particleIcon());
				properties.applyToLayer(layerState, itemDisplayContext);
				layerState.setTransform(ITEM_TRANSFORMS.getTransform(itemDisplayContext));
				layerState.prepareQuadList().addAll(model.baseQuads.getAll());
			});
		}

		public record Unbaked(ResourceLocation model) implements ItemModel.Unbaked {
			public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder
					.mapCodec(instance -> instance.group(ResourceLocation.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(instance, Unbaked::new));

			@Override
			public MapCodec<? extends ItemModel.Unbaked> type() {
				return MAP_CODEC;
			}

			@Override
			public ItemModel bake(ItemModel.BakingContext context) {
				ResolvedModel resolvedModel = context.blockModelBaker().getModel(model);
				if (resolvedModel.wrapped() instanceof SimpleMaterialModel simpleMaterialModel) {
					TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
					ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(context.blockModelBaker(), resolvedModel, textureSlots);
					return new SimpleMaterialItemModel(
							simpleMaterialModel.bakeBlockStateModel(context.blockModelBaker(), resolvedModel, BlockModelRotation.X0_Y0), properties);
				}
				throw new IllegalStateException("Expected SimpleMaterialModel for " + model + ", got " + resolvedModel.wrapped().getClass().getName());
			}

			@Override
			public void resolveDependencies(ResolvableModel.Resolver resolver) {
				resolver.markDependency(model);
			}
		}
	}

	public record UnbakedBlockStateModel(Variant variant) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<UnbakedBlockStateModel> CODEC = Variant.MAP_CODEC.xmap(UnbakedBlockStateModel::new, UnbakedBlockStateModel::variant);
		public static final ResourceLocation ID = SophisticatedStorage.getRL("simple_material_blockstate_model_loader");

		@Override
		public BlockStateModel bake(ModelBaker modelBaker) {
			ResolvedModel resolvedModel = modelBaker.getModel(variant.modelLocation());
			if (resolvedModel.wrapped() instanceof SimpleMaterialModel simpleMaterialModel) {
				return simpleMaterialModel.bakeBlockStateModel(modelBaker, resolvedModel, variant.modelState().asModelState());
			}
			throw new IllegalStateException(
					"Expected SimpleMaterialModel for " + variant.modelLocation() + ", got " + resolvedModel.wrapped().getClass().getName());
		}

		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
			variant.resolveDependencies(resolver);
		}

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
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
