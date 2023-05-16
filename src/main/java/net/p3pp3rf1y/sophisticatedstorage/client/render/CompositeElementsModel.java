package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class CompositeElementsModel extends BlockModel {
	private final List<BlockElement> elements;

	public CompositeElementsModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textureMap) {
		super(parentLocation, Collections.emptyList(), textureMap, true, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
		elements = new ArrayList<>();
	}

	@SuppressWarnings({"java:S1874", "deprecation"}) //need to use bakeVanilla here directly
	@Override
	public BakedModel bake(ModelBakery modelBakery, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation, boolean guiLight3d) {
		return model.bakeVanilla(modelBakery, this, spriteGetter, modelState, modelLocation, guiLight3d);
	}

	@SuppressWarnings({"java:S1874", "deprecation"}) //overriding getElements here
	@Override
	public List<BlockElement> getElements() {
		return elements;
	}

	@Override
	public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> pModelGetter, Set<Pair<String, String>> pMissingTextureErrors) {
		Collection<Material> materials = super.getMaterials(pModelGetter, pMissingTextureErrors);

		copyElementsFromAllIncludedModels();
		copyTexturesFromAllIncludedModels();

		return materials;
	}

	@SuppressWarnings({"java:S1874", "deprecation"}) //need to call getElements even though deprecated
	private void copyElementsFromAllIncludedModels() {
		if (parent != null) {
			elements.addAll(parent.getElements());
			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel.Geometry simpleCompositeModel) {
				elements.addAll(simpleCompositeModel.getElements());
			}
		}
	}

	@SuppressWarnings("java:S5803") //need to call textureMap here even though only visible for testing
	private void copyTexturesFromAllIncludedModels() {
		if (parent != null) {
			parent.textureMap.forEach(textureMap::putIfAbsent);
			if (parent.customData.hasCustomGeometry() && parent.customData.getCustomGeometry() instanceof SimpleCompositeModel.Geometry simpleCompositeModel) {
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
