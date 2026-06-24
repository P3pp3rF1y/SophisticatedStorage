package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BarrelBlockStateModelBase implements DynamicBlockStateModel {
	public static final Map<Direction, Transformation> DIRECTION_ROTATES = Map.of(Direction.UP, getDirectionRotationTransform(Direction.UP), Direction.DOWN,
			getDirectionRotationTransform(Direction.DOWN), Direction.NORTH, getDirectionRotationTransform(Direction.NORTH), Direction.SOUTH,
			getDirectionRotationTransform(Direction.SOUTH), Direction.WEST, getDirectionRotationTransform(Direction.WEST), Direction.EAST,
			getDirectionRotationTransform(Direction.EAST));
	private static final LoadingCache<Direction, Cache<Integer, Transformation>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder()
			.expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
				@Override
				public Cache<Integer, Transformation> load(Direction key) {
					return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
				}
			});
	private static final Cache<Integer, Transformation> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES)
			.build();
	private static final Cache<BarrelRenderCacheKey, List<BlockStateModelPart>> BAKED_PARTS_CACHE = CacheBuilder.newBuilder()
			.expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final Cache<BarrelItemRenderCacheKey, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES)
			.build();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL,
			BarrelMaterial.TOP);
	private boolean showsLock;

	public static void invalidateCache() {
		DIRECTION_MOVES_3D_ITEMS.invalidateAll();
		DIRECTION_MOVE_BACK_TO_SIDE.invalidateAll();
		BAKED_PARTS_CACHE.invalidateAll();
		BAKED_QUADS_CACHE.invalidateAll();
	}

	private final ModelBaker baker;
	protected final Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts;
	private final Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons;

	private Item barrelItem = Items.AIR;
	@Nullable
	private String woodName = null;
	private boolean hasMainColor = false;
	private boolean hasAccentColor = false;
	private boolean isPacked = false;
	private boolean showsTier = true;
	private Map<BarrelMaterial, Identifier> materials = new EnumMap<>(BarrelMaterial.class);
	private Map<Identifier, Integer> materialTintColors = Collections.emptyMap();

	private boolean flatTop = false;
	private final Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData;
	private final Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts;
	private final Cache<Integer, QuadCollection> dynamicBakedModelCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	protected BarrelBlockStateModelBase(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts,
			Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons,
			Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData,
			Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts) {
		this.baker = baker;
		this.woodModelParts = woodModelParts;
		this.particleIcons = particleIcons;
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

	public void setBarrelMaterials(Map<BarrelMaterial, Identifier> barrelMaterials) {
		this.materials = barrelMaterials;
		materialTintColors = getMaterialTintColors(barrelMaterials, null, null);
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
	}

	private static Transformation getDirectionRotationTransform(Direction dir) {
		return new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null);
	}

	private TextureAtlasSprite particleIcon() {
		if (hasMainColor) {
			return particleIcons.values().iterator().next().get(BarrelModelPart.TINTABLE_MAIN);
		}

		if (!materials.isEmpty()) {
			for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
				if (materials.containsKey(barrelMaterial)) {
					BlockState blockState = getDefaultBlockState(materials.get(barrelMaterial));
					return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(blockState).sprite();
				}
			}
		}

		if (particleIcons.containsKey(woodName)) {
			return particleIcons.get(woodName).get(BarrelModelPart.BASE);
		}
		return particleIcons.values().iterator().next().get(BarrelModelPart.BASE);
	}

	public List<BakedQuad> getQuads(RandomSource rand) {
		showsLock = false;

		BarrelItemRenderCacheKey cacheKey = createItemCacheKey();
		List<BakedQuad> cachedQuads = BAKED_QUADS_CACHE.getIfPresent(cacheKey);
		if (cachedQuads != null) {
			return cachedQuads;
		}
		List<BlockStateModelPart> parts = getParts(null, rand);
		List<BakedQuad> bakedQuads = new ArrayList<>();

		for (BlockStateModelPart part : parts) {
			for (Direction dir : Direction.values()) {
				bakedQuads.addAll(part.getQuads(dir));
			}
			bakedQuads.addAll(part.getQuads(null));
		}

		BAKED_QUADS_CACHE.put(cacheKey, bakedQuads);

		return bakedQuads;
	}

	@Override
	public void collectParts(@Nullable BlockAndTintGetter level, BlockPos pos, @Nullable BlockState state, RandomSource rand, List<BlockStateModelPart> parts) {
		showsLock = false;

		BarrelBlockEntity be = WorldHelper.getBlockEntity(level, pos, BarrelBlockEntity.class).orElse(null);

		if (be != null) {
			hasMainColor = be.getStorageWrapper().hasMainColor();
			hasAccentColor = be.getStorageWrapper().hasAccentColor();
			isPacked = be.isPacked();
			showsTier = be.shouldShowTier();
			woodName = be.getWoodType().map(WoodType::name).orElse(WoodType.ACACIA.name());
			materials = be.getMaterials();
			materialTintColors = getMaterialTintColors(materials, level, pos);
			flatTop = state != null && state.hasProperty(BarrelBlock.FLAT_TOP) && state.getValue(BarrelBlock.FLAT_TOP);

			showsLock = be.isLocked() && be.shouldShowLock();
			showsTier = be.shouldShowTier();
		}

		BarrelRenderCacheKey cacheKey = createCacheKey(state);
		List<BlockStateModelPart> cachedParts = BAKED_PARTS_CACHE.getIfPresent(cacheKey);
		if (cachedParts != null) {
			parts.addAll(cachedParts);
			return;
		}
		List<BlockStateModelPart> partsToCache = getParts(state, rand);
		BAKED_PARTS_CACHE.put(cacheKey, partsToCache);

		parts.addAll(partsToCache);
	}

	private List<BlockStateModelPart> getParts(@Nullable BlockState state, RandomSource rand) {
		QuadCollection.Builder cutoutQuadCollectionBuilder = new QuadCollection.Builder();
		QuadCollection.Builder translucentQuadCollectionBuilder = new QuadCollection.Builder();

		boolean isBakedDynamically = !materials.isEmpty();
		Set<BarrelMaterial.MaterialModelPart> materialModelParts = materials.keySet().stream().map(BarrelMaterial::getMaterialModelPart)
				.collect(Collectors.toSet());
		boolean rendersUsingSplitModel = materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE)
				|| materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM);

		Map<BarrelModelPart, QuadCollection> modelParts = getWoodModelParts(isBakedDynamically && rendersUsingSplitModel);
		if (modelParts.isEmpty()) {
			return Collections.emptyList();
		}

		if ((!hasMainColor || !hasAccentColor) && !isBakedDynamically) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, getBasePart(state));
		}

		addTintableModelQuads(cutoutQuadCollectionBuilder, state, modelParts);

		if (isBakedDynamically) {
			bakeAndAddDynamicQuads(cutoutQuadCollectionBuilder, translucentQuadCollectionBuilder, rand, rendersUsingSplitModel,
					!hasMainColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE),
					!hasAccentColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM));
		}

		if (showsTier) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.TIER);
		}

		if (isPacked) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.PACKED);
		} else if (showsLock) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.LOCKED);
		}

		List<BlockStateModelPart> parts = new ArrayList<>();
		parts.add(new SimpleModelWrapper(cutoutQuadCollectionBuilder.build(), true, particleMaterial()));
		QuadCollection translucentQuads = translucentQuadCollectionBuilder.build();
		if (!translucentQuads.getAll().isEmpty()) {
			parts.add(new SimpleModelWrapper(translucentQuads, true, new Material.Baked(particleMaterial().sprite(), true)));
		}

		return parts;
	}

	public QuadCollection getTierQuads() {
		return getPartQuads(BarrelModelPart.TIER);
	}

	public QuadCollection getLockQuads() {
		return getPartQuads(BarrelModelPart.LOCKED);
	}

	private QuadCollection getPartQuads(BarrelModelPart part) {
		Map<BarrelModelPart, QuadCollection> modelParts = getWoodModelParts(false);

		QuadCollection.Builder builder = new QuadCollection.Builder();
		addPartQuads(builder, modelParts, part);

		return builder.build();
	}

	private void bakeAndAddDynamicQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder, RandomSource rand,
			boolean rendersUsingSplitModel, boolean renderCore, boolean renderTrim) {

		Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData = woodDynamicBakingData
				.get(woodName != null ? woodName : WoodType.ACACIA.name());

		Map<String, Material> mats = new HashMap<>();
		Map<Identifier, RenderHelper.SpriteData> materialSpriteData = new HashMap<>();
		for (Map.Entry<BarrelMaterial, Identifier> entry : materials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				Identifier blockName = entry.getValue();
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(blockName, childMaterial.getLeafSide(), rand);
				Identifier spriteName = spriteData.sprite().contents().name();
				materialSpriteData.put(spriteName, spriteData);
				mats.put(childMaterial.getSerializedName(), new Material(spriteName, spriteData.translucent()));
			}
		}

		if (rendersUsingSplitModel) {
			if (renderCore) {
				addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData, materialTintColors,
						getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.CORE));
			}
			if (renderTrim) {
				addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData, materialTintColors,
						getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.TRIM));
			}
		} else {
			addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData, materialTintColors,
					getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.WHOLE));
		}
	}

	private void addDynamicQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder,
			Map<Identifier, RenderHelper.SpriteData> materialSpriteData, Map<Identifier, Integer> materialTintColors, QuadCollection quads) {
		for (Direction direction : Direction.values()) {
			for (BakedQuad quad : quads.getQuads(direction)) {
				getDynamicQuadBuilder(quad, cutoutBuilder, translucentBuilder, materialSpriteData).addCulledFace(direction,
						applyMaterialTint(quad, materialTintColors));
			}
		}

		for (BakedQuad quad : quads.getQuads(null)) {
			getDynamicQuadBuilder(quad, cutoutBuilder, translucentBuilder, materialSpriteData).addUnculledFace(applyMaterialTint(quad, materialTintColors));
		}
	}

	private static QuadCollection.Builder getDynamicQuadBuilder(BakedQuad quad, QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder,
			Map<Identifier, RenderHelper.SpriteData> materialSpriteData) {
		boolean translucent = Optional.ofNullable(materialSpriteData.get(quad.materialInfo().sprite().contents().name()))
				.map(RenderHelper.SpriteData::translucent).orElse(false);
		return translucent ? translucentBuilder : cutoutBuilder;
	}

	private static BakedQuad applyMaterialTint(BakedQuad quad, Map<Identifier, Integer> materialTintColors) {
		Integer tintColor = materialTintColors.get(quad.materialInfo().sprite().contents().name());
		return tintColor == null
				? quad
				: new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(), quad.packedUV1(), quad.packedUV2(),
						quad.packedUV3(), quad.direction(), quad.materialInfo(), quad.bakedNormals(), BakedColors.of(tintColor));
	}

	private QuadCollection getDynamicModel(@Nullable String woodName, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData,
			Map<String, Material> materials, DynamicBarrelBakingData.DynamicPart dynamicPart) {
		int hash = Objects.hash(woodName, materials, dynamicPart.name());
		QuadCollection bakedModel = dynamicBakedModelCache.getIfPresent(hash);
		if (bakedModel == null) {
			bakedModel = compileAndBakeModel(materials, bakingData.get(dynamicPart));
			dynamicBakedModelCache.put(hash, bakedModel);
		}
		return bakedModel;
	}

	private static BlockState getDefaultBlockState(Identifier blockName) {
		return BuiltInRegistries.BLOCK.get(blockName).orElseThrow().value().defaultBlockState();
	}

	private static Map<Identifier, Integer> getMaterialTintColors(Map<BarrelMaterial, Identifier> materials, @Nullable BlockAndTintGetter world,
			@Nullable BlockPos pos) {
		Map<Identifier, Integer> materialTintColors = new HashMap<>();
		RandomSource rand = RandomSource.create();
		for (Map.Entry<BarrelMaterial, Identifier> entry : materials.entrySet()) {
			BlockState blockState = getDefaultBlockState(entry.getValue());
			for (BarrelMaterial childMaterial : entry.getKey().getChildren()) {
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(entry.getValue(), childMaterial.getLeafSide(), rand);
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

	public static int getMaterialParticleTintColor(Map<BarrelMaterial, Identifier> materials, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos) {
		for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
			Identifier material = materials.get(barrelMaterial);
			if (material == null) {
				continue;
			}

			BlockState blockState = getDefaultBlockState(material);
			BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
			if (tintSource != null) {
				return world == null || pos == null ? tintSource.color(blockState) : tintSource.colorAsTerrainParticle(blockState, world, pos);
			}
		}
		return -1;
	}

	private QuadCollection compileAndBakeModel(Map<String, Material> textures, DynamicBarrelBakingData bakingData) {
		bakingData.baseTextures().forEach((textureName, texture) -> {
			if (!textures.containsKey(textureName)) {
				textures.put(textureName, texture);
			}
		});

		TextureSlots.Data.Builder texturesBuilder = new TextureSlots.Data.Builder();
		textures.forEach(texturesBuilder::addTexture);
		TextureSlots.Resolver resolver = new TextureSlots.Resolver();
		resolver.addLast(texturesBuilder.build());

		UnbakedGeometry geometry = bakingData.baseModel().geometry();
		return geometry == null
				? QuadCollection.EMPTY
				: geometry.bake(resolver.resolve(bakingData.debugName()), baker, bakingData.modelState(), bakingData.debugName(), ContextMap.EMPTY);
	}

	protected abstract BarrelModelPart getBasePart(@Nullable BlockState state);

	private BarrelItemRenderCacheKey createItemCacheKey() {
		return new BarrelItemRenderCacheKey(barrelItem, createCacheKey(null));
	}

	private BarrelRenderCacheKey createCacheKey(@Nullable BlockState state) {
		return new BarrelRenderCacheKey(this, state, woodName, hasMainColor, hasAccentColor, isPacked, showsLock, showsTier, flatTop, Map.copyOf(materials),
				Map.copyOf(materialTintColors));
	}

	private record BarrelItemRenderCacheKey(Item barrelItem, BarrelRenderCacheKey renderCacheKey) {
	}

	private record BarrelRenderCacheKey(BarrelBlockStateModelBase model, @Nullable BlockState state, @Nullable String woodName, boolean hasMainColor,
			boolean hasAccentColor, boolean isPacked, boolean showsLock, boolean showsTier, boolean flatTop, Map<BarrelMaterial, Identifier> materials,
			Map<Identifier, Integer> materialTintColors) {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof BarrelRenderCacheKey other && model == other.model && Objects.equals(state, other.state)
					&& Objects.equals(woodName, other.woodName) && hasMainColor == other.hasMainColor && hasAccentColor == other.hasAccentColor
					&& isPacked == other.isPacked && showsLock == other.showsLock && showsTier == other.showsTier && flatTop == other.flatTop
					&& Objects.equals(materials, other.materials) && Objects.equals(materialTintColors, other.materialTintColors);
		}

		@Override
		public int hashCode() {
			int hash = System.identityHashCode(model);
			hash = 31 * hash + Objects.hashCode(state);
			hash = 31 * hash + Objects.hashCode(woodName);
			hash = 31 * hash + Boolean.hashCode(hasMainColor);
			hash = 31 * hash + Boolean.hashCode(hasAccentColor);
			hash = 31 * hash + Boolean.hashCode(isPacked);
			hash = 31 * hash + Boolean.hashCode(showsLock);
			hash = 31 * hash + Boolean.hashCode(showsTier);
			hash = 31 * hash + Boolean.hashCode(flatTop);
			hash = 31 * hash + materials.hashCode();
			hash = 31 * hash + materialTintColors.hashCode();
			return hash;
		}
	}

	private void addTintableModelQuads(QuadCollection.Builder builder, @Nullable BlockState state, Map<BarrelModelPart, QuadCollection> modelParts) {
		if (hasAccentColor) {
			addPartQuads(builder, modelParts, BarrelModelPart.TINTABLE_ACCENT);
		}

		if (hasMainColor) {
			addPartQuads(builder, modelParts, getMainPart(state));
		}
	}

	private BarrelModelPart getMainPart(@Nullable BlockState state) {
		return rendersOpen() && state != null && state.hasProperty(BarrelBlock.OPEN) && state.getValue(BarrelBlock.OPEN)
				? BarrelModelPart.TINTABLE_MAIN_OPEN
				: BarrelModelPart.TINTABLE_MAIN;
	}

	protected abstract boolean rendersOpen();

	private void addPartQuads(QuadCollection.Builder builder, Map<BarrelModelPart, QuadCollection> modelParts, BarrelModelPart part) {
		if (modelParts.containsKey(part)) {
			builder.addAll(modelParts.get(part));
		}
	}

	private Map<BarrelModelPart, QuadCollection> getWoodModelParts(boolean requiresPartitionedModel) {
		if (requiresPartitionedModel && woodPartitionedModelParts.containsKey(woodName)) {
			return woodPartitionedModelParts.get(woodName);
		} else {
			if (woodModelParts.isEmpty()) {
				return Collections.emptyMap();
			} else if (woodName == null || !woodModelParts.containsKey(woodName)) {
				return woodModelParts.values().iterator().next();
			} else {
				return woodModelParts.get(woodName);
			}
		}
	}

	public void setModelPropertiesFromBlockEntity(BarrelBlockEntity be) {
		hasMainColor = be.getStorageWrapper().hasMainColor();
		hasAccentColor = be.getStorageWrapper().hasAccentColor();
		isPacked = be.isPacked();
		showsLock = be.isLocked() && be.shouldShowLock();
		showsTier = be.shouldShowTier();
		Optional<WoodType> woodType = be.getWoodType();
		if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
			woodName = woodType.orElse(WoodType.ACACIA).name();
		} else {
			woodName = null;
		}

		materials = be.getMaterials();
		materialTintColors = getMaterialTintColors(materials, null, null);
	}

	@Override
	public Material.Baked particleMaterial() {
		return new Material.Baked(particleIcon(), false);
	}

	@Override
	public int materialFlags() {
		return 0;
	}
}
