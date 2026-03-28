package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.sprite.Material;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record DynamicBarrelBakingData(UnbakedModel baseModel, Map<String, Material> baseTextures, ModelState modelState, ModelDebugName debugName) {

	public enum DynamicPart {
		WHOLE,
		TRIM,
		CORE,
		PARTITIONED;

		private static final Map<String, DynamicPart> NAME_TO_PART;

		static {
			ImmutableMap.Builder<String, DynamicPart> builder = ImmutableMap.builder();
			for (DynamicPart part : values()) {
				builder.put(part.name().toLowerCase(Locale.ROOT), part);
			}
			NAME_TO_PART = builder.build();
		}

		public static Optional<DynamicPart> getByNameOptional(String name) {
			return Optional.ofNullable(NAME_TO_PART.get(name.toLowerCase(Locale.ROOT)));
		}
	}
}
