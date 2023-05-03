package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.common.util.ConcatenatedListView;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class SimpleCompositeModel implements IUnbakedGeometry<SimpleCompositeModel> {

	private final ImmutableMap<String, BlockModel> children;
	private final ImmutableList<String> itemPasses;

	private SimpleCompositeModel(ImmutableMap<String, BlockModel> children, ImmutableList<String> itemPasses) {
		this.children = children;
		this.itemPasses = itemPasses;
	}

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
		Material particleLocation = context.getMaterial("particle");
		TextureAtlasSprite particle = spriteGetter.apply(particleLocation);

		var rootTransform = context.getRootTransform();
		if (!rootTransform.isIdentity()) {
			modelState = new SimpleModelState(modelState.getRotation().compose(rootTransform), modelState.isUvLocked());
		}

		var bakedPartsBuilder = ImmutableMap.<String, BakedModel>builder();
		for (var entry : children.entrySet()) {
			var name = entry.getKey();
			if (!context.isComponentVisible(name, true)) {
				continue;
			}
			var model = entry.getValue();
			bakedPartsBuilder.put(name, model.bake(bakery, model, spriteGetter, modelState, modelLocation, true));
		}
		var bakedParts = bakedPartsBuilder.build();

		var itemPassesBuilder = ImmutableList.<BakedModel>builder();
		for (String name : this.itemPasses) {
			var model = bakedParts.get(name);
			if (model == null) {throw new IllegalStateException("Specified \"" + name + "\" in \"item_render_order\", but that is not a child of this model.");}
			itemPassesBuilder.add(model);
		}

		return new Baked(context.isGui3d(), context.useBlockLight(), context.useAmbientOcclusion(), particle, context.getTransforms(), overrides, bakedParts, itemPassesBuilder.build());
	}

	@Override
	public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		Set<Material> textures = new HashSet<>();
		if (context.hasMaterial("particle")) {textures.add(context.getMaterial("particle"));}
		for (BlockModel part : children.values()) {textures.addAll(part.getMaterials(modelGetter, missingTextureErrors));}
		return textures;
	}

	@Override
	public Set<String> getConfigurableComponentNames() {
		return children.keySet();
	}

	@SuppressWarnings("java:S5803") //textureMap is needed here to update customData geometry textures with that
	public void overrideTextures(Map<String, Either<Material, String>> textureMap) {
		children.values().forEach(model -> {
			if (model.customData.hasCustomGeometry() && model.customData.getCustomGeometry() instanceof SimpleCompositeModel geometry) {
				geometry.overrideTextures(textureMap);
			} else {
				model.textureMap.putAll(textureMap);
			}
		});
	}

	public static class Baked implements IDynamicBakedModel {
		private final boolean isAmbientOcclusion;
		private final boolean isGui3d;
		private final boolean isSideLit;
		private final TextureAtlasSprite particle;
		private final ItemOverrides overrides;
		private final ItemTransforms transforms;
		private final ImmutableMap<String, BakedModel> children;
		private final ImmutableList<BakedModel> itemPasses;

		public Baked(boolean isGui3d, boolean isSideLit, boolean isAmbientOcclusion, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides, ImmutableMap<String, BakedModel> children, ImmutableList<BakedModel> itemPasses) {
			this.children = children;
			this.isAmbientOcclusion = isAmbientOcclusion;
			this.isGui3d = isGui3d;
			this.isSideLit = isSideLit;
			this.particle = particle;
			this.overrides = overrides;
			this.transforms = transforms;
			this.itemPasses = itemPasses;
		}

		@NotNull
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
			List<List<BakedQuad>> quadLists = new ArrayList<>();
			for (Map.Entry<String, BakedModel> entry : children.entrySet()) {
				quadLists.add(entry.getValue().getQuads(state, side, rand, ModelData.EMPTY, renderType));
			}
			return ConcatenatedListView.of(quadLists);
		}

		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
			return ModelData.EMPTY;
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
		public ItemTransforms getTransforms() {
			return transforms;
		}

		@Override
		public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
			return itemPasses;
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
			var sets = new ArrayList<ChunkRenderTypeSet>();
			for (Map.Entry<String, BakedModel> entry : children.entrySet()) {
				sets.add(entry.getValue().getRenderTypes(state, rand, ModelData.EMPTY));
			}
			return ChunkRenderTypeSet.union(sets);
		}
	}

	public static final class Loader implements IGeometryLoader<SimpleCompositeModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public SimpleCompositeModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {

			List<String> itemPasses = new ArrayList<>();
			ImmutableMap.Builder<String, BlockModel> childrenBuilder = ImmutableMap.builder();
			readChildren(jsonObject, deserializationContext, childrenBuilder, itemPasses);

			var children = childrenBuilder.build();
			if (children.isEmpty()) {
				throw new JsonParseException("Composite model requires a \"parts\" element with at least one element.");
			}

			if (jsonObject.has("item_render_order")) {
				itemPasses.clear();
				for (var element : jsonObject.getAsJsonArray("item_render_order")) {
					var name = element.getAsString();
					if (!children.containsKey(name)) {
						throw new JsonParseException("Specified \"" + name + "\" in \"item_render_order\", but that is not a child of this model.");
					}
					itemPasses.add(name);
				}
			}

			return new SimpleCompositeModel(children, ImmutableList.copyOf(itemPasses));
		}

		private void readChildren(JsonObject jsonObject, JsonDeserializationContext deserializationContext, ImmutableMap.Builder<String, BlockModel> children, List<String> itemPasses) {
			if (!jsonObject.has("parts")) {
				return;
			}
			var childrenJsonObject = jsonObject.getAsJsonObject("parts");
			for (Map.Entry<String, JsonElement> entry : childrenJsonObject.entrySet()) {
				children.put(entry.getKey(), deserializationContext.deserialize(entry.getValue(), BlockModel.class));
				itemPasses.add(entry.getKey());
			}
		}
	}
}
