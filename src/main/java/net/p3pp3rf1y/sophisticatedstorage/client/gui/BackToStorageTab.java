package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ImageButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryMessage;

public class BackToStorageTab extends Tab {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(64, 80), Dimension.SQUARE_16);
	private final BlockPos pos;

	protected BackToStorageTab(Position position, BlockPos pos) {
		super(position, Component.translatable(StorageTranslationHelper.INSTANCE.translGui("back_to_storage.tooltip")),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		this.pos = pos;
	}

	@Override
	protected void onTabIconClicked(int button) {
		SophisticatedStorage.PACKET_HANDLER.sendToServer(new OpenStorageInventoryMessage(pos));
	}
}
