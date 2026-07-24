package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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
				new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f), DEFAULT_ROTATION),
				new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(1, 1, 1), DEFAULT_ROTATION), ImmutableMap.of());
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
		Material.Baked particleMaterial = resolvedBase.resolveParticleMaterial(resolvedBase.getTopTextureSlots(), baker);
		boolean ambientOcclusion = resolvedBase.getTopAmbientOcclusion();

		QuadCollection overlayQuads = QuadCollection.EMPTY;
		if (overlayModel != null) {
			ResolvedModel resolvedOverlay = baker.resolveInlineModel(overlayModel, () -> resolvedModel.debugName() + "_overlay");
			overlayQuads = bakeModel(resolvedOverlay, baker, modelState);
		}

		return new SimpleMaterialBlockStateModel(baseQuads, overlayQuads, particleMaterial, ambientOcclusion);
	}

	private static QuadCollection bakeModel(ResolvedModel resolvedModel, ModelBaker baker, ModelState modelState) {
		return resolvedModel.bakeTopGeometry(resolvedModel.getTopTextureSlots(), baker, modelState);
	}

	public static class SimpleMaterialBlockStateModel implements DynamicBlockStateModel {
		private final QuadCollection baseQuads;
		private final QuadCollection overlayQuads;
		private final Material.Baked particleMaterial;
		private final boolean ambientOcclusion;

		private SimpleMaterialBlockStateModel(QuadCollection baseQuads, QuadCollection overlayQuads, Material.Baked particleMaterial,
				boolean ambientOcclusion) {
			this.baseQuads = baseQuads;
			this.overlayQuads = overlayQuads;
			this.particleMaterial = particleMaterial;
			this.ambientOcclusion = ambientOcclusion;
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, List<BlockStateModelPart> parts) {
			Optional<ISimpleMaterialHolder> simpleMaterialHolder = WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class);
			Identifier material = simpleMaterialHolder.flatMap(ISimpleMaterialHolder::getMaterial).orElse(null);
			if (material == null) {
				parts.add(new SimpleModelWrapper(baseQuads, ambientOcclusion, particleMaterial));
				return;
			}

			addMaterialParts(parts, material, simpleMaterialHolder.map(ISimpleMaterialHolder::isOverlayHidden).orElse(false), false, rand,
					getMaterialTintColors(material, level, pos));
		}

		@Override
		public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
			output.add(new SimpleModelWrapper(baseQuads, ambientOcclusion, particleMaterial));
		}

		@Override
		public Material.Baked particleMaterial() {
			return particleMaterial;
		}

		@Override
		public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			return WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).flatMap(ISimpleMaterialHolder::getMaterial)
					.map(material -> new Material.Baked(RenderHelper.getSprite(material, null, RandomSource.create(0)), false)).orElse(particleMaterial);
		}

		@Override
		public int materialFlags() {
			return 0;
		}

		public List<BakedQuad> getItemQuads(Identifier material, RandomSource rand) {
			List<BlockStateModelPart> parts = new ArrayList<>();
			addMaterialParts(parts, material, false, false, rand, getMaterialTintColors(material, null, null));
			List<BakedQuad> quads = new ArrayList<>();
			for (BlockStateModelPart part : parts) {
				for (Direction direction : Direction.values()) {
					quads.addAll(part.getQuads(direction));
				}
				quads.addAll(part.getQuads(null));
			}
			return quads;
		}

		public List<BakedQuad> getOverlayOnlyQuads(Identifier material, boolean expanded) {
			if (overlayQuads.getAll().isEmpty()) {
				return List.of();
			}

			List<BakedQuad> quads = new ArrayList<>();
			addOverlayQuads(overlayQuads, quads, expanded);
			return quads;
		}

		private void addMaterialParts(List<BlockStateModelPart> parts, Identifier material, boolean overlayHidden, boolean overlayExpanded, RandomSource rand,
				Map<Identifier, Integer> materialTintColors) {
			QuadCollection.Builder cutoutBuilder = new QuadCollection.Builder();
			QuadCollection.Builder translucentBuilder = new QuadCollection.Builder();

			addMaterialQuads(cutoutBuilder, translucentBuilder, material, rand, materialTintColors);
			if (!overlayHidden && !overlayQuads.getAll().isEmpty()) {
				addOverlayQuads(overlayQuads, translucentBuilder, overlayExpanded);
			}

			QuadCollection cutoutQuads = cutoutBuilder.build();
			if (!cutoutQuads.getAll().isEmpty()) {
				parts.add(new SimpleModelWrapper(cutoutQuads, ambientOcclusion, particleMaterial));
			}

			QuadCollection translucentQuads = translucentBuilder.build();
			if (!translucentQuads.getAll().isEmpty()) {
				parts.add(new SimpleModelWrapper(translucentQuads, ambientOcclusion, new Material.Baked(particleMaterial.sprite(), true)));
			}
		}

		private void addMaterialQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder, Identifier material, RandomSource rand,
				Map<Identifier, Integer> materialTintColors) {
			for (Direction direction : Direction.values()) {
				for (BakedQuad quad : baseQuads.getQuads(direction)) {
					for (RenderHelper.SpriteData spriteData : RenderHelper.getSpriteDataList(material, direction, rand)) {
						(spriteData.translucent() ? translucentBuilder : cutoutBuilder).addCulledFace(direction,
								applyMaterialTint(respriteQuad(quad, spriteData.sprite()), materialTintColors));
					}
				}
				rand.setSeed(42L);
			}
			for (BakedQuad quad : baseQuads.getQuads(null)) {
				for (RenderHelper.SpriteData spriteData : RenderHelper.getSpriteDataList(material, quad.direction(), rand)) {
					(spriteData.translucent() ? translucentBuilder : cutoutBuilder)
							.addUnculledFace(applyMaterialTint(respriteQuad(quad, spriteData.sprite()), materialTintColors));
				}
			}
		}

		private static BakedQuad applyMaterialTint(BakedQuad quad, Map<Identifier, Integer> materialTintColors) {
			Integer tintColor = materialTintColors.get(quad.materialInfo().sprite().contents().name());
			return tintColor == null
					? quad
					: new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(), quad.packedUV1(),
							quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo(), quad.bakedNormals(), BakedColors.of(tintColor));
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

	private static Map<Identifier, Integer> getMaterialTintColors(Identifier material, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos) {
		Map<Identifier, Integer> materialTintColors = new HashMap<>();
		BlockState blockState = BuiltInRegistries.BLOCK.get(material).orElseThrow().value().defaultBlockState();
		RandomSource rand = RandomSource.create();
		for (Direction direction : Direction.values()) {
			for (RenderHelper.SpriteData spriteData : RenderHelper.getSpriteDataList(material, direction, rand)) {
				if (spriteData.tintIndex() >= 0) {
					BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, spriteData.tintIndex());
					if (tintSource != null) {
						int tintColor = world == null || pos == null ? tintSource.color(blockState) : tintSource.colorInWorld(blockState, world, pos);
						materialTintColors.put(spriteData.sprite().contents().name(), 0xFF000000 | (tintColor & 0xFFFFFF));
					}
				}
			}
		}
		return Map.copyOf(materialTintColors);
	}

	private static BakedQuad respriteQuad(BakedQuad quad, TextureAtlasSprite newSprite) {
		TextureAtlasSprite oldSprite = quad.materialInfo().sprite();
		ChunkSectionLayer layer = newSprite.transparency().hasTranslucent() ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;
		QuadBakingVertexConsumer quadBuilder = new QuadBakingVertexConsumer();
		quadBuilder.setSprite(newSprite, layer, layer.translucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet());
		quadBuilder.setDirection(quad.direction());
		quadBuilder.setTintIndex(-1);
		quadBuilder.setShade(quad.materialInfo().shade());
		quadBuilder.setLightEmission(quad.materialInfo().lightEmission());
		quadBuilder.setAmbientOcclusion(quad.materialInfo().ambientOcclusion());
		Direction normal = quad.direction();

		for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
			Vector3fc position = quad.position(vertexIndex);
			long packedUv = quad.packedUV(vertexIndex);
			float u = remapU(oldSprite, newSprite, UVPair.unpackU(packedUv));
			float v = remapV(oldSprite, newSprite, UVPair.unpackV(packedUv));

			quadBuilder.addVertex(position.x(), position.y(), position.z()).setColor(255, 255, 255, 255).setUv(u, v).setNormal(normal.getStepX(),
					normal.getStepY(), normal.getStepZ());
		}

		return quadBuilder.bakeQuad();
	}

	private static BakedQuad offsetQuad(BakedQuad quad, float offset) {
		Direction normal = quad.direction();
		float xOffset = normal.getStepX() * offset;
		float yOffset = normal.getStepY() * offset;
		float zOffset = normal.getStepZ() * offset;

		return new BakedQuad(offsetPosition(quad.position0(), xOffset, yOffset, zOffset), offsetPosition(quad.position1(), xOffset, yOffset, zOffset),
				offsetPosition(quad.position2(), xOffset, yOffset, zOffset), offsetPosition(quad.position3(), xOffset, yOffset, zOffset), quad.packedUV0(),
				quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo(), quad.bakedNormals(), quad.bakedColors());
	}

	private static Vector3f offsetPosition(Vector3fc position, float xOffset, float yOffset, float zOffset) {
		return new Vector3f(position).add(xOffset, yOffset, zOffset);
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

	public record SimpleMaterialItemModel(SimpleMaterialBlockStateModel model, Supplier<Vector3fc[]> extents, ModelRenderProperties properties,
			Matrix4fc transformation) implements ItemModel {
		private SimpleMaterialItemModel(SimpleMaterialBlockStateModel model, ModelRenderProperties properties, Matrix4fc transformation) {
			this(model, Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(model.baseQuads.getAll())), properties, transformation);
		}

		@Override
		public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
				@Nullable ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int seed) {
			state.appendModelIdentityElement(this);
			SimpleMaterialBlockItem.getMaterial(stack).ifPresentOrElse(material -> {
				state.appendModelIdentityElement(material);
				ItemStackRenderState.LayerRenderState layerState = state.newLayer();
				layerState.setExtents(extents);
				layerState.setUsesBlockLight(true);
				layerState.setParticleMaterial(new Material.Baked(RenderHelper.getSprite(material, null, RandomSource.create(0)), false));
				layerState.setLocalTransform(transformation);
				properties.applyToLayer(layerState, itemDisplayContext);
				layerState.setItemTransform(ITEM_TRANSFORMS.getTransform(itemDisplayContext));
				layerState.prepareQuadList().addAll(model.getItemQuads(material, clientLevel != null ? clientLevel.getRandom() : RandomSource.create(42L)));
			}, () -> {
				ItemStackRenderState.LayerRenderState layerState = state.newLayer();
				layerState.setExtents(extents);
				layerState.setUsesBlockLight(true);
				layerState.setParticleMaterial(model.particleMaterial());
				layerState.setLocalTransform(transformation);
				properties.applyToLayer(layerState, itemDisplayContext);
				layerState.setItemTransform(ITEM_TRANSFORMS.getTransform(itemDisplayContext));
				layerState.prepareQuadList().addAll(model.baseQuads.getAll());
			});
		}

		public record Unbaked(Identifier model) implements ItemModel.Unbaked {
			public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder
					.mapCodec(instance -> instance.group(Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(instance, Unbaked::new));

			@Override
			public MapCodec<? extends ItemModel.Unbaked> type() {
				return MAP_CODEC;
			}

			@Override
			public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
				ResolvedModel resolvedModel = context.blockModelBaker().getModel(model);
				if (resolvedModel.wrapped() instanceof SimpleMaterialModel simpleMaterialModel) {
					TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
					ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(context.blockModelBaker(), resolvedModel, textureSlots);
					return new SimpleMaterialItemModel(
							simpleMaterialModel.bakeBlockStateModel(context.blockModelBaker(), resolvedModel, BlockModelRotation.IDENTITY), properties,
							Transformation.compose(transformation, Optional.empty()));
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
		public static final Identifier ID = SophisticatedStorage.getIdentifier("simple_material_blockstate_model_loader");

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
