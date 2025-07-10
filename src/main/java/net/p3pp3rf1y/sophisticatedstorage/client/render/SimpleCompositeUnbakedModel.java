package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleCompositeUnbakedModel extends AbstractUnbakedModel {
	private final SimpleCompositeUnbakedGeometry geometry;

	private SimpleCompositeUnbakedModel(SimpleCompositeUnbakedGeometry geometry, StandardModelParameters parameters) {
		super(parameters);
		this.geometry = geometry;
	}

	@Nullable
	@Override
	public UnbakedGeometry geometry() {
		return geometry;
	}

	@Override
	public void resolveDependencies(ResolvableModel.Resolver resolver) {
		super.resolveDependencies(resolver);

		geometry.resolveDependencies(resolver);
	}

	public static class SimpleCompositeUnbakedGeometry implements ExtendedUnbakedGeometry {
		private final ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children;

		public Map<String, Either<ResourceLocation, UnbakedModel>> children() {
			return children;
		}

		public SimpleCompositeUnbakedGeometry(ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children) {
			this.children = children;
		}

		@Override
		public QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName debugName, ContextMap additionalProperties) {
			List<BlockElement> allElements = new ArrayList<>();
			addAllChildElements(baker, debugName, allElements);

			Transformation rootTransform = additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity());
			if (!rootTransform.isIdentity()) {
				state = UnbakedElementsHelper.composeRootTransformIntoModelState(state, rootTransform);
			}

			return new SimpleUnbakedGeometry(allElements).bake(slots, baker, state, debugName, additionalProperties);
		}

		private void addAllChildElements(ModelBaker baker, ModelDebugName debugName, List<BlockElement> elements) {
			children.forEach((key, value) -> {
				ResolvedModel model = value.map(baker::getModel,
						(inline) -> baker.resolveInlineModel(inline, () -> debugName.debugName() + "_" + key));
				addModelElements(baker, debugName, elements, model);
			});
		}

		private void addModelElements(ModelBaker baker, ModelDebugName debugName, List<BlockElement> elements, ResolvedModel child) {
			if (child.wrapped() instanceof SimpleCompositeUnbakedModel compositeModel) {
				compositeModel.geometry.addAllChildElements(baker, debugName, elements);
			} else if (child.wrapped() instanceof BlockModel blockModel && blockModel.geometry() instanceof SimpleUnbakedGeometry geometry) {
				elements.addAll(geometry.elements());
			}
			ResolvedModel parent = child.parent();
			if (parent != null) {
				addModelElements(baker, debugName, elements, parent);
			}
		}

		public void resolveDependencies(Resolver resolver) {
			children.values().forEach(child -> child.ifLeft(resolver::markDependency).ifRight(model -> {
				ResourceLocation parent = model.parent();
				if (parent != null) {
					resolver.markDependency(parent);
				}
				model.resolveDependencies(resolver);
			}));
		}
	}


	@SuppressWarnings("java:S6548") // singleton implementation is good here
	public static final class Loader implements UnbakedModelLoader<SimpleCompositeUnbakedModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public SimpleCompositeUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
			ImmutableMap.Builder<String, Either<ResourceLocation, UnbakedModel>> childrenBuilder = ImmutableMap.builder();
			readChildren(jsonObject, "parts", childrenBuilder, deserializationContext);
			ImmutableMap<String, Either<ResourceLocation, UnbakedModel>> children = childrenBuilder.build();
			if (children.isEmpty()) {
				throw new JsonParseException("Simple Composite model requires a \"parts\" element with at least one element.");
			}
			StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, deserializationContext);
			return new SimpleCompositeUnbakedModel(new SimpleCompositeUnbakedGeometry(children), parameters);
		}

		private static void readChildren(JsonObject jsonObject, String name, ImmutableMap.Builder<String, Either<ResourceLocation, UnbakedModel>> children, JsonDeserializationContext context) {
			if (jsonObject.has(name)) {
				JsonObject childrenJsonObject = jsonObject.getAsJsonObject(name);

				for (Map.Entry<String, JsonElement> entry : childrenJsonObject.entrySet()) {
					JsonElement jsonElement = entry.getValue();
					Either<ResourceLocation, UnbakedModel> child = switch (jsonElement) {
						case JsonPrimitive reference -> Either.left(ResourceLocation.parse(reference.getAsString()));
						case JsonObject inline ->
								Either.right((UnbakedModel) context.deserialize(inline, UnbakedModel.class));
						default -> throw new IllegalArgumentException("");
					};

					children.put(entry.getKey(), child);
				}
			}
		}
	}
}
