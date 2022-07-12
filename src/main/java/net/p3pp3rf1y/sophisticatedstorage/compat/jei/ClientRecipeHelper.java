package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static Optional<? extends Recipe<?>> getRecipeByKey(ResourceLocation recipeKey) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel world = minecraft.level;
		if (world == null) {
			return Optional.empty();
		}
		return world.getRecipeManager().byKey(recipeKey);
	}
}
