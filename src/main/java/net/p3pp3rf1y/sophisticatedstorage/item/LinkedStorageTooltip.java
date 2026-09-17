package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;

import java.util.UUID;

public record LinkedStorageTooltip(LinkedStorageEndpointRole role, UUID groupId) implements TooltipComponent {
}
