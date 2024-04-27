package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.p3pp3rf1y.sophisticatedstorage.Config;

public class DropPackedDisabledCondition implements ICondition {
	private static final DropPackedDisabledCondition INSTANCE = new DropPackedDisabledCondition();
	public static final Codec<DropPackedDisabledCondition> CODEC = MapCodec.unit(INSTANCE).stable().codec();
	@Override
	public boolean test(IContext context) {
		return Boolean.FALSE.equals(Config.COMMON.dropPacked.get());
	}

	@Override
	public Codec<? extends ICondition> codec() {
		return CODEC;
	}
}
