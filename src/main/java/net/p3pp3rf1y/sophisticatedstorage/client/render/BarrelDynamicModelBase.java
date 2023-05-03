package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BarrelDynamicModelBase<T extends BarrelDynamicModelBase<T>> implements IUnbakedGeometry<T> {
	private static final Map<Integer, BakedModel> BAKED_PART_MODELS = new HashMap<>();

	public static void invalidateCache() {
		BAKED_PART_MODELS.clear();
	}

	private final Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartdefinitions;

	@Nullable
	private final ResourceLocation parentLocation;

	@Nullable
	private BarrelDynamicModelBase<?> parent;

	@Nullable
	private final ResourceLocation flatTopModelName;

	protected BarrelDynamicModelBase(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartdefinitions, @Nullable ResourceLocation flatTopModelName) {
		this.parentLocation = parentLocation;
		this.woodModelPartdefinitions = woodModelPartdefinitions;
		this.flatTopModelName = flatTopModelName;
	}

	@Override
	public BakedModel bake(IGeometryBakingContext owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		ImmutableMap.Builder<String, Map<BarrelModelPart, UnbakedModel>> woodModelsBuilder = ImmutableMap.builder();

		woodModelPartdefinitions.forEach((woodName, woodModelPartDefinitions) -> {
			ImmutableMap.Builder<BarrelModelPart, UnbakedModel> modelsBuilder = ImmutableMap.builder();
			woodModelPartDefinitions.forEach((barrelPart, barrelPartDefinition) ->
					barrelPartDefinition.modelLocation().ifPresent(partModelLocation -> {
						Map<String, Either<Material, String>> materials = new HashMap<>();

						barrelPartDefinition.textures().forEach((textureName, texture) -> materials.put(textureName, Either.left(texture)));

						modelsBuilder.put(barrelPart, new BlockModel(partModelLocation, Collections.emptyList(), materials, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList()));

					}));
			woodModelsBuilder.put(woodName, modelsBuilder.build());
		});

		ImmutableMap<String, Map<BarrelModelPart, UnbakedModel>> woodModels = woodModelsBuilder.build();

		ImmutableMap.Builder<String, Map<BarrelModelPart, BakedModel>> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, partModels) -> {
			ImmutableMap.Builder<BarrelModelPart, BakedModel> partBuilder = ImmutableMap.builder();
			partModels.forEach((part, model) -> {
				int hash = getBakedModelHash(model, bakery, modelTransform);

				overrideCompositeModelPartsTextures(model);

				BakedModel bakedModel = BAKED_PART_MODELS.computeIfAbsent(hash, h -> model.bake(bakery, spriteGetter, modelTransform, modelLocation));
				if (bakedModel != null) {
					partBuilder.put(part, bakedModel);
				}
			});
			builder.put(woodName, partBuilder.build());
		});

		ImmutableMap<String, Map<BarrelModelPart, BakedModel>> woodModelParts = builder.build();

		BakedModel flatTopModel = getFlatTopModelName().map(modelName -> bakery.getModel(modelName).bake(bakery, spriteGetter, modelTransform, modelLocation)).orElse(null);
		return instantiateBakedModel(woodModelParts, flatTopModel);
	}

	private Optional<ResourceLocation> getFlatTopModelName() {
		for (BarrelDynamicModelBase<?> model = this; model != null; model = model.parent) {
			if (model.flatTopModelName != null) {
				return Optional.of(model.flatTopModelName);
			}
		}

		return Optional.empty();
	}

	@SuppressWarnings("java:S5803") //need to use textureMap here to update the customData geometry textures with that
	private static void overrideCompositeModelPartsTextures(UnbakedModel model) {
		if (model instanceof BlockModel blockModel && blockModel.customData.hasCustomGeometry() && blockModel.customData.getCustomGeometry() instanceof SimpleCompositeModel geometry) {
			geometry.overrideTextures(blockModel.textureMap);
		}
	}

	private int getBakedModelHash(UnbakedModel model, ModelBakery bakery, ModelState modelTransform) {
		int hash = 0;
		for (ResourceLocation dependency : model.getDependencies()) {
			hash = 31 * hash + dependency.hashCode();
		}

		Set<Pair<String, String>> missingTextures = new HashSet<>();
		for (Material material : model.getMaterials(bakery::getModel, missingTextures)) {
			hash = 31 * hash + material.texture().hashCode();
		}
		for (Pair<String, String> missingTexture : missingTextures) {
			hash = 31 * hash + missingTexture.getFirst().hashCode();
		}

		hash = 31 * hash + Boolean.valueOf(modelTransform.isUvLocked()).hashCode();

		Transformation rotation = modelTransform.getRotation();
		hash = 31 * hash + rotation.getMatrix().hashCode();
		hash = 31 * hash + rotation.getTranslation().hashCode();
		hash = 31 * hash + rotation.getLeftRotation().hashCode(); //need to both use the base leftRotation hashCode as well as toXYZ one as there are different cases where one of these is the same between east and west
		hash = 31 * hash + rotation.getLeftRotation().toXYZ().hashCode();
		hash = 31 * hash + rotation.getScale().hashCode();

		return hash;
	}

	protected abstract BarrelBakedModelBase instantiateBakedModel(Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel);

	@Override
	public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		visitAndUpdateParents(modelGetter);

		updatePartDefinitionsFromParents();

		Set<Material> materials = new HashSet<>();

		woodModelPartdefinitions.forEach((woodType, definitions) -> definitions.forEach((part, definition) -> {
			definition.textures().forEach((textureName, material) -> materials.add(material));
			materials.addAll(modelGetter.apply(definition.modelLocation).getMaterials(modelGetter, missingTextureErrors));
		}));

		return materials;
	}

	private void updatePartDefinitionsFromParents() {
		for (BarrelDynamicModelBase<?> model = parent; model != null; model = model.parent) {
			updateWoodModelPartDefinitionsFromModel(model);
		}
	}

	private void updateWoodModelPartDefinitionsFromModel(BarrelDynamicModelBase<?> model) {
		model.woodModelPartdefinitions.forEach((woodType, parentModelDefinitions) -> {
			if (!woodModelPartdefinitions.containsKey(woodType)) {
				woodModelPartdefinitions.put(woodType, parentModelDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
			} else {
				parentModelDefinitions.forEach((part, definition) -> {
					if (!woodModelPartdefinitions.get(woodType).containsKey(part)) {
						woodModelPartdefinitions.get(woodType).put(part, definition.copy());
					} else {
						woodModelPartdefinitions.get(woodType).get(part).mergeMissing(definition);
					}
				});
			}
		});
	}

	private void visitAndUpdateParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
		Set<BarrelDynamicModelBase<?>> visitedModels = Sets.newLinkedHashSet();

		for (BarrelDynamicModelBase<?> currentModel = this; currentModel.parentLocation != null && currentModel.parent == null; currentModel = currentModel.parent) {
			visitedModels.add(currentModel);
			if (!updateModelParent(modelGetter, visitedModels, currentModel)) {
				break;
			}
		}
	}

	private boolean updateModelParent(Function<ResourceLocation, UnbakedModel> modelGetter, Set<BarrelDynamicModelBase<?>> visitedModels, BarrelDynamicModelBase<?> currentModel) {
		if (!(modelGetter.apply(currentModel.parentLocation) instanceof BlockModel parentBlockModel)) {
			SophisticatedStorage.LOGGER.warn("Parent '{}' doesn't hold a block model while loading '{}'", currentModel.parentLocation, currentModel);
			return false;
		}

		if (!(parentBlockModel.customData.getCustomGeometry() instanceof BarrelDynamicModelBase<?> parentModel)) {
			SophisticatedStorage.LOGGER.warn("Parent '{}' doesn't hold a barrel model of 'BarrelDynamicModelBase' while loading '{}'", parentBlockModel, currentModel);
			return false;
		}

		if (visitedModels.contains(parentModel)) {
			SophisticatedStorage.LOGGER.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", () -> currentModel,
					() -> visitedModels.stream().map(Object::toString).collect(Collectors.joining(" -> ")), () -> currentModel.parentLocation);
			return false;
		}

		currentModel.parent = parentModel;

		return true;
	}

	public abstract static class Loader<T extends BarrelDynamicModelBase<T>> implements IGeometryLoader<T> {
		@Override
		public T read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			ResourceLocation parentLocation = null;
			if (modelContents.has("parent")) {
				parentLocation = new ResourceLocation(modelContents.get("parent").getAsString());
			}

			Map<BarrelModelPart, BarrelModelPartDefinition> modelParts = new EnumMap<>(BarrelModelPart.class);
			if (modelContents.has("model_parts")) {
				JsonObject modelPartsJson = modelContents.getAsJsonObject("model_parts");
				for (Map.Entry<String, JsonElement> entry : modelPartsJson.entrySet()) {
					JsonObject modelPartJson = entry.getValue().getAsJsonObject();
					BarrelModelPart.getByNameOptional(entry.getKey())
							.ifPresent(part -> modelParts.put(part, BarrelModelPartDefinition.deserialize(modelPartJson)));
				}
			}

			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides = new HashMap<>();
			if (modelContents.has("wood_overrides")) {
				JsonObject woodOverridesJson = modelContents.getAsJsonObject("wood_overrides");
				for (Map.Entry<String, JsonElement> entry : woodOverridesJson.entrySet()) {
					JsonObject woodOverrideJson = entry.getValue().getAsJsonObject();
					Map<BarrelModelPart, BarrelModelPartDefinition> woodOverride = new EnumMap<>(BarrelModelPart.class);
					for (Map.Entry<String, JsonElement> woodModelParts : woodOverrideJson.entrySet()) {
						JsonObject modelPartJson = woodModelParts.getValue().getAsJsonObject();
						BarrelModelPart.getByNameOptional(woodModelParts.getKey())
								.ifPresent(part -> woodOverride.put(part, BarrelModelPartDefinition.deserialize(modelPartJson)));
					}
					woodOverrides.put(entry.getKey(), woodOverride);
				}
			}

			if (parentLocation == null && modelParts.isEmpty() && woodOverrides.isEmpty()) {
				SophisticatedStorage.LOGGER.warn("None of 'parent', 'model_parts' and 'wood_overrides' present in model definition");
			}

			mergeModelPartDefinitionsIntoWoodOnes(modelParts, woodOverrides);

			ResourceLocation flatTopModelName = null;
			if (modelContents.has("flat_top_model")) {
				flatTopModelName = new ResourceLocation(modelContents.get("flat_top_model").getAsString());
			}

			return instantiateModel(parentLocation, woodOverrides, flatTopModelName);
		}

		private void mergeModelPartDefinitionsIntoWoodOnes(Map<BarrelModelPart, BarrelModelPartDefinition> modelPartDefinitions, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartdefinitions) {
			for (BarrelModelPart part : BarrelModelPart.values()) {
				if (modelPartDefinitions.containsKey(part)) {
					WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> {
						String woodName = woodType.name().toLowerCase(Locale.ROOT);
						if (woodModelPartdefinitions.containsKey(woodName)) {
							Map<BarrelModelPart, BarrelModelPartDefinition> definitions = woodModelPartdefinitions.get(woodName);
							if (definitions.containsKey(part)) {
								definitions.get(part).mergeMissing(modelPartDefinitions.get(part));
							} else {
								definitions.put(part, modelPartDefinitions.get(part).copy());
							}
						} else {
							woodModelPartdefinitions.put(woodName, new EnumMap<>(Map.of(part, modelPartDefinitions.get(part).copy())));
						}
					});
				}
			}
		}

		protected abstract T instantiateModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, @Nullable ResourceLocation flatTopModelName);
	}

	public static final class BarrelModelPartDefinition {
		@Nullable
		private ResourceLocation modelLocation;
		private final Map<String, Material> textures;

		private BarrelModelPartDefinition(@Nullable ResourceLocation modelLocation, Map<String, Material> textures) {
			this.modelLocation = modelLocation;
			this.textures = textures;
		}

		public BarrelModelPartDefinition copy() {
			return new BarrelModelPartDefinition(modelLocation, new HashMap<>(textures));
		}

		public void mergeMissing(BarrelModelPartDefinition other) {
			if (other.modelLocation != null && modelLocation == null) {
				modelLocation = other.modelLocation;
			}

			other.textures.forEach((key, value) -> {
				if (!textures.containsKey(key)) {
					textures.put(key, value);
				}
			});
		}

		public static BarrelModelPartDefinition deserialize(JsonObject json) {
			ResourceLocation modelLocation = null;
			if (json.has("model")) {
				modelLocation = new ResourceLocation(json.get("model").getAsString());
			}
			Map<String, Material> textures = new HashMap<>();
			if (json.has("textures")) {
				JsonObject texturesJson = json.getAsJsonObject("textures");
				for (Map.Entry<String, JsonElement> entry : texturesJson.entrySet()) {
					textures.put(entry.getKey(), new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(entry.getValue().getAsString())));
				}
			}
			return new BarrelModelPartDefinition(modelLocation, textures);
		}

		public Optional<ResourceLocation> modelLocation() {
			return Optional.ofNullable(modelLocation);
		}

		public Map<String, Material> textures() {return textures;}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {return true;}
			if (obj == null || obj.getClass() != getClass()) {return false;}
			var that = (BarrelModelPartDefinition) obj;
			return Objects.equals(modelLocation, that.modelLocation) &&
					Objects.equals(textures, that.textures);
		}

		@Override
		public int hashCode() {
			return Objects.hash(modelLocation, textures);
		}

		@Override
		public String toString() {
			return "BarrelModelPartDefinition[" +
					"modelLocation=" + modelLocation + ", " +
					"textures=" + textures + ']';
		}

	}
}
