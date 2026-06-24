package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BarrelUnbakedModelBase implements UnbakedModel {
	private static final Map<Integer, QuadCollection> BAKED_PART_MODELS = new ConcurrentHashMap<>();
	private static final String REFERENCE_PREFIX = "reference/";

	public static void invalidateCache() {
		BAKED_PART_MODELS.clear();
	}

	private final Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartDefinitions;

	@Nullable
	private final Identifier parentLocation;

	private final Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels;
	private final Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions;

	protected BarrelUnbakedModelBase(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartDefinitions,
			Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels,
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions) {
		this.parentLocation = parentLocation;
		this.woodModelPartDefinitions = woodModelPartDefinitions;
		this.dynamicPartModels = dynamicPartModels;
		this.woodPartitionedModelPartDefinitions = woodPartitionedModelPartDefinitions;
	}

	@Nullable
	@Override
	public Identifier parent() {
		return parentLocation;
	}

	private void copyAndResolveTextures(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		copyTextures(woodOverrides, partitionedWoodOverrides);
		resolveTextureReferences(partitionedWoodOverrides);
	}

	private static void resolveTextureReferences(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		partitionedWoodOverrides.values().forEach(partDefinitions -> partDefinitions.values().forEach(definition -> {
			Map<String, Material> replacements = new HashMap<>();
			definition.textures.forEach((key, value) -> {
				String path = value.sprite().getPath();
				if (value.sprite().getNamespace().equals("minecraft") && path.startsWith(REFERENCE_PREFIX)) {
					String referredTextureName = path.substring(REFERENCE_PREFIX.length());
					if (definition.textures().containsKey(referredTextureName)) {
						replacements.put(key, definition.textures.get(referredTextureName));
					}
				}
			});
			definition.textures.putAll(replacements);
		}));
	}

	private static void copyTextures(Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides) {
		woodOverrides.forEach((woodType, partDefinitions) -> {
			if (partitionedWoodOverrides.containsKey(woodType)) {
				Map<BarrelModelPart, BarrelModelPartDefinition> partitionedWoodOverride = partitionedWoodOverrides.get(woodType);
				partDefinitions.forEach((part, definition) -> {
					if (partitionedWoodOverride.containsKey(part)) {
						partitionedWoodOverride.get(part).textures.putAll(definition.textures);
					} else {
						partitionedWoodOverride.put(part, new BarrelModelPartDefinition(null, new ConcurrentHashMap<>(definition.textures())));
					}
				});
			}
		});
	}

	private Map<String, Map<BarrelModelPart, ResolvedModel>> createUnbakedWoodModelParts(
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> definitions, ModelBaker baker, ModelDebugName debugName) {
		ImmutableMap.Builder<String, Map<BarrelModelPart, ResolvedModel>> woodModelsBuilder = ImmutableMap.builder();

		definitions.forEach((woodName, woodDefinitions) -> {
			ImmutableMap.Builder<BarrelModelPart, ResolvedModel> modelsBuilder = ImmutableMap.builder();
			woodDefinitions.forEach((barrelPart, barrelPartDefinition) -> barrelPartDefinition.modelLocation().ifPresent(partModelLocation -> {
				TextureSlots.Data.Builder textureBuilder = new TextureSlots.Data.Builder();
				barrelPartDefinition.textures().forEach(textureBuilder::addTexture);
				String partName = barrelPart.name().toLowerCase(Locale.ROOT);
				ResolvedModel resolvedInlineModel = baker.resolveInlineModel(
						new CuboidModel(null, null, true, ItemTransforms.NO_TRANSFORMS, textureBuilder.build(), partModelLocation),
						() -> debugName.debugName() + "_" + woodName + "_" + partName);
				modelsBuilder.put(barrelPart, resolvedInlineModel);
			}));
			woodModelsBuilder.put(woodName, modelsBuilder.build());
		});

		return woodModelsBuilder.build();
	}

	private Map<String, Map<BarrelModelPart, QuadCollection>> bakeWoodModelParts(ModelBaker baker, ModelState modelState,
			Map<String, Map<BarrelModelPart, ResolvedModel>> woodModels) {
		ImmutableMap.Builder<String, Map<BarrelModelPart, QuadCollection>> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, partModels) -> {
			ImmutableMap.Builder<BarrelModelPart, QuadCollection> partBuilder = ImmutableMap.builder();
			partModels.forEach((part, model) -> {
				int hash = getBakedModelHash(model, modelState, part);
				QuadCollection quads = BAKED_PART_MODELS.computeIfAbsent(hash,
						h -> model.getTopGeometry().bake(findTopTextureSlots(model, baker), baker, modelState, model, ContextMap.EMPTY));
				if (!quads.getAll().isEmpty()) {
					partBuilder.put(part, quads);
				}
			});
			builder.put(woodName, partBuilder.build());
		});

		return builder.build();
	}

	private TextureSlots findTopTextureSlots(ResolvedModel model, ModelBaker baker) {
		TextureSlots.Resolver resolver = new TextureSlots.Resolver();
		ResolvedModel resolvedModel = model;
		while (resolvedModel != null) {
			resolver.addLast(resolvedModel.wrapped().textureSlots());
			if (resolvedModel.wrapped() instanceof SimpleCompositeUnbakedModel simpleCompositeModel) {
				simpleCompositeModel.children()
						.forEach((key, childModel) -> addTextureSlots(resolver,
								childModel.map(baker::getModel, inline -> baker.resolveInlineModel(inline, () -> model.debugName() + "_" + key)), baker,
								model.debugName() + "_" + key));
			}
			resolvedModel = resolvedModel.parent();
		}
		return resolver.resolve(model);
	}

	private void addTextureSlots(TextureSlots.Resolver resolver, ResolvedModel model, ModelBaker baker, String debugName) {
		ResolvedModel resolvedModel = model;
		while (resolvedModel != null) {
			resolver.addLast(resolvedModel.wrapped().textureSlots());
			if (resolvedModel.wrapped() instanceof SimpleCompositeUnbakedModel simpleCompositeModel) {
				simpleCompositeModel.children()
						.forEach((key, childModel) -> addTextureSlots(resolver,
								childModel.map(baker::getModel, inline -> baker.resolveInlineModel(inline, () -> debugName + "_" + key)), baker,
								debugName + "_" + key));
			}
			resolvedModel = resolvedModel.parent();
		}
	}

	private Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> getDynamicBakingData(ModelState modelTransform,
			ModelDebugName modelDebugName, Map<DynamicBarrelBakingData.DynamicPart, ResolvedModel> resolvedDynamicPartModels) {
		Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData = new HashMap<>();
		woodModelPartDefinitions.forEach((woodName, partDefinitions) -> {
			Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> dynamicPartBakingData = new EnumMap<>(DynamicBarrelBakingData.DynamicPart.class);
			dynamicPartModels.forEach((dynamicPart, dynamicPartModel) -> dynamicPartBakingData.put(dynamicPart,
					new DynamicBarrelBakingData(resolvedDynamicPartModels.get(dynamicPart).wrapped(), partDefinitions.get(BarrelModelPart.BASE).textures(),
							modelTransform, modelDebugName)));
			woodDynamicBakingData.put(woodName, dynamicPartBakingData);
		});
		return woodDynamicBakingData;
	}

	@SuppressWarnings("java:S5803") // need to use textureMap to calculate hash based on it as well
	private int getBakedModelHash(ResolvedModel model, ModelState modelTransform, BarrelModelPart part) {
		int hash = part.hashCode();

		hash = 31 * hash + getDepHash(model);

		for (TextureSlots.SlotContents material : model.wrapped().textureSlots().values().values()) {
			hash = 31 * hash + material.hashCode();
		}

		Transformation transformation = modelTransform.transformation();
		hash = 31 * hash + transformation.getMatrix().hashCode();
		hash = 31 * hash + transformation.translation().hashCode();
		hash = 31 * hash + robustHash(transformation.rightRotation());
		hash = 31 * hash + robustHash(transformation.leftRotation());
		hash = 31 * hash + transformation.scale().hashCode();
		return hash;
	}

	public static int robustHash(Quaternionfc q) {
		long h = 1;
		h = 31 * h + Float.floatToIntBits(q.w());
		h = 31 * h + Float.floatToIntBits(q.x());
		h = 31 * h + Float.floatToIntBits(q.y());
		h = 31 * h + Float.floatToIntBits(q.z());
		return Long.hashCode(h);
	}

	private int getDepHash(ResolvedModel model) {
		int depHash = 0;
		while (model != null && (model.wrapped() instanceof BarrelUnbakedModelBase || model.wrapped() instanceof CuboidModel)) {
			if (model.wrapped() instanceof BarrelUnbakedModelBase barrelModel) {
				if (barrelModel.parentLocation != null) {
					depHash = 31 * depHash + barrelModel.parentLocation.hashCode();
				}
			} else if (model.wrapped() instanceof CuboidModel cuboidModel && cuboidModel.parent() != null) {
				depHash = 31 * depHash + cuboidModel.parent().hashCode();
			}
			model = model.parent();
		}
		return depHash;
	}

	protected abstract BarrelBlockStateModelBase instantiateBlockStateModel(ModelBaker baker, Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts,
			Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons,
			Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData,
			Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts);

	@Override
	public void resolveDependencies(Resolver resolver) {
		if (parentLocation != null) {
			resolver.markDependency(parentLocation);
		}

		woodModelPartDefinitions.values().forEach(partDefs -> partDefs.values().forEach(def -> {
			if (def.modelLocation != null) {
				resolver.markDependency(def.modelLocation);
			}
		}));
		woodPartitionedModelPartDefinitions.values().forEach(partDefs -> partDefs.values().forEach(def -> {
			if (def.modelLocation != null) {
				resolver.markDependency(def.modelLocation);
			}
		}));
		dynamicPartModels.values().forEach(resolver::markDependency);
	}

	private Map<DynamicBarrelBakingData.DynamicPart, ResolvedModel> createUnbakedDynamicPartModels(ModelBaker baker) {
		ImmutableMap.Builder<DynamicBarrelBakingData.DynamicPart, ResolvedModel> dynamicPartModelsBuilder = ImmutableMap.builder();

		dynamicPartModels.forEach((part, modelLocation) -> dynamicPartModelsBuilder.put(part, baker.getModel(modelLocation)));

		return dynamicPartModelsBuilder.build();
	}

	private void updateDynamicPartModelsFromModel(BarrelUnbakedModelBase model) {
		model.dynamicPartModels.forEach((dynamicPart, dynamicPartModel) -> {
			if (!dynamicPartModels.containsKey(dynamicPart)) {
				dynamicPartModels.put(dynamicPart, dynamicPartModel);
			}
		});
	}

	private void updateDefinitionsFromParents(ModelBaker baker) {
		if (parentLocation == null) {
			return;
		}
		ResolvedModel resolvedModel = baker.getModel(parentLocation);
		while (resolvedModel != null && resolvedModel.wrapped() instanceof BarrelUnbakedModelBase barrelUnbakedModel) {
			updateWoodModelPartDefinitionsFromModel(barrelUnbakedModel);
			updateWoodPartitionedModelPartDefinitionsFromModel(barrelUnbakedModel);
			updateDynamicPartModelsFromModel(barrelUnbakedModel);
			resolvedModel = resolvedModel.parent();
		}
	}

	private void updateWoodModelPartDefinitionsFromModel(BarrelUnbakedModelBase model) {
		model.woodModelPartDefinitions.forEach((woodType, parentModelDefinitions) -> {
			if (!woodModelPartDefinitions.containsKey(woodType)) {
				woodModelPartDefinitions.put(woodType,
						parentModelDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
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

	private void updateWoodPartitionedModelPartDefinitionsFromModel(BarrelUnbakedModelBase model) {
		model.woodPartitionedModelPartDefinitions.forEach((woodType, parentModelDefinitions) -> {
			if (!woodPartitionedModelPartDefinitions.containsKey(woodType)) {
				woodPartitionedModelPartDefinitions.put(woodType,
						parentModelDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
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

	public BarrelBlockStateModelBase bakeBlockStateModel(ModelBaker baker, ResolvedModel resolvedModel, ModelState modelState) {
		updateDefinitionsFromParents(baker);
		copyAndResolveTextures(woodModelPartDefinitions, woodPartitionedModelPartDefinitions);

		Map<String, Map<BarrelModelPart, ResolvedModel>> resolvedWoodModelParts = createUnbakedWoodModelParts(woodModelPartDefinitions, baker, resolvedModel);
		Map<String, Map<BarrelModelPart, ResolvedModel>> resolvedWoodPartitionedModelParts = createUnbakedWoodModelParts(woodPartitionedModelPartDefinitions,
				baker, resolvedModel);
		Map<DynamicBarrelBakingData.DynamicPart, ResolvedModel> resolvedDynamicPartModels = createUnbakedDynamicPartModels(baker);

		Map<String, Map<BarrelModelPart, QuadCollection>> woodModelParts = bakeWoodModelParts(baker, modelState, resolvedWoodModelParts);
		Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData = getDynamicBakingData(modelState, resolvedModel,
				resolvedDynamicPartModels);
		Map<String, Map<BarrelModelPart, QuadCollection>> woodPartitionedModelParts = bakeWoodModelParts(baker, modelState, resolvedWoodPartitionedModelParts);

		Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons = getParticleIcons(baker, resolvedWoodModelParts);

		return instantiateBlockStateModel(baker, woodModelParts, particleIcons, woodDynamicBakingData, woodPartitionedModelParts);
	}

	private Map<String, Map<BarrelModelPart, TextureAtlasSprite>> getParticleIcons(ModelBaker baker,
			Map<String, Map<BarrelModelPart, ResolvedModel>> resolvedWoodModelParts) {
		Map<String, Map<BarrelModelPart, TextureAtlasSprite>> particleIcons = new HashMap<>();
		resolvedWoodModelParts.forEach((woodName, modelParts) -> {
			Map<BarrelModelPart, TextureAtlasSprite> textures = new EnumMap<>(BarrelModelPart.class);
			modelParts.forEach((part, model) -> {
				if (part == BarrelModelPart.BASE || part == BarrelModelPart.TINTABLE_MAIN) {
					textures.put(part, model.resolveParticleMaterial(findTopTextureSlots(model, baker), baker).sprite());
				}
			});
			particleIcons.put(woodName, textures);
		});
		return particleIcons;
	}

	public abstract static class Loader<T extends BarrelUnbakedModelBase> implements UnbakedModelLoader<T> {
		@Override
		public T read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			Identifier parentLocation = null;
			if (modelContents.has("parent")) {
				parentLocation = Identifier.parse(modelContents.get("parent").getAsString());
			}

			Map<BarrelModelPart, BarrelModelPartDefinition> modelParts = readModelParts(modelContents, "model_parts");
			Map<BarrelModelPart, BarrelModelPartDefinition> partitionedModelParts = readModelParts(modelContents, "partitioned_model_parts");
			Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels = readDynamicPartModels(modelContents);
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides = readWoodOverrides(modelContents);

			if (parentLocation == null && modelParts.isEmpty() && woodOverrides.isEmpty() && dynamicPartModels.isEmpty()) {
				SophisticatedStorage.LOGGER.warn("None of 'parent', 'model_parts' and 'wood_overrides' present in model definition");
			}

			mergeModelPartDefinitionsIntoWoodOnes(modelParts, woodOverrides);

			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodPartitionedModelPartDefinitions = readWoodOverrides(modelContents,
					Set.of(BarrelModelPart.BASE, BarrelModelPart.BASE_OPEN));
			mergeModelPartDefinitionsIntoWoodOnes(partitionedModelParts, woodPartitionedModelPartDefinitions);

			return instantiateModel(parentLocation, woodOverrides, dynamicPartModels, woodPartitionedModelPartDefinitions);
		}

		private static Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> readWoodOverrides(JsonObject modelContents) {
			return readWoodOverrides(modelContents, Collections.emptySet());
		}

		private static Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> readWoodOverrides(JsonObject modelContents,
				Set<BarrelModelPart> partsToIgnore) {
			Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides = new HashMap<>();
			if (modelContents.has("wood_overrides")) {
				JsonObject woodOverridesJson = modelContents.getAsJsonObject("wood_overrides");
				for (Map.Entry<String, JsonElement> entry : woodOverridesJson.entrySet()) {
					JsonObject woodOverrideJson = entry.getValue().getAsJsonObject();
					Map<BarrelModelPart, BarrelModelPartDefinition> woodOverride = new EnumMap<>(BarrelModelPart.class);
					for (Map.Entry<String, JsonElement> woodModelParts : woodOverrideJson.entrySet()) {
						JsonObject modelPartJson = woodModelParts.getValue().getAsJsonObject();
						BarrelModelPart.getByNameOptional(woodModelParts.getKey()).ifPresent(part -> {
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

		private static Map<DynamicBarrelBakingData.DynamicPart, Identifier> readDynamicPartModels(JsonObject modelContents) {
			Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels = new EnumMap<>(DynamicBarrelBakingData.DynamicPart.class);
			if (modelContents.has("dynamic_part_models")) {
				JsonObject dynamicPartsJson = modelContents.getAsJsonObject("dynamic_part_models");
				for (Map.Entry<String, JsonElement> entry : dynamicPartsJson.entrySet()) {
					DynamicBarrelBakingData.DynamicPart.getByNameOptional(entry.getKey())
							.ifPresent(part -> dynamicPartModels.put(part, Identifier.parse(entry.getValue().getAsString())));
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

		private void mergeModelPartDefinitionsIntoWoodOnes(Map<BarrelModelPart, BarrelModelPartDefinition> modelPartDefinitions,
				Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodModelPartdefinitions) {
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

		protected abstract T instantiateModel(@Nullable Identifier parentLocation, Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> woodOverrides,
				Map<DynamicBarrelBakingData.DynamicPart, Identifier> dynamicPartModels,
				Map<String, Map<BarrelModelPart, BarrelModelPartDefinition>> partitionedWoodOverrides);
	}

	public static final class BarrelModelPartDefinition {
		@Nullable
		private Identifier modelLocation;
		private final Map<String, Material> textures;

		private BarrelModelPartDefinition(@Nullable Identifier modelLocation, Map<String, Material> textures) {
			this.modelLocation = modelLocation;
			this.textures = textures;
		}

		public BarrelModelPartDefinition copy() {
			return new BarrelModelPartDefinition(modelLocation, new ConcurrentHashMap<>(textures));
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
			Identifier modelLocation = null;
			if (json.has("model")) {
				modelLocation = Identifier.parse(json.get("model").getAsString());
			}
			Map<String, Material> textures = new ConcurrentHashMap<>();
			if (json.has("textures")) {
				JsonObject texturesJson = json.getAsJsonObject("textures");
				for (Map.Entry<String, JsonElement> entry : texturesJson.entrySet()) {
					String textureName = entry.getValue().getAsString();
					if (textureName.startsWith("#")) {
						textureName = REFERENCE_PREFIX + textureName.substring(1);
					}
					textures.put(entry.getKey(), new Material(Identifier.parse(textureName)));
				}
			}
			return new BarrelModelPartDefinition(modelLocation, textures);
		}

		public Optional<Identifier> modelLocation() {
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
			return Objects.equals(modelLocation, that.modelLocation) && Objects.equals(textures, that.textures);
		}

		@Override
		public int hashCode() {
			return Objects.hash(modelLocation, textures);
		}

		@Override
		public String toString() {
			return "BarrelModelPartDefinition[" + "modelLocation=" + modelLocation + ", " + "textures=" + textures + ']';
		}
	}

	public record UnbakedBlockStateModel(Variant variant) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<UnbakedBlockStateModel> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(Variant.MAP_CODEC.forGetter(UnbakedBlockStateModel::variant)).apply(instance, UnbakedBlockStateModel::new));
		public static final Identifier ID = SophisticatedStorage.getIdentifier("barrel_blockstate_model_loader");

		@Override
		public BlockStateModel bake(ModelBaker modelBaker) {
			ResolvedModel resolvedModel = modelBaker.getModel(variant.modelLocation());
			if (resolvedModel.wrapped() instanceof BarrelUnbakedModelBase model) {
				return model.bakeBlockStateModel(modelBaker, resolvedModel, variant.modelState().asModelState());
			}

			throw new IllegalStateException(
					"Expected BarrelUnbakedModelBase but got " + resolvedModel.wrapped().getClass().getName() + " for model " + variant.modelLocation());
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			resolver.markDependency(variant.modelLocation());
		}

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}
	}
}
