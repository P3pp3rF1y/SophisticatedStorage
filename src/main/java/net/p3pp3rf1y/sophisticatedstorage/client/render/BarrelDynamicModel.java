package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BarrelDynamicModel implements IUnbakedGeometry<BarrelDynamicModel> {
	private static final Map<String, Map<String, ResourceLocation>> WOOD_TEXTURES = new HashMap<>();

	private static final String BLOCK_FOLDER = "block/";

	private static final String TOP_OPEN_TEXTURE_NAME = "top_open";

	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> {
			String woodName = woodType.name();
			Map<String, ResourceLocation> barrelTextures = new HashMap<>();
			barrelTextures.put("top", SophisticatedStorage.getRL(BLOCK_FOLDER + woodName + "_barrel_top"));
			barrelTextures.put(TOP_OPEN_TEXTURE_NAME, SophisticatedStorage.getRL(BLOCK_FOLDER + woodName + "_barrel_top_open"));
			barrelTextures.put("bottom", SophisticatedStorage.getRL(BLOCK_FOLDER + woodName + "_barrel_bottom"));
			barrelTextures.put("side", SophisticatedStorage.getRL(BLOCK_FOLDER + woodName + "_barrel_side"));

			WOOD_TEXTURES.put(woodName, barrelTextures);
		});
	}

	private final Map<String, UnbakedModel> woodModels;
	private final Map<ModelPart, UnbakedModel> additionalModelParts;

	public static Collection<Map<String, ResourceLocation>> getWoodTextures() {
		return WOOD_TEXTURES.values();
	}

	public BarrelDynamicModel(Map<String, UnbakedModel> woodModels, Map<ModelPart, UnbakedModel> additionalModelParts) {
		this.woodModels = woodModels;
		this.additionalModelParts = additionalModelParts;
	}

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		ImmutableMap.Builder<String, BakedModel> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, model) -> {
			BakedModel bakedModel = model.bake(bakery, spriteGetter, modelTransform, modelLocation);
			if (bakedModel != null) {
				builder.put(woodName, bakedModel);
			}
		});
		ImmutableMap.Builder<ModelPart, BakedModel> additionalModelPartsBuilder = ImmutableMap.builder();
		additionalModelParts.forEach((part, model) -> {
			BakedModel bakedModel = model.bake(bakery, spriteGetter, modelTransform, modelLocation);
			if (bakedModel != null) {
				additionalModelPartsBuilder.put(part, bakedModel);
			}
		});
		return new BarrelBakedModel(builder.build(), additionalModelPartsBuilder.build());
	}

	@Override
	public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		ImmutableSet.Builder<Material> builder = ImmutableSet.builder();
		woodModels.forEach((woodName, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors)));
		additionalModelParts.forEach((modelPart, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors)));
		return builder.build();
	}

	private static class BarrelBakedModel implements IDynamicBakedModel {
		private static final IQuadTransformer MOVE_TO_CORNER = QuadTransformers.applying(new Transformation(new Vector3f(-.5f, -.5f, -.5f), null, null, null));
		private static final IQuadTransformer FLIP_AND_SCALE = QuadTransformers.applying(new Transformation(null, Vector3f.YN.rotationDegrees(180), new Vector3f(0.65f, 0.65f, 0.65f), null));
		private static final IQuadTransformer ROTATE_90_DEGREES = QuadTransformers.applying(new Transformation(null, Vector3f.XN.rotationDegrees(90), null, null));
		private static final IQuadTransformer ROTATE_270_DEGREES = QuadTransformers.applying(new Transformation(null, Vector3f.XN.rotationDegrees(270), null, null));
		private static final Map<Direction, IQuadTransformer> DIRECTION_ROTATES = Map.of(
				Direction.UP, getDirectionRotationTransform(Direction.UP),
				Direction.DOWN, getDirectionRotationTransform(Direction.DOWN),
				Direction.NORTH, getDirectionRotationTransform(Direction.NORTH),
				Direction.SOUTH, getDirectionRotationTransform(Direction.SOUTH),
				Direction.WEST, getDirectionRotationTransform(Direction.WEST),
				Direction.EAST, getDirectionRotationTransform(Direction.EAST)
		);
		private static final Map<Direction, IQuadTransformer> DIRECTION_MOVES = Map.of(
				Direction.UP, getDirectionMoveBackToSide(Direction.UP, 0.5f),
				Direction.DOWN, getDirectionMoveBackToSide(Direction.DOWN, 0.5f),
				Direction.NORTH, getDirectionMoveBackToSide(Direction.NORTH, 0.5f),
				Direction.SOUTH, getDirectionMoveBackToSide(Direction.SOUTH, 0.5f),
				Direction.WEST, getDirectionMoveBackToSide(Direction.WEST, 0.5f),
				Direction.EAST, getDirectionMoveBackToSide(Direction.EAST, 0.5f)
		);
		private static final LoadingCache<Direction, Cache<Integer, IQuadTransformer>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build(new CacheLoader<>() {
			@Override
			public Cache<Integer, IQuadTransformer> load(Direction key) {
				return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
			}
		});

		private static IQuadTransformer getDirectionRotationTransform(Direction dir) {
			Quaternion rotation = dir.getRotation();
			if (dir.getAxis().isVertical()) {
				rotation.mul(Vector3f.YP.rotationDegrees(180.0F));
			}
			return QuadTransformers.applying(new Transformation(null, rotation, null, null));
		}

		private static IQuadTransformer getDirectionMoveBackToSide(Direction dir, float distFromCenter) {
			Vec3i normal = dir.getNormal();
			Vector3f offset = new Vector3f(distFromCenter, distFromCenter, distFromCenter);
			offset.mul(normal.getX(), normal.getY(), normal.getZ());
			offset.add(.5f, .5f, .5f);
			return QuadTransformers.applying(new Transformation(offset, null, null, null));
		}

		private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
		private static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
		private static final ModelProperty<ItemStack> DISPLAY_ITEM = new ModelProperty<>();
		private static final ModelProperty<Integer> DISPLAY_ITEM_ROTATION = new ModelProperty<>();
		private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
		private static final ItemTransforms ITEM_TRANSFORMS = createItemTransforms();

		@SuppressWarnings("java:S4738") //ItemTransforms require Guava ImmutableMap to be passed in so no way to change that to java Map
		private static ItemTransforms createItemTransforms() {
			return new ItemTransforms(
					new ItemTransform(
							new Vector3f(75, 45, 0),
							new Vector3f(0, 2.5f / 16f, 0),
							new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(75, 45, 0),
							new Vector3f(0, 2.5f / 16f, 0),
							new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(0, 225, 0),
							new Vector3f(0, 0, 0),
							new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(0, 45, 0),
							new Vector3f(0, 0, 0),
							new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(0, 0, 0),
							new Vector3f(0, 14.25f / 16f, 0),
							new Vector3f(1, 1, 1), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(30, 225, 0),
							new Vector3f(0, 0, 0),
							new Vector3f(0.625f, 0.625f, 0.625f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(0, 0, 0),
							new Vector3f(0, 3 / 16f, 0),
							new Vector3f(0.25f, 0.25f, 0.25f), DEFAULT_ROTATION
					),
					new ItemTransform(
							new Vector3f(0, 0, 0),
							new Vector3f(0, 0, 0),
							new Vector3f(0.5f, 0.5f, 0.5f), DEFAULT_ROTATION
					), ImmutableMap.of());
		}
		private final Map<String, BakedModel> woodModels;
		private final Map<ModelPart, BakedModel> additionalModelParts;
		private final ItemOverrides barrelItemOverrides = new BarrelItemOverrides(this);
		@Nullable
		private String barrelWoodName = null;
		private boolean barrelHasMainColor = false;
		private boolean barrelHasAccentColor = false;

		private boolean barrelIsPacked = false;

		public BarrelBakedModel(Map<String, BakedModel> woodModels, Map<ModelPart, BakedModel> additionalModelParts) {
			this.woodModels = woodModels;
			this.additionalModelParts = additionalModelParts;
		}

		@Nonnull
		@Override
		public List<BakedQuad> getQuads(
				@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
			String woodName = null;
			boolean hasMainColor;
			boolean hasAccentColor;
			boolean isPacked;
			if (state != null) {
				hasMainColor = Boolean.TRUE.equals(extraData.get(HAS_MAIN_COLOR));
				hasAccentColor = Boolean.TRUE.equals(extraData.get(HAS_ACCENT_COLOR));
				if (extraData.has(WOOD_NAME)) {
					woodName = extraData.get(WOOD_NAME);
					if (Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN))) {
						woodName += "_open";
					}
				}
				isPacked = extraData.has(IS_PACKED) && Boolean.TRUE.equals(extraData.get(IS_PACKED));
			} else {
				woodName = barrelWoodName;
				hasMainColor = barrelHasMainColor;
				hasAccentColor = barrelHasAccentColor;
				isPacked = barrelIsPacked;
			}

			List<BakedQuad> ret = new ArrayList<>();

			if (!hasMainColor || !hasAccentColor) {
				addWoodModelQuads(state, side, rand, ret, woodName, renderType);
			}

			addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor, renderType);

			ret.addAll(additionalModelParts.get(ModelPart.TIER).getQuads(state, side, rand, ModelData.EMPTY, renderType));

			if (isPacked) {
				addPackedModelQuads(state, side, rand, ret, renderType);
			} else {
				addDisplayItemQuads(state, side, rand, ret, extraData);
			}
			return ret;
		}

		private void addPackedModelQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret,
				@Nullable RenderType renderType) {
			ret.addAll(additionalModelParts.get(ModelPart.PACKED).getQuads(state, side, rand, ModelData.EMPTY, renderType));
		}

		private void addDisplayItemQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, ModelData extraData) {
			if (state == null || side != null) {
				return;
			}

			ItemStack displayItem = extraData.get(DISPLAY_ITEM);

			if (displayItem != null && !displayItem.isEmpty()) {
				Minecraft minecraft = Minecraft.getInstance();
				ItemRenderer itemRenderer = minecraft.getItemRenderer();
				BakedModel model = itemRenderer.getModel(displayItem, null, minecraft.player, 0);
				ret.clear();
				Integer rotationData = extraData.get(DISPLAY_ITEM_ROTATION);
				int rotation = rotationData == null ? 0 : rotationData;
				for (Direction face : Direction.values()) {
					addRenderedItemSide(state, rand, ret, displayItem, model, rotation, face);
				}
				addRenderedItemSide(state, rand, ret, displayItem, model, rotation, null);
			}
		}

		private Transformation toTransformation(ItemTransform transform) {
			if (transform.equals(ItemTransform.NO_TRANSFORM)) {
				return Transformation.identity();
			}

			return new Transformation(transform.translation, quatFromXYZ(transform.rotation, true), transform.scale, null);
		}

		public Quaternion quatFromXYZ(Vector3f xyz, boolean degrees) {
			return new Quaternion(xyz.x(), xyz.y(), xyz.z(), degrees);
		}

		@SuppressWarnings("deprecation")
		private void addRenderedItemSide(BlockState state, RandomSource rand, List<BakedQuad> ret, ItemStack displayItem, BakedModel model, int rotation,
				@Nullable Direction s) {
			List<BakedQuad> quads = model.getQuads(null, s, rand);
			quads = MOVE_TO_CORNER.process(quads);
			if (!model.isGui3d()) {
				quads = FLIP_AND_SCALE.process(quads);
			}
			quads = QuadTransformers.applying(toTransformation(model.getTransforms().getTransform(ItemTransforms.TransformType.FIXED))).process(quads);
			if (model.isGui3d()) {
				quads = ROTATE_270_DEGREES.process(quads);
			} else {
				quads = ROTATE_90_DEGREES.process(quads);
			}
			if (rotation != 0) {
				quads = getDisplayRotation(rotation).process(quads);
			}
			Direction facing = state.getValue(BarrelBlock.FACING);
			quads = DIRECTION_ROTATES.get(facing).process(quads);

			if (model.isGui3d()) {
				IQuadTransformer transformer = getDirectionMove(displayItem, model, facing);
				quads = transformer.process(quads);
				recalculateDirections(quads);
			} else {
				quads = DIRECTION_MOVES.get(facing).process(quads);
			}

			ret.addAll(quads);
		}

		private void recalculateDirections(List<BakedQuad> quads) {
			quads.forEach(quad -> quad.direction = FaceBakery.calculateFacing(quad.getVertices()));
		}

		private IQuadTransformer getDirectionMove(ItemStack displayItem, BakedModel model, Direction direction) {
			int hash = ItemStackKey.getHashCode(displayItem);
			Cache<Integer, IQuadTransformer> directionCache = DIRECTION_MOVES_3D_ITEMS.getUnchecked(direction);
			IQuadTransformer transformer = directionCache.getIfPresent(hash);
			if (transformer != null) {
				return transformer;
			}
			double offset = DisplayItemRenderer.getDisplayItemOffset(displayItem, model);
			transformer = getDirectionMoveBackToSide(direction, (float) (0.5f + offset));
			directionCache.put(hash, transformer);

			return transformer;
		}

		private static final Map<Integer, IQuadTransformer> DISPLAY_ROTATIONS = new HashMap<>();

		private IQuadTransformer getDisplayRotation(int rotation) {
			return DISPLAY_ROTATIONS.computeIfAbsent(rotation, r -> QuadTransformers.applying(new Transformation(null, Vector3f.YN.rotationDegrees(rotation), null, null)));
		}

		private void addTintableModelQuads(
				@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, boolean hasMainColor, boolean hasAccentColor,
				@Nullable RenderType renderType) {
			if (hasMainColor) {
				ModelPart modePart = state != null && state.getValue(BarrelBlock.OPEN) ? ModelPart.MAIN_OPEN : ModelPart.MAIN;
				ret.addAll(additionalModelParts.get(modePart).getQuads(state, side, rand, ModelData.EMPTY, renderType));
			}
			if (hasAccentColor) {
				ret.addAll(additionalModelParts.get(ModelPart.ACCENT).getQuads(state, side, rand, ModelData.EMPTY, renderType));
			}

			if (hasMainColor || hasAccentColor) {
				ret.addAll(additionalModelParts.get(ModelPart.METAL_BANDS).getQuads(state, side, rand, ModelData.EMPTY, renderType));
			}
		}

		private void addWoodModelQuads(
				@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, @Nullable String woodName,
				@Nullable RenderType renderType) {
			if (woodName == null) {
				return;
			}

			if (woodModels.containsKey(woodName)) {
				ret.addAll(woodModels.get(woodName).getQuads(state, side, rand, ModelData.EMPTY, renderType));
			} else {
				ret.addAll(woodModels.values().iterator().next().getQuads(state, side, rand, ModelData.EMPTY, renderType));
			}
		}

		@Override
		public boolean useAmbientOcclusion() {
			return false; //because occlusion calculation makes display item dark on faces that are exposed to light
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@SuppressWarnings("deprecation")
		@Override
		public TextureAtlasSprite getParticleIcon() {
			return woodModels.values().iterator().next().getParticleIcon();
		}

		@SuppressWarnings("deprecation")
		@Override
		public ItemTransforms getTransforms() {
			return ITEM_TRANSFORMS;
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			if (data.has(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.get(HAS_MAIN_COLOR))) {
				return additionalModelParts.get(ModelPart.MAIN).getParticleIcon(data);
			} else if (data.has(WOOD_NAME)) {
				String name = data.get(WOOD_NAME);
				if (!woodModels.containsKey(name)) {
					return getParticleIcon();
				}
				return woodModels.get(name).getParticleIcon(data);
			}
			return getParticleIcon();
		}

		@Nonnull
		@Override
		public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileData) {
			return WorldHelper.getBlockEntity(world, pos, WoodStorageBlockEntity.class)
					.map(be -> {
						ModelData.Builder builder = ModelData.builder();
						boolean hasMainColor = be.getStorageWrapper().hasMainColor();
						builder.with(HAS_MAIN_COLOR, hasMainColor);
						boolean hasAccentColor = be.getStorageWrapper().hasAccentColor();
						builder.with(HAS_ACCENT_COLOR, hasAccentColor);
						RenderInfo.ItemDisplayRenderInfo itemDisplayRenderInfo = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo();
						builder.with(DISPLAY_ITEM, itemDisplayRenderInfo.getItem());
						builder.with(DISPLAY_ITEM_ROTATION, itemDisplayRenderInfo.getRotation());
						builder.with(IS_PACKED, be.isPacked());
						Optional<WoodType> woodType = be.getWoodType();
						if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
							builder.with(WOOD_NAME, woodType.orElse(WoodType.ACACIA).name());
						}
						return builder.build();
					}).orElse(ModelData.EMPTY);
		}

		@Override
		public ItemOverrides getOverrides() {
			return barrelItemOverrides;
		}

		@Override
		public BakedModel applyTransform(ItemTransforms.TransformType transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
			if (transformType == ItemTransforms.TransformType.NONE) {
				return this;
			}

			ITEM_TRANSFORMS.getTransform(transformType).apply(applyLeftHandTransform, poseStack);

			return this;
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
			return ChunkRenderTypeSet.of(RenderType.cutout());
		}
	}

	private static class BarrelItemOverrides extends ItemOverrides {
		private final BarrelBakedModel barrelBakedModel;

		public BarrelItemOverrides(BarrelBakedModel barrelBakedModel) {
			this.barrelBakedModel = barrelBakedModel;
		}

		@Nullable
		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			barrelBakedModel.barrelHasMainColor = StorageBlockItem.getMainColorFromStack(stack).isPresent();
			barrelBakedModel.barrelHasAccentColor = StorageBlockItem.getAccentColorFromStack(stack).isPresent();
			barrelBakedModel.barrelWoodName = WoodStorageBlockItem.getWoodType(stack).map(WoodType::name)
					.orElse(barrelBakedModel.barrelHasAccentColor && barrelBakedModel.barrelHasMainColor ? null : WoodType.ACACIA.name());
			barrelBakedModel.barrelIsPacked = WoodStorageBlockItem.isPacked(stack);
			return barrelBakedModel;
		}
	}

	public static final class Loader implements IGeometryLoader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public BarrelDynamicModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			ImmutableMap.Builder<String, UnbakedModel> woodModelsBuilder = ImmutableMap.builder();

			WOOD_TEXTURES.forEach((woodName, textures) -> {
				addWoodModels(woodModelsBuilder, woodName, textures, false);
				addWoodModels(woodModelsBuilder, woodName + "_open", textures, true);
			});

			ImmutableMap.Builder<ModelPart, UnbakedModel> additionalModelsBuilder = ImmutableMap.builder();
			for (ModelPart modelPart : ModelPart.values()) {
				Map<String, Either<Material, String>> textureMap = Collections.emptyMap();
				if (modelPart == ModelPart.TIER && modelContents.has("tierTextures")) {
					ImmutableMap.Builder<String, Either<Material, String>> texturesBuilder = ImmutableMap.builder();
					JsonObject texturesJson = modelContents.getAsJsonObject("tierTextures");
					putTierTexture(texturesBuilder, texturesJson, "top");
					putTierTexture(texturesBuilder, texturesJson, "side");
					putTierTexture(texturesBuilder, texturesJson, "bottom");
					textureMap = texturesBuilder.build();
				}

				additionalModelsBuilder.put(modelPart, new BlockModel(modelPart.modelName, Collections.emptyList(), textureMap, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));
			}

			return new BarrelDynamicModel(woodModelsBuilder.build(), additionalModelsBuilder.build());
		}

		private void putTierTexture(ImmutableMap.Builder<String, Either<Material, String>> texturesBuilder, JsonObject texturesJson, String textureName) {
			ResourceLocation texture = ResourceLocation.tryParse(texturesJson.get(textureName).getAsString());
			if (texture != null) {
				texturesBuilder.put(textureName, Either.left(new Material(InventoryMenu.BLOCK_ATLAS, texture)));
			}
		}

		private void addWoodModels(ImmutableMap.Builder<String, UnbakedModel> woodModelsBuilder, String woodName, Map<String, ResourceLocation> textures, boolean open) {
			ImmutableMap.Builder<String, Either<Material, String>> textureMapBuilder = ImmutableMap.builder();
			textures.forEach((textureName, rl) -> {
				if (open && textureName.equals("top") || !open && textureName.equals(TOP_OPEN_TEXTURE_NAME)) {
					return;
				}
				if (open && textureName.equals(TOP_OPEN_TEXTURE_NAME)) {
					textureName = "top";
				}
				textureMapBuilder.put(textureName, Either.left(new Material(InventoryMenu.BLOCK_ATLAS, rl)));
			});
			woodModelsBuilder.put(woodName, new BlockModel(new ResourceLocation("minecraft:block/cube_bottom_top"), Collections.emptyList(), textureMapBuilder.build(), true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));
		}
	}

	private enum ModelPart {
		METAL_BANDS(SophisticatedStorage.getRL("block/barrel_metal_bands")),
		ACCENT(SophisticatedStorage.getRL("block/barrel_tintable_accent")),
		MAIN(SophisticatedStorage.getRL("block/barrel_tintable_main")),
		MAIN_OPEN(SophisticatedStorage.getRL("block/barrel_tintable_main_open")),
		TIER(new ResourceLocation("minecraft:block/cube_bottom_top")),
		PACKED(SophisticatedStorage.getRL("block/barrel_packed"));

		private final ResourceLocation modelName;

		ModelPart(ResourceLocation modelName) {
			this.modelName = modelName;
		}
	}
}
