package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.BlockSide;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.SideIOContainer;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.IOMode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class SideIOControl extends CompositeWidgetBase<WidgetBase> {

	public SideIOControl(SideIOContainer container, Position position) {
		super(position, new Dimension(54, 54));

		addSideIOButton(container, new Position(x + 18, y), BlockSide.TOP);
		addSideIOButton(container, new Position(x, y + 18), BlockSide.LEFT);
		addSideIOButton(container, new Position(x + 18, y + 18), BlockSide.FRONT);
		addSideIOButton(container, new Position(x + 36, y + 18), BlockSide.RIGHT);
		addSideIOButton(container, new Position(x + 18, y + 36), BlockSide.BOTTOM);
		addSideIOButton(container, new Position(x + 36, y + 36), BlockSide.BACK);
	}

	private void addSideIOButton(SideIOContainer container, Position position1, BlockSide side) {
		addChild(new SideIOToggleButton(position1, StorageButtonDefinitions.IO_MODE, button -> container.toggleSideIO(side), () -> container.getSideIOMode(side), side, container::toDirection));
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//noop
	}

	private static class SideIOToggleButton extends ToggleButton<IOMode> {

		@Nullable
		private List<Component> tooltip;
		private final BlockSide side;
		private final Function<BlockSide, Direction> getDirection;

		public SideIOToggleButton(Position position, ButtonDefinition.Toggle<IOMode> buttonDefinition, IntConsumer onClick, Supplier<IOMode> getState, BlockSide side, Function<BlockSide, Direction> getDirection) {
			super(position, buttonDefinition, button -> {}, getState);
			this.side = side;
			this.getDirection = getDirection;
			setOnClick(button -> {
				tooltip = null;
				onClick.accept(button);
			});
		}

		@Override
		protected List<Component> getTooltip(StateData data) {
			if (tooltip == null) {
				tooltip = new ArrayList<>();
				tooltip.addAll(data.getTooltip());
				tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translUpgradeButton("io_mode_side_info"),
								Component.translatable(StorageTranslationHelper.INSTANCE.translGui("block_side." + side.getSerializedName())),
								Component.translatable(StorageTranslationHelper.INSTANCE.translGui("direction." + getDirection.apply(side).getSerializedName())))
						.withStyle(ChatFormatting.DARK_GRAY));
			}
			return tooltip;
		}
	}
}
