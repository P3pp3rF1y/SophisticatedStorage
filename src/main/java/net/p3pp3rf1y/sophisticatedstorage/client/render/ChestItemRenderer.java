package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class ChestItemRenderer implements SpecialModelRenderer<ChestItemRenderer.ChestAttributes> {
	private final LoadingCache<BlockItem, ChestBlockEntity> chestBlockEntities = CacheBuilder.newBuilder().maximumSize(512L).weakKeys()
			.build(new CacheLoader<>() {
				@Override
				public ChestBlockEntity load(BlockItem blockItem) {
					return new ChestBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
				}
			});

	private final LoadingCache<DoubleChestBlockEntityKey, ChestBlockEntity> doubleChestBlockEntities = CacheBuilder.newBuilder().maximumSize(512L).weakKeys()
			.build(new CacheLoader<>() {
				@Override
				public ChestBlockEntity load(DoubleChestBlockEntityKey key) {
					return new ChestBlockEntity(BlockPos.ZERO, key.blockItem().getBlock().defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH)
							.setValue(ChestBlock.TYPE, key.chestType()));
				}
			});

	@Nullable
	@Override
	public ChestAttributes extractArgument(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return null;
		}
		boolean isDoubleChest = ChestBlockItem.isDoubleChest(stack);
		int mainColor = -1;
		int accentColor = -1;
		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			mainColor = tintableBlockItem.getMainColor(stack).orElse(-1);
			accentColor = tintableBlockItem.getAccentColor(stack).orElse(-1);
		}
		Optional<WoodType> woodType = WoodStorageBlockItem.getWoodType(stack);
		boolean isPacked = WoodStorageBlockItem.isPacked(stack);
		boolean showsTier = StorageBlockItem.showsTier(stack);
		return new ChestAttributes(blockItem, isDoubleChest, mainColor, accentColor, woodType, isPacked, showsTier);
	}

	@Override
	public void submit(@Nullable ChestAttributes chestAttributes, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
			int packedOverlay, boolean hasFoil, int color) {
		if (chestAttributes == null) {
			return;
		}

		if (chestAttributes.isDoubleChest()) {
			ChestBlockEntity leftChestBlockEntity = doubleChestBlockEntities
					.getUnchecked(new DoubleChestBlockEntityKey(chestAttributes.blockItem(), ChestType.LEFT));
			poseStack.pushPose();
			poseStack.scale(0.8F, 0.8F, 0.8F);
			poseStack.translate(0.72D, 0.0D, 0.0D);
			renderBlockEntity(chestAttributes, poseStack, submitNodeCollector, packedLight, packedOverlay, leftChestBlockEntity);
			ChestBlockEntity rightChestBlockEntity = doubleChestBlockEntities
					.getUnchecked(new DoubleChestBlockEntityKey(chestAttributes.blockItem(), ChestType.RIGHT));
			poseStack.translate(-1D, 0.0D, 0.0D);
			renderBlockEntity(chestAttributes, poseStack, submitNodeCollector, packedLight, packedOverlay, rightChestBlockEntity);
			poseStack.popPose();
			return;
		}

		ChestBlockEntity chestBlockEntity = chestBlockEntities.getUnchecked(chestAttributes.blockItem());
		renderBlockEntity(chestAttributes, poseStack, submitNodeCollector, packedLight, packedOverlay, chestBlockEntity);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> consumer) {
		PoseStack posestack = new PoseStack();
		ChestBlockEntity chestBlockEntity = chestBlockEntities.getUnchecked(ModBlocks.CHEST_ITEM.get());
		BlockEntityRenderer<ChestBlockEntity, ChestRenderer.ChestRenderState> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher()
				.getRenderer(chestBlockEntity);
		if (blockentityrenderer instanceof ChestRenderer chestRenderer) {
			chestRenderer.rootModelPart().getExtentsForGui(posestack, consumer);
		}
	}

	private void renderBlockEntity(ChestAttributes chestAttributes, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
			int packedOverlay, ChestBlockEntity chestBlockEntity) {
		chestBlockEntity.getStorageWrapper().setColors(chestAttributes.mainColor(), chestAttributes.accentColor());
		Optional<WoodType> woodType = chestAttributes.woodType();
		if (woodType.isPresent() || !(chestBlockEntity.getStorageWrapper().hasAccentColor() && chestBlockEntity.getStorageWrapper().hasMainColor())) {
			chestBlockEntity.setWoodType(woodType.orElse(WoodType.ACACIA));
		}
		chestBlockEntity.setPacked(chestAttributes.isPacked());
		if (chestAttributes.showsTier() != chestBlockEntity.shouldShowTier()) {
			chestBlockEntity.toggleTierVisiblity();
		}
		BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
		var blockentityrenderer = blockEntityRenderDispatcher.getRenderer(chestBlockEntity);
		if (blockentityrenderer != null) {
			BlockEntityRenderState renderState = blockentityrenderer.createRenderState();
			blockentityrenderer.extractRenderState(chestBlockEntity, renderState, 0, Vec3.ZERO, null);
			blockentityrenderer.submit(renderState, poseStack, submitNodeCollector, RenderHelper.ZERO_POS_CAMERA_RENDER_STATE);
		}
	}

	public record ChestAttributes(BlockItem blockItem, boolean isDoubleChest, int mainColor, int accentColor, Optional<WoodType> woodType, boolean isPacked,
			boolean showsTier) {
	}

	private record DoubleChestBlockEntityKey(BlockItem blockItem, ChestType chestType) {
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			DoubleChestBlockEntityKey that = (DoubleChestBlockEntityKey) o;
			return Objects.equal(blockItem, that.blockItem) && chestType == that.chestType;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(blockItem, chestType);
		}
	}

	public static class Unbaked implements SpecialModelRenderer.Unbaked<ChestAttributes> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Nullable
		@Override
		public SpecialModelRenderer<ChestAttributes> bake(BakingContext bakingContext) {
			return new ChestItemRenderer();
		}

		@Override
		public MapCodec<? extends SpecialModelRenderer.Unbaked<ChestAttributes>> type() {
			return MAP_CODEC;
		}
	}
}
