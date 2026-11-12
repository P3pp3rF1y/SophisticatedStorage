package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankForm;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TankItemRenderer implements SpecialModelRenderer<TankItemRenderer.TankAttributes> {
	@Override
	public @Nullable TankAttributes extractArgument(ItemStack stack) {
		if (!(stack.getItem() instanceof TankBlockItem item)) {
			return null;
		}
		int height = stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
		FluidStack fluid = FluidStack.EMPTY;
		int capacity = ((TankBlock) item.getBlock()).getTier().capacity(height, 1);
		if (RegistryHelper.getRegistryAccess().isPresent()) {
			TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
			fluid = wrapper.getTankFluidHandler().getFluidInTank(0).copy();
			capacity = wrapper.getCapacity();
		}
		return new TankAttributes((TankBlock) item.getBlock(), height, stack.getOrDefault(ModDataComponents.TIER_VISIBLE, true),
				stack.getOrDefault(ModDataComponents.PACKED, false), fluid, capacity);
	}

	@Override
	public void submit(@Nullable TankAttributes attributes, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int color) {
		if (attributes == null) {
			return;
		}
		int height = attributes.height();
		Matrix4f matrix = pose.last().pose();
		float projectedHeight = Math.abs(matrix.m01()) + Math.abs(matrix.m11()) + Math.abs(matrix.m21());
		float scale = projectedHeight > 0 ? projectedHeight / (projectedHeight + (height - 1) * Math.abs(matrix.m11())) : 1F / height;
		pose.pushPose();
		pose.translate(.5, (1 - height * scale) / 2, .5);
		pose.scale(scale, scale, scale);
		pose.translate(-.5, 0, -.5);
		for (int index = 0; index < height; index++) {
			pose.pushPose();
			pose.translate(0, index, 0);
			submitSegment(attributes, index, pose, collector, light, overlay);
			pose.popPose();
		}
		TankRenderer.submitFluid(attributes.fluid(), attributes.capacity(), height, pose, collector, light);
		pose.popPose();
	}

	private void submitSegment(TankAttributes attributes, int index, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
		BlockState blockState = attributes.block().defaultBlockState().setValue(TankBlock.FORM, TankForm.at(index, attributes.height()))
				.setValue(TankBlock.SHOW_TIER, attributes.showsTier()).setValue(TankBlock.PACKED, attributes.packed());
		BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(Minecraft.getInstance().level, BlockPos.ZERO, blockState, RandomSource.create(42), parts);
		List<BakedQuad> opaque = new ArrayList<>();
		List<BakedQuad> translucent = new ArrayList<>();
		for (BlockStateModelPart part : parts) {
			addQuads(part.getQuads(null), opaque, translucent);
			for (Direction direction : Direction.values()) {
				addQuads(part.getQuads(direction), opaque, translucent);
			}
		}
		submitQuads(opaque, pose, collector, Sheets.cutoutBlockSheet(), light, overlay);
		submitQuads(translucent, pose, collector, Sheets.translucentBlockSheet(), light, overlay);
	}

	private static void addQuads(List<BakedQuad> quads, List<BakedQuad> opaque, List<BakedQuad> translucent) {
		for (BakedQuad quad : quads) {
			(quad.materialInfo().layer().translucent() ? translucent : opaque).add(quad);
		}
	}

	private static void submitQuads(List<BakedQuad> quads, PoseStack pose, SubmitNodeCollector collector,
			net.minecraft.client.renderer.rendertype.RenderType type, int light, int overlay) {
		if (quads.isEmpty()) {
			return;
		}
		collector.submitCustomGeometry(pose, type, (matrix, vertices) -> {
			QuadInstance instance = new QuadInstance();
			instance.setLightCoords(light);
			instance.setOverlayCoords(overlay);
			instance.setColor(-1);
			for (BakedQuad quad : quads) {
				vertices.putBakedQuad(matrix, quad, instance);
			}
		});
	}

	@Override
	public void getExtents(Consumer<Vector3fc> consumer) {
		consumer.accept(new org.joml.Vector3f(0, 0, 0));
		consumer.accept(new org.joml.Vector3f(1, 1, 1));
	}

	public record TankAttributes(TankBlock block, int height, boolean showsTier, boolean packed, FluidStack fluid, int capacity) {
	}

	public static class Unbaked implements SpecialModelRenderer.Unbaked<TankAttributes> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public @Nullable SpecialModelRenderer<TankAttributes> bake(BakingContext context) {
			return new TankItemRenderer();
		}

		@Override
		public MapCodec<? extends SpecialModelRenderer.Unbaked<TankAttributes>> type() {
			return MAP_CODEC;
		}
	}
}
