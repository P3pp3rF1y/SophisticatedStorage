package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.client.renderer.block.model.VariantMutator;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

public abstract class UnitBlockStateModelBuilder extends CustomBlockStateModelBuilder {
	@Override
	public CustomBlockStateModelBuilder with(VariantMutator variantMutator) {
		return this;
	}

	@Override
	public CustomBlockStateModelBuilder with(UnbakedMutator unbakedMutator) {
		return this;
	}
}
