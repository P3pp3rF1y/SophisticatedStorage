package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.BlockSide;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BarrelBakedModelBase implements IDynamicBakedModel {
	private static final LoadingCache<Direction, Cache<Integer, IQuadTransformer>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder()
			.expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
				@Override
				public Cache<Integer, IQuadTransformer> load(Direction key) {
					return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
				}
			});
	private static final Cache<Integer, IQuadTransformer> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES)
			.build();
	private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
	private static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
	private static final ModelProperty<Boolean> SHOWS_LOCK = new ModelProperty<>();
	private static final ModelProperty<Boolean> SHOWS_TIER = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
	private static final ModelProperty<Map<BarrelMaterial, ResourceLocation>> MATERIALS = new ModelProperty<>();
	private static final ModelProperty<Map<ResourceLocation, Integer>> MATERIAL_TINT_COLORS = new ModelProperty<>();
	public static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final ItemTransforms ITEM_TRANSFORMS = createItemTransforms();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL,
			BarrelMaterial.TOP);

	@SuppressWarnings("java:S4738")
	// ItemTransforms require Guava ImmutableMap to be passed in so no way to change that to java Map
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

	public static void invalidateCache() {
		DIRECTION_MOVES_3D_ITEMS.invalidateAll();
		DIRECTION_MOVE_BACK_TO_SIDE.invalidateAll();
		BAKED_QUADS_CACHE.invalidateAll();
	}

	private final ModelBaker baker;
	protected final Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts;

	private Item barrelItem = Items.AIR;
	@Nullable
	private String woodName = null;
	private boolean hasMainColor = false;
	private boolean hasAccentColor = false;
	private boolean isPacked = false;
	private boolean showsTier = true;
	private Map<BarrelMaterial, ResourceLocation> barrelMaterials = new EnumMap<>(BarrelMaterial.class);

	private boolean flatTop = false;
	private final Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData;
	private final Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts;
	private final Cache<Integer, BakedModel> dynamicBakedModelCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	protected BarrelBakedModelBase(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts,
			Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData,
			Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
		this.baker = baker;
		this.woodModelParts = woodModelParts;
		this.woodDynamicBakingData = woodDynamicBakingData;
		this.woodPartitionedModelParts = woodPartitionedModelParts;
	}

	public void setBarrelItem(Item barrelItem) {
		this.barrelItem = barrelItem;
	}

	public void setWoodName(@Nullable String woodName) {
		this.woodName = woodName;
	}

	public void setHasMainColor(boolean hasMainColor) {
		this.hasMainColor = hasMainColor;
	}

	public void setHasAccentColor(boolean hasAccentColor) {
		this.hasAccentColor = hasAccentColor;
	}

	public void setPacked(boolean packed) {
		isPacked = packed;
	}

	public void setShowsTier(boolean showsTier) {
		this.showsTier = showsTier;
	}

	public void setBarrelMaterials(Map<BarrelMaterial, ResourceLocation> barrelMaterials) {
		this.barrelMaterials = barrelMaterials;
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData,
			@Nullable RenderType renderType) {
		int hash = createHash(state, side, extraData, renderType);
		List<BakedQuad> quads = BAKED_QUADS_CACHE.getIfPresent(hash);
		if (quads != null) {
			return quads;
		}

		String woodName = null;
		boolean hasMainColor;
		boolean hasAccentColor;
		boolean isPacked;
		boolean showsTier;
		Map<BarrelMaterial, ResourceLocation> materials;
		Map<ResourceLocation, Integer> materialTintColors;
		if (state != null) {
			hasMainColor = Boolean.TRUE.equals(extraData.get(HAS_MAIN_COLOR));
			hasAccentColor = Boolean.TRUE.equals(extraData.get(HAS_ACCENT_COLOR));
			if (extraData.has(WOOD_NAME)) {
				woodName = extraData.get(WOOD_NAME);
			}
			isPacked = isPacked(extraData);
			materials = getMaterials(extraData);
			materialTintColors = getMaterialTintColors(extraData);
			showsTier = showsTier(extraData);
		} else {
			woodName = this.woodName;
			hasMainColor = this.hasMainColor;
			hasAccentColor = this.hasAccentColor;
			isPacked = this.isPacked;
			materials = barrelMaterials;
			materialTintColors = getMaterialTintColors(materials, null, null);
			showsTier = this.showsTier;
		}

		List<BakedQuad> ret = new ArrayList<>();

		boolean isBakedDynamically = !materials.isEmpty();
		Set<BarrelMaterial.MaterialModelPart> materialModelParts = materials.keySet().stream().map(BarrelMaterial::getMaterialModelPart)
				.collect(Collectors.toSet());
		boolean rendersUsingSplitModel = materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE)
				|| materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM);

		Map<BarrelModelPart, BakedModel> modelParts = getWoodModelParts(woodName, isBakedDynamically && rendersUsingSplitModel);
		if (modelParts.isEmpty()) {
			return Collections.emptyList();
		}

		if ((!hasMainColor || !hasAccentColor) && !isBakedDynamically) {
			addPartQuads(state, side, rand, ret, modelParts, getBasePart(state), renderType);
		}

		addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor, modelParts, renderType);

		if (isBakedDynamically && shouldRenderDynamicMaterials(state, renderType)) {
			Direction spriteSide = getSpriteSide(state, side);
			Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData = state != null
					? getMaterialSpriteData(spriteSide, rand, materials)
					: Collections.emptyMap();
			bakeAndAddDynamicQuads(spriteSide, rand, woodName, materials, rendersUsingSplitModel,
					!hasMainColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE),
					!hasAccentColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM))
					.forEach(bakedModel -> addDynamicQuads(state, side, rand, ret, renderType, materialSpriteData, materialTintColors, bakedModel));
		}

		if (showsTier) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TIER, renderType);
		}

		if (isPacked) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.PACKED, renderType);
		} else {
			if (showsLocked(extraData)) {
				addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.LOCKED, renderType);
			}
		}

		BAKED_QUADS_CACHE.put(hash, ret);

		return ret;
	}

	public List<BakedQuad> getTierQuads(BlockState state, RandomSource rand, String woodName, RenderType renderType) {
		return getPartQuads(state, rand, woodName, BarrelModelPart.TIER, renderType);
	}

	public List<BakedQuad> getLockQuads(BlockState state, RandomSource rand, String woodName, RenderType renderType) {
		return getPartQuads(state, rand, woodName, BarrelModelPart.LOCKED, renderType);
	}

	private List<BakedQuad> getPartQuads(BlockState state, RandomSource rand, String woodName, BarrelModelPart part, RenderType renderType) {
		List<BakedQuad> ret = new ArrayList<>();

		Map<BarrelModelPart, BakedModel> modelParts = getWoodModelParts(woodName, false);

		for (Direction dir : Direction.values()) {
			addPartQuads(state, dir, rand, ret, modelParts, part, renderType);
		}

		return ret;
	}

	private static Direction getSpriteSide(@Nullable BlockState state, @Nullable Direction side) {
		if (side == null) {
			return Direction.NORTH;
		}
		Direction sideBeforeRotation;
		if (state != null && state.getBlock() instanceof BarrelBlock barrelBlock) {
			sideBeforeRotation = BlockSide.fromDirection(side, barrelBlock.getHorizontalDirection(state), barrelBlock.getVerticalFacing(state))
					.toDirection(Direction.NORTH, VerticalFacing.NO);
		} else {
			sideBeforeRotation = BlockSide.fromDirection(side, Direction.NORTH, VerticalFacing.UP).toDirection(Direction.NORTH, VerticalFacing.NO);
		}
		return sideBeforeRotation;
	}

	private List<BakedModel> bakeAndAddDynamicQuads(@Nullable Direction spriteSide, RandomSource rand, @Nullable String woodName,
			Map<BarrelMaterial, ResourceLocation> barrelMaterials, boolean rendersUsingSplitModel, boolean renderCore, boolean renderTrim) {

		Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData = woodDynamicBakingData
				.get(woodName != null ? woodName : WoodType.ACACIA.name());

		Map<String, Material> materials = new HashMap<>();
		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : barrelMaterials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();
			ResourceLocation blockName = entry.getValue();
			TextureAtlasSprite sprite = RenderHelper.getSprite(blockName, spriteSide, rand);

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				materials.put(childMaterial.getSerializedName(), new Material(TextureAtlas.LOCATION_BLOCKS, sprite.contents().name()));
			}
		}

		List<BakedModel> models = new ArrayList<>();
		if (rendersUsingSplitModel) {
			if (renderCore) {
				models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.CORE));
			}
			if (renderTrim) {
				models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.TRIM));
			}
		} else {
			models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.WHOLE));
		}

		return models;
	}

	private static boolean shouldRenderDynamicMaterials(@Nullable BlockState state, @Nullable RenderType renderType) {
		return renderType == null || renderType == RenderType.cutout() || state != null && renderType == RenderType.translucent();
	}

	private static Map<ResourceLocation, RenderHelper.SpriteData> getMaterialSpriteData(@Nullable Direction spriteSide, RandomSource rand,
			Map<BarrelMaterial, ResourceLocation> barrelMaterials) {
		Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData = new HashMap<>();
		for (ResourceLocation blockName : barrelMaterials.values()) {
			RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(blockName, spriteSide, rand);
			materialSpriteData.put(spriteData.sprite().contents().name(), spriteData);
		}
		return materialSpriteData;
	}

	private static void addDynamicQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret,
			@Nullable RenderType renderType, Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData,
			Map<ResourceLocation, Integer> materialTintColors, BakedModel bakedModel) {
		List<BakedQuad> quads = bakedModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
		for (BakedQuad quad : quads) {
			ResourceLocation spriteName = quad.getSprite().contents().name();
			if (state != null && !shouldRenderDynamicQuad(spriteName, materialSpriteData, renderType)) {
				continue;
			}

			Integer tintColor = materialTintColors.get(spriteName);
			ret.add(tintColor == null ? quad : QuadTransformers.applyingColor(tintColor).process(quad));
		}
	}

	private static boolean shouldRenderDynamicQuad(ResourceLocation spriteName, Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData,
			@Nullable RenderType renderType) {
		if (renderType == null) {
			return true;
		}

		boolean translucent = Optional.ofNullable(materialSpriteData.get(spriteName)).map(RenderHelper.SpriteData::translucent).orElse(false);
		return (renderType == RenderType.translucent()) == translucent;
	}

	private BakedModel getDynamicModel(@Nullable String woodName, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData,
			Map<String, Material> materials, DynamicBarrelBakingData.DynamicPart dynamicPart) {
		int hash = Objects.hash(woodName, materials, dynamicPart.name());
		BakedModel bakedModel = dynamicBakedModelCache.getIfPresent(hash);
		if (bakedModel == null) {
			bakedModel = compileAndBakeModel(materials, bakingData.get(dynamicPart));
			dynamicBakedModelCache.put(hash, bakedModel);
		}
		return bakedModel;
	}

	private static BlockState getDefaultBlockState(ResourceLocation blockName) {
		return BuiltInRegistries.BLOCK.get(blockName).orElseThrow().value().defaultBlockState();
	}

	private Map<BarrelMaterial, ResourceLocation> getMaterials(ModelData extraData) {
		return extraData.has(MATERIALS) ? Objects.requireNonNull(extraData.get(MATERIALS)) : Collections.emptyMap();
	}

	private Map<ResourceLocation, Integer> getMaterialTintColors(ModelData extraData) {
		return extraData.has(MATERIAL_TINT_COLORS) ? Objects.requireNonNull(extraData.get(MATERIAL_TINT_COLORS)) : Collections.emptyMap();
	}

	private BakedModel compileAndBakeModel(Map<String, Material> textures, DynamicBarrelBakingData bakingData) {
		bakingData.baseTextures().forEach((textureName, texture) -> {
			if (!textures.containsKey(textureName)) {
				textures.put(textureName, texture);
			}
		});

		TextureSlots.Data.Builder texturesBuilder = new TextureSlots.Data.Builder();
		textures.forEach(texturesBuilder::addTexture);
		TextureSlots.Resolver resolver = new TextureSlots.Resolver();
		resolver.addLast(texturesBuilder.build());

		return bakingData.baseModel().bake(resolver.resolve(baker.rootName()), baker, bakingData.modelState(), false, true, ItemTransforms.NO_TRANSFORMS,
				ContextMap.EMPTY);
	}

	protected abstract BarrelModelPart getBasePart(@Nullable BlockState state);

	private boolean isPacked(ModelData extraData) {
		return extraData.has(IS_PACKED) && Boolean.TRUE.equals(extraData.get(IS_PACKED));
	}

	private boolean showsLocked(ModelData extraData) {
		return extraData.has(SHOWS_LOCK) && Boolean.TRUE.equals(extraData.get(SHOWS_LOCK));
	}

	private boolean showsTier(ModelData extraData) {
		return extraData.has(SHOWS_TIER) && Boolean.TRUE.equals(extraData.get(SHOWS_TIER));
	}

	private int createHash(@Nullable BlockState state, @Nullable Direction side, ModelData data, @Nullable RenderType renderType) {
		int hash;
		if (state != null) {
			hash = getInWorldBlockHash(state, data, renderType);
		} else {
			hash = getItemBlockHash();
		}
		hash = hash * 31 + (side == null ? 0 : side.get3DDataValue() + 1);
		return hash;
	}

	private int getItemBlockHash() {
		int hash = barrelItem.hashCode();
		hash = hash * 31 + (woodName != null ? woodName.hashCode() + 1 : 0);
		hash = hash * 31 + (hasMainColor ? 1 : 0);
		hash = hash * 31 + (hasAccentColor ? 1 : 0);
		hash = hash * 31 + (isPacked ? 1 : 0);
		hash = hash * 31 + (showsTier ? 1 : 0);
		hash = hash * 31 + (flatTop ? 1 : 0);
		hash = hash * 31 + barrelMaterials.hashCode();
		return hash;
	}

	protected int getInWorldBlockHash(BlockState state, ModelData data, @Nullable RenderType renderType) {
		int hash = state.getBlock().hashCode();

		hash = hash * 31 + (renderType == null ? 0 : renderType.hashCode());
		// noinspection ConstantConditions
		hash = hash * 31 + (data.has(WOOD_NAME) ? data.get(WOOD_NAME).hashCode() + 1 : 0);
		hash = hash * 31 + (data.has(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.get(HAS_MAIN_COLOR)) ? 1 : 0);
		hash = hash * 31 + (data.has(HAS_ACCENT_COLOR) && Boolean.TRUE.equals(data.get(HAS_ACCENT_COLOR)) ? 1 : 0);
		hash = hash * 31 + (isPacked(data) ? 1 : 0);
		hash = hash * 31 + (showsLocked(data) ? 1 : 0);
		hash = hash * 31 + (showsTier(data) ? 1 : 0);
		hash = hash * 31 + (state.getValue(BarrelBlock.FLAT_TOP) ? 1 : 0);
		// noinspection ConstantConditions
		hash = hash * 31 + (data.has(MATERIALS) ? data.get(MATERIALS).hashCode() : 0);
		// noinspection ConstantConditions
		hash = hash * 31 + (data.has(MATERIAL_TINT_COLORS) ? data.get(MATERIAL_TINT_COLORS).hashCode() : 0);
		return hash;
	}

	private void addTintableModelQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, boolean hasMainColor,
			boolean hasAccentColor, Map<BarrelModelPart, BakedModel> modelParts, @Nullable RenderType renderType) {
		if (renderType != null && renderType != RenderType.cutout()) {
			return;
		}

		if (hasAccentColor) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TINTABLE_ACCENT, renderType);
		}

		if (hasMainColor) {
			addPartQuads(state, side, rand, ret, modelParts, getMainPart(state), renderType);
		}
	}

	private BarrelModelPart getMainPart(@Nullable BlockState state) {
		return rendersOpen() && state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.TINTABLE_MAIN_OPEN : BarrelModelPart.TINTABLE_MAIN;
	}

	protected abstract boolean rendersOpen();

	private void addPartQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret,
			Map<BarrelModelPart, BakedModel> modelParts, BarrelModelPart part, @Nullable RenderType renderType) {
		if (renderType != null && renderType != RenderType.cutout()) {
			return;
		}

		if (modelParts.containsKey(part)) {
			ret.addAll(modelParts.getOrDefault(part, Minecraft.getInstance().getModelManager().getMissingModel()).getQuads(state, side, rand, ModelData.EMPTY,
					renderType));
		}
	}

	private Map<BarrelModelPart, BakedModel> getWoodModelParts(@Nullable String barrelWoodName, boolean requiresPartitionedModel) {
		if (requiresPartitionedModel && woodPartitionedModelParts.containsKey(barrelWoodName)) {
			return woodPartitionedModelParts.get(barrelWoodName);
		} else {
			if (woodModelParts.isEmpty()) {
				return Collections.emptyMap();
			} else if (barrelWoodName == null || !woodModelParts.containsKey(barrelWoodName)) {
				return woodModelParts.values().iterator().next();
			} else {
				return woodModelParts.get(barrelWoodName);
			}
		}
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public TextureAtlasSprite getParticleIcon() {
		return getWoodModelParts(null, false).getOrDefault(BarrelModelPart.BASE, Minecraft.getInstance().getModelManager().getMissingModel()).getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return ITEM_TRANSFORMS;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		if (data.has(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.get(HAS_MAIN_COLOR))) {
			return getWoodModelParts(null, false).get(BarrelModelPart.TINTABLE_MAIN).getParticleIcon(data);
		}

		if (data.has(MATERIALS)) {
			Map<BarrelMaterial, ResourceLocation> materials = data.get(MATERIALS);
			if (materials != null) {
				for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
					if (materials.containsKey(barrelMaterial)) {
						BlockState blockState = getDefaultBlockState(materials.get(barrelMaterial));
						return Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState).getParticleIcon(ModelData.EMPTY);
					}
				}
			}
		}

		if (data.has(WOOD_NAME)) {
			String name = data.get(WOOD_NAME);
			if (!woodModelParts.containsKey(name)) {
				return getParticleIcon();
			}
			return getWoodModelParts(name, false).get(BarrelModelPart.BASE).getParticleIcon(data);
		}
		return getParticleIcon();
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileData) {
		return WorldHelper.getBlockEntity(world, pos, BarrelBlockEntity.class).map(be -> getModelDataFromBlockEntity(be, world, pos)).orElse(ModelData.EMPTY);
	}

	public static ModelData getModelDataFromBlockEntity(BarrelBlockEntity be) {
		return getModelDataFromBlockEntity(be, null, null);
	}

	private static ModelData getModelDataFromBlockEntity(BarrelBlockEntity be, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos) {
		ModelData.Builder builder = ModelData.builder();
		Optional<WoodType> woodType = be.getWoodType();
		boolean isGenericWood = woodType.map(GenericWoodStorageHelper::isGenericWood).orElse(false);
		boolean hasMainColor = be.getStorageWrapper().hasMainColor();
		builder.with(HAS_MAIN_COLOR, hasMainColor || isGenericWood);
		boolean hasAccentColor = be.getStorageWrapper().hasAccentColor();
		builder.with(HAS_ACCENT_COLOR, hasAccentColor || isGenericWood);
		builder.with(IS_PACKED, be.isPacked());
		builder.with(SHOWS_LOCK, be.isLocked() && be.shouldShowLock());
		builder.with(SHOWS_TIER, be.shouldShowTier());
		if (!isGenericWood && (woodType.isPresent() || !(hasMainColor && hasAccentColor))) {
			builder.with(WOOD_NAME, woodType.orElse(WoodType.ACACIA).name());
		}

		Map<BarrelMaterial, ResourceLocation> materials = be.getMaterials();
		if (!materials.isEmpty()) {
			builder.with(MATERIALS, materials);
			Map<ResourceLocation, Integer> materialTintColors = getMaterialTintColors(materials, world, pos);
			if (!materialTintColors.isEmpty()) {
				builder.with(MATERIAL_TINT_COLORS, materialTintColors);
			}
		}
		return builder.build();
	}

	private static Map<ResourceLocation, Integer> getMaterialTintColors(Map<BarrelMaterial, ResourceLocation> materials, @Nullable BlockAndTintGetter world,
			@Nullable BlockPos pos) {
		Map<ResourceLocation, Integer> materialTintColors = new HashMap<>();
		RandomSource rand = RandomSource.create();
		for (ResourceLocation blockName : materials.values()) {
			BlockState blockState = getDefaultBlockState(blockName);
			for (Direction direction : Direction.values()) {
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(blockName, direction, rand);
				if (spriteData.tintIndex() >= 0) {
					int tintColor = Minecraft.getInstance().getBlockColors().getColor(blockState, world, pos, spriteData.tintIndex());
					if (tintColor != -1) {
						materialTintColors.put(spriteData.sprite().contents().name(), 0xFF000000 | (tintColor & 0xFFFFFF));
					}
				}
			}
		}
		return Map.copyOf(materialTintColors);
	}

	public static int getMaterialParticleTintColor(Map<BarrelMaterial, ResourceLocation> materials, @Nullable BlockAndTintGetter world,
			@Nullable BlockPos pos) {
		for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
			ResourceLocation material = materials.get(barrelMaterial);
			if (material == null) {
				continue;
			}

			int tintColor = Minecraft.getInstance().getBlockColors().getColor(getDefaultBlockState(material), world, pos, 0);
			if (tintColor != -1) {
				return tintColor;
			}
		}
		return -1;
	}

	@Override
	public void applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
		if (transformType == ItemDisplayContext.NONE) {
			return;
		}

		ITEM_TRANSFORMS.getTransform(transformType).apply(applyLeftHandTransform, poseStack);
	}
}
