package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record BarrelItemModel(BarrelBlockStateModelBase model, @Nullable BarrelBlockStateModelBase flatTopModel, List<ItemTintSource> tints,
		Supplier<Vector3f[]> extents) implements ItemModel {
	private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final ItemTransforms ITEM_TRANSFORMS = createItemTransforms();

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

	public BarrelItemModel(BarrelBlockStateModelBase model, @Nullable BarrelBlockStateModelBase flatTopModel, List<ItemTintSource> tints) {
		this(model, flatTopModel, tints, Suppliers.memoize(() -> BlockModelWrapper.computeExtents(model.getTierQuads().getAll())));
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
			@Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
		boolean flatTop = BarrelBlockItem.isFlatTop(stack);
		BarrelBlockStateModelBase updatedModel = flatTop && flatTopModel != null ? flatTopModel : model;

		boolean hasMainColor = StorageBlockItem.getMainColorFromComponentHolder(stack).isPresent();
		updatedModel.setHasMainColor(hasMainColor);
		boolean hasAccentColor = StorageBlockItem.getAccentColorFromComponentHolder(stack).isPresent();
		updatedModel.setHasAccentColor(hasAccentColor);
		Map<BarrelMaterial, ResourceLocation> materials = BarrelBlockItem.getMaterials(stack);
		updatedModel.setBarrelMaterials(materials);
		String woodName = WoodStorageBlockItem.getWoodType(stack).map(WoodType::name)
				.orElse(hasMainColor && hasAccentColor && materials.isEmpty() ? null : WoodType.ACACIA.name());
		updatedModel.setWoodName(woodName);
		boolean packed = WoodStorageBlockItem.isPacked(stack);
		updatedModel.setPacked(packed);
		updatedModel.setFlatTop(flatTop);
		updatedModel.setShowsTier(StorageBlockItem.showsTier(stack));
		updatedModel.setBarrelItem(stack.getItem());

		ItemStackRenderState.LayerRenderState layerState = state.newLayer();
		int[] tintArray = new int[tints.size()];

		for (int j = 0; j < tintArray.length; ++j) {
			tintArray[j] = tints.get(j).calculate(stack, clientLevel, livingEntity);
		}
		int[] aint = layerState.prepareTintLayers(tintArray.length);
		System.arraycopy(tintArray, 0, aint, 0, tintArray.length);

		layerState.setExtents(extents);
		layerState.setUsesBlockLight(true);
		layerState.setRenderType(Sheets.translucentItemSheet());
		layerState.setParticleIcon(updatedModel.particleIcon());
		layerState.setTransform(ITEM_TRANSFORMS.getTransform(itemDisplayContext));
		layerState.prepareQuadList().addAll(updatedModel.getQuads(clientLevel != null ? clientLevel.random : Minecraft.getInstance().level.random));
	}

	public record Unbaked(ResourceLocation model, @Nullable ResourceLocation flatTopModel, List<ItemTintSource> tints) implements ItemModel.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder
				.mapCodec(
						instance -> instance
								.group(ResourceLocation.CODEC.fieldOf("model").forGetter(Unbaked::model),
										ResourceLocation.CODEC.optionalFieldOf("flat_top_model")
												.forGetter(unbaked -> Optional.ofNullable(unbaked.flatTopModel())),
										ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints))
								.apply(instance, (model, flatTopModel, tints) -> new Unbaked(model, flatTopModel.orElse(null), tints)));
		@Override
		public MapCodec<? extends ItemModel.Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ItemModel bake(BakingContext context) {
			ResolvedModel resolved = context.blockModelBaker().getModel(model);
			BarrelBlockStateModelBase barrelModel;
			if (resolved.wrapped() instanceof BarrelUnbakedModelBase barrelUnbakedModel) {
				barrelModel = barrelUnbakedModel.bakeBlockStateModel(context.blockModelBaker(), resolved, BlockModelRotation.X0_Y0);
			} else {
				throw new IllegalStateException("Expected BarrelUnbakedModelBase for " + model + ", got " + resolved.wrapped().getClass().getName());
			}

			BarrelBlockStateModelBase barrelFlatTopModel = null;
			if (flatTopModel != null) {
				ResolvedModel flatTopResolved = context.blockModelBaker().getModel(flatTopModel);
				if (flatTopResolved.wrapped() instanceof BarrelUnbakedModelBase flatTopUnbakedModel) {
					barrelFlatTopModel = flatTopUnbakedModel.bakeBlockStateModel(context.blockModelBaker(), flatTopResolved, BlockModelRotation.X0_Y0);
				}
			}

			return new BarrelItemModel(barrelModel, barrelFlatTopModel, tints);
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			resolver.markDependency(model);
			if (flatTopModel != null) {
				resolver.markDependency(flatTopModel);
			}
		}

		public List<ItemTintSource> tints() {
			return tints;
		}
	}
}
