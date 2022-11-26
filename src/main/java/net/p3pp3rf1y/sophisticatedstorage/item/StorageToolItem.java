package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
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
import net.p3pp3rf1y.sophisticatedstorage.block.ICountDisplay;
import net.p3pp3rf1y.sophisticatedstorage.block.ILockable;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Optional;

public class StorageToolItem extends ItemBase {

	private static final String CONTROLLER_POS_TAG = "controllerPos";
	private static final String MODE_TAG = "mode";

	public StorageToolItem() {
		super(new Item.Properties().stacksTo(1), SophisticatedStorage.CREATIVE_TAB);
	}

	public static void useOffHandOnPlaced(ItemStack tool, StorageBlockEntity be) {
		if (be.getLevel().isClientSide()) {
			return;
		}
		Mode mode = getMode(tool);

		if (mode == Mode.LINK) {
			getControllerLink(tool).ifPresentOrElse(be::linkToController, be::unlinkFromController);
		} else if (mode == Mode.LOCK) {
			be.toggleLock();
		}
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack tool, UseOnContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		Block blockClicked = level.getBlockState(pos).getBlock();
		Mode mode = getMode(tool);
		switch (mode) {
			case LINK -> {
				if (tryLinking(pos, level, blockClicked, tool)) {
					return InteractionResult.SUCCESS;
				}
			}
			case LOCK -> {
				if (tryLocking(pos, level)) {
					return InteractionResult.SUCCESS;
				}
			}
			case COUNT_DISPLAY -> {
				if (tryTogglingCountDisplay(pos, level)) {
					return InteractionResult.SUCCESS;
				}
			}
			case LOCK_DISPLAY -> {
				if (tryTogglingLockDisplay(pos, level)) {
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.onItemUseFirst(tool, context);
	}

	private boolean tryTogglingLockDisplay(BlockPos pos, Level level) {
		return WorldHelper.getLoadedBlockEntity(level, pos, ILockable.class).map(lockable -> {
			if (level.isClientSide()) {
				return true;
			}

			lockable.toggleLockVisibility();
			return true;
		}).orElse(false);
	}

	private boolean tryTogglingCountDisplay(BlockPos pos, Level level) {
		return WorldHelper.getLoadedBlockEntity(level, pos, ICountDisplay.class).map(countDisplay -> {
			if (level.isClientSide()) {
				return true;
			}

			countDisplay.toggleCountVisibility();
			return true;
		}).orElse(false);
	}

	private boolean tryLocking(BlockPos pos, Level level) {
		return WorldHelper.getLoadedBlockEntity(level, pos, ILockable.class).map(lockable -> {
			if (level.isClientSide()) {
				return true;
			}

			lockable.toggleLock();
			return true;
		}).orElse(false);
	}

	private boolean tryLinking(BlockPos pos, Level level, Block blockClicked, ItemStack tool) {
		if (blockClicked == ModBlocks.CONTROLLER.get()) {
			setControllerLink(tool, pos);
			return true;
		}
		return WorldHelper.getBlockEntity(level, pos, ILinkable.class).map(linkable -> {
			getControllerLink(tool).ifPresentOrElse(linkable::linkToController, linkable::unlinkFromController);
			return true;
		}).orElse(false);
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
		Mode mode = getMode(tool);
		Item item = tool.getItem();
		return switch (mode) {
			case LINK ->
					getControllerLink(tool).map(controllerPos -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "linking", controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()))
							.orElseGet(() -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "unlinking"));
			case LOCK -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_lock");
			case LOCK_DISPLAY -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_lock_display");
			case COUNT_DISPLAY -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_count_display");
		};
	}

	public static Mode getMode(ItemStack tool) {
		return NBTHelper.getEnumConstant(tool, MODE_TAG, Mode::valueOf).orElse(Mode.LINK);
	}

	public static void cycleMode(ItemStack tool, boolean next) {
		NBTHelper.setEnumConstant(tool, MODE_TAG, (next ? getMode(tool).next() : getMode(tool).previous()));
	}

	public enum Mode implements StringRepresentable {
		LINK,
		LOCK,
		COUNT_DISPLAY,
		LOCK_DISPLAY;

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		public Mode previous() {
			return values()[Math.floorMod(ordinal() - 1, values().length)];
		}

		@Override
		public String getSerializedName() {
			return name();
		}
	}

}
