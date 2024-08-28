package net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class PropertyBasedSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
	private final List<Function<ItemStack, @Nullable Object>> propertyGetters = new ArrayList<>();

	protected void addOptionalProperty(Function<ItemStack, Optional<?>> propertyGetter) {
		Function<ItemStack, @Nullable Object> nullableGetter = propertyGetter.andThen(i -> i.orElse(null));
		this.propertyGetters.add(nullableGetter);
	}

	protected void addProperty(Function<ItemStack, @Nullable Object> propertyGetter) {
		this.propertyGetters.add(propertyGetter);
	}

	@Override
	public final @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
		boolean allNulls = true;
		List<@Nullable Object> results = new ArrayList<>(propertyGetters.size());
		for (Function<ItemStack, @Nullable Object> propertyGetter : propertyGetters) {
			@Nullable Object value = propertyGetter.apply(ingredient);
			if (value != null) {
				allNulls = false;
			}
			results.add(value);
		}
		if (allNulls) {
			return null;
		}
		return results;
	}
}
