package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
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

	private final LoadingCache<DoubleChestBlockEntityKey, ChestBlockEntity> doubleChestBlockEntities = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build(new CacheLoader<>() {
		@Override
		public ChestBlockEntity load(DoubleChestBlockEntityKey key) {
			return new ChestBlockEntity(BlockPos.ZERO, key.blockItem().getBlock().defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, key.chestType()));
		}
	});

	public static IClientItemExtensions getItemRenderProperties() {
		return new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return new ChestItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
			}
		};
	}

	public ChestItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
		super(blockEntityRenderDispatcher, entityModelSet);
		this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return;
		}

		if (ChestBlockItem.isDoubleChest(stack)) {
			ChestBlockEntity leftChestBlockEntity = doubleChestBlockEntities.getUnchecked(new DoubleChestBlockEntityKey(blockItem, ChestType.LEFT));
			poseStack.pushPose();
			poseStack.scale(0.8F, 0.8F, 0.8F);
			poseStack.translate(0.72D, 0.0D, 0.0D);
			renderBlockEntity(stack, poseStack, buffer, packedLight, packedOverlay, leftChestBlockEntity);
			ChestBlockEntity rightChestBlockEntity = doubleChestBlockEntities.getUnchecked(new DoubleChestBlockEntityKey(blockItem, ChestType.RIGHT));
			poseStack.translate(-1D, 0.0D, 0.0D);
			renderBlockEntity(stack, poseStack, buffer, packedLight, packedOverlay, rightChestBlockEntity);
			poseStack.popPose();
			return;
		}

		ChestBlockEntity chestBlockEntity = chestBlockEntities.getUnchecked(blockItem);
		renderBlockEntity(stack, poseStack, buffer, packedLight, packedOverlay, chestBlockEntity);
	}

	private void renderBlockEntity(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, ChestBlockEntity chestBlockEntity) {
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

	private record DoubleChestBlockEntityKey(BlockItem blockItem, ChestType chestType) {
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DoubleChestBlockEntityKey that = (DoubleChestBlockEntityKey) o;
			return Objects.equal(blockItem, that.blockItem) && chestType == that.chestType;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(blockItem, chestType);
		}
	}
}
