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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.QuadTransformer;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.common.model.TransformationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageTier;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.*;

public class BarrelDynamicModel implements IModelGeometry<BarrelDynamicModel> {
	private final Map<String, Map<ModelPart, UnbakedModel>> woodModels;

	private static final BarrelDynamicModel INSTANCE;

	private static final Map<String, Map<ModelPart, Map<String, Material>>> WOOD_TEXTURES = new HashMap<>();
	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> {
			String woodName = woodType.name();
			ImmutableMap.Builder<ModelPart, Map<String, Material>> modelPartTexturesBuilder = ImmutableMap.builder();
			for (ModelPart modelPart : ModelPart.values()) {
				Map<String, Material> textures = new HashMap<>();
				for (StorageTextureManager.BarrelMaterial barrelMaterial : modelPart.barrelMaterials) {
					addBarrelMaterial(woodType, textures, barrelMaterial, StorageTextureManager.BarrelFace.TOP);
					addBarrelMaterial(woodType, textures, barrelMaterial, StorageTextureManager.BarrelFace.BOTTOM);
					addBarrelMaterial(woodType, textures, barrelMaterial, StorageTextureManager.BarrelFace.SIDE);

					if (modelPart == ModelPart.MAIN || modelPart == ModelPart.MAIN_OPEN) {
						StorageTextureManager.INSTANCE.getBarrelMaterial(woodType, StorageTextureManager.BarrelFace.TOP, StorageTextureManager.BarrelMaterial.HANDLE).ifPresent(mat -> textures.put("handle", mat));
					}
				}
				modelPartTexturesBuilder.put(modelPart, textures);

			}
			WOOD_TEXTURES.put(woodName, modelPartTexturesBuilder.build());
		});

		ImmutableMap.Builder<String, Map<ModelPart, UnbakedModel>> woodModelsBuilder = ImmutableMap.builder();

		WOOD_TEXTURES.forEach((woodName, modelPartMaterials) -> {
			ImmutableMap.Builder<ModelPart, UnbakedModel> modelsBuilder = ImmutableMap.builder();
			modelPartMaterials.forEach((modelPart, materials) -> {
				ImmutableMap.Builder<String, Either<Material, String>> texturesBuilder = ImmutableMap.builder();

				materials.forEach((textureName, material) -> texturesBuilder.put(textureName, Either.left(material)));

				modelsBuilder.put(modelPart, new BlockModel(modelPart.modelName, Collections.emptyList(), texturesBuilder.build(), true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));
			});
			woodModelsBuilder.put(woodName, modelsBuilder.build());
		});

		INSTANCE = new BarrelDynamicModel(woodModelsBuilder.build());
	}

	private static void addBarrelMaterial(WoodType woodType, Map<String, Material> textures, StorageTextureManager.BarrelMaterial barrelMaterial, StorageTextureManager.BarrelFace barrelFace) {
		StorageTextureManager.INSTANCE.getBarrelMaterial(woodType, barrelFace, barrelMaterial).ifPresent(mat -> textures.put(barrelFace.name().toLowerCase(Locale.ROOT), mat));
	}

	public static Collection<Map<ModelPart, Map<String, Material>>> getWoodTextures() {
		return WOOD_TEXTURES.values();
	}

	public BarrelDynamicModel(Map<String, Map<ModelPart, UnbakedModel>> woodModels) {
		this.woodModels = woodModels;
	}

	@Override
	public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		ImmutableMap.Builder<String, Map<ModelPart, BakedModel>> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, partModels) -> {
			ImmutableMap.Builder<ModelPart, BakedModel> partBuilder = ImmutableMap.builder();
			partModels.forEach((part, model) -> {
				BakedModel bakedModel = model.bake(bakery, spriteGetter, modelTransform, modelLocation);
				if (bakedModel != null) {
					partBuilder.put(part, bakedModel);
				}
			});
			builder.put(woodName, partBuilder.build());
		});

		return new BarrelBakedModel(builder.build());
	}

	@Override
	public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		ImmutableSet.Builder<Material> builder = ImmutableSet.builder();
		woodModels.forEach((woodName, partModels) -> partModels.forEach((part, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors))));
		return builder.build();
	}

	private static class BarrelBakedModel implements IDynamicBakedModel {
		private static final QuadTransformer MOVE_TO_CORNER = new QuadTransformer(new Transformation(new Vector3f(-.5f, -.5f, -.5f), null, null, null));
		private static final Map<Direction, QuadTransformer> DIRECTION_ROTATES = Map.of(
				Direction.UP, getDirectionRotationTransform(Direction.UP),
				Direction.DOWN, getDirectionRotationTransform(Direction.DOWN),
				Direction.NORTH, getDirectionRotationTransform(Direction.NORTH),
				Direction.SOUTH, getDirectionRotationTransform(Direction.SOUTH),
				Direction.WEST, getDirectionRotationTransform(Direction.WEST),
				Direction.EAST, getDirectionRotationTransform(Direction.EAST)
		);

		private static final LoadingCache<Direction, Cache<Integer, QuadTransformer>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
			@Override
			public Cache<Integer, QuadTransformer> load(Direction key) {
				return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
			}
		});
		private static final QuadTransformer SCALE_BIG_2D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE), null));
		private static final QuadTransformer SCALE_SMALL_3D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE), null));
		private static final QuadTransformer SCALE_SMALL_2D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE), null));
		private StorageTier barrelStorageTier;

		private static QuadTransformer getDirectionRotationTransform(Direction dir) {
			return new QuadTransformer(new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null));
		}

		private static final Cache<Integer, QuadTransformer> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();

		private static QuadTransformer getDirectionMoveBackToSide(Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
			int hash = calculateMoveBackToSideHash(dir, distFromCenter, displayItemIndex, displayItemCount);
			QuadTransformer transform = DIRECTION_MOVE_BACK_TO_SIDE.getIfPresent(hash);
			if (transform == null) {
				Vec3i normal = dir.getNormal();
				Vector3f offset = new Vector3f(distFromCenter, distFromCenter, distFromCenter);
				offset.mul(normal.getX(), normal.getY(), normal.getZ());
				Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount, dir);
				offset.add(frontOffset);
				transform = new QuadTransformer(new Transformation(offset, null, null, null));

				DIRECTION_MOVE_BACK_TO_SIDE.put(hash, transform);
			}
			return transform;
		}

		private static int calculateMoveBackToSideHash(Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
			int hash = dir.hashCode();
			hash = 31 * hash + Float.hashCode(distFromCenter);
			hash = 31 * hash + displayItemIndex;
			hash = 31 * hash + displayItemCount;
			return hash;
		}

		private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
		private static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
		private static final ModelProperty<List<RenderInfo.DisplayItem>> DISPLAY_ITEMS = new ModelProperty<>();
		private static final Map<ItemTransforms.TransformType, Transformation> TRANSFORMS;
		private static final ItemTransforms ITEM_TRANSFORMS;

		private final Map<String, Map<ModelPart, BakedModel>> woodModelParts;
		private final ItemOverrides barrelItemOverrides = new BarrelItemOverrides(this);

		private Item barrelItem = Items.AIR;
		@Nullable
		private String barrelWoodName = null;
		private boolean barrelHasMainColor = false;
		private boolean barrelHasAccentColor = false;
		private boolean barrelIsPacked = false;

		private static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();

		public BarrelBakedModel(Map<String, Map<ModelPart, BakedModel>> woodModelParts) {
			this.woodModelParts = woodModelParts;
		}

		static {
			ImmutableMap.Builder<ItemTransforms.TransformType, Transformation> builder = ImmutableMap.builder();
			builder.put(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, new Transformation(
					new Vector3f(0, 2.5f / 16f, 0),
					new Quaternion(75, 45, 0, true),
					new Vector3f(0.375f, 0.375f, 0.375f), null
			));
			builder.put(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, new Transformation(
					new Vector3f(0, 2.5f / 16f, 0),
					new Quaternion(75, 45, 0, true),
					new Vector3f(0.375f, 0.375f, 0.375f), null
			));
			builder.put(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, new Transformation(
					new Vector3f(0, 0, 0),
					new Quaternion(0, 225, 0, true),
					new Vector3f(0.4f, 0.4f, 0.4f), null
			));
			builder.put(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, new Transformation(
					new Vector3f(0, 0, 0),
					new Quaternion(0, 45, 0, true),
					new Vector3f(0.4f, 0.4f, 0.4f), null
			));
			builder.put(ItemTransforms.TransformType.HEAD, new Transformation(
					new Vector3f(0, 14.25f / 16f, 0),
					new Quaternion(0, 0, 0, true),
					new Vector3f(1, 1, 1), null
			));
			builder.put(ItemTransforms.TransformType.GUI, new Transformation(
					new Vector3f(0, 0, 0),
					new Quaternion(30, 225, 0, true),
					new Vector3f(0.625f, 0.625f, 0.625f), null
			));
			builder.put(ItemTransforms.TransformType.GROUND, new Transformation(
					new Vector3f(0, 3 / 16f, 0),
					new Quaternion(0, 0, 0, true),
					new Vector3f(0.25f, 0.25f, 0.25f), null
			));
			builder.put(ItemTransforms.TransformType.FIXED, new Transformation(
					new Vector3f(0, 0, 0),
					new Quaternion(0, 0, 0, true),
					new Vector3f(0.5f, 0.5f, 0.5f), null
			));
			TRANSFORMS = builder.build();

			ITEM_TRANSFORMS = createItemTransforms();
		}

		@SuppressWarnings("deprecation")
		private static ItemTransforms createItemTransforms() {
			return new ItemTransforms(fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)),
					fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND)),
					fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.HEAD)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.GUI)),
					fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.GROUND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIXED)));
		}

		@SuppressWarnings("deprecation")
		private static ItemTransform fromTransformation(Transformation transformation) {
			return new ItemTransform(transformation.getLeftRotation().toXYZ(), transformation.getTranslation(), transformation.getScale());
		}

		@Nonnull
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extraData) {
			int hash = createHash(state, side, extraData);
			List<BakedQuad> quads = BAKED_QUADS_CACHE.getIfPresent(hash);
			if (quads != null) {
				return quads;
			}

			String woodName = null;
			boolean hasMainColor;
			boolean hasAccentColor;
			boolean isPacked;
			StorageTier storageTier;
			if (state != null) {
				hasMainColor = Boolean.TRUE.equals(extraData.getData(HAS_MAIN_COLOR));
				hasAccentColor = Boolean.TRUE.equals(extraData.getData(HAS_ACCENT_COLOR));
				if (extraData.hasProperty(WOOD_NAME)) {
					woodName = extraData.getData(WOOD_NAME);
				}
				isPacked = isPacked(extraData);
				storageTier = getStorageTier(state);
			} else {
				woodName = barrelWoodName;
				hasMainColor = barrelHasMainColor;
				hasAccentColor = barrelHasAccentColor;
				isPacked = barrelIsPacked;
				storageTier = barrelStorageTier;
			}

			List<BakedQuad> ret = new ArrayList<>();

			Map<ModelPart, BakedModel> modelParts = getWoodModelParts(woodName);
			if (modelParts.isEmpty()) {
				return Collections.emptyList();
			}

			if (!hasMainColor || !hasAccentColor) {
				addPartQuads(state, side, rand, ret, modelParts, state != null && Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? ModelPart.BASE_OPEN : ModelPart.BASE);
			}

			addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor, modelParts);
			addTierQuads(state, side, rand, storageTier, ret, modelParts);

			if (isPacked) {
				addPartQuads(state, side, rand, ret, modelParts, ModelPart.PACKED);
			} else {
				addDisplayItemQuads(state, side, rand, ret, extraData);
			}

			BAKED_QUADS_CACHE.put(hash, ret);

			return ret;
		}

		private boolean isPacked(IModelData extraData) {
			return extraData.hasProperty(IS_PACKED) && Boolean.TRUE.equals(extraData.getData(IS_PACKED));
		}

		@Nonnull
		private StorageTier getStorageTier(@Nonnull BlockState state) {
			return state.getBlock() instanceof BarrelBlock barrelBlock ? barrelBlock.getStorageTier() : StorageTier.WOOD;
		}

		private void addTierQuads(
				@Nullable BlockState state,
				@Nullable Direction side, Random rand, StorageTier storageTier, List<BakedQuad> ret, Map<ModelPart, BakedModel> modelParts) {
			ModelPart tierModelPart =
					switch (storageTier) {
						case WOOD -> ModelPart.WOOD_TIER;
						case IRON -> ModelPart.IRON_TIER;
						case GOLD -> ModelPart.GOLD_TIER;
						case DIAMOND -> ModelPart.DIAMOND_TIER;
						case NETHERITE -> ModelPart.NETHERITE_TIER;
					};

			addPartQuads(state, side, rand, ret, modelParts, tierModelPart);
		}

		private int createHash(@Nullable BlockState state, @Nullable Direction side, IModelData data) {
			int hash;
			if (state != null) {
				hash = getInWorldBlockHash(state, data);
			} else {
				hash = getItemBlockHash();
			}
			hash = hash * 31 + (side == null ? 0 : side.get3DDataValue() + 1);
			hash = getDisplayItemsHash(data, hash);
			return hash;
		}

		private int getItemBlockHash() {
			int hash = barrelItem.hashCode();
			hash = hash * 31 + (barrelWoodName != null ? barrelWoodName.hashCode() + 1 : 0);
			hash = hash * 31 + (barrelHasMainColor ? 1 : 0);
			hash = hash * 31 + (barrelHasAccentColor ? 1 : 0);
			hash = hash * 31 + (barrelIsPacked ? 1 : 0);
			return hash;
		}

		private int getInWorldBlockHash(BlockState state, IModelData data) {
			int hash = state.getBlock().hashCode();
			hash = hash * 31 + state.getValue(BarrelBlock.FACING).get3DDataValue();
			hash = hash * 31 + (Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? 1 : 0);

			//noinspection ConstantConditions
			hash = hash * 31 + (data.hasProperty(WOOD_NAME) ? data.getData(WOOD_NAME).hashCode() + 1 : 0);
			hash = hash * 31 + (data.hasProperty(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.getData(HAS_MAIN_COLOR)) ? 1 : 0);
			hash = hash * 31 + (data.hasProperty(HAS_ACCENT_COLOR) && Boolean.TRUE.equals(data.getData(HAS_ACCENT_COLOR)) ? 1 : 0);
			hash = hash * 31 + (isPacked(data) ? 1 : 0);
			return hash;
		}

		private int getDisplayItemsHash(IModelData data, int hash) {
			if (data.hasProperty(DISPLAY_ITEMS)) {
				List<RenderInfo.DisplayItem> displayItems = data.getData(DISPLAY_ITEMS);
				//noinspection ConstantConditions
				for (RenderInfo.DisplayItem displayItem : displayItems) {
					hash = hash * 31 + getDisplayItemHash(displayItem);
				}
			}
			return hash;
		}

		private int getDisplayItemHash(RenderInfo.DisplayItem displayItem) {
			int hash = displayItem.getRotation();
			hash = hash * 31 + ItemStackKey.getHashCode(displayItem.getItem());
			return hash;
		}

		private void addDisplayItemQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, List<BakedQuad> ret, IModelData data) {
			if (state == null || side != null) {
				return;
			}

			List<RenderInfo.DisplayItem> displayItems = data.getData(DISPLAY_ITEMS);

			if (displayItems != null && !displayItems.isEmpty()) {
				Minecraft minecraft = Minecraft.getInstance();
				ItemRenderer itemRenderer = minecraft.getItemRenderer();
				int index = 0;
				for (RenderInfo.DisplayItem displayItem : displayItems) {
					ItemStack item = displayItem.getItem();
					BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
					if (!model.isCustomRenderer()) {
						int rotation = displayItem.getRotation();
						for (Direction face : Direction.values()) {
							addRenderedItemSide(state, rand, ret, item, model, rotation, face, index, displayItems.size());
						}
						addRenderedItemSide(state, rand, ret, item, model, rotation, null, index, displayItems.size());
					}
					index++;
				}
			}
		}

		@SuppressWarnings("deprecation")
		private void addRenderedItemSide(BlockState state, Random rand, List<BakedQuad> ret, ItemStack displayItem, BakedModel model, int rotation,
				@Nullable Direction dir, int displayItemIndex, int displayItemCount) {
			List<BakedQuad> quads = model.getQuads(null, dir, rand);
			quads = MOVE_TO_CORNER.processMany(quads);
			quads = new QuadTransformer(TransformationHelper.toTransformation(model.getTransforms().getTransform(ItemTransforms.TransformType.FIXED))).processMany(quads);
			if (!model.isGui3d()) {
				if (displayItemCount == 1) {
					quads = SCALE_BIG_2D_ITEM.processMany(quads);
				} else {
					quads = SCALE_SMALL_2D_ITEM.processMany(quads);
				}
			} else if (displayItemCount > 1) {
				quads = SCALE_SMALL_3D_ITEM.processMany(quads);
			}

			if (rotation != 0) {
				quads = getDisplayRotation(rotation).processMany(quads);
			}

			Direction facing = state.getValue(BarrelBlock.FACING);
			quads = DIRECTION_ROTATES.get(facing).processMany(quads);

			if (model.isGui3d()) {
				QuadTransformer transformer = getDirectionMove(displayItem, model, facing, displayItemIndex, displayItemCount, displayItemCount == 1 ? 1 : SMALL_3D_ITEM_SCALE);
				quads = transformer.processMany(quads);
				recalculateDirections(quads);
			} else {
				quads = getDirectionMove(displayItem, model, facing, displayItemIndex, displayItemCount, 1).processMany(quads);
			}

			updateTintIndexes(quads, displayItemIndex);

			ret.addAll(quads);
		}

		private void updateTintIndexes(List<BakedQuad> quads, int displayItemIndex) {
			int offset = (displayItemIndex + 1) * 10;
			quads.forEach(quad -> {
				if (quad.tintIndex >= 0) {
					quad.tintIndex = quad.tintIndex + offset;
				}
			});
		}

		private void recalculateDirections(List<BakedQuad> quads) {
			quads.forEach(quad -> quad.direction = FaceBakery.calculateFacing(quad.getVertices()));
		}

		private QuadTransformer getDirectionMove(ItemStack displayItem, BakedModel model, Direction direction, int displayItemIndex, int displayItemCount, float itemScale) {
			int hash = calculateDirectionMoveHash(displayItem, displayItemIndex, displayItemCount);
			Cache<Integer, QuadTransformer> directionCache = DIRECTION_MOVES_3D_ITEMS.getUnchecked(direction);
			QuadTransformer transformer = directionCache.getIfPresent(hash);

			if (transformer == null) {
				double offset = 0;
				if (model.isGui3d()) {
					offset = DisplayItemRenderer.getDisplayItemOffset(displayItem, model, itemScale);
				}
				transformer = getDirectionMoveBackToSide(direction, (float) (0.5f + offset), displayItemIndex, displayItemCount);
				directionCache.put(hash, transformer);
			}

			return transformer;
		}

		private int calculateDirectionMoveHash(ItemStack displayItem, int displayItemIndex, int displayItemCount) {
			int hashCode = ItemStackKey.getHashCode(displayItem);
			hashCode = hashCode * 31 + displayItemIndex;
			hashCode = hashCode * 31 + displayItemCount;
			return hashCode;
		}

		private static final Map<Integer, QuadTransformer> DISPLAY_ROTATIONS = new HashMap<>();

		private QuadTransformer getDisplayRotation(int rotation) {
			return DISPLAY_ROTATIONS.computeIfAbsent(rotation, r -> new QuadTransformer(new Transformation(null, Vector3f.ZP.rotationDegrees(rotation), null, null)));
		}

		private void addTintableModelQuads(
				@Nullable BlockState state, @Nullable
		Direction side, Random rand, List<BakedQuad> ret, boolean hasMainColor, boolean hasAccentColor, Map<ModelPart, BakedModel> modelParts) {
			if (hasMainColor) {
				addPartQuads(state, side, rand, ret, modelParts, state != null && state.getValue(BarrelBlock.OPEN) ? ModelPart.MAIN_OPEN : ModelPart.MAIN);
			}
			if (hasAccentColor) {
				addPartQuads(state, side, rand, ret, modelParts, ModelPart.ACCENT);
			}

			if (hasMainColor || hasAccentColor) {
				addPartQuads(state, side, rand, ret, modelParts, ModelPart.METAL_BANDS);
			}
		}

		private void addPartQuads(
				@Nullable BlockState state, @Nullable Direction side, Random rand, List<BakedQuad> ret, Map<ModelPart, BakedModel> modelParts, ModelPart part) {
			if (modelParts.containsKey(part)) {
				ret.addAll(modelParts.get(part).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			}
		}

		private Map<ModelPart, BakedModel> getWoodModelParts(@Nullable String barrelWoodName) {
			if (woodModelParts.containsKey(barrelWoodName)) {
				return woodModelParts.get(barrelWoodName);
			} else {
				return woodModelParts.values().iterator().next();
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
			return getWoodModelParts(null).get(ModelPart.BASE).getParticleIcon();
		}

		@SuppressWarnings("deprecation")
		@Override
		public ItemTransforms getTransforms() {
			return ITEM_TRANSFORMS;
		}

		@Override
		public TextureAtlasSprite getParticleIcon(IModelData data) {
			if (data.hasProperty(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.getData(HAS_MAIN_COLOR))) {
				return getWoodModelParts(null).get(ModelPart.MAIN).getParticleIcon(data);
			} else if (data.hasProperty(WOOD_NAME)) {
				String name = data.getData(WOOD_NAME);
				if (!woodModelParts.containsKey(name)) {
					return getParticleIcon();
				}
				return getWoodModelParts(name).get(ModelPart.BASE).getParticleIcon(data);
			}
			return getParticleIcon();
		}

		@Nonnull
		@Override
		public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileData) {
			return WorldHelper.getBlockEntity(world, pos, BarrelBlockEntity.class)
					.map(be -> {
						ModelDataMap.Builder builder = new ModelDataMap.Builder();
						boolean hasMainColor = be.getStorageWrapper().hasMainColor();
						builder.withInitial(HAS_MAIN_COLOR, hasMainColor);
						boolean hasAccentColor = be.getStorageWrapper().hasAccentColor();
						builder.withInitial(HAS_ACCENT_COLOR, hasAccentColor);
						if (!be.hasFullyDynamicRenderer()) {
							List<RenderInfo.DisplayItem> displayItems = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems();
							if (!displayItems.isEmpty()) {
								builder.withInitial(DISPLAY_ITEMS, displayItems);
							}
						}
						builder.withInitial(IS_PACKED, be.isPacked());
						Optional<WoodType> woodType = be.getWoodType();
						if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
							builder.withInitial(WOOD_NAME, woodType.orElse(WoodType.ACACIA).name());
						}
						return (IModelData) builder.build();
					}).orElse(EmptyModelData.INSTANCE);
		}

		@Override
		public ItemOverrides getOverrides() {
			return barrelItemOverrides;
		}

		@Override
		public BakedModel handlePerspective(ItemTransforms.TransformType cameraTransformType, PoseStack matrixStack) {
			if (cameraTransformType == ItemTransforms.TransformType.NONE) {
				return this;
			}

			Transformation tr = TRANSFORMS.get(cameraTransformType);

			if (!tr.isIdentity()) {
				tr.push(matrixStack);
			}
			return this;
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
			barrelBakedModel.barrelStorageTier = stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock barrelBlock ? barrelBlock.getStorageTier() : StorageTier.WOOD;
			barrelBakedModel.barrelItem = stack.getItem();
			return barrelBakedModel;
		}
	}

	public static final class Loader implements IModelLoader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public BarrelDynamicModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
			return BarrelDynamicModel.INSTANCE;
		}

		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			//noop
		}
	}

	public enum ModelPart {
		BASE(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.BASE),
		BASE_OPEN(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.BASE, StorageTextureManager.BarrelMaterial.BASE_OPEN),
		METAL_BANDS(SophisticatedStorage.getRL("block/barrel_metal_bands"), StorageTextureManager.BarrelMaterial.METAL_BANDS),
		ACCENT(SophisticatedStorage.getRL("block/barrel_tintable_accent"), StorageTextureManager.BarrelMaterial.TINTABLE_ACCENT),
		MAIN(SophisticatedStorage.getRL("block/barrel_tintable_main"), StorageTextureManager.BarrelMaterial.TINTABLE_MAIN),
		MAIN_OPEN(SophisticatedStorage.getRL("block/barrel_tintable_main_open"), StorageTextureManager.BarrelMaterial.TINTABLE_MAIN_OPEN),
		WOOD_TIER(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.WOOD_TIER),
		IRON_TIER(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.IRON_TIER),
		GOLD_TIER(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.GOLD_TIER),
		DIAMOND_TIER(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.DIAMOND_TIER),
		NETHERITE_TIER(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.NETHERITE_TIER),
		PACKED(Constants.CUBE_BOTTOM_TOP, StorageTextureManager.BarrelMaterial.PACKED);

		public final ResourceLocation modelName;
		private final StorageTextureManager.BarrelMaterial[] barrelMaterials;

		ModelPart(ResourceLocation modelName, StorageTextureManager.BarrelMaterial... barrelMaterials) {
			this.modelName = modelName;
			this.barrelMaterials = barrelMaterials;
		}

		private static class Constants {
			private static final ResourceLocation CUBE_BOTTOM_TOP = new ResourceLocation("minecraft:block/cube_bottom_top");
		}
	}
}
