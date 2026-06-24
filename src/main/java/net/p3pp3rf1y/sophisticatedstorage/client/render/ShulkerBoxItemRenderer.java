package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import javax.annotation.Nullable;

public class ShulkerBoxItemRenderer implements SpecialModelRenderer<ShulkerBoxItemRenderer.ShulkerBoxAttributes> {
	private final LoadingCache<BlockItem, ShulkerBoxBlockEntity> shulkerBoxBlockEntities = CacheBuilder.newBuilder().maximumSize(512L).weakKeys()
			.build(new CacheLoader<>() {
				@Override
				public ShulkerBoxBlockEntity load(BlockItem blockItem) {
					return new ShulkerBoxBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.SOUTH));
				}
			});

	@Nullable
	@Override
	public ShulkerBoxAttributes extractArgument(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return null;
		}
		int mainColor = -1;
		int accentColor = -1;
		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			mainColor = tintableBlockItem.getMainColor(stack).orElse(-1);
			accentColor = tintableBlockItem.getAccentColor(stack).orElse(-1);
		}
		boolean showsTier = StorageBlockItem.showsTier(stack);
		return new ShulkerBoxAttributes(blockItem, mainColor, accentColor, showsTier);
	}

	@Override
	public void render(@Nullable ShulkerBoxAttributes shulkerBoxAttributes, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasFoil) {
		if (shulkerBoxAttributes == null) {
			return;
		}

		ShulkerBoxBlockEntity shulkerBoxBlockEntity = shulkerBoxBlockEntities.getUnchecked(shulkerBoxAttributes.blockItem());
		shulkerBoxBlockEntity.getStorageWrapper().setColors(shulkerBoxAttributes.mainColor(), shulkerBoxAttributes.accentColor());
		if (shulkerBoxAttributes.showsTier() != shulkerBoxBlockEntity.shouldShowTier()) {
			shulkerBoxBlockEntity.toggleTierVisiblity();
		}
		var blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(shulkerBoxBlockEntity);
		if (blockentityrenderer != null) {
			blockentityrenderer.render(shulkerBoxBlockEntity, 0.0F, poseStack, buffer, packedLight, packedOverlay);
		}
	}

	public record ShulkerBoxAttributes(BlockItem blockItem, int mainColor, int accentColor, boolean showsTier) {
	}

	public static class Unbaked implements SpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Nullable
		@Override
		public SpecialModelRenderer<?> bake(EntityModelSet entityModelSet) {
			return new ShulkerBoxItemRenderer();
		}

		@Override
		public MapCodec<? extends Unbaked> type() {
			return MAP_CODEC;
		}
	}
}
