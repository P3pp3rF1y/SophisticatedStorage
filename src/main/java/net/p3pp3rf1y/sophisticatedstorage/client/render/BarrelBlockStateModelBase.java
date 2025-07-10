package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.*;

public abstract class BarrelBlockStateModelBase implements DynamicBlockStateModel {
	private static final IQuadTransformer MOVE_TO_CORNER = QuadTransformers.applying(new Transformation(new Vector3f(-.5f, -.5f, -.5f), null, null, null));
	public static final Map<Direction, IQuadTransformer> DIRECTION_ROTATES = Map.of(
			Direction.UP, getDirectionRotationTransform(Direction.UP),
			Direction.DOWN, getDirectionRotationTransform(Direction.DOWN),
			Direction.NORTH, getDirectionRotationTransform(Direction.NORTH),
			Direction.SOUTH, getDirectionRotationTransform(Direction.SOUTH),
			Direction.WEST, getDirectionRotationTransform(Direction.WEST),
			Direction.EAST, getDirectionRotationTransform(Direction.EAST)
	);
	private static final LoadingCache<Direction, Cache<Integer, IQuadTransformer>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
		@Override
		public Cache<Integer, IQuadTransformer> load(Direction key) {
			return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
		}
	});
	private static final IQuadTransformer SCALE_BIG_2D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(BIG_ITEM_SCALE, BIG_ITEM_SCALE, BIG_ITEM_SCALE), null));
	private static final IQuadTransformer SCALE_SMALL_3D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(SMALL_BLOCK_ITEM_SCALE, SMALL_BLOCK_ITEM_SCALE, SMALL_BLOCK_ITEM_SCALE), null));
	private static final IQuadTransformer SCALE_SMALL_2D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(SMALL_ITEM_SCALE, SMALL_ITEM_SCALE, SMALL_ITEM_SCALE), null));
	private static final Cache<Integer, IQuadTransformer> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	public static final Cache<Integer, List<BlockModelPart>> BAKED_PARTS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	public static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final Map<Integer, IQuadTransformer> DISPLAY_ROTATIONS = new HashMap<>();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL, BarrelMaterial.TOP);
	private List<RenderInfo.DisplayItem> displayItems;
	private List<Integer> inaccessibleSlots;
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
	private Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);

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

	public void setBarrelMaterials(Map<BarrelMaterial, ResourceLocation> barrelMaterials) {
		this.materials = barrelMaterials;
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
	}

	private static IQuadTransformer getDirectionRotationTransform(Direction dir) {
		return QuadTransformers.applying(new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null));
	}

	private IQuadTransformer getDirectionMoveBackToSide(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
		IQuadTransformer transform = DIRECTION_MOVE_BACK_TO_SIDE.getIfPresent(hash);
		if (transform == null) {
			Vec3i normal = dir.getUnitVec3i();
			Vector3f offset = new Vector3f(distFromCenter, distFromCenter, distFromCenter);
			offset.mul(normal.getX(), normal.getY(), normal.getZ());
			Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount);
			frontOffset.add(-0.5f, -0.5f, -0.5f);
			rotateDisplayItemFrontOffset(state, dir, frontOffset);
			frontOffset.add(0.5f, 0.5f, 0.5f);
			offset.add(frontOffset);
			transform = QuadTransformers.applying(new Transformation(offset, null, null, null));

			DIRECTION_MOVE_BACK_TO_SIDE.put(hash, transform);
		}
		return transform;
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

	@SuppressWarnings("java:S1172") //state used in override
	protected void rotateDisplayItemFrontOffset(BlockState state, Direction dir, Vector3f frontOffset) {
		frontOffset.rotate(getNorthBasedRotation(dir));
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = Float.hashCode(distFromCenter);
		hash = 31 * hash + displayItemIndex;
		hash = 31 * hash + displayItemCount;
		return hash;
	}

	public List<BakedQuad> getQuads(RandomSource rand) {
		showsLock = false;
		inaccessibleSlots = Collections.emptyList();
		displayItems = Collections.emptyList();

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
		displayItems = Collections.emptyList();
		inaccessibleSlots = Collections.emptyList();
		showsLock = false;

		BarrelBlockEntity be = WorldHelper.getBlockEntity(level, pos, BarrelBlockEntity.class).orElse(null);

		if (be != null) {
			hasMainColor = be.getStorageWrapper().hasMainColor();
			hasAccentColor = be.getStorageWrapper().hasAccentColor();
			if (!be.hasFullyDynamicRenderer()) {
				displayItems = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems();
				inaccessibleSlots = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getInaccessibleSlots();
			}
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
		} else {
			if (showsLock) {
				addPartQuads(cutoutQuadCollectionBuilder, modelParts, BarrelModelPart.LOCKED);
			}
			addDisplayItemQuads(cutoutQuadCollectionBuilder, translucentQuadCollectionBuilder, state);
		}

		List<BlockModelPart> parts = new ArrayList<>();
		parts.add(new SimpleModelWrapper(cutoutQuadCollectionBuilder.build(), true, particleIcon(), RenderType.cutout()));
		QuadCollection translucentQuads = translucentQuadCollectionBuilder.build();
		if (!translucentQuads.getAll().isEmpty()) {
			parts.add(new SimpleModelWrapper(translucentQuads, true, particleIcon(), RenderType.TRANSLUCENT));
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
		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : materials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				ResourceLocation blockName = entry.getValue();
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

	private BlockState getDefaultBlockState(ResourceLocation blockName) {
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
		hash = getDisplayItemsHash(displayItems, inaccessibleSlots, hash);
		return hash;
	}

	private int getDisplayItemsHash(List<RenderInfo.DisplayItem> displayItems, List<Integer> inaccessibleSlots, int hash) {
		for (RenderInfo.DisplayItem displayItem : displayItems) {
			hash = hash * 31 + getDisplayItemHash(displayItem);
		}
		for (Integer inaccessibleSlot : inaccessibleSlots) {
			hash = hash * 31 + inaccessibleSlot;
		}
		return hash;
	}

	private int getDisplayItemHash(RenderInfo.DisplayItem displayItem) {
		int hash = displayItem.getRotation();
		ItemStack stack = displayItem.getItem();
		hash = hash * 31 + ItemStack.hashItemAndComponents(stack);
		hash = hash * 31 + displayItem.getSlotIndex();
		return hash;
	}

	private void addDisplayItemQuads(QuadCollection.Builder cutoutBuilder, QuadCollection.Builder translucentBuilder, @Nullable BlockState state) {
		if (state == null || !(state.getBlock() instanceof BarrelBlock barrelBlock)) {
			return;
		}

		int displayItemsCount = barrelBlock.getDisplayItemsCount(displayItems);
		if (!displayItems.isEmpty()) {
			int index = 0;
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				ItemStack item = displayItem.getItem();
				if (barrelBlock.hasFixedIndexDisplayItems()) {
					index = displayItem.getSlotIndex();
				}
				if (item.isEmpty()) {
					continue;
				}

				ItemStackRenderState renderState = new ItemStackRenderState();
				Minecraft.getInstance().getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.FIXED, null, null, 0);
				for (ItemStackRenderState.LayerRenderState layer : renderState.layers) {
					if (layer.specialRenderer == null) {
						int rotation = displayItem.getRotation();
						QuadCollection.Builder builder = layer.renderType == RenderType.translucent() ? translucentBuilder : cutoutBuilder;

						addRenderedItem(builder, state, item, renderState, layer.prepareQuadList(), layer.transform, DisplayItemRenderer.isGui3d(renderState), rotation, index, displayItemsCount);
					}
					index++;
				}
			}
		}

		addInaccessibleSlotsQuads(cutoutBuilder, inaccessibleSlots, state, displayItemsCount);
	}

	private void addInaccessibleSlotsQuads(QuadCollection.Builder builder, List<Integer> inaccessibleSlots, BlockState state, int displayItemsCount) {
		ItemStack inaccessibleSlotStack = new ItemStack(ModItems.INACCESSIBLE_SLOT.get());
		ItemStackRenderState renderState = new ItemStackRenderState();
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(renderState, inaccessibleSlotStack, ItemDisplayContext.FIXED, null, null, 0);
		boolean gui3d = DisplayItemRenderer.isGui3d(renderState);
		for (int inaccessibleSlot : inaccessibleSlots) {
			for (ItemStackRenderState.LayerRenderState layer : renderState.layers) {
				if (layer.specialRenderer == null) {
					addRenderedItem(builder, state, inaccessibleSlotStack, renderState, layer.prepareQuadList(), layer.transform, gui3d, 0, inaccessibleSlot, displayItemsCount);
				}
			}
		}
	}

	@SuppressWarnings("java:S107")
	private void addRenderedItem(QuadCollection.Builder builder, BlockState state, ItemStack displayItem, ItemStackRenderState renderState, List<BakedQuad> quads, ItemTransform transform, boolean gui3d, int rotation, int displayItemIndex, int displayItemCount) {
		List<BakedQuad> originalQuads = quads;
		quads = MOVE_TO_CORNER.process(quads);
		quads = QuadTransformers.applying(toTransformation(transform)).process(quads);
		if (gui3d) {
			if (displayItemCount > 1) {
				quads = SCALE_SMALL_3D_ITEM.process(quads);
			}
		} else {
			if (displayItemCount == 1) {
				quads = SCALE_BIG_2D_ITEM.process(quads);
			} else {
				quads = SCALE_SMALL_2D_ITEM.process(quads);
			}
		}

		if (rotation != 0) {
			quads = getDisplayRotation(rotation).process(quads);
		}

		Direction facing = state.getBlock() instanceof BarrelBlock barrelBlock ? barrelBlock.getFacing(state) : Direction.NORTH;
		quads = rotateDisplayItemQuads(quads, state);

		if (gui3d) {
			IQuadTransformer transformer = getDirectionMove(displayItem, renderState, transform, originalQuads, gui3d, state, facing, displayItemIndex, displayItemCount, displayItemCount == 1 ? 1 : SMALL_BLOCK_ITEM_SCALE);
			quads = transformer.process(quads);
		} else {
			quads = getDirectionMove(displayItem, renderState, transform, originalQuads, gui3d, state, facing, displayItemIndex, displayItemCount, 1).process(quads);
		}
		quads = recalculateDirections(quads);

		quads = updateTintIndexes(quads, displayItemIndex);

		quads.forEach(builder::addUnculledFace);
	}

	private Transformation toTransformation(ItemTransform transform) {
		if (transform.equals(ItemTransform.NO_TRANSFORM)) {
			return Transformation.identity();
		}
		return new Transformation(toVector3f(transform.translation()), quatFromXYZ(transform.rotation(), true), toVector3f(transform.scale()), null);
	}

	private Vector3f toVector3f(Vector3fc vec) {
		return new Vector3f(vec.x(), vec.y(), vec.z());
	}

	public Quaternionf quatFromXYZ(Vector3fc xyz, boolean degrees) {
		return TransformationHelper.quatFromXYZ(xyz.x(), xyz.y(), xyz.z(), degrees);
	}

	protected abstract List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state);

	private List<BakedQuad> updateTintIndexes(List<BakedQuad> quads, int displayItemIndex) {
		List<BakedQuad> ret = new ArrayList<>(quads.size());
		int offset = (displayItemIndex + 1) * 10;
		for (BakedQuad quad : quads) {
			if (quad.tintIndex() >= 0) {
				ret.add(new BakedQuad(quad.vertices(), quad.tintIndex() + offset, quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission(), quad.hasAmbientOcclusion()));
			} else {
				ret.add(quad);
			}
		}
		return ret;
	}

	private List<BakedQuad> recalculateDirections(List<BakedQuad> quads) {
		List<BakedQuad> ret = new ArrayList<>(quads.size());
		for (BakedQuad quad : quads) {
			Direction calculatedFacing = FaceBakery.calculateFacing(quad.vertices());
			if (quad.direction() != calculatedFacing) {
				ret.add(new BakedQuad(quad.vertices(), quad.tintIndex(), calculatedFacing, quad.sprite(), quad.shade(), quad.lightEmission(), quad.hasAmbientOcclusion()));
			} else {
				ret.add(quad);
			}
		}
		return ret;
	}

	private IQuadTransformer getDirectionMove(ItemStack displayItem, ItemStackRenderState renderState, ItemTransform transform, List<BakedQuad> quads, boolean isGui3d, BlockState state, Direction direction, int displayItemIndex, int displayItemCount, float itemScale) {
		boolean isFlatTop = state.getValue(BarrelBlock.FLAT_TOP);
		int hash = calculateDirectionMoveHash(state, displayItem, displayItemIndex, displayItemCount, isFlatTop);
		Cache<Integer, IQuadTransformer> directionCache = DIRECTION_MOVES_3D_ITEMS.getUnchecked(direction);
		IQuadTransformer transformer = directionCache.getIfPresent(hash);

		if (transformer == null) {
			double offset = DisplayItemRenderer.getDisplayItemOffset(displayItem, renderState, transform, quads, isGui3d, itemScale);
			if (!isFlatTop) {
				offset -= 1 / 16D;
			}

			transformer = getDirectionMoveBackToSide(state, direction, (float) (0.5f + offset), displayItemIndex, displayItemCount);
			directionCache.put(hash, transformer);
		}

		return transformer;
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected int calculateDirectionMoveHash(BlockState state, ItemStack displayItem, int displayItemIndex, int displayItemCount, boolean isFlatTop) {
		int hashCode = ItemStack.hashItemAndComponents(displayItem);
		hashCode = hashCode * 31 + displayItemIndex;
		hashCode = hashCode * 31 + displayItemCount;
		hashCode = hashCode * 31 + (isFlatTop ? 1 : 0);
		return hashCode;
	}

	private IQuadTransformer getDisplayRotation(int rotation) {
		return DISPLAY_ROTATIONS.computeIfAbsent(rotation, r -> QuadTransformers.applying(new Transformation(null, Axis.ZP.rotationDegrees(rotation), null, null)));
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
		return rendersOpen() && state != null && Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? BarrelModelPart.TINTABLE_MAIN_OPEN : BarrelModelPart.TINTABLE_MAIN;
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
		if (!be.hasFullyDynamicRenderer()) {
			displayItems = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems();
			inaccessibleSlots = be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getInaccessibleSlots();
		} else {
			displayItems = Collections.emptyList();
			inaccessibleSlots = Collections.emptyList();
		}
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
