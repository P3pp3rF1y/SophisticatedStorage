package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record DynamicBarrelBakingData(BarrelDynamicModelBase.BarrelModelPartDefinition modelPartDefinition, ModelState modelState, ResourceLocation modelLocation) {

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
