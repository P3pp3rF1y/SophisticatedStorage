package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BarrelDynamicModelBase<T extends BarrelDynamicModelBase<T>> implements IUnbakedGeometry<T> {
	private static final Map<Integer, BakedModel> BAKED_PART_MODELS = new HashMap<>();
	private static final String REFERENCE_PREFIX = "reference/";

	public static void invalidateCache() {
		BAKED_PART_MODELS.clear();
	}

	private final Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartDefinitions;

	@Nullable
	private final ResourceLocation parentLocation;

	@Nullable
	private BarrelDynamicModelBase<?> parent;

	@Nullable
	private final ResourceLocation flatTopModelName;
	private final Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels;
	private final Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions;

	protected BarrelDynamicModelBase(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartDefinitions,
									 @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		this.parentLocation = parentLocation;
		this.woodModelPartDefinitions = woodModelPartDefinitions;
		this.flatTopModelName = flatTopModelName;
		this.dynamicPartModels = dynamicPartModels;
		this.woodPartitionedModelPartDefinitions = woodPartitionedModelPartDefinitions;
	}

	@Override
	public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts = bakeWoodModelParts(baker, spriteGetter, modelTransform, modelLocation, woodModelPartDefinitions);
		Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData = getDynamicBakingData(modelTransform, modelLocation);

		copyAndResolveTextures(woodModelPartDefinitions, woodPartitionedModelPartDefinitions);

		Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts = bakeWoodModelParts(baker, spriteGetter, modelTransform, modelLocation, woodPartitionedModelPartDefinitions);
		BakedModel flatTopModel = getFlatTopModelName().map(modelName -> baker.getModel(modelName).bake(baker, spriteGetter, modelTransform, modelLocation)).orElse(null);

		return instantiateBakedModel(baker, woodModelParts, flatTopModel, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private void copyAndResolveTextures(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		copyTextures(woodOverrides, partitionedWoodOverrides);
		resolveTextureReferences(partitionedWoodOverrides);
	}

	private static void resolveTextureReferences(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		partitionedWoodOverrides.values().forEach(partDefinitions -> partDefinitions.values().forEach(definition -> {
			Map<String, Material> replacements = new HashMap<>();
			definition.textures.forEach((key, value) -> {
				String path = value.texture().getPath();
				if (value.texture().getNamespace().equals("minecraft") && path.startsWith(REFERENCE_PREFIX)) {
					String referredTextureName = path.substring(REFERENCE_PREFIX.length());
					if (definition.textures().containsKey(referredTextureName)) {
						replacements.put(key, definition.textures.get(referredTextureName));
					}
				}
			});
			definition.textures.putAll(replacements);
		}));
	}

	private static void copyTextures(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		woodOverrides.forEach((woodType, partDefinitions) -> {
			if (partitionedWoodOverrides.containsKey(woodType)) {
				Map<BarrelModelPart, BarrelModelPartDefinition> partitionedWoodOverride = partitionedWoodOverrides.get(woodType);
				partDefinitions.forEach((part, definition) -> {
					if (partitionedWoodOverride.containsKey(part)) {
						partitionedWoodOverride.get(part).textures.putAll(definition.textures);
					} else {
						partitionedWoodOverride.put(part, new BarrelModelPartDefinition(null, new HashMap<>(definition.textures())));
					}
				});
			}
		});
	}

	private Map<String, Map<BarrelModelPart, UnbakedModel>> createUnbakedWoodModelParts(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> definitions) {
		ImmutableMap.Builder<String, Map<BarrelModelPart, UnbakedModel>> woodModelsBuilder = ImmutableMap.builder();

		definitions.forEach((woodName, woodDefinitions) -> {
			ImmutableMap.Builder<BarrelModelPart, UnbakedModel> modelsBuilder = ImmutableMap.builder();
			woodDefinitions.forEach((barrelPart, barrelPartDefinition) ->
					barrelPartDefinition.modelLocation().ifPresent(partModelLocation -> {
						Map<String, Either<Material, String>> materials = new HashMap<>();
						barrelPartDefinition.textures().forEach((textureName, texture) -> materials.put(textureName, Either.left(texture)));
						modelsBuilder.put(barrelPart, new CompositeElementsModel(partModelLocation, materials));
					}));
			woodModelsBuilder.put(woodName, modelsBuilder.build());
		});

		return woodModelsBuilder.build();
	}

	private Map<String, Map<BarrelModelPart, BakedModel>> bakeWoodModelParts(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
																			 ModelState modelTransform, ResourceLocation modelLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> definitions) {
		Map<String, Map<BarrelModelPart, UnbakedModel>> woodModels = createUnbakedWoodModelParts(definitions);

		ImmutableMap.Builder<String, Map<BarrelModelPart, BakedModel>> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, partModels) -> {
			ImmutableMap.Builder<BarrelModelPart, BakedModel> partBuilder = ImmutableMap.builder();
			partModels.forEach((part, model) -> {
				model.resolveParents(baker::getModel);
				int hash = getBakedModelHash(model, modelTransform, part);
				BakedModel bakedModel = BAKED_PART_MODELS.computeIfAbsent(hash, h -> model.bake(baker, spriteGetter, modelTransform, modelLocation));
				if (bakedModel != null) {
					partBuilder.put(part, bakedModel);
				}
			});
			builder.put(woodName, partBuilder.build());
		});

		return builder.build();
	}

	private Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> getDynamicBakingData(ModelState modelTransform, ResourceLocation modelLocation) {
		Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData = new HashMap<>();
		woodModelPartDefinitions.forEach((woodName, partDefinitions) -> {
			Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> dynamicPartBakingData = new EnumMap<>(DynamicBarrelBakingData.DynamicPart.class);
			dynamicPartModels.forEach((dynamicPart, dynamicPartModel) ->
					dynamicPartBakingData.put(dynamicPart, new DynamicBarrelBakingData(new BarrelModelPartDefinition(dynamicPartModel, partDefinitions.get(BarrelModelPart.BASE).textures()), modelTransform, modelLocation)));
			woodDynamicBakingData.put(woodName, dynamicPartBakingData);
		});
		return woodDynamicBakingData;
	}

	private Optional<ResourceLocation> getFlatTopModelName() {
		for (BarrelDynamicModelBase<?> model = this; model != null; model = model.parent) {
			if (model.flatTopModelName != null) {
				return Optional.of(model.flatTopModelName);
			}
		}

		return Optional.empty();
	}

	@SuppressWarnings("java:S5803") //need to use textureMap to calculate hash based on it as well
	private int getBakedModelHash(UnbakedModel model, ModelState modelTransform, BarrelModelPart part) {
		int hash = part.hashCode();
		for (ResourceLocation dependency : model.getDependencies()) {
			hash = 31 * hash + dependency.hashCode();
		}

		if (model instanceof BlockModel blockModel) {
			for (Either<Material, String> material : blockModel.textureMap.values()) {
				Optional<Material> mat = material.left();
				if (mat.isPresent()) {
					hash = 31 * hash + mat.get().hashCode();
				}
			}
		}

		if (model instanceof CompositeElementsModel compositeElementsModel) {
			for (Map.Entry<String, Either<Material, String>> entry : compositeElementsModel.textureMap.entrySet()) {
				hash = 31 * hash + entry.getKey().hashCode();
				hash = 31 * hash + entry.getValue().hashCode();
			}
		}

		hash = 31 * hash + Boolean.valueOf(modelTransform.isUvLocked()).hashCode();

		Transformation rotation = modelTransform.getRotation();
		hash = 31 * hash + rotation.getMatrix().hashCode();
		hash = 31 * hash + rotation.getTranslation().hashCode();
		Quaternionf leftRotation = rotation.getLeftRotation();
		hash = 31 * hash + leftRotation.hashCode();
		hash = 31 * hash + Objects.hash(leftRotation.x(), leftRotation.y(), leftRotation.z());
		hash = 31 * hash + rotation.getScale().hashCode();

		return hash;
	}

	protected abstract BarrelBakedModelBase instantiateBakedModel(ModelBaker baker, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel,
																  Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts);

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
		visitAndUpdateParents(modelGetter);

		updatePartDefinitionsFromParents();
		updatePartitionedPartDefinitionsFromParents();
		updateDynamicPartModelsFromParents();
	}

	private void updateDynamicPartModelsFromParents() {
		for (BarrelDynamicModelBase<?> model = parent; model != null; model = model.parent) {
			updateDynamicPartModelsFromModel(model);
		}
	}

	private void updateDynamicPartModelsFromModel(BarrelDynamicModelBase<?> model) {
		model.dynamicPartModels.forEach((dynamicPart, dynamicPartModel) -> {
			if (!dynamicPartModels.containsKey(dynamicPart)) {
				dynamicPartModels.put(dynamicPart, dynamicPartModel);
			}
		});
	}

	private void updatePartDefinitionsFromParents() {
		for (BarrelDynamicModelBase<?> model = parent; model != null; model = model.parent) {
			updateWoodModelPartDefinitionsFromModel(model);
		}
	}

	private void updatePartitionedPartDefinitionsFromParents() {
		for (BarrelDynamicModelBase<?> model = parent; model != null; model = model.parent) {
			updateWoodPartitionedModelPartDefinitionsFromModel(model);
		}
	}

	private void updateWoodModelPartDefinitionsFromModel(BarrelDynamicModelBase<?> model) {
		model.woodModelPartDefinitions.forEach((woodType, parentModelDefinitions) -> {
			if (!woodModelPartDefinitions.containsKey(woodType)) {
				woodModelPartDefinitions.put(woodType, parentModelDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
			} else {
				parentModelDefinitions.forEach((part, definition) -> {
					if (!woodModelPartDefinitions.get(woodType).containsKey(part)) {
						woodModelPartDefinitions.get(woodType).put(part, definition.copy());
					} else {
						woodModelPartDefinitions.get(woodType).get(part).mergeMissing(definition);
					}
				});
			}
		});
	}

	private void updateWoodPartitionedModelPartDefinitionsFromModel(BarrelDynamicModelBase<?> model) {
		model.woodPartitionedModelPartDefinitions.forEach((woodType, parentModelDefinitions) -> {
			if (!woodPartitionedModelPartDefinitions.containsKey(woodType)) {
				woodPartitionedModelPartDefinitions.put(woodType, parentModelDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
			} else {
				parentModelDefinitions.forEach((part, definition) -> {
					if (!woodPartitionedModelPartDefinitions.get(woodType).containsKey(part)) {
						woodPartitionedModelPartDefinitions.get(woodType).put(part, definition.copy());
					} else {
						woodPartitionedModelPartDefinitions.get(woodType).get(part).mergeMissing(definition);
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
				parentLocation = ResourceLocation.fromNamespaceAndPath(modelContents.get("parent").getAsString());
			}

			Map<BarrelModelPart, BarrelModelPartDefinition> modelParts = readModelParts(modelContents, "model_parts");
			Map<BarrelModelPart, BarrelModelPartDefinition> partitionedModelParts = readModelParts(modelContents, "partitioned_model_parts");
			Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels = readDynamicPartModels(modelContents);
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides = readWoodOverrides(modelContents);

			if (parentLocation == null && modelParts.isEmpty() && woodOverrides.isEmpty() && dynamicPartModels.isEmpty()) {
				SophisticatedStorage.LOGGER.warn("None of 'parent', 'model_parts' and 'wood_overrides' present in model definition");
			}

			mergeModelPartDefinitionsIntoWoodOnes(modelParts, woodOverrides);

			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions = readWoodOverrides(modelContents, Set.of(BarrelModelPart.BASE, BarrelModelPart.BASE_OPEN));
			mergeModelPartDefinitionsIntoWoodOnes(partitionedModelParts, woodPartitionedModelPartDefinitions);

			ResourceLocation flatTopModelName = readFlatTopModel(modelContents);

			return instantiateModel(parentLocation, woodOverrides, flatTopModelName, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}

		@Nullable
		private static ResourceLocation readFlatTopModel(JsonObject modelContents) {
			ResourceLocation flatTopModelName = null;
			if (modelContents.has("flat_top_model")) {
				flatTopModelName = ResourceLocation.fromNamespaceAndPath(modelContents.get("flat_top_model").getAsString());
			}
			return flatTopModelName;
		}

		private static Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> readWoodOverrides(JsonObject modelContents) {
			return readWoodOverrides(modelContents, Collections.emptySet());
		}

		private static Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> readWoodOverrides(JsonObject modelContents, Set<BarrelModelPart> partsToIgnore) {
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides = new HashMap<>();
			if (modelContents.has("wood_overrides")) {
				JsonObject woodOverridesJson = modelContents.getAsJsonObject("wood_overrides");
				for (Map.Entry<String, JsonElement> entry : woodOverridesJson.entrySet()) {
					JsonObject woodOverrideJson = entry.getValue().getAsJsonObject();
					Map<BarrelModelPart, BarrelModelPartDefinition> woodOverride = new EnumMap<>(BarrelModelPart.class);
					for (Map.Entry<String, JsonElement> woodModelParts : woodOverrideJson.entrySet()) {
						JsonObject modelPartJson = woodModelParts.getValue().getAsJsonObject();
						BarrelModelPart.getByNameOptional(woodModelParts.getKey())
								.ifPresent(part -> {
									if (partsToIgnore.contains(part)) {
										return;
									}
									woodOverride.put(part, BarrelModelPartDefinition.deserialize(modelPartJson));
								});
					}
					woodOverrides.put(entry.getKey(), woodOverride);
				}
			}
			return woodOverrides;
		}

		private static Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> readDynamicPartModels(JsonObject modelContents) {
			Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels = new EnumMap<>(DynamicBarrelBakingData.DynamicPart.class);
			if (modelContents.has("dynamic_part_models")) {
				JsonObject dynamicPartsJson = modelContents.getAsJsonObject("dynamic_part_models");
				for (Map.Entry<String, JsonElement> entry : dynamicPartsJson.entrySet()) {
					DynamicBarrelBakingData.DynamicPart.getByNameOptional(entry.getKey()).ifPresent(part ->
							dynamicPartModels.put(part, ResourceLocation.fromNamespaceAndPath(entry.getValue().getAsString())));
				}
			}
			return dynamicPartModels;
		}

		private static Map<BarrelModelPart, BarrelModelPartDefinition> readModelParts(JsonObject modelContents, String memberName) {
			Map<BarrelModelPart, BarrelModelPartDefinition> modelParts = new EnumMap<>(BarrelModelPart.class);
			if (modelContents.has(memberName)) {
				JsonObject modelPartsJson = modelContents.getAsJsonObject(memberName);
				for (Map.Entry<String, JsonElement> entry : modelPartsJson.entrySet()) {
					JsonObject modelPartJson = entry.getValue().getAsJsonObject();
					BarrelModelPart.getByNameOptional(entry.getKey())
							.ifPresent(part -> modelParts.put(part, BarrelModelPartDefinition.deserialize(modelPartJson)));
				}
			}
			return modelParts;
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

		protected abstract T instantiateModel(@Nullable ResourceLocation parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
											  @Nullable ResourceLocation flatTopModelName, Map<DynamicBarrelBakingData.DynamicPart, ResourceLocation> dynamicPartModels, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides);
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
				modelLocation = ResourceLocation.fromNamespaceAndPath(json.get("model").getAsString());
			}
			Map<String, Material> textures = new HashMap<>();
			if (json.has("textures")) {
				JsonObject texturesJson = json.getAsJsonObject("textures");
				for (Map.Entry<String, JsonElement> entry : texturesJson.entrySet()) {
					String textureName = entry.getValue().getAsString();
					if (textureName.startsWith("#")) {
						textureName = REFERENCE_PREFIX + textureName.substring(1);
					}
					textures.put(entry.getKey(), new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath(textureName)));
				}
			}
			return new BarrelModelPartDefinition(modelLocation, textures);
		}

		public Optional<ResourceLocation> modelLocation() {
			return Optional.ofNullable(modelLocation);
		}

		public Map<String, Material> textures() {
			return textures;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
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
