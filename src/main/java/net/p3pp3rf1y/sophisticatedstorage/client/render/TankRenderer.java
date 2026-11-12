package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.TankForm;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TankRenderer extends StorageRenderer<TankBlockEntity, TankRenderer.TankRenderState> {
	private final DisplayItemRenderer displayItems = new DisplayItemRenderer(.5, new Vec3(0, 0, -.002));

	public TankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public TankRenderState createRenderState() {
		return new TankRenderState();
	}

	@Override
	public void extractRenderState(TankBlockEntity tank, TankRenderState state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(tank, state, partialTick, cameraPos, crumblingOverlay);
		state.primary = tank.isPrimary();
		if (!state.primary) {
			return;
		}
		state.packed = tank.isPacked();
		state.height = tank.getHeight();
		state.facing = tank.getBlockState().getValue(TankBlock.FACING);
		state.fluid = tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).copy();
		state.capacity = tank.getStorageWrapper().getCapacity();
		state.tierQuads = List.of();
		if (state.packed) {
			return;
		}
		if (!state.showsTier && holdsItemThatShowsHiddenTiers()) {
			List<List<BakedQuad>> quads = new ArrayList<>();
			for (int index = 0; index < state.height; index++) {
				BlockState blockState = tank.getBlockState().setValue(TankBlock.FORM, TankForm.at(index, state.height));
				Set<TextureAtlasSprite> base = getModelQuads(blockState.setValue(TankBlock.SHOW_TIER, false)).stream().map(quad -> quad.materialInfo().sprite())
						.collect(Collectors.toSet());
				quads.add(getModelQuads(blockState.setValue(TankBlock.SHOW_TIER, true)).stream().filter(quad -> !base.contains(quad.materialInfo().sprite()))
						.toList());
			}
			state.tierQuads = quads;
		}
	}

	@Override
	public void submit(TankRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.primary) {
			return;
		}
		submitFluid(state.fluid, state.capacity, state.height, pose, collector, state.lightCoords);
		if (state.packed) {
			return;
		}
		if (!state.showsTier && !state.tierQuads.isEmpty()) {
			for (int segment = 0; segment < state.tierQuads.size(); segment++) {
				pose.pushPose();
				pose.translate(0, segment, 0);
				SimpleMaterialOverlayRenderer.submitHiddenOverlayQuads(collector, pose, state.lightCoords, state.tierQuads.get(segment), false);
				pose.popPose();
			}
		}
		pose.pushPose();
		pose.translate(.5, .5, .5);
		pose.mulPose(DisplayItemRenderer.getNorthBasedRotation(state.facing));
		pose.translate(-.5, -.5, -.5);
		if (state.showsUpgrades || holdsItemThatShowsUpgrades()) {
			displayItems.submitUpgradeItems(collector, state, pose, OverlayTexture.NO_OVERLAY, holdsItemThatShowsUpgrades());
		}
		LockRenderer.submitLock(collector, state, pose, state.height - 1F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(TankBlockEntity tank) {
		return new AABB(tank.getBlockPos()).expandTowards(0, tank.getHeight() - 1, 0);
	}

	private static List<BakedQuad> getModelQuads(BlockState state) {
		BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(Minecraft.getInstance().level, net.minecraft.core.BlockPos.ZERO, state, RandomSource.create(42), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart part : parts) {
			quads.addAll(part.getQuads(null));
			for (Direction side : Direction.values()) {
				quads.addAll(part.getQuads(side));
			}
		}
		return quads;
	}

	static void submitFluid(FluidStack fluid, int capacity, int height, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (fluid.isEmpty() || capacity <= 0) {
			return;
		}
		var fluidState = fluid.getFluid().defaultFluidState();
		FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
		TextureAtlasSprite sprite = model.stillMaterial().sprite();
		int color = model.tintSource() instanceof FluidTintSource tint
				? tint.colorAsStack(fluid)
				: model.tintSource() != null ? model.tintSource().color(fluidState.createLegacyBlock()) : -1;
		int fluidLight = light & ~0xF0 | Math.max((light >> 4) & 15, fluid.getFluidType().getLightLevel(fluid)) << 4;
		float min = 2.51F / 16;
		float max = 1 - min;
		float floor = 2F / 16;
		float ceiling = height - floor;
		int availablePixels = height * 16 - 4;
		int filledPixels = (int) Math.min(availablePixels, ((long) fluid.getAmount() * availablePixels + capacity - 1) / capacity);
		float filled = filledPixels / 16F;
		boolean gas = fluid.getFluidType().isLighterThanAir();
		float bottom = Math.max(floor + .01F / 16, gas ? ceiling - filled : floor);
		float top = Math.min(ceiling - .01F / 16, gas ? ceiling : floor + filled);
		float u0 = sprite.getU(min);
		float u1 = sprite.getU(max);
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(sprite.atlasLocation()), (matrix, vertices) -> {
			for (int segment = 0; segment < height; segment++) {
				float y = Math.max(bottom, segment);
				float end = Math.min(top, segment + 1);
				if (end <= y) {
					continue;
				}
				float textureBottom = Math.max(floor, segment);
				float textureTop = Math.min(ceiling, segment + 1);
				float v0 = sprite.getV(gas ? textureTop - end : 1 - (end - textureBottom));
				float v1 = sprite.getV(gas ? textureTop - y : 1 - (y - textureBottom));
				quad(vertices, matrix, color, fluidLight, u0, u1, v0, v1, Direction.NORTH,
						new float[][]{{min, y, min}, {min, end, min}, {max, end, min}, {max, y, min}});
				quad(vertices, matrix, color, fluidLight, u0, u1, v0, v1, Direction.SOUTH,
						new float[][]{{max, y, max}, {max, end, max}, {min, end, max}, {min, y, max}});
				quad(vertices, matrix, color, fluidLight, u0, u1, v0, v1, Direction.WEST,
						new float[][]{{min, y, max}, {min, end, max}, {min, end, min}, {min, y, min}});
				quad(vertices, matrix, color, fluidLight, u0, u1, v0, v1, Direction.EAST,
						new float[][]{{max, y, min}, {max, end, min}, {max, end, max}, {max, y, max}});
			}
			quad(vertices, matrix, color, fluidLight, u0, u1, sprite.getV(min), sprite.getV(max), Direction.UP,
					new float[][]{{min, top, min}, {min, top, max}, {max, top, max}, {max, top, min}});
			quad(vertices, matrix, color, fluidLight, u0, u1, sprite.getV(min), sprite.getV(max), Direction.DOWN,
					new float[][]{{min, bottom, max}, {min, bottom, min}, {max, bottom, min}, {max, bottom, max}});
		});
	}

	private static void quad(VertexConsumer vertices, PoseStack.Pose pose, int color, int light, float u0, float u1, float v0, float v1, Direction normal,
			float[][] points) {
		for (int index = 0; index < 4; index++) {
			float[] point = points[index];
			vertices.addVertex(pose, point[0], point[1], point[2]).setColor(color).setUv(index < 2 ? u0 : u1, index == 0 || index == 3 ? v1 : v0)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
		}
	}

	public static class TankRenderState extends StorageRenderState {
		public boolean primary;
		public boolean packed;
		public int height;
		public Direction facing = Direction.NORTH;
		public FluidStack fluid = FluidStack.EMPTY;
		public int capacity;
		public List<List<BakedQuad>> tierQuads = List.of();
	}
}
