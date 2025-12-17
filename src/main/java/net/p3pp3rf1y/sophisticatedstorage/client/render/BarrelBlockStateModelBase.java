package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BarrelBlockStateModelBase implements DynamicBlockStateModel {
	public static final Map<Direction, Transformation> DIRECTION_ROTATES = Map.of(
			Direction.UP, getDirectionRotationTransform(Direction.UP),
			Direction.DOWN, getDirectionRotationTransform(Direction.DOWN),
			Direction.NORTH, getDirectionRotationTransform(Direction.NORTH),
			Direction.SOUTH, getDirectionRotationTransform(Direction.SOUTH),
			Direction.WEST, getDirectionRotationTransform(Direction.WEST),
			Direction.EAST, getDirectionRotationTransform(Direction.EAST)
	);
	private static final LoadingCache<Direction, Cache<Integer, Transformation>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
		@Override
		public Cache<Integer, Transformation> load(Direction key) {
			return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
		}
	});
	private static final Cache<Integer, Transformation> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	public static final Cache<Integer, List<BlockModelPart>> BAKED_PARTS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	public static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL, BarrelMaterial.TOP);
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

	private boolean flatTop = false;
	private final Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData;
	private final Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts;
	private final Cache<Integer, QuadCollection> dynamicBakedModelCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

	protected BarrelBlockStateModelBase(ModelBaker baker,
										Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts,
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
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
	}

	private static Transformation getDirectionRotationTransform(Direction dir) {
		return new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null);
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		if (hasMainColor) {
			return particleIcons.values().iterator().next().get(BarrelModelPart.TINTABLE_MAIN);
		}

		if (!materials.isEmpty()) {
			for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
				if (materials.containsKey(barrelMaterial)) {
					BlockState blockState = getDefaultBlockState(materials.get(barrelMaterial));
					return Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState).particleIcon();
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

		int hash = createItemHash();
		List<BakedQuad> cachedQuads = BAKED_QUADS_CACHE.getIfPresent(hash);
		if (cachedQuads != null) {
			return cachedQuads;
		}
		List<BlockModelPart> parts = getParts(null, rand);
		List<BakedQuad> bakedQuads = new ArrayList<>();

		for (BlockModelPart part : parts) {
			for (Direction dir : Direction.values()) {
				bakedQuads.addAll(part.getQuads(dir));
			}
			bakedQuads.addAll(part.getQuads(null));
		}

		BAKED_QUADS_CACHE.put(hash, bakedQuads);

		return bakedQuads;
	}

	@Override
	public void collectParts(@Nullable BlockAndTintGetter level, BlockPos pos, @Nullable BlockState state, RandomSource rand, List<BlockModelPart> parts) {
		showsLock = false;

		BarrelBlockEntity be = WorldHelper.getBlockEntity(level, pos, BarrelBlockEntity.class).orElse(null);

		if (be != null) {
			hasMainColor = be.getStorageWrapper().hasMainColor();
			hasAccentColor = be.getStorageWrapper().hasAccentColor();
			isPacked = be.isPacked();
			showsTier = be.shouldShowTier();
			woodName = be.getWoodType().map(WoodType::name).orElse(WoodType.ACACIA.name());
			materials = be.getMaterials();
			flatTop = state != null && state.getValue(BarrelBlock.FLAT_TOP);

			showsLock = be.isLocked() && be.shouldShowLock();
			showsTier = be.shouldShowTier();
		}

		int hash = createHash(state);
		List<BlockModelPart> cachedParts = BAKED_PARTS_CACHE.getIfPresent(hash);
		if (cachedParts != null) {
			parts.addAll(cachedParts);
			return;
		}
		List<BlockModelPart> partsToCache = getParts(state, rand);
		BAKED_PARTS_CACHE.put(hash, partsToCache);

		parts.addAll(partsToCache);
	}

	private List<BlockModelPart> getParts(@Nullable BlockState state, RandomSource rand) {
		QuadCollection.Builder cutoutQuadCollectionBuilder = new QuadCollection.Builder();
		QuadCollection.Builder translucentQuadCollectionBuilder = new QuadCollection.Builder();

		boolean isBakedDynamically = !materials.isEmpty();
		Set<BarrelMaterial.MaterialModelPart> materialModelParts = materials.keySet().stream().map(BarrelMaterial::getMaterialModelPart).collect(Collectors.toSet());
		boolean rendersUsingSplitModel = materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE) || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM);

		Map<BarrelModelPart, QuadCollection> modelParts = getWoodModelParts(isBakedDynamically && rendersUsingSplitModel);
		if (modelParts.isEmpty()) {
			return Collections.emptyList();
		}

		if ((!hasMainColor || !hasAccentColor) && !isBakedDynamically) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, getBasePart(state));
		}

		addTintableModelQuads(cutoutQuadCollectionBuilder, state, modelParts);

		if (isBakedDynamically) {
			bakeAndAddDynamicQuads(cutoutQuadCollectionBuilder, rand, rendersUsingSplitModel,
					!hasMainColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE), !hasAccentColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM));
		}

		if (showsTier) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.TIER);
		}

		if (isPacked) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.PACKED);
		} else if (showsLock) {
			addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.LOCKED);
		}

		List<BlockModelPart> parts = new ArrayList<>();
		parts.add(new SimpleModelWrapper(cutoutQuadCollectionBuilder.build(), true, particleIcon(), ChunkSectionLayer.CUTOUT));
		QuadCollection translucentQuads = translucentQuadCollectionBuilder.build();
		if (!translucentQuads.getAll().isEmpty()) {
			parts.add(new SimpleModelWrapper(translucentQuads, true, particleIcon(), ChunkSectionLayer.TRANSLUCENT));
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

	private void bakeAndAddDynamicQuads(QuadCollection.Builder builder, RandomSource rand, boolean rendersUsingSplitModel, boolean renderCore, boolean renderTrim) {

		Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData = woodDynamicBakingData.get(woodName != null ? woodName : WoodType.ACACIA.name());

		Map<String, Material> mats = new HashMap<>();
		for (Map.Entry<BarrelMaterial, Identifier> entry : materials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				Identifier blockName = entry.getValue();
				TextureAtlasSprite sprite = RenderHelper.getSprite(blockName, childMaterial.getLeafSide(), rand);
				mats.put(childMaterial.getSerializedName(), new Material(TextureAtlas.LOCATION_BLOCKS, sprite.contents().name()));
			}
		}

		if (rendersUsingSplitModel) {
			if (renderCore) {
				builder.addAll(getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.CORE));
			}
			if (renderTrim) {
				builder.addAll(getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.TRIM));
			}
		} else {
			builder.addAll(getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.WHOLE));
		}
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

	private BlockState getDefaultBlockState(Identifier blockName) {
		return BuiltInRegistries.BLOCK.get(blockName).orElseThrow().value().defaultBlockState();
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
		return geometry == null ? QuadCollection.EMPTY : geometry.bake(resolver.resolve(bakingData.debugName()), baker, bakingData.modelState(), bakingData.debugName(), ContextMap.EMPTY);
	}

	protected abstract BarrelModelPart getBasePart(@Nullable BlockState state);

	public int createItemHash() {
		return barrelItem.hashCode() * 31 + createHash(null);
	}

	protected int createHash(@Nullable BlockState state) {
		int hash = state != null ? state.getBlock().hashCode() : 0;

		if (woodName != null) {
			hash = hash * 31 + woodName.hashCode() + 1;
		}
		hash = hash * 31 + (hasMainColor ? 1 : 0);
		hash = hash * 31 + (hasAccentColor ? 1 : 0);
		hash = hash * 31 + (isPacked ? 1 : 0);
		hash = hash * 31 + (showsLock ? 1 : 0);
		hash = hash * 31 + (showsTier ? 1 : 0);
		hash = hash * 31 + (flatTop ? 1 : 0);
		hash = hash * 31 + materials.hashCode();
		return hash;
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
		return rendersOpen() && state != null && state.getValue(BarrelBlock.OPEN) ? BarrelModelPart.TINTABLE_MAIN_OPEN : BarrelModelPart.TINTABLE_MAIN;
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
	}
}
