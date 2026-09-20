package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.LinkedStorageEndpointRoleRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedstorage.item.LinkedStorageTooltip;

import java.util.UUID;

public class ClientLinkedStorageTooltip implements ClientTooltipComponent {
	private static final int ROLE_TEXT_COLOR = 0xFF_AE8BC7;
	private final LinkedStorageEndpointRole role;
	private final UUID groupId;

	public ClientLinkedStorageTooltip(LinkedStorageTooltip tooltip) {
		role = tooltip.role();
		groupId = tooltip.groupId();
	}

	@Override
	public int getHeight() {
		return 16;
	}

	@Override
	public int getWidth(Font font) {
		return 16 + font.width(getDescription());
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
		LinkedStorageEndpointRoleRenderer.renderIcon(guiGraphics, x - 2, y, role);
		guiGraphics.drawString(font, getDescription(), x + 16, y + 4, ROLE_TEXT_COLOR, false);
	}

	private Component getDescription() {
		Component roleDescription = LinkedStorageEndpointRoleRenderer.getDescription(role);
		return ClientLinkedStorageContents.getGroupName(groupId)
				.<Component>map(groupName -> TranslationHelper.INSTANCE.translTooltip(
						role == LinkedStorageEndpointRole.PRIMARY ? "linked_storage.primary_named" : "linked_storage.secondary_named", roleDescription,
						groupName))
				.orElse(roleDescription);
	}
}
