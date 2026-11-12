package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankInventoryControl;
import net.p3pp3rf1y.sophisticatedcore.util.FluidTooltipHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;

import java.util.ArrayList;
import java.util.List;

public class TankScreen extends StorageScreenBase<TankContainerMenu> {
	private static final int GAUGE_HEIGHT = 90;
	private static final TextureBlitData ARROW = new TextureBlitData(GuiHelper.GUI_CONTROLS, new UV(97, 216), new Dimension(15, 8));
	public TankScreen(TankContainerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected String getStorageSettingsTabTooltip() {
		return "";
	}

	@Override
	protected int getStorageInventoryHeight(int rows) {
		return GAUGE_HEIGHT;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTick, mouseX, mouseY);
		FluidStack fluid = menu.getFluid();
		int capacity = menu.getCapacity();
		int x = leftPos + SLOTS_X_OFFSET;
		int y = topPos + SLOTS_Y_OFFSET;
		TankInventoryControl.renderTank(graphics, x, y, GAUGE_HEIGHT, fluid, capacity);
		graphics.drawString(font, fluid.isEmpty() ? Component.translatable(StorageTranslationHelper.INSTANCE.translGui("tank.empty")) : fluid.getHoverName(),
				x + 24, y + 2, 0x404040, false);
		graphics.drawString(font,
				Component.translatable(StorageTranslationHelper.INSTANCE.translGui("tank." + (FluidTooltipHelper.isExperience(fluid) ? "xp_amount" : "amount")),
						FluidTooltipHelper.formatAmount(fluid, fluid.getAmount()), FluidTooltipHelper.formatAmount(fluid, capacity)),
				x + 24, y + 13, 0x404040, false);
		for (Slot slot : menu.getExtraSlots()) {
			GuiHelper.renderSlotsBackground(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 1, 1);
		}
		for (int column = 0; column < 2; column++) {
			Slot input = menu.getExtraSlots().get(column);
			GuiHelper.blit(graphics, leftPos + input.x, topPos + input.y + 20, ARROW);
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isOverGauge(mouseX, mouseY) && menu.getCarried().getCount() <= 1) {
			menu.interactWithFluidContainer();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private boolean isOverGauge(double mouseX, double mouseY) {
		return mouseX >= leftPos + SLOTS_X_OFFSET && mouseX < leftPos + SLOTS_X_OFFSET + 18 && mouseY >= topPos + SLOTS_Y_OFFSET
				&& mouseY < topPos + SLOTS_Y_OFFSET + GAUGE_HEIGHT;
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);
		if (isOverGauge(mouseX, mouseY)) {
			FluidStack fluid = menu.getFluid();
			List<Component> tooltip = new ArrayList<>();
			if (!fluid.isEmpty()) {
				tooltip.add(fluid.getHoverName());
			}
			tooltip.add(FluidTooltipHelper.getContentsTooltip(fluid, menu.getCapacity()));
			graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		}
	}
}
