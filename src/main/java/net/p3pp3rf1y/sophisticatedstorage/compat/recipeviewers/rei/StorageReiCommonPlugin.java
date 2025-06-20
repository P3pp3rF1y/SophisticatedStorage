package net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.forge.REIPluginCommon;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.comparator.ReiSubtypeInterpreter;

import static net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

@SuppressWarnings("unused")
@REIPluginCommon
public class StorageReiCommonPlugin implements REICommonPlugin {
	@Override
	public void registerItemComparators(ItemComparatorRegistry registry) {
		getSubtypeInterpreters()
				.forEach((item, subtypeInterpreter) -> registry.register(ReiSubtypeInterpreter.of(subtypeInterpreter), item));
	}
}