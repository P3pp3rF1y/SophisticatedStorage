package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.Optional;

public class ChestItemRenderer extends BlockEntityWithoutLevelRenderer {
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	private final LoadingCache<BlockItem, ChestBlockEntity> chestBlockEntities = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build(new CacheLoader<>() {
		@Override
		public ChestBlockEntity load(BlockItem blockItem) {
			return new ChestBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		}
	});

	public ChestItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
		super(blockEntityRenderDispatcher, entityModelSet);
		this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return;
		}

		ChestBlockEntity chestBlockEntity = chestBlockEntities.getUnchecked(blockItem);

		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			chestBlockEntity.getStorageWrapper().setMainColor(tintableBlockItem.getMainColor(stack).orElse(-1));
			chestBlockEntity.getStorageWrapper().setAccentColor(tintableBlockItem.getAccentColor(stack).orElse(-1));
		}
		Optional<WoodType> woodType = WoodStorageBlockItem.getWoodType(stack);
		if (woodType.isPresent() || !(chestBlockEntity.getStorageWrapper().hasAccentColor() && chestBlockEntity.getStorageWrapper().hasMainColor())) {
			chestBlockEntity.setWoodType(woodType.orElse(WoodType.ACACIA));
		}
		chestBlockEntity.setPacked(WoodStorageBlockItem.isPacked(stack));
		if (StorageBlockItem.showsTier(stack) != chestBlockEntity.shouldShowTier()) {
			chestBlockEntity.toggleTierVisiblity();
		}
		var blockentityrenderer = blockEntityRenderDispatcher.getRenderer(chestBlockEntity);
		if (blockentityrenderer != null) {
			blockentityrenderer.render(chestBlockEntity, 0.0F, poseStack, buffer, packedLight, packedOverlay);
		}
	}
}
