package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.p3pp3rf1y.sophisticatedstorage.client.StorageTextureManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public abstract class BarrelDynamicModelBase<T extends BarrelDynamicModelBase<T>> implements IModelGeometry<T> {
	public static final Map<MaterialKey, Material> TEXTURES = new HashMap<>();
	public static Collection<Material> getTextures() {
		return TEXTURES.values();
	}

	public static final Map<Integer, BakedModel> BAKED_PART_MODELS = new HashMap<>();

	protected final Map<String, Map<BarrelModelPart, UnbakedModel>> woodModels;

	protected BarrelDynamicModelBase(Map<String, Map<BarrelModelPart, UnbakedModel>> woodModels) {
		this.woodModels = woodModels;
	}

	@Override
	public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		ImmutableMap.Builder<String, Map<BarrelModelPart, BakedModel>> builder = ImmutableMap.builder();
		woodModels.forEach((woodName, partModels) -> {
			ImmutableMap.Builder<BarrelModelPart, BakedModel> partBuilder = ImmutableMap.builder();
			partModels.forEach((part, model) -> {
				int hash = getBakedModelHash(model, bakery, modelTransform);
				BakedModel bakedModel = BAKED_PART_MODELS.computeIfAbsent(hash, h -> model.bake(bakery, spriteGetter, modelTransform, modelLocation));
				if (bakedModel != null) {
					partBuilder.put(part, bakedModel);
				}
			});
			builder.put(woodName, partBuilder.build());
		});

		ImmutableMap<String, Map<BarrelModelPart, BakedModel>> woodModelParts = builder.build();
		return instantiateBakedModel(woodModelParts);
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

	protected abstract BarrelBakedModelBase instantiateBakedModel(ImmutableMap<String, Map<BarrelModelPart, BakedModel>> woodModelParts);

	public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		ImmutableSet.Builder<Material> builder = ImmutableSet.builder();
		woodModels.forEach((woodName, partModels) -> partModels.forEach((part, model) -> builder.addAll(model.getMaterials(modelGetter, missingTextureErrors))));
		return builder.build();
	}

	protected static void putMaterial(Map<String, Either<Material, String>> materials, IBarrelMaterialGetter barrelMaterialGetter, WoodType woodType, StorageTextureManager.BarrelFace barrelFace, StorageTextureManager.BarrelMaterial barrelMaterial) {
		putMaterial(materials, barrelMaterialGetter, woodType, barrelFace, barrelMaterial, barrelFace.name().toLowerCase(Locale.ROOT));
	}

	protected static void putMaterial(Map<String, Either<Material, String>> materials, IBarrelMaterialGetter barrelMaterialGetter, WoodType woodType, StorageTextureManager.BarrelFace barrelFace, StorageTextureManager.BarrelMaterial barrelMaterial, String textureName) {
		MaterialKey key = new MaterialKey(woodType, barrelFace, barrelMaterial);
		if (!TEXTURES.containsKey(key)) {
			Optional<Material> mat = barrelMaterialGetter.get(woodType, barrelFace, barrelMaterial);
			if (mat.isEmpty()) {
				return;
			}
			TEXTURES.put(key, mat.get());
		}
		Material material = TEXTURES.get(key);
		materials.put(textureName, Either.left(material));
	}

	public interface IBarrelMaterialGetter {
		Optional<Material> get(WoodType woodType, StorageTextureManager.BarrelFace barrelFace, StorageTextureManager.BarrelMaterial barrelMaterial);
	}

	private record MaterialKey(WoodType woodType, StorageTextureManager.BarrelFace barrelFace, StorageTextureManager.BarrelMaterial barrelMaterial) {
		@Override
		public boolean equals(Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}
			MaterialKey that = (MaterialKey) o;
			return woodType.equals(that.woodType) && barrelFace == that.barrelFace && barrelMaterial == that.barrelMaterial;
		}

		@Override
		public int hashCode() {
			return Objects.hash(woodType, barrelFace, barrelMaterial);
		}
	}
}
