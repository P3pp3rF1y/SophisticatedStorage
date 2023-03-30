package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class AccessHelper {
	private static final Constructor<RecipeHelper.CompactingResult> COMPACTING_RESULT_INIT;

	static {
		try {
			COMPACTING_RESULT_INIT = RecipeHelper.CompactingResult.class.getDeclaredConstructor(ItemStack.class, List.class);
			COMPACTING_RESULT_INIT.setAccessible(true);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static RecipeHelper.CompactingResult initCompactingResult(ItemStack result, List<ItemStack> remainingItems) {
		try {
		return COMPACTING_RESULT_INIT.newInstance(result, remainingItems);
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
