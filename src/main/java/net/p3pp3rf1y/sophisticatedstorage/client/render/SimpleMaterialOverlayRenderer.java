package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SimpleMaterialOverlayRenderer<T extends BlockEntity & ISimpleMaterialHolder> implements BlockEntityRenderer<T, SimpleMaterialOverlayRenderer.SimpleMaterialOverlayRenderState> {
	private static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
	private static long lastCacheTime = -1;
	private static boolean holdsStorageToolThatShowsHiddenOverlay = false;

	@Override
	public SimpleMaterialOverlayRenderState createRenderState() {
		return new SimpleMaterialOverlayRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, SimpleMaterialOverlayRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);
		renderState.quads = Collections.emptyList();
		renderState.doubleRender = false;
		if (!blockEntity.isOverlayHidden() || !holdsStorageToolThatShowsHiddenOverlay()) {
			return;
		}

		blockEntity.getMaterial().ifPresent(material -> {
			BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockEntity.getBlockState());
			if (blockModel instanceof SimpleMaterialModel.SimpleMaterialBlockStateModel simpleMaterialModel) {
				renderState.quads = simpleMaterialModel.getOverlayOnlyQuads(material, true);
				renderState.doubleRender = blockEntity instanceof StorageLinkBlockEntity;
			}
		});
	}

	@Override
	public void submit(SimpleMaterialOverlayRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		submitHiddenOverlayQuads(submitNodeCollector, poseStack, renderState.lightCoords, renderState.quads, renderState.doubleRender);
	}

	public static void submitHiddenOverlayQuads(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int lightCoords, List<BakedQuad> quads, boolean doubleRender) {
		if (quads.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(-0.005, -0.005, -0.005);
		poseStack.scale(1.01f, 1.01f, 1.01f);
		submitOverlayQuads(submitNodeCollector, poseStack, lightCoords, quads);
		if (doubleRender) {
			submitOverlayQuads(submitNodeCollector, poseStack, lightCoords, quads);
		}
		poseStack.popPose();
	}

	private static void submitOverlayQuads(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int lightCoords, List<BakedQuad> quads) {
		submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, vertexConsumer) -> {
			for (BakedQuad quad : quads) {
				vertexConsumer.putBulkData(pose, quad, 1, 1, 1, 1, lightCoords, OverlayTexture.NO_OVERLAY);
			}
		});
	}

	public static boolean holdsStorageToolThatShowsHiddenOverlay() {
		refreshCache();
		return holdsStorageToolThatShowsHiddenOverlay;
	}

	private static void refreshCache() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.level.getGameTime() == lastCacheTime) {
			return;
		}

		lastCacheTime = minecraft.level.getGameTime();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			holdsStorageToolThatShowsHiddenOverlay = false;
			return;
		}

		holdsStorageToolThatShowsHiddenOverlay = InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
				.map(storageTool -> StorageToolItem.getMode(storageTool) == StorageToolItem.Mode.TIER_DISPLAY).orElse(false);
	}

	public static class SimpleMaterialOverlayRenderState extends BlockEntityRenderState {
		public List<BakedQuad> quads = Collections.emptyList();
		public boolean doubleRender = false;
	}
}
