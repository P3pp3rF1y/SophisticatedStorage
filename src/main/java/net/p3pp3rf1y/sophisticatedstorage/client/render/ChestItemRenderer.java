package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class ChestItemRenderer extends BlockEntityWithoutLevelRenderer {
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

	public ChestItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
		super(blockEntityRenderDispatcher, entityModelSet);
		this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
	}

	@Override
	public void renderByItem(ItemStack pStack, ItemTransforms.TransformType pTransformType, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
		if (!(pStack.getItem() instanceof BlockItem blockItem)) {
			return;
		}
		//
		ChestBlockEntity chestBlockEntity = new ChestBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		WoodStorageBlockItem.getWoodType(pStack).ifPresent(chestBlockEntity::setWoodType);
		if (pStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			tintableBlockItem.getMainColor(pStack).ifPresent(chestBlockEntity.getStorageWrapper()::setMainColor);
			tintableBlockItem.getAccentColor(pStack).ifPresent(chestBlockEntity.getStorageWrapper()::setAccentColor);
		}
		var blockentityrenderer = blockEntityRenderDispatcher.getRenderer(chestBlockEntity);
		if (blockentityrenderer != null) {
			blockentityrenderer.render(chestBlockEntity, 0.0F, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
		}
	}
}
