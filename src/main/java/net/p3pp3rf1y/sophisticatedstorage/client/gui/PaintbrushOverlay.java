package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class PaintbrushOverlay {

	private static Optional<PaintbrushItem.ItemRequirements> ITEM_REQUIREMENTS_CACHE = Optional.empty();
	@Nullable
	private static BlockPos lastPosCached = null;
	@Nullable
	private static ItemStack lastPaintbrushCached = null;

	public static Optional<PaintbrushItem.ItemRequirements> getItemRequirementsFor(ItemStack paintbrush, Player player, Level level, BlockPos pos) {
		if (!pos.equals(lastPosCached) || paintbrush != lastPaintbrushCached) {
			ITEM_REQUIREMENTS_CACHE = PaintbrushItem.getItemRequirements(paintbrush, player, level, pos);
			lastPosCached = pos;
			lastPaintbrushCached = paintbrush;
		}
		return ITEM_REQUIREMENTS_CACHE;
	}

	public static final GuiLayer HUD_PAINTBRUSH_INFO = (guiGraphics, deltaTracker) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() != null) {
			if (!mc.gui.screen().isPauseScreen()) {
				lastPosCached = null;
				lastPaintbrushCached = null;
			}
			return;
		}

		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null || !(mc.hitResult instanceof BlockHitResult blockHitResult)) {
			return;
		}

		BlockPos pos = blockHitResult.getBlockPos();
		if (!(level.getBlockState(pos).getBlock() instanceof StorageBlockBase) && level.getBlockState(pos).getBlock() != ModBlocks.CONTROLLER.get() && !(level.getBlockEntity(pos) instanceof ISimpleMaterialHolder)) {
			return;
		}

		InventoryHelper.getItemFromEitherHand(player, ModItems.PAINTBRUSH.get()).flatMap(paintbrush -> getItemRequirementsFor(paintbrush, player, level, pos))
				.ifPresent(itemRequirements -> {
					if (itemRequirements.itemsMissing().isEmpty()) {
						return;
					}

					Component missingItems = StorageTranslationHelper.INSTANCE.translItemOverlayMessage(ModItems.PAINTBRUSH.get(), "missing_items");
					Font font = mc.font;
					int i = font.width(missingItems);
					int x = (guiGraphics.guiWidth() - i) / 2;
					int y = guiGraphics.guiHeight() - 75 - 10;
					guiGraphics.text(font, missingItems, x + 1, y, DyeColor.WHITE.getTextColor(), true);

					x = (guiGraphics.guiWidth() - itemRequirements.itemsMissing().size() * 18) / 2;
					for (ItemStack missingItem : itemRequirements.itemsMissing()) {
						guiGraphics.item(missingItem, x, y + 10);
						guiGraphics.itemDecorations(font, missingItem, x, y + 10);
						x += 18;
					}
				});
	};
}
