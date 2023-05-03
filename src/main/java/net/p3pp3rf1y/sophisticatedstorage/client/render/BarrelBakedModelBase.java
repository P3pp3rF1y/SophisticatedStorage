package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.*;

public abstract class BarrelBakedModelBase implements IDynamicBakedModel {
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
	private static final IQuadTransformer SCALE_BIG_2D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE), null));
	private static final IQuadTransformer SCALE_SMALL_3D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE), null));
	private static final IQuadTransformer SCALE_SMALL_2D_ITEM = QuadTransformers.applying(new Transformation(null, null, new Vector3f(SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE), null));
	private static final Cache<Integer, IQuadTransformer> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
	private static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
	private static final ModelProperty<Boolean> SHOWS_LOCK = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
	private static final ModelProperty<List<RenderInfo.DisplayItem>> DISPLAY_ITEMS = new ModelProperty<>();
	private static final ModelProperty<List<Integer>> INACCESSIBLE_SLOTS = new ModelProperty<>();
	public static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final Map<Integer, IQuadTransformer> DISPLAY_ROTATIONS = new HashMap<>();
	private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final ItemTransforms ITEM_TRANSFORMS = createItemTransforms();

	@SuppressWarnings("java:S4738") //ItemTransforms require Guava ImmutableMap to be passed in so no way to change that to java Map
	private static ItemTransforms createItemTransforms() {
		return new ItemTransforms(new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 14.25f / 16f, 0), new Vector3f(1, 1, 1), DEFAULT_ROTATION), new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3 / 16f, 0), new Vector3f(0.25f, 0.25f, 0.25f), DEFAULT_ROTATION), new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f), DEFAULT_ROTATION), ImmutableMap.of());
	}

	public static void invalidateCache() {
		DIRECTION_MOVES_3D_ITEMS.invalidateAll();
		DIRECTION_MOVE_BACK_TO_SIDE.invalidateAll();
		BAKED_QUADS_CACHE.invalidateAll();
	}

	protected final Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts;
	private final ItemOverrides barrelItemOverrides;
	private Item barrelItem = Items.AIR;
	@Nullable
	private String barrelWoodName = null;
	private boolean barrelHasMainColor = false;
	private boolean barrelHasAccentColor = false;
	private boolean barrelIsPacked = false;

	private boolean flatTop = false;

	protected BarrelBakedModelBase(Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel) {
		this.woodModelParts = woodModelParts;
		barrelItemOverrides = new BarrelItemOverrides(this, flatTopModel);
	}

	private static IQuadTransformer getDirectionRotationTransform(Direction dir) {
		return QuadTransformers.applying(new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null));
	}

	private IQuadTransformer getDirectionMoveBackToSide(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
		IQuadTransformer transform = DIRECTION_MOVE_BACK_TO_SIDE.getIfPresent(hash);
		if (transform == null) {
			Vec3i normal = dir.getNormal();
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

	@SuppressWarnings("java:S1172") //state used in override
	protected void rotateDisplayItemFrontOffset(BlockState state, Direction dir, Vector3f frontOffset) {
		frontOffset.transform(getNorthBasedRotation(dir));
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = Float.hashCode(distFromCenter);
		hash = 31 * hash + displayItemIndex;
		hash = 31 * hash + displayItemCount;
		return hash;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.of(RenderType.cutout());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		int hash = createHash(state, side, extraData);
		List<BakedQuad> quads = BAKED_QUADS_CACHE.getIfPresent(hash);
		if (quads != null) {
			return quads;
		}

		String woodName = null;
		boolean hasMainColor;
		boolean hasAccentColor;
		boolean isPacked;
		if (state != null) {
			hasMainColor = Boolean.TRUE.equals(extraData.get(HAS_MAIN_COLOR));
			hasAccentColor = Boolean.TRUE.equals(extraData.get(HAS_ACCENT_COLOR));
			if (extraData.has(WOOD_NAME)) {
				woodName = extraData.get(WOOD_NAME);
			}
			isPacked = isPacked(extraData);
		} else {
			woodName = barrelWoodName;
			hasMainColor = barrelHasMainColor;
			hasAccentColor = barrelHasAccentColor;
			isPacked = barrelIsPacked;
		}

		List<BakedQuad> ret = new ArrayList<>();

		Map<BarrelModelPart, BakedModel> modelParts = getWoodModelParts(woodName);
		if (modelParts.isEmpty()) {
			return Collections.emptyList();
		}

		if (!hasMainColor || !hasAccentColor) {
			addPartQuads(state, side, rand, ret, modelParts, getBasePart(state), renderType);
		}

		addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor, modelParts, renderType);
		addTierQuads(state, side, rand, ret, modelParts, renderType);

		if (isPacked) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.PACKED, renderType);
		} else {
			if (showsLocked(extraData)) {
				addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.LOCKED, renderType);
			}
			addDisplayItemQuads(state, side, rand, ret, extraData);
		}

		BAKED_QUADS_CACHE.put(hash, ret);

		return ret;
	}

	protected abstract BarrelModelPart getBasePart(@Nullable BlockState state);

	private boolean isPacked(ModelData extraData) {
		return extraData.has(IS_PACKED) && Boolean.TRUE.equals(extraData.get(IS_PACKED));
	}

	private boolean showsLocked(ModelData extraData) {
		return extraData.has(SHOWS_LOCK) && Boolean.TRUE.equals(extraData.get(SHOWS_LOCK));
	}

	private void addTierQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, Map<BarrelModelPart, BakedModel> modelParts, @Nullable RenderType renderType) {
		addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TIER, renderType);
	}

	private int createHash(@Nullable BlockState state, @Nullable Direction side, ModelData data) {
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
		hash = hash * 31 + (flatTop ? 1 : 0);
		return hash;
	}

	protected int getInWorldBlockHash(BlockState state, ModelData data) {
		int hash = state.getBlock().hashCode();

		//noinspection ConstantConditions
		hash = hash * 31 + (data.has(WOOD_NAME) ? data.get(WOOD_NAME).hashCode() + 1 : 0);
		hash = hash * 31 + (data.has(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.get(HAS_MAIN_COLOR)) ? 1 : 0);
		hash = hash * 31 + (data.has(HAS_ACCENT_COLOR) && Boolean.TRUE.equals(data.get(HAS_ACCENT_COLOR)) ? 1 : 0);
		hash = hash * 31 + (isPacked(data) ? 1 : 0);
		hash = hash * 31 + (showsLocked(data) ? 1 : 0);
		hash = hash * 31 + (Boolean.TRUE.equals(state.getValue(BarrelBlock.FLAT_TOP)) ? 1 : 0);
		return hash;
	}

	private int getDisplayItemsHash(ModelData data, int hash) {
		if (data.has(DISPLAY_ITEMS)) {
			List<RenderInfo.DisplayItem> displayItems = data.get(DISPLAY_ITEMS);
			//noinspection ConstantConditions
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				hash = hash * 31 + getDisplayItemHash(displayItem);
			}
		}
		if (data.has(INACCESSIBLE_SLOTS)) {
			List<Integer> inaccessibleSlots = data.get(INACCESSIBLE_SLOTS);
			//noinspection ConstantConditions
			for (Integer inaccessibleSlot : inaccessibleSlots) {
				hash = hash * 31 + inaccessibleSlot;
			}
		}
		return hash;
	}

	private int getDisplayItemHash(RenderInfo.DisplayItem displayItem) {
		int hash = displayItem.getRotation();
		hash = hash * 31 + ItemStackKey.getHashCode(displayItem.getItem());
		hash = hash * 31 + displayItem.getSlotIndex();
		return hash;
	}

	private void addDisplayItemQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, ModelData data) {
		if (state == null || side != null || !(state.getBlock() instanceof BarrelBlock barrelBlock)) {
			return;
		}

		List<RenderInfo.DisplayItem> displayItems = data.get(DISPLAY_ITEMS);

		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer itemRenderer = minecraft.getItemRenderer();
		if (displayItems != null && !displayItems.isEmpty()) {
			int index = 0;
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				ItemStack item = displayItem.getItem();
				if (barrelBlock.hasFixedIndexDisplayItems()) {
					index = displayItem.getSlotIndex();
				}
				if (item.isEmpty()) {
					continue;
				}

				BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
				if (!model.isCustomRenderer()) {
					int rotation = displayItem.getRotation();
					for (Direction face : Direction.values()) {
						addRenderedItemSide(state, rand, ret, item, model, rotation, face, index, barrelBlock.getDisplayItemsCount(displayItems));
					}
					addRenderedItemSide(state, rand, ret, item, model, rotation, null, index, barrelBlock.getDisplayItemsCount(displayItems));
				}
				index++;
			}
		}

		addInaccessibleSlotsQuads(state, rand, ret, data, barrelBlock, displayItems, minecraft);
	}

	private void addInaccessibleSlotsQuads(BlockState state, RandomSource rand, List<BakedQuad> ret, ModelData data, BarrelBlock barrelBlock,
			@Nullable List<RenderInfo.DisplayItem> displayItems, Minecraft minecraft) {
		List<Integer> inaccessibleSlots = data.get(INACCESSIBLE_SLOTS);
		if (displayItems != null && inaccessibleSlots != null) {
			ItemStack inaccessibleSlotStack = new ItemStack(ModItems.INACCESSIBLE_SLOT.get());
			BakedModel model = minecraft.getItemRenderer().getModel(inaccessibleSlotStack, null, minecraft.player, 0);
			for (int inaccessibleSlot : inaccessibleSlots) {
				if (!model.isCustomRenderer()) {
					for (Direction face : Direction.values()) {
						addRenderedItemSide(state, rand, ret, inaccessibleSlotStack, model, 0, face, inaccessibleSlot, barrelBlock.getDisplayItemsCount(displayItems));
					}
					addRenderedItemSide(state, rand, ret, inaccessibleSlotStack, model, 0, null, inaccessibleSlot, barrelBlock.getDisplayItemsCount(displayItems));
				}
			}
		}
	}

	@SuppressWarnings({"deprecation", "java:S107"})
	private void addRenderedItemSide(BlockState state, RandomSource rand, List<BakedQuad> ret, ItemStack displayItem, BakedModel model, int rotation,
			@Nullable Direction dir, int displayItemIndex, int displayItemCount) {
		List<BakedQuad> quads = model.getQuads(null, dir, rand);
		quads = MOVE_TO_CORNER.process(quads);
		quads = QuadTransformers.applying(toTransformation(model.getTransforms().getTransform(ItemTransforms.TransformType.FIXED))).process(quads);
		if (!model.isGui3d()) {
			if (displayItemCount == 1) {
				quads = SCALE_BIG_2D_ITEM.process(quads);
			} else {
				quads = SCALE_SMALL_2D_ITEM.process(quads);
			}
		} else if (displayItemCount > 1) {
			quads = SCALE_SMALL_3D_ITEM.process(quads);
		}

		if (rotation != 0) {
			quads = getDisplayRotation(rotation).process(quads);
		}

		Direction facing = state.getBlock() instanceof BarrelBlock barrelBlock ? barrelBlock.getFacing(state) : Direction.NORTH;
		quads = rotateDisplayItemQuads(quads, state);

		if (model.isGui3d()) {
			IQuadTransformer transformer = getDirectionMove(displayItem, model, state, facing, displayItemIndex, displayItemCount, displayItemCount == 1 ? 1 : SMALL_3D_ITEM_SCALE);
			quads = transformer.process(quads);
			recalculateDirections(quads);
		} else {
			quads = getDirectionMove(displayItem, model, state, facing, displayItemIndex, displayItemCount, 1).process(quads);
			recalculateDirections(quads);
		}

		updateTintIndexes(quads, displayItemIndex);

		ret.addAll(quads);
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

	protected abstract List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state);

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

	private IQuadTransformer getDirectionMove(ItemStack displayItem, BakedModel model, BlockState state, Direction direction, int displayItemIndex, int displayItemCount, float itemScale) {
		boolean isFlatTop = state.getValue(BarrelBlock.FLAT_TOP);
		int hash = calculateDirectionMoveHash(state, displayItem, displayItemIndex, displayItemCount, isFlatTop);
		Cache<Integer, IQuadTransformer> directionCache = DIRECTION_MOVES_3D_ITEMS.getUnchecked(direction);
		IQuadTransformer transformer = directionCache.getIfPresent(hash);

		if (transformer == null) {
			double offset = DisplayItemRenderer.getDisplayItemOffset(displayItem, model, itemScale);
			if (!isFlatTop) {
				offset -= 1/16D;
			}

			transformer = getDirectionMoveBackToSide(state, direction, (float) (0.5f + offset), displayItemIndex, displayItemCount);
			directionCache.put(hash, transformer);
		}

		return transformer;
	}

	@SuppressWarnings("java:S1172") //state used in override
	protected int calculateDirectionMoveHash(BlockState state, ItemStack displayItem, int displayItemIndex, int displayItemCount, boolean isFlatTop) {
		int hashCode = ItemStackKey.getHashCode(displayItem);
		hashCode = hashCode * 31 + displayItemIndex;
		hashCode = hashCode * 31 + displayItemCount;
		hashCode = hashCode * 31 + (isFlatTop ? 1 : 0);
		return hashCode;
	}

	private IQuadTransformer getDisplayRotation(int rotation) {
		return DISPLAY_ROTATIONS.computeIfAbsent(rotation, r -> QuadTransformers.applying(new Transformation(null, Vector3f.ZP.rotationDegrees(rotation), null, null)));
	}

	private void addTintableModelQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret, boolean hasMainColor,
			boolean hasAccentColor, Map<BarrelModelPart, BakedModel> modelParts, @Nullable RenderType renderType) {
		if (hasAccentColor) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TINTABLE_ACCENT, renderType);
		}

		if (hasMainColor) {
			addPartQuads(state, side, rand, ret, modelParts, getMainPart(state), renderType);
		}
	}

	private BarrelModelPart getMainPart(@Nullable BlockState state) {
		return rendersOpen() && state != null && Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? BarrelModelPart.TINTABLE_MAIN_OPEN : BarrelModelPart.TINTABLE_MAIN;
	}

	protected abstract boolean rendersOpen();

	private void addPartQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, List<BakedQuad> ret,
			Map<BarrelModelPart, BakedModel> modelParts, BarrelModelPart part, @Nullable RenderType renderType) {
		if (modelParts.containsKey(part)) {
			ret.addAll(modelParts.getOrDefault(part, Minecraft.getInstance().getModelManager().getMissingModel()).getQuads(state, side, rand, ModelData.EMPTY, renderType));
		}
	}

	private Map<BarrelModelPart, BakedModel> getWoodModelParts(@Nullable String barrelWoodName) {
		if (woodModelParts.isEmpty()) {
			return Collections.emptyMap();
		} else if (barrelWoodName == null || !woodModelParts.containsKey(barrelWoodName)) {
			return woodModelParts.values().iterator().next();
		} else {
			return woodModelParts.get(barrelWoodName);
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
		return getWoodModelParts(null).getOrDefault(BarrelModelPart.BASE, Minecraft.getInstance().getModelManager().getMissingModel()).getParticleIcon();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemTransforms getTransforms() {
		return ITEM_TRANSFORMS;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		if (data.has(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.get(HAS_MAIN_COLOR))) {
			return getWoodModelParts(null).get(BarrelModelPart.TINTABLE_MAIN).getParticleIcon(data);
		} else if (data.has(WOOD_NAME)) {
			String name = data.get(WOOD_NAME);
			if (!woodModelParts.containsKey(name)) {
				return getParticleIcon();
			}
			return getWoodModelParts(name).get(BarrelModelPart.BASE).getParticleIcon(data);
		}
		return getParticleIcon();
	}

	@Nonnull
	@Override
	public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileData) {
		return WorldHelper.getBlockEntity(world, pos, BarrelBlockEntity.class)
				.map(be -> {

					ModelData.Builder builder = ModelData.builder();
					boolean hasMainColor = be.getStorageWrapper().hasMainColor();
					builder.with(HAS_MAIN_COLOR, hasMainColor);
					boolean hasAccentColor = be.getStorageWrapper().hasAccentColor();
					builder.with(HAS_ACCENT_COLOR, hasAccentColor);
					if (!be.hasFullyDynamicRenderer()) {
						builder.with(DISPLAY_ITEMS, be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems());
						builder.with(INACCESSIBLE_SLOTS, be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getInaccessibleSlots());
					}
					builder.with(IS_PACKED, be.isPacked());
					builder.with(SHOWS_LOCK, be.isLocked() && be.shouldShowLock());
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

	private static class BarrelItemOverrides extends ItemOverrides {
		private final BarrelBakedModelBase barrelBakedModel;
		@Nullable
		private final BakedModel flatTopModel;

		public BarrelItemOverrides(BarrelBakedModelBase barrelBakedModel, @Nullable BakedModel flatTopModel) {
			this.barrelBakedModel = barrelBakedModel;
			this.flatTopModel = flatTopModel;
		}

		@Nullable
		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			boolean flatTop = BarrelBlockItem.isFlatTop(stack);
			if (flatTopModel != null && flatTop) {
				return flatTopModel.getOverrides().resolve(flatTopModel, stack, level, entity, seed);
			}

			barrelBakedModel.barrelHasMainColor = StorageBlockItem.getMainColorFromStack(stack).isPresent();
			barrelBakedModel.barrelHasAccentColor = StorageBlockItem.getAccentColorFromStack(stack).isPresent();
			barrelBakedModel.barrelWoodName = WoodStorageBlockItem.getWoodType(stack).map(WoodType::name)
					.orElse(barrelBakedModel.barrelHasAccentColor && barrelBakedModel.barrelHasMainColor ? null : WoodType.ACACIA.name());
			barrelBakedModel.barrelIsPacked = WoodStorageBlockItem.isPacked(stack);
			barrelBakedModel.barrelItem = stack.getItem();
			barrelBakedModel.flatTop = flatTop;
			return barrelBakedModel;
		}
	}
}
