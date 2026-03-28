package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelUnbakedModelBase;

public class BarrelBlockStateModelBuilder extends CustomBlockStateModelBuilder {
	private final Variant variant;

	public BarrelBlockStateModelBuilder(Identifier modelLocation) {
		this(new Variant(modelLocation));
	}

	private BarrelBlockStateModelBuilder(Variant variant) {
		this.variant = variant;
	}

	@Override
	public CustomBlockStateModelBuilder with(VariantMutator variantMutator) {
		return new BarrelBlockStateModelBuilder(variantMutator.apply(variant));
	}

	@Override
	public CustomBlockStateModelBuilder with(UnbakedMutator unbakedMutator) {
		return this;
	}

	@Override
	public CustomUnbakedBlockStateModel toUnbaked() {
		return new BarrelUnbakedModelBase.UnbakedBlockStateModel(variant);
	}
}
