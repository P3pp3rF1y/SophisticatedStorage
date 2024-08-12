package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.ElementsModel;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class CompositeElementsModel extends BlockModel {
	private final List<BlockElement> elements;

	public CompositeElementsModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textureMap) {
		super(parentLocation, Collections.emptyList(), textureMap, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
		elements = new ArrayList<>();
	}

	@Override
	public BakedModel bake(ModelBaker modelBaker, BlockModel owner, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, boolean guiLight3d) {
		if (getRootModel() == ModelBakery.BLOCK_ENTITY_MARKER) {
			var particleSprite = spriteGetter.apply(getMaterial("particle"));
			return new BuiltInModel(getTransforms(), getOverrides(modelBaker, owner, spriteGetter), particleSprite, getGuiLight().lightLikeBlock());
		}

		var elementsModel = new ElementsModel(getElements());
		return elementsModel.bake(customData, modelBaker, spriteGetter, modelState, getOverrides(modelBaker, owner, spriteGetter));
	}

	@SuppressWarnings({"java:S1874", "deprecation"}) //overriding getElements here
	@Override
	public List<BlockElement> getElements() {
		return elements;
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
		super.resolveParents(modelGetter);

		copyElementsFromAllIncludedModels();
		copyTexturesFromAllIncludedModels();
	}

	@SuppressWarnings({"java:S1874", "deprecation"}) //need to call getElements even though deprecated
	private void copyElementsFromAllIncludedModels() {
		if (parent != null) {
			elements.addAll(parent.getElements());
			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel simpleCompositeModel) {
				elements.addAll(simpleCompositeModel.getElements());
			}
		}
	}

	@SuppressWarnings("java:S5803") //need to call textureMap here even though only visible for testing
	private void copyTexturesFromAllIncludedModels() {
		if (parent != null) {
			parent.textureMap.forEach(textureMap::putIfAbsent);
			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel simpleCompositeModel) {
				simpleCompositeModel.getTextures().forEach(textureMap::putIfAbsent);
			}
		}
	}

	@Override
	public Material getMaterial(String textureName) {
		if (textureName.charAt(0) == '#') {
			textureName = textureName.substring(1);
		}

		List<String> visitedTextureReferences = Lists.newArrayList();
		while (true) {
			Either<Material, String> either = findTexture(textureName);
			Optional<Material> optional = either.left();
			if (optional.isPresent()) {
				return optional.get();
			}

			textureName = either.right().orElse("");

			if (visitedTextureReferences.contains(textureName)) {
				String finalTextureName = textureName;
				SophisticatedStorage.LOGGER.warn("Unable to resolve texture due to reference chain {}->{} in {}", () -> Joiner.on("->").join(visitedTextureReferences), () -> finalTextureName, () -> name);
				return new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation());
			}

			visitedTextureReferences.add(textureName);
		}
	}

	private Either<Material, String> findTexture(String textureName) {
		for (BlockModel blockmodel = this; blockmodel != null; blockmodel = blockmodel.parent) {
			Either<Material, String> either = blockmodel.textureMap.get(textureName);
			if (either != null) {
				return either;
			}
		}

		return Either.left(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
	}
}
