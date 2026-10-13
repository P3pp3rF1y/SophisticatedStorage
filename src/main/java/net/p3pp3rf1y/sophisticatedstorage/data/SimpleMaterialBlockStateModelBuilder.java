package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;
import net.p3pp3rf1y.sophisticatedstorage.client.render.SimpleMaterialModel;

public class SimpleMaterialBlockStateModelBuilder extends CustomBlockStateModelBuilder {
	private final Variant variant;

	public SimpleMaterialBlockStateModelBuilder(Identifier modelLocation) {
		this(new Variant(modelLocation));
	}

	private SimpleMaterialBlockStateModelBuilder(Variant variant) {
		this.variant = variant;
	}

	@Override
	public CustomBlockStateModelBuilder with(VariantMutator variantMutator) {
		return new SimpleMaterialBlockStateModelBuilder(variantMutator.apply(variant));
	}

	@Override
	public CustomBlockStateModelBuilder with(UnbakedMutator unbakedMutator) {
		return this;
	}

	@Override
	public CustomUnbakedBlockStateModel toUnbaked() {
		return new SimpleMaterialModel.UnbakedBlockStateModel(variant);
	}
}
