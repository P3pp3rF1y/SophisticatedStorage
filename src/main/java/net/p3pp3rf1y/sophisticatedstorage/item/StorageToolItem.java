package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Optional;

public class StorageToolItem extends ItemBase {

	private static final String CONTROLLER_POS_TAG = "controllerPos";

	public StorageToolItem() {
		super(new Item.Properties().stacksTo(1), SophisticatedStorage.CREATIVE_TAB);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		Block blockClicked = level.getBlockState(pos).getBlock();
		ItemStack tool = context.getItemInHand();
		if (blockClicked == ModBlocks.CONTROLLER.get()) {
			setControllerLink(tool, pos);
			return InteractionResult.SUCCESS;
		}
		if (WorldHelper.getBlockEntity(level, pos, ILinkable.class).map(linkable -> {
			getControllerLink(tool).ifPresentOrElse(linkable::linkToController, linkable::unlinkFromController);
			return true;
		}).orElse(false)) {
			return InteractionResult.SUCCESS;
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
		if (player.isShiftKeyDown()) {
			ItemStack tool = player.getItemInHand(usedHand);
			if (getControllerLink(tool).isPresent()) {
				removeControllerLink(tool);
				return InteractionResultHolder.success(tool);
			}
		}

		return super.use(level, player, usedHand);
	}

	private void setControllerLink(ItemStack tool, BlockPos pos) {
		NBTHelper.setLong(tool, CONTROLLER_POS_TAG, pos.asLong());
	}

	public static Optional<BlockPos> getControllerLink(ItemStack tool) {
		return NBTHelper.getLong(tool, CONTROLLER_POS_TAG).map(BlockPos::of);
	}

	private void removeControllerLink(ItemStack tool) {
		NBTHelper.removeTag(tool, CONTROLLER_POS_TAG);
	}

	public static Component getOverlayMessage(ItemStack tool) {
		return getControllerLink(tool).map(controllerPos -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(tool.getItem(), "linking", controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()))
				.orElseGet(() -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(tool.getItem(), "unlinking"));
	}
}
