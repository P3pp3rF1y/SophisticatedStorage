package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.List;

public class SimpleMaterialOverlayRenderer<T extends BlockEntity & ISimpleMaterialHolder> implements BlockEntityRenderer<T> {
	private static long lastCacheTime = -1;
	private static boolean holdsStorageToolThatShowsHiddenOverlay = false;

	@Override
	public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		renderHiddenOverlay(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	public static void renderHiddenOverlay(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!(blockEntity instanceof ISimpleMaterialHolder simpleMaterialHolder) || !simpleMaterialHolder.isOverlayHidden()
				|| !holdsStorageToolThatShowsHiddenOverlay()) {
			return;
		}

		simpleMaterialHolder.getMaterial().ifPresent(material -> {
			ModelData overlayModelData = ModelData.builder().with(SimpleMaterialModel.MATERIAL, material).with(SimpleMaterialModel.OVERLAY_ONLY, true)
					.with(SimpleMaterialModel.OVERLAY_EXPANDED, true).build();
			poseStack.pushPose();
			poseStack.translate(-0.005, -0.005, -0.005);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			renderOverlayOnly(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, overlayModelData);
			if (blockEntity instanceof StorageLinkBlockEntity) {
				renderOverlayOnly(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, overlayModelData);
			}
			poseStack.popPose();
		});
	}

	private static void renderOverlayOnly(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
			ModelData overlayModelData) {
		BlockState state = blockEntity.getBlockState();
		BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		VertexConsumer vertexConsumer = TranslucentVertexConsumer.getVertexConsumer(bufferSource, 255);
		RandomSource random = RandomSource.create(42L);
		for (Direction direction : Direction.values()) {
			renderQuads(blockModel.getQuads(state, direction, random, overlayModelData, RenderType.translucent()), poseStack, vertexConsumer, packedLight,
					packedOverlay);
			random.setSeed(42L);
		}
		renderQuads(blockModel.getQuads(state, null, random, overlayModelData, RenderType.translucent()), poseStack, vertexConsumer, packedLight,
				packedOverlay);
	}

	private static void renderQuads(List<BakedQuad> quads, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
		for (BakedQuad quad : quads) {
			vertexConsumer.putBulkData(poseStack.last(), quad, 1, 1, 1, 1, packedLight, packedOverlay, false);
		}
	}

	private static boolean holdsStorageToolThatShowsHiddenOverlay() {
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
}
