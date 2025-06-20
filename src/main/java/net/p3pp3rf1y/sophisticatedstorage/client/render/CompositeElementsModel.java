//package net.p3pp3rf1y.sophisticatedstorage.client.render;
//
//import com.mojang.datafixers.util.Either;
//import com.mojang.math.Transformation;
//import net.minecraft.client.renderer.block.model.BlockElement;
//import net.minecraft.client.renderer.block.model.BlockModel;
//import net.minecraft.client.renderer.block.model.ItemTransforms;
//import net.minecraft.client.renderer.block.model.TextureSlots;
//import net.minecraft.client.resources.model.*;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.util.context.ContextMap;
//import net.neoforged.neoforge.client.RenderTypeGroup;
//import net.neoforged.neoforge.client.model.ExtendedUnbakedModel;
//import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//public class CompositeElementsModel extends BlockModel implements ExtendedUnbakedModel {
//	private final List<BlockElement> elements;
//
//	public CompositeElementsModel(@Nullable ResourceLocation parentLocation, TextureSlots.Data textures) {
//		super(parentLocation, Collections.emptyList(), textures, true, null, ItemTransforms.NO_TRANSFORMS);
//		elements = new ArrayList<>();
//	}
//
//	@Override
//	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState modelState, boolean hasAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap additionalProperties) {
//		return SimpleBakedModel.bakeElements(getElements(), textures, baker.sprites(), modelState, hasAmbientOcclusion, usesBlockLight, true, itemTransforms,
//				additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity()),
//				additionalProperties.getOrDefault(NeoForgeModelProperties.RENDER_TYPE, RenderTypeGroup.EMPTY));
//	}
//
//	public List<BlockElement> getElements() {
//		return elements;
//	}
//
//	@Override
//	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
//		super.resolveParents(modelGetter);
//
//		copyElementsFromAllIncludedModels();
//		copyTexturesFromAllIncludedModels();
//	}
//
//	@SuppressWarnings({"java:S1874", "deprecation"}) //need to call getElements even though deprecated
//	private void copyElementsFromAllIncludedModels() {
//		if (parent != null) {
//			elements.addAll(parent.getElements());
//			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel simpleCompositeModel) {
//				elements.addAll(simpleCompositeModel.getElements());
//			}
//		}
//	}
//
//	@SuppressWarnings("java:S5803") //need to call textureMap here even though only visible for testing
//	private void copyTexturesFromAllIncludedModels() {
//		if (parent != null) {
//			parent.textureMap.forEach(textureMap::putIfAbsent);
//			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel simpleCompositeModel) {
//				simpleCompositeModel.getTextures().forEach(textureMap::putIfAbsent);
//			}
//		}
//	}
//
//	@Override
//	public Material getMaterial(String textureName) {
//		if (textureName.charAt(0) == '#') {
//			textureName = textureName.substring(1);
//		}
//
//		List<String> visitedTextureReferences = Lists.newArrayList();
//		while (true) {
//			Either<Material, String> either = findTexture(textureName);
//			Optional<Material> optional = either.left();
//			if (optional.isPresent()) {
//				return optional.get();
//			}
//
//			textureName = either.right().orElse("");
//
//			if (visitedTextureReferences.contains(textureName)) {
//				String finalTextureName = textureName;
//				SophisticatedStorage.LOGGER.warn("Unable to resolve texture due to reference chain {}->{} in {}", () -> Joiner.on("->").join(visitedTextureReferences), () -> finalTextureName, () -> name);
//				return new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation());
//			}
//
//			visitedTextureReferences.add(textureName);
//		}
//	}
//
//	private Either<Material, String> findTexture(String textureName) {
//		for (BlockModel blockmodel = this; blockmodel != null; blockmodel = blockmodel.parent) {
//			Either<Material, String> either = blockmodel.textureMap.get(textureName);
//			if (either != null) {
//				return either;
//			}
//		}
//
//		return Either.left(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));
//	}
//}
