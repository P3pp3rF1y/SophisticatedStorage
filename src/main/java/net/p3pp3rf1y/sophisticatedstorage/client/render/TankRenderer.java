package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.TankForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TankRenderer extends StorageRenderer<TankBlockEntity> {
	private final DisplayItemRenderer displayItems = new DisplayItemRenderer(.5, new Vec3(0, 0, -.002));

	@Override
	public void render(TankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!tank.isPrimary()) {
			return;
		}
		renderFluid(tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0), tank.getStorageWrapper().getCapacity(), tank.getHeight(), pose, buffers,
				light);
		if (tank.isPacked()) {
			return;
		}
		renderHiddenTier(tank, pose, buffers, light, overlay);
		pose.pushPose();
		pose.translate(.5, .5, .5);
		pose.mulPose(DisplayItemRenderer.getNorthBasedRotation(tank.getBlockState().getValue(TankBlock.FACING)));
		pose.translate(-.5, -.5, -.5);
		if (tank.shouldShowUpgrades() || holdsItemThatShowsUpgrades()) {
			displayItems.renderUpgradeItems(tank, pose, buffers, light, overlay, holdsItemThatShowsUpgrades(), shouldShowDisabledUpgradesDisplay(tank));
		}
		LockRenderer.renderLock(tank, pose, buffers, light, overlay, tank.getHeight() - 1F / 16F, this::holdsToolInToggleLockOrLockDisplay);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(TankBlockEntity tank) {
		return tank.getHeight() > 1;
	}

	private void renderHiddenTier(TankBlockEntity tank, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (tank.shouldShowTier() || !holdsItemThatShowsHiddenTiers()) {
			return;
		}
		VertexConsumer vertices = TranslucentVertexConsumer.getVertexConsumer(buffers, 128);
		for (int index = 0; index < tank.getHeight(); index++) {
			BlockState state = tank.getBlockState().setValue(TankBlock.FORM, TankForm.at(index, tank.getHeight()));
			Set<TextureAtlasSprite> baseSprites = getModelQuads(state.setValue(TankBlock.SHOW_TIER, false)).stream().map(BakedQuad::getSprite)
					.collect(Collectors.toSet());
			pose.pushPose();
			pose.translate(0, index, 0);
			getModelQuads(state.setValue(TankBlock.SHOW_TIER, true)).stream().filter(quad -> !baseSprites.contains(quad.getSprite()))
					.forEach(quad -> vertices.putBulkData(pose.last(), quad, 1, 1, 1, 1, light, overlay, false));
			pose.popPose();
		}
	}

	private static List<BakedQuad> getModelQuads(BlockState state) {
		BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, RandomSource.create(42), ModelData.EMPTY, null));
		for (Direction side : Direction.values()) {
			quads.addAll(model.getQuads(state, side, RandomSource.create(42), ModelData.EMPTY, null));
		}
		return quads;
	}

	static void renderFluid(FluidStack fluid, int capacity, int height, PoseStack pose, MultiBufferSource buffers, int light) {
		if (fluid.isEmpty() || capacity <= 0) {
			return;
		}
		IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
		int color = extensions.getTintColor(fluid);
		light = LightTexture.pack(Math.max(LightTexture.block(light), fluid.getFluid().getFluidType().getLightLevel(fluid)), LightTexture.sky(light));
		float min = 2.51F / 16;
		float max = 1 - min;
		float floor = 2F / 16;
		float ceiling = height - floor;
		int availablePixels = height * 16 - 4;
		int filledPixels = (int) Math.min(availablePixels, ((long) fluid.getAmount() * availablePixels + capacity - 1) / capacity);
		float filled = filledPixels / 16F;
		boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
		float bottom = Math.max(floor + .01F / 16, gas ? ceiling - filled : floor);
		float top = Math.min(ceiling - .01F / 16, gas ? ceiling : floor + filled);
		float u0 = sprite.getU(min * 16);
		float u1 = sprite.getU(max * 16);
		VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
		for (int segment = 0; segment < height; segment++) {
			float y = Math.max(bottom, segment);
			float end = Math.min(top, segment + 1);
			if (end <= y) {
				continue;
			}
			float textureBottom = Math.max(floor, segment);
			float textureTop = Math.min(ceiling, segment + 1);
			float v0 = sprite.getV((gas ? textureTop - end : 1 - (end - textureBottom)) * 16);
			float v1 = sprite.getV((gas ? textureTop - y : 1 - (y - textureBottom)) * 16);
			quad(vertices, pose, color, light, u0, u1, v0, v1, Direction.NORTH, new float[][]{{min, y, min}, {min, end, min}, {max, end, min}, {max, y, min}});
			quad(vertices, pose, color, light, u0, u1, v0, v1, Direction.SOUTH, new float[][]{{max, y, max}, {max, end, max}, {min, end, max}, {min, y, max}});
			quad(vertices, pose, color, light, u0, u1, v0, v1, Direction.WEST, new float[][]{{min, y, max}, {min, end, max}, {min, end, min}, {min, y, min}});
			quad(vertices, pose, color, light, u0, u1, v0, v1, Direction.EAST, new float[][]{{max, y, min}, {max, end, min}, {max, end, max}, {max, y, max}});
		}
		quad(vertices, pose, color, light, u0, u1, sprite.getV(min * 16), sprite.getV(max * 16), Direction.UP,
				new float[][]{{min, top, min}, {min, top, max}, {max, top, max}, {max, top, min}});
		quad(vertices, pose, color, light, u0, u1, sprite.getV(min * 16), sprite.getV(max * 16), Direction.DOWN,
				new float[][]{{min, bottom, max}, {min, bottom, min}, {max, bottom, min}, {max, bottom, max}});
	}

	private static void quad(VertexConsumer vertices, PoseStack pose, int color, int light, float u0, float u1, float v0, float v1, Direction normal,
			float[][] points) {
		for (int index = 0; index < 4; index++) {
			float[] point = points[index];
			vertices.vertex(pose.last().pose(), point[0], point[1], point[2]).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
					.uv(index < 2 ? u0 : u1, index == 0 || index == 3 ? v1 : v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
					.normal(pose.last().normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ()).endVertex();
		}
	}
}
