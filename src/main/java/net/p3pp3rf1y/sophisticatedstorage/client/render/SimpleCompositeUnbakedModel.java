package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.jspecify.annotations.Nullable;

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
	public void resolveDependencies(Resolver resolver) {
		super.resolveDependencies(resolver);
		geometry.resolveDependencies(resolver);
	}

	public Map<String, Either<Identifier, UnbakedModel>> children() {
		return geometry.children();
	}

	private static class SimpleCompositeUnbakedGeometry implements ExtendedUnbakedGeometry {
		private final ImmutableMap<String, Either<Identifier, UnbakedModel>> children;

		private SimpleCompositeUnbakedGeometry(ImmutableMap<String, Either<Identifier, UnbakedModel>> children) {
			this.children = children;
		}

		public Map<String, Either<Identifier, UnbakedModel>> children() {
			return children;
		}

		@Override
		public QuadCollection bake(TextureSlots slots, ModelBaker baker, net.minecraft.client.renderer.block.dispatch.ModelState state,
				ModelDebugName debugName, ContextMap additionalProperties) {
			List<CuboidModelElement> allElements = new ArrayList<>();
			addAllChildElements(baker, debugName, allElements);

			Transformation rootTransform = additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.IDENTITY);
			if (!rootTransform.isIdentity()) {
				state = UnbakedElementsHelper.composeRootTransformIntoModelState(state, rootTransform);
			}

			return UnbakedCuboidGeometry.bake(allElements, slots, baker, state, debugName);
		}

		private void addAllChildElements(ModelBaker baker, ModelDebugName debugName, List<CuboidModelElement> elements) {
			children.forEach((key, value) -> {
				ResolvedModel model = value.map(baker::getModel, inline -> baker.resolveInlineModel(inline, () -> debugName.debugName() + "_" + key));
				addModelElements(baker, debugName, elements, model);
			});
		}

		private void addModelElements(ModelBaker baker, ModelDebugName debugName, List<CuboidModelElement> elements, ResolvedModel child) {
			if (child.wrapped() instanceof SimpleCompositeUnbakedModel compositeModel) {
				compositeModel.geometry.addAllChildElements(baker, debugName, elements);
			} else if (child.wrapped() instanceof CuboidModel cuboidModel && cuboidModel.geometry() instanceof UnbakedCuboidGeometry geometry) {
				elements.addAll(geometry.elements());
			}

			ResolvedModel parent = child.parent();
			if (parent != null) {
				addModelElements(baker, debugName, elements, parent);
			}
		}

		public void resolveDependencies(Resolver resolver) {
			children.values().forEach(child -> child.ifLeft(resolver::markDependency).ifRight(model -> {
				Identifier parent = model.parent();
				if (parent != null) {
					resolver.markDependency(parent);
				}
				model.resolveDependencies(resolver);
			}));
		}
	}

	@SuppressWarnings("java:S6548")
	public static final class Loader implements UnbakedModelLoader<SimpleCompositeUnbakedModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {
		}

		@Override
		public SimpleCompositeUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
			ImmutableMap.Builder<String, Either<Identifier, UnbakedModel>> childrenBuilder = ImmutableMap.builder();
			readChildren(jsonObject, "parts", childrenBuilder, deserializationContext);
			ImmutableMap<String, Either<Identifier, UnbakedModel>> children = childrenBuilder.build();
			if (children.isEmpty()) {
				throw new JsonParseException("Simple Composite model requires a \"parts\" element with at least one element.");
			}
			StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, deserializationContext);
			return new SimpleCompositeUnbakedModel(new SimpleCompositeUnbakedGeometry(children), parameters);
		}

		private static void readChildren(JsonObject jsonObject, String name, ImmutableMap.Builder<String, Either<Identifier, UnbakedModel>> children,
				JsonDeserializationContext context) {
			if (jsonObject.has(name)) {
				JsonObject childrenJsonObject = jsonObject.getAsJsonObject(name);

				for (Map.Entry<String, JsonElement> entry : childrenJsonObject.entrySet()) {
					JsonElement jsonElement = entry.getValue();
					Either<Identifier, UnbakedModel> child = switch (jsonElement) {
						case JsonPrimitive reference -> Either.left(Identifier.parse(reference.getAsString()));
						case JsonObject inline -> Either.right((UnbakedModel) context.deserialize(inline, UnbakedModel.class));
						default -> throw new IllegalArgumentException("");
					};

					children.put(entry.getKey(), child);
				}
			}
		}
	}
}
