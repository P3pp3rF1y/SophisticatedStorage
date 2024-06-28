package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class StorageToolItem extends ItemBase {

	public StorageToolItem() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
		tooltipComponents.addAll(StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
		String itemName = RegistryHelper.getItemKey(stack.getItem()).getPath();
		tooltipComponents.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(itemName) + ".controls",
				Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip(itemName) + ".controls.combination").withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
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
				if (tryToggling(pos, level, ILockable.class, ILockable::toggleLock)) {
					return InteractionResult.SUCCESS;
				}
			}
			case COUNT_DISPLAY -> {
				if (tryToggling(pos, level, ICountDisplay.class, ICountDisplay::toggleCountVisibility)) {
					return InteractionResult.SUCCESS;
				}
			}
			case LOCK_DISPLAY -> {
				if (tryToggling(pos, level, ILockable.class, ILockable::toggleLockVisibility)) {
					return InteractionResult.SUCCESS;
				}
			}
			case TIER_DISPLAY -> {
				if (tryToggling(pos, level, ITierDisplay.class, ITierDisplay::toggleTierVisiblity)) {
					return InteractionResult.SUCCESS;
				}
			}
			case UPGRADES_DISPLAY -> {
				if (tryToggling(pos, level, IUpgradeDisplay.class, IUpgradeDisplay::toggleUpgradesVisiblity)) {
					return InteractionResult.SUCCESS;
				}
			}
			case FILL_LEVEL_DISPLAY -> {
				if (tryToggling(pos, level, IFillLevelDisplay.class, IFillLevelDisplay::toggleFillLevelVisibility)) {
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.onItemUseFirst(tool, context);
	}

	private static <T> boolean tryToggling(BlockPos pos, Level level, Class<T> clazz, Consumer<T> toggle) {
		return WorldHelper.getLoadedBlockEntity(level, pos, clazz).map(be -> {
			if (level.isClientSide()) {
				return true;
			}

			toggle.accept(be);
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
		tool.set(ModDataComponents.CONTROLLER_POS, pos);
	}

	public static Optional<BlockPos> getControllerLink(ItemStack tool) {
		return Optional.ofNullable(tool.get(ModDataComponents.CONTROLLER_POS));
	}

	private void removeControllerLink(ItemStack tool) {
		tool.remove(ModDataComponents.CONTROLLER_POS);
	}

	public static Component getOverlayMessage(ItemStack tool) {
		Mode mode = getMode(tool);
		Item item = tool.getItem();
		return switch (mode) {
			case LINK ->
					getControllerLink(tool).map(controllerPos -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "linking", controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()))
							.orElseGet(() -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "unlinking"));
			case LOCK -> StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_lock");
			case LOCK_DISPLAY ->
					StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_lock_display");
			case COUNT_DISPLAY ->
					StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_count_display");
			case TIER_DISPLAY ->
					StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_tier_display");
			case UPGRADES_DISPLAY ->
					StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_upgrades_display");
			case FILL_LEVEL_DISPLAY ->
					StorageTranslationHelper.INSTANCE.translItemOverlayMessage(item, "toggling_fill_level_display");
		};
	}

	public static Mode getMode(ItemStack tool) {
		Mode mode = tool.get(ModDataComponents.TOOL_MODE);
		return mode != null ? mode : Mode.LINK;
	}

	public static void cycleMode(ItemStack tool, boolean next) {
		tool.set(ModDataComponents.TOOL_MODE, next ? getMode(tool).next() : getMode(tool).previous());
	}

	public enum Mode implements StringRepresentable {
		LINK,
		LOCK,
		COUNT_DISPLAY,
		LOCK_DISPLAY,
		TIER_DISPLAY,
		UPGRADES_DISPLAY,
		FILL_LEVEL_DISPLAY;

		public static final StringRepresentable.EnumCodec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
		public static final StreamCodec<FriendlyByteBuf, Mode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Mode.class);

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
