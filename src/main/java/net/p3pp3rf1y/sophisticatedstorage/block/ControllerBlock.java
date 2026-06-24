package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.util.BlockBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import javax.annotation.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControllerBlock extends BlockBase implements ISneakItemInteractionBlock, EntityBlock {
	public ControllerBlock() {
		super(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3F, 6.0F));
		registerDefaultState(stateDefinition.any().setValue(SimpleMaterialBlockData.OPAQUE, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SimpleMaterialBlockData.OPAQUE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(SimpleMaterialBlockData.OPAQUE, SimpleMaterialBlockData.isOpaque(context.getItemInHand()));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltipComponents, TooltipFlag flag) {
		tooltipComponents.addAll(StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX,
				null, ChatFormatting.DARK_GRAY));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ControllerBlockEntity(pos, state);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			WorldHelper.getBlockEntity(level, pos, ControllerBlockEntityBase.class).ifPresent(ControllerBlockEntityBase::detachFromStoragesAndUnlinkBlocks);
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(SimpleMaterialBlockData.OPAQUE) ? Shapes.block() : Shapes.empty();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		SimpleMaterialBlockData.copyToBlockEntity(level, pos, stack);
		if (level.isClientSide()) {
			return;
		}
		WorldHelper.getBlockEntity(level, pos, ControllerBlockEntity.class).ifPresent(ControllerBlockEntityBase::searchAndAddBoundables);
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		WorldHelper.getBlockEntity(level, pos, ControllerBlockEntity.class).ifPresent(controller -> {
			AtomicBoolean appliedUpgrade = new AtomicBoolean(false);
			controller.getStoragePositions().forEach(storagePos -> WorldHelper.getBlockEntity(level, storagePos, StorageBlockEntity.class).ifPresent(be -> {
				if (be.getBlockState().getBlock() instanceof StorageBlockBase storageblock) {
					ItemStack itemInHand = player.getItemInHand(hand);
					if (storageblock.tryAddSingleUpgrade(player, hand, be, itemInHand)) {
						appliedUpgrade.set(true);
					} else if (itemInHand.getItem() instanceof StorageTierUpgradeItem storageTierUpgradeItem && storageTierUpgradeItem
							.tryUpgradeStorage(itemInHand, level, storagePos, be.getBlockState(), player) == InteractionResult.SUCCESS) {
						appliedUpgrade.set(true);
					}
				}
			}));

			if (!appliedUpgrade.get()) {
				controller.depositPlayerItems(player, hand);
			}
		});

		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
		ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
		SimpleMaterialBlockData.copyFromBlockEntity(level, pos, stack);
		return stack;
	}

	@Override
	public boolean trySneakItemInteraction(Player player, InteractionHand hand, BlockState state, Level level, BlockPos pos, BlockHitResult hitVec,
			ItemStack itemInHand) {
		if (level.isClientSide()) {
			return false;
		}

		return WorldHelper.getBlockEntity(level, pos, ControllerBlockEntity.class).map(controller -> {
			AtomicBoolean result = new AtomicBoolean(false);
			controller.getStoragePositions().forEach(storagePos -> {
				Block block = level.getBlockState(storagePos).getBlock();
				if (block instanceof StorageBlockBase storageblock
						&& storageblock.tryFillUpgrades(player, hand, level, storagePos, player.getItemInHand(hand))) {
					result.set(true);
				}
			});
			return result.get();
		}).orElse(false);
	}
}
