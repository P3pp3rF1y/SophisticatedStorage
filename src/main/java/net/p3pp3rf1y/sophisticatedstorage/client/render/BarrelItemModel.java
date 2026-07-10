package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BarrelItemModel(BakedModel model, @Nullable BakedModel flatTopModel, List<ItemTintSource> tints) implements ItemModel {
	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
			@Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
		if (!(model instanceof BarrelBakedModelBase barrelBakedModel)) {
			throw new IllegalStateException("BarrelItemModel must be used with BarrelBakedModelBase");
		}

		boolean flatTop = BarrelBlockItem.isFlatTop(stack);
		BarrelBakedModelBase updatedModel = flatTopModel instanceof BarrelBakedModelBase flatTopBakedModel && flatTop ? flatTopBakedModel : barrelBakedModel;

		Optional<WoodType> woodType = WoodStorageBlockItem.getWoodType(stack);
		boolean isGenericWood = woodType.map(GenericWoodStorageHelper::isGenericWood).orElse(false);
		boolean hasMainColor = StorageBlockItem.getMainColorFromComponentHolder(stack).isPresent() || isGenericWood;
		updatedModel.setHasMainColor(hasMainColor);
		boolean hasAccentColor = StorageBlockItem.getAccentColorFromComponentHolder(stack).isPresent() || isGenericWood;
		updatedModel.setHasAccentColor(hasAccentColor);
		Map<BarrelMaterial, ResourceLocation> materials = BarrelBlockItem.getMaterials(stack);
		updatedModel.setBarrelMaterials(materials);
		String woodName = isGenericWood
				? null
				: woodType.map(WoodType::name).orElse(hasMainColor && hasAccentColor && materials.isEmpty() ? null : WoodType.ACACIA.name());
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

		layerState.setupBlockModel(updatedModel, updatedModel.getRenderType(stack));
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
			BakedModel bake = context.blockModelBaker().bake(model, BlockModelRotation.X0_Y0);
			BakedModel bakedFlatTopModel = flatTopModel == null ? null : context.blockModelBaker().bake(flatTopModel, BlockModelRotation.X0_Y0);
			return new BarrelItemModel(bake, bakedFlatTopModel, tints);
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			resolver.resolve(model);
		}

		public List<ItemTintSource> tints() {
			return tints;
		}
	}
}
