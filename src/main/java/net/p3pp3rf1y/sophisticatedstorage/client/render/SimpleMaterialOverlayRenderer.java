package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
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
	public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		renderHiddenOverlay(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
	}

	public static void renderHiddenOverlay(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!(blockEntity instanceof ISimpleMaterialHolder simpleMaterialHolder) || !simpleMaterialHolder.isOverlayHidden() || !holdsStorageToolThatShowsHiddenOverlay()) {
			return;
		}

		simpleMaterialHolder.getMaterial().ifPresent(material -> {
			poseStack.pushPose();
			poseStack.translate(-0.005, -0.005, -0.005);
			poseStack.scale(1.01f, 1.01f, 1.01f);
			renderOverlayOnly(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, material);
			if (blockEntity instanceof StorageLinkBlockEntity) {
				renderOverlayOnly(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, material);
			}
			poseStack.popPose();
		});
	}

	private static void renderOverlayOnly(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ResourceLocation material) {
		BlockState state = blockEntity.getBlockState();
		BlockStateModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		if (!(blockModel instanceof SimpleMaterialModel.SimpleMaterialBlockStateModel simpleMaterialModel)) {
			return;
		}
		VertexConsumer vertexConsumer = TranslucentVertexConsumer.getVertexConsumer(bufferSource, 255);
		renderQuads(simpleMaterialModel.getOverlayOnlyQuads(material, true), poseStack, vertexConsumer, packedLight, packedOverlay);
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
