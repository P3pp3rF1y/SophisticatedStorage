package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleCompositeModel extends AbstractUnbakedModel {
	private final ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children;
	private List<UnbakedModel> resolvedChildren = List.of();

	private SimpleCompositeModel(ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children, StandardModelParameters parameters) {
		super(parameters);
		this.children = children;
	}

	@Override
	public BakedModel bake(TextureSlots slots, ModelBaker baker, ModelState state, boolean useAmbientOcclusion, boolean usesBlockLight,
			ItemTransforms transforms, ContextMap additionalProperties) {
		List<BlockElement> allElements = new ArrayList<>();
		addAllChildElements(allElements);

		Transformation rootTransform = additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity());
		if (!rootTransform.isIdentity()) {
			state = UnbakedElementsHelper.composeRootTransformIntoModelState(state, rootTransform);
		}

		return SimpleBakedModel.bakeElements(allElements, slots, baker.sprites(), state, useAmbientOcclusion, usesBlockLight, true, transforms, rootTransform,
				RenderTypeGroup.EMPTY);
	}

	@Override
	public void resolveDependencies(ResolvableModel.Resolver resolver) {
		super.resolveDependencies(resolver);

		resolvedChildren = children.values().stream().map(child -> child.map(resolver::resolve, model -> {
			model.resolveDependencies(resolver);
			return model;
		})).toList();
	}

	private void addAllChildElements(List<BlockElement> elements) {
		resolvedChildren.forEach(child -> {
			addModelElements(elements, child);
		});
	}

	private void addModelElements(List<BlockElement> elements, UnbakedModel child) {
		if (child instanceof SimpleCompositeModel compositeModel) {
			compositeModel.addAllChildElements(elements);
		} else if (child instanceof BlockModel blockModel) {
			elements.addAll(blockModel.elements);
		}
		if (child.getParent() != null) {
			addModelElements(elements, child.getParent());
		}
	}

	@Override
	public TextureSlots.Data getTextureSlots() {
		Map<String, TextureSlots.SlotContents> textures = new HashMap<>(super.getTextureSlots().values());

		children.values().forEach(child -> child.ifRight(unbakedModel -> addAllTextureSlots(unbakedModel, textures)));

		return new TextureSlots.Data(textures);
	}

	private void addAllTextureSlots(UnbakedModel unbakedModel, Map<String, TextureSlots.SlotContents> textures) {
		unbakedModel.getTextureSlots().values().forEach(textures::putIfAbsent);
		UnbakedModel model = unbakedModel;
		while (model.getParent() != null) {
			model = model.getParent();
			model.getTextureSlots().values().forEach(textures::putIfAbsent);
		}
	}

	@SuppressWarnings("java:S6548") // singleton implementation is good here
	public static final class Loader implements UnbakedModelLoader<SimpleCompositeModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public SimpleCompositeModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
			ImmutableMap.Builder<String, Either<ResourceLocation, UnbakedModel>> childrenBuilder = ImmutableMap.builder();
			readChildren(jsonObject, "parts", childrenBuilder, deserializationContext);
			ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children = childrenBuilder.build();
			if (children.isEmpty()) {
				throw new JsonParseException("Simple Composite model requires a \"parts\" element with at least one element.");
			}
			StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, deserializationContext);
			return new SimpleCompositeModel(children, parameters);
		}

		private static void readChildren(JsonObject jsonObject, String name, ImmutableMap.Builder<String, Either<ResourceLocation, UnbakedModel>> children,
				JsonDeserializationContext context) {
			if (jsonObject.has(name)) {
				JsonObject childrenJsonObject = jsonObject.getAsJsonObject(name);

				for (Map.Entry<String, JsonElement> entry : childrenJsonObject.entrySet()) {
					JsonElement jsonElement = entry.getValue();
					Either<ResourceLocation, UnbakedModel> child = switch (jsonElement) {
						case JsonPrimitive reference -> Either.left(ResourceLocation.parse(reference.getAsString()));
						case JsonObject inline -> Either.right((UnbakedModel) context.deserialize(inline, UnbakedModel.class));
						default -> throw new IllegalArgumentException("");
					};

					children.put(entry.getKey(), child);
				}
			}
		}
	}
}
