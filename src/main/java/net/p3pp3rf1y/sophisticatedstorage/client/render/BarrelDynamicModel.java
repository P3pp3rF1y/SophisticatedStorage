package net.p3pp3rf1y.sophisticatedstorage.client.render;

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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class BarrelDynamicModel implements IModelGeometry<BarrelDynamicModel> {
	public static final Map<String, Map<String, ResourceLocation>> WOOD_TEXTURES = new HashMap<>();

	private static final String BLOCK_FOLDER = "block/";

	private static final String TOP_OPEN_TEXTURE_NAME = "top_open";

	static {
		BarrelBlock.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> {
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

	public BarrelDynamicModel(Map<String, UnbakedModel> woodModels, Map<ModelPart, UnbakedModel> additionalModelParts) {
		this.woodModels = woodModels;
		this.additionalModelParts = additionalModelParts;
	}

	@Override
	public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
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
	public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		ImmutableSet.Builder<Material> builder = ImmutableSet.builder();
		woodModels.forEach((woodName, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors)));
		additionalModelParts.forEach((modelPart, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors)));
		return builder.build();
	}

	private static class BarrelBakedModel implements IDynamicBakedModel {
		private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
		private static final Map<ItemTransforms.TransformType, Transformation> TRANSFORMS;
		private final Map<String, BakedModel> woodModels;
		private final Map<ModelPart, BakedModel> additionalModelParts;
		private final ItemOverrides barrelItemOverrides = new BarrelItemOverrides(this);
		@Nullable
		private String barrelWoodName = null;
		private boolean barrelHasMainColor = false;
		private boolean barrelHasAccentColor = false;

		public BarrelBakedModel(Map<String, BakedModel> woodModels, Map<ModelPart, BakedModel> additionalModelParts) {
			this.woodModels = woodModels;
			this.additionalModelParts = additionalModelParts;
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
		}

		@NotNull
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand, @NotNull IModelData extraData) {
			String woodName = null;
			boolean hasMainColor;
			boolean hasAccentColor;
			if (state != null) {
				hasMainColor = Boolean.TRUE.equals(extraData.getData(HAS_MAIN_COLOR));
				hasAccentColor = Boolean.TRUE.equals(extraData.getData(HAS_ACCENT_COLOR));
				if (extraData.hasProperty(WOOD_NAME)) {
					woodName = extraData.getData(WOOD_NAME);
					if (Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN))) {
						woodName += "_open";
					}
				}
			} else {
				woodName = barrelWoodName;
				hasMainColor = barrelHasMainColor;
				hasAccentColor = barrelHasAccentColor;
			}

			List<BakedQuad> ret = new ArrayList<>();

			if (!hasMainColor || !hasAccentColor) {
				addWoodModelQuads(state, side, rand, ret, woodName);
			}

			addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor);

			ret.addAll(additionalModelParts.get(ModelPart.TIER).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			return ret;
		}

		private void addTintableModelQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand, List<BakedQuad> ret, boolean hasMainColor, boolean hasAccentColor) {
			if (hasMainColor) {
				ModelPart modePart = state != null && state.getValue(BarrelBlock.OPEN) ? ModelPart.MAIN_OPEN : ModelPart.MAIN;
				ret.addAll(additionalModelParts.get(modePart).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			}
			if (hasAccentColor) {
				ret.addAll(additionalModelParts.get(ModelPart.ACCENT).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			}

			if (hasMainColor || hasAccentColor) {
				ret.addAll(additionalModelParts.get(ModelPart.METAL_BANDS).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			}
		}

		private void addWoodModelQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand, List<BakedQuad> ret, @Nullable String woodName) {
			if (woodName == null) {
				return;
			}

			if (woodModels.containsKey(woodName)) {
				ret.addAll(woodModels.get(woodName).getQuads(state, side, rand, EmptyModelData.INSTANCE));
			} else {
				ret.addAll(woodModels.values().iterator().next().getQuads(state, side, rand, EmptyModelData.INSTANCE));
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

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return woodModels.values().iterator().next().getParticleIcon();
		}

		@Override
		public TextureAtlasSprite getParticleIcon(@NotNull IModelData data) {
			if (data.hasProperty(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.getData(HAS_MAIN_COLOR))) {
				return additionalModelParts.get(ModelPart.MAIN).getParticleIcon(data);
			} else if (data.hasProperty(WOOD_NAME)) {
				String name = data.getData(WOOD_NAME);
				if (!woodModels.containsKey(name)) {
					return getParticleIcon();
				}
				return woodModels.get(name).getParticleIcon(data);
			}
			return getParticleIcon();
		}

		@NotNull
		@Override
		public IModelData getModelData(@NotNull BlockAndTintGetter world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData tileData) {
			return WorldHelper.getBlockEntity(world, pos, StorageBlockEntity.class)
					.map(be -> {
						ModelDataMap.Builder builder = new ModelDataMap.Builder();
						builder.withInitial(HAS_MAIN_COLOR, be.getMainColor() > -1);
						builder.withInitial(HAS_ACCENT_COLOR, be.getAccentColor() > -1);
						be.getWoodType().ifPresent(n -> builder.withInitial(WOOD_NAME, n.name()));
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
			barrelBakedModel.barrelWoodName = BarrelBlock.getWoodType(stack).map(WoodType::name).orElse(null);
			barrelBakedModel.barrelHasMainColor =  BarrelBlock.getMaincolorFromStack(stack).isPresent();
			barrelBakedModel.barrelHasAccentColor = BarrelBlock.getAccentColorFromStack(stack).isPresent();
			return barrelBakedModel;
		}
	}

	public static final class Loader implements IModelLoader<BarrelDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public BarrelDynamicModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
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

		@Override
		public void onResourceManagerReload(ResourceManager pResourceManager) {
			//noop
		}
	}

	private enum ModelPart {
		METAL_BANDS(SophisticatedStorage.getRL("block/barrel_metal_bands")),
		ACCENT(SophisticatedStorage.getRL("block/barrel_tintable_accent")),
		MAIN(SophisticatedStorage.getRL("block/barrel_tintable_main")),
		MAIN_OPEN(SophisticatedStorage.getRL("block/barrel_tintable_main_open")),
		TIER(new ResourceLocation("minecraft:block/cube_bottom_top"));

		private final ResourceLocation modelName;

		ModelPart(ResourceLocation modelName) {
			this.modelName = modelName;
		}
	}
}
