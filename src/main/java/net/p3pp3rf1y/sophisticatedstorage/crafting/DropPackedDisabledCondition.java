package net.p3pp3rf1y.sophisticatedstorage.crafting;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

public class DropPackedDisabledCondition implements ICondition {
	private static final ResourceLocation NAME = SophisticatedStorage.getRL("drop_packed_disabled");

	@Override
	public ResourceLocation getID() {
		return NAME;
	}

	@Override
	public boolean test(IContext context) {
		return Boolean.FALSE.equals(Config.COMMON.dropPacked.get());
	}

	public static class Serializer implements IConditionSerializer<DropPackedDisabledCondition> {
		public static final DropPackedDisabledCondition.Serializer INSTANCE = new DropPackedDisabledCondition.Serializer();

		@Override
		public void write(JsonObject json, DropPackedDisabledCondition value) {
			//noop
		}

		@Override
		public DropPackedDisabledCondition read(JsonObject json) {
			return new DropPackedDisabledCondition();
		}

		@Override
		public ResourceLocation getID() {
			return NAME;
		}
	}
}
