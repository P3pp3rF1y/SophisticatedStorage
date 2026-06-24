package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BarrelBlockStateModelBase implements DynamicBlockStateModel {
	public static final Cache<Integer, List<BlockModelPart>> BAKED_PARTS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	public static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL,
			BarrelMaterial.TOP);
	private boolean showsLock;

	public static void invalidateCache() {
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
	private Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
	private Map<ResourceLocation, Integer> materialTintColors = Collections.emptyMap();

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

	public void setBarrelMaterials(Map<BarrelMaterial, ResourceLocation> barrelMaterials) {
		this.materials = barrelMaterials;
		materialTintColors = getMaterialTintColors(barrelMaterials, null, null);
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
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
			materialTintColors = getMaterialTintColors(materials, level, pos);
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

	private void bakeAndAddDynamicQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder, RandomSource rand,
			boolean rendersUsingSplitModel, boolean renderCore, boolean renderTrim) {

		Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData = woodDynamicBakingData
				.get(woodName != null ? woodName : WoodType.ACACIA.name());

		Map<String, Material> mats = new HashMap<>();
		Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData = new HashMap<>();
		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : materials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				ResourceLocation blockName = entry.getValue();
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(blockName, childMaterial.getLeafSide(), rand);
				ResourceLocation spriteName = spriteData.sprite().contents().name();
				materialSpriteData.put(spriteName, spriteData);
				mats.put(childMaterial.getSerializedName(), new Material(TextureAtlas.LOCATION_BLOCKS, spriteName));
			}
		}

		if (rendersUsingSplitModel) {
			if (renderCore) {
				addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData,
						getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.CORE));
			}
			if (renderTrim) {
				addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData,
						getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.TRIM));
			}
		} else {
			addDynamicQuads(cutoutBuilder, translucentBuilder, materialSpriteData,
					getDynamicModel(woodName, bakingData, mats, DynamicBarrelBakingData.DynamicPart.WHOLE));
		}
	}

	private void addDynamicQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder,
			Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData, QuadCollection quads) {
		for (Direction direction : Direction.values()) {
			for (BakedQuad quad : quads.getQuads(direction)) {
				getDynamicQuadBuilder(quad, cutoutBuilder, translucentBuilder, materialSpriteData).addCulledFace(direction, applyMaterialTint(quad));
			}
		}

		for (BakedQuad quad : quads.getQuads(null)) {
			getDynamicQuadBuilder(quad, cutoutBuilder, translucentBuilder, materialSpriteData).addUnculledFace(applyMaterialTint(quad));
		}
	}

	private static QuadCollection.Builder getDynamicQuadBuilder(BakedQuad quad, QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder,
			Map<ResourceLocation, RenderHelper.SpriteData> materialSpriteData) {
		boolean translucent = Optional.ofNullable(materialSpriteData.get(quad.sprite().contents().name())).map(RenderHelper.SpriteData::translucent)
				.orElse(false);
		return translucent ? translucentBuilder : cutoutBuilder;
	}

	private BakedQuad applyMaterialTint(BakedQuad quad) {
		Integer tintColor = materialTintColors.get(quad.sprite().contents().name());
		return tintColor == null ? quad : QuadTransformers.applyingColor(tintColor).process(quad);
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

	private static BlockState getDefaultBlockState(ResourceLocation blockName) {
		return BuiltInRegistries.BLOCK.get(blockName).orElseThrow().value().defaultBlockState();
	}

	private static Map<ResourceLocation, Integer> getMaterialTintColors(Map<BarrelMaterial, ResourceLocation> materials, @Nullable BlockAndTintGetter world,
			@Nullable BlockPos pos) {
		Map<ResourceLocation, Integer> materialTintColors = new HashMap<>();
		RandomSource rand = RandomSource.create();
		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : materials.entrySet()) {
			BlockState blockState = getDefaultBlockState(entry.getValue());
			for (BarrelMaterial childMaterial : entry.getKey().getChildren()) {
				RenderHelper.SpriteData spriteData = RenderHelper.getSpriteData(entry.getValue(), childMaterial.getLeafSide(), rand);
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
		hash = hash * 31 + materialTintColors.hashCode();
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
