package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.CompositeModelState;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;
import net.minecraftforge.client.model.geometry.IMultipartModelGeometry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class SimpleCompositeModel implements IDynamicBakedModel {
	private final ImmutableMap<String, BakedModel> bakedParts;
	private final boolean isAmbientOcclusion;
	private final boolean isGui3d;
	private final boolean isSideLit;
	private final TextureAtlasSprite particle;
	private final ItemOverrides overrides;

	public SimpleCompositeModel(boolean isGui3d, boolean isSideLit, boolean isAmbientOcclusion, TextureAtlasSprite particle, ImmutableMap<String, BakedModel> bakedParts, ItemOverrides overrides) {
		this.bakedParts = bakedParts;
		this.isAmbientOcclusion = isAmbientOcclusion;
		this.isGui3d = isGui3d;
		this.isSideLit = isSideLit;
		this.particle = particle;
		this.overrides = overrides;
	}

	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
		List<BakedQuad> quads = new ArrayList<>();
		for (Map.Entry<String, BakedModel> entry : bakedParts.entrySet()) {
			quads.addAll(entry.getValue().getQuads(state, side, rand, extraData));
		}
		return quads;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return isAmbientOcclusion;
	}

	@Override
	public boolean isGui3d() {
		return isGui3d;
	}

	@Override
	public boolean usesBlockLight() {
		return isSideLit;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return particle;
	}

	@Override
	public ItemOverrides getOverrides() {
		return overrides;
	}

	@Override
	public boolean doesHandlePerspectives() {
		return true;
	}

	public static class Submodel implements IModelGeometryPart {
		private final String name;
		private final BlockModel model;
		private final ModelState modelTransform;

		private Submodel(String name, BlockModel model, ModelState modelTransform) {
			this.name = name;
			this.model = model;
			this.modelTransform = modelTransform;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public void addQuads(IModelConfiguration owner, IModelBuilder<?> modelBuilder, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
			throw new UnsupportedOperationException("Attempted to call adQuads on a Submodel instance. Please don't.");
		}

		public BakedModel bakeModel(ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
			return model.bake(bakery, spriteGetter, new CompositeModelState(this.modelTransform, modelTransform,
					this.modelTransform.isUvLocked() || modelTransform.isUvLocked()), modelLocation);
		}

		@Override
		public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
			return model.getMaterials(modelGetter, missingTextureErrors);
		}

		@SuppressWarnings("java:S5803") //textureMap is needed here to update customData geometry textures with that
		public void overrideTextures(Map<String, Either<Material, String>> textureMap) {
			if (model.customData.hasCustomGeometry() && model.customData.getCustomGeometry() instanceof SimpleCompositeModel.Geometry geometry) {
				geometry.getParts().forEach(compositePart -> compositePart.overrideTextures(textureMap));
			} else {
				model.textureMap.putAll(textureMap);
			}
		}
	}

	public static class Geometry implements IMultipartModelGeometry<Geometry> {
		private final ImmutableMap<String, Submodel> parts;

		Geometry(ImmutableMap<String, Submodel> parts) {
			this.parts = parts;
		}

		@Override
		public Collection<Submodel> getParts() {
			return parts.values();
		}

		@Override
		public Optional<? extends IModelGeometryPart> getPart(String name) {
			return Optional.ofNullable(parts.get(name));
		}

		@Override
		public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
			Material particleLocation = owner.resolveTexture("particle");
			TextureAtlasSprite particle = spriteGetter.apply(particleLocation);

			ImmutableMap.Builder<String, BakedModel> bakedParts = ImmutableMap.builder();
			for (Map.Entry<String, Submodel> part : parts.entrySet()) {
				Submodel submodel = part.getValue();
				if (!owner.getPartVisibility(submodel)) {continue;}
				bakedParts.put(part.getKey(), submodel.bakeModel(bakery, spriteGetter, modelTransform, modelLocation));
			}
			return new SimpleCompositeModel(owner.isShadedInGui(), owner.isSideLit(), owner.useSmoothLighting(), particle, bakedParts.build(), overrides);
		}

		@Override
		public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
			Set<Material> textures = new HashSet<>();
			for (Submodel part : parts.values()) {
				textures.addAll(part.getTextures(owner, modelGetter, missingTextureErrors));
			}
			return textures;
		}
	}

	@SuppressWarnings("java:S6548") //singleton is intended here
	public static class Loader implements IModelLoader<Geometry> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {}

		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			//noop
		}

		@Override
		public Geometry read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
			if (!modelContents.has("parts")) {throw new RuntimeException("Composite model requires a \"parts\" element.");}
			ImmutableMap.Builder<String, Submodel> parts = ImmutableMap.builder();
			for (Map.Entry<String, JsonElement> part : modelContents.get("parts").getAsJsonObject().entrySet()) {
				ModelState modelTransform = SimpleModelState.IDENTITY;
				parts.put(part.getKey(), new Submodel(
						part.getKey(),
						deserializationContext.deserialize(part.getValue(), BlockModel.class),
						modelTransform));
			}
			return new Geometry(parts.build());
		}
	}
}
